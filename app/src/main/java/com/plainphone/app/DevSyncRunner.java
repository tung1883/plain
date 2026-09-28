package com.plainphone.app;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The actual diff-and-transfer engine for one {@link DevSyncPair} run.
 * Listing is one pass per side (not itself resumable — a directory walk is
 * cheap relative to transferring files); the transfer phase is checkpointed
 * per file (see {@link DevSyncJobs#readDone}), and each individual upload or
 * download further resumes mid-file via the daemon's disk-based partial-file
 * offset ({@link DevSyncClient#putBegin}) or the phone's own local cache
 * partial ({@link #pullOne}).
 *
 * <p>Direction rules:
 * <ul>
 *   <li>{@code push} — local is the source of truth; never deletes or
 *       creates anything remote-only.
 *   <li>{@code pull} — remote is the source of truth; never touches a
 *       local-only file.
 *   <li>{@code mirror} — three-way diff against the last-synced
 *       {@link DevSyncJobs#readBaseline baseline}: a path changed on exactly
 *       one side goes that direction; changed on both is a conflict,
 *       resolved by newer-mtime-wins (and counted, never silent); a path
 *       present in the baseline but now missing on one side is a deletion —
 *       propagated only when {@link DevSyncPair#deletesPropagate()}.
 * </ul>
 *
 * <p>Deciding "same" is cheap first, like rsync's quick check: a path where
 * each side still matches its own baseline record (size + modified time), or
 * where both sides have the same size and modified time, is unchanged without
 * reading it. Only paths whose sizes match but times don't are hashed — on
 * both sides, the daemon's half through {@code sync.hash} — so a big folder
 * that hasn't changed costs a directory walk, not reading every byte.
 */
final class DevSyncRunner {

    private DevSyncRunner() {}

    interface KeepAlive { void keepAlive(); }

    static void run(Context context, JobQueue.Job job, DevSyncPair pair, DevSyncClient client,
                    boolean remoteCanHash, KeepAlive keepAlive) {
        Uri tree = pair.localTree();

        List<DevSyncLocal.Entry> localList;
        List<DevSyncClient.RemoteEntry> remoteList;
        long runStart = now();
        try {
            long t0 = now();
            localList = DevSyncLocal.walk(context, tree);
            LatencyStats.record("sync.list_local", now() - t0);
            t0 = now();
            remoteList = client.syncList(pair.remotePath);
            LatencyStats.record("sync.list_remote", now() - t0);
        } catch (Exception e) {
            android.util.Log.w("DevSyncRunner", "listing failed for pair " + pair.id, e);
            pair.lastRunAt = System.currentTimeMillis();
            pair.lastFailed = 1;
            pair.lastSynced = 0;
            pair.lastConflicts = 0;
            pair.lastError = "couldn't list files: " + e.getMessage();
            pair.save(context);
            return;
        }

        Map<String, DevSyncLocal.Entry> local = new LinkedHashMap<>();
        for (DevSyncLocal.Entry e : localList) local.put(e.path, e);
        Map<String, DevSyncClient.RemoteEntry> remote = new LinkedHashMap<>();
        for (DevSyncClient.RemoteEntry e : remoteList) remote.put(e.path, e);
        Map<String, DevSyncJobs.BaselineEntry> baseline = DevSyncJobs.readBaseline(context, pair.id);

        Set<String> allPaths = new LinkedHashSet<>();
        allPaths.addAll(local.keySet());
        allPaths.addAll(remote.keySet());
        allPaths.addAll(baseline.keySet());

        Plan plan = new Plan();
        List<String> needHash = new ArrayList<>();

        for (String path : allPaths) {
            DevSyncLocal.Entry L = local.get(path);
            DevSyncClient.RemoteEntry R = remote.get(path);
            DevSyncJobs.BaselineEntry B = baseline.get(path);

            if (L != null && R != null) {
                boolean lSame = B != null && B.localSame(L.size, L.mtimeMs);
                boolean rSame = B != null && B.remoteSame(R.size, R.mtimeMs);
                if (lSame && rSame) continue; // neither side touched since the last sync
                if (L.size == R.size && DevSyncJobs.sameTime(L.mtimeMs, R.mtimeMs)) {
                    baseline.put(path, both(L, R, B != null ? B.hash : null)); // quick check: equal
                    continue;
                }
                if (L.size == R.size && remoteCanHash) {
                    needHash.add(path); // might be equal with a different timestamp: look inside
                    continue;
                }
                plan.differ(pair, path, L, R, lSame, rSame);
            } else if (L != null) {
                if (DevSyncPair.DIR_PULL.equals(pair.direction)) continue;
                if (pair.isMirror() && B != null) {
                    if (pair.deletesPropagate()) plan.deleteLocal.add(path);
                } else {
                    plan.push.add(path);
                }
            } else if (R != null) {
                if (DevSyncPair.DIR_PUSH.equals(pair.direction)) continue;
                if (pair.isMirror() && B != null) {
                    if (pair.deletesPropagate()) plan.deleteRemote.add(path);
                } else {
                    plan.pull.add(path);
                }
            } else {
                baseline.remove(path); // gone from both sides
            }
        }

        DevSyncJobs.Snapshot snap = new DevSyncJobs.Snapshot();
        snap.jobId = job.id;
        snap.pairId = pair.id;

        if (!needHash.isEmpty()) {
            long hashStart = now();
            Map<String, String> remoteHash = new java.util.HashMap<>();
            try {
                client.hashRemote(pair.remotePath, needHash, (p, sha) -> {
                    keepAlive.keepAlive();
                    snap.currentPath = "checking " + p;
                    snap.currentDirection = null;
                    DevSyncJobs.publish(snap);
                    remoteHash.put(p, sha);
                });
            } catch (IOException e) {
                android.util.Log.w("DevSyncRunner", "remote hashing failed", e); // unhashed paths just transfer
            }
            for (String path : needHash) {
                keepAlive.keepAlive();
                DevSyncLocal.Entry L = local.get(path);
                DevSyncClient.RemoteEntry R = remote.get(path);
                DevSyncJobs.BaselineEntry B = baseline.get(path);
                String hr = remoteHash.get(path);
                long th = now();
                String hl = hr == null ? null : DevSyncLocal.hash(context, tree, path);
                if (hr != null) LatencyStats.record("sync.hash_local", now() - th);
                if (hl != null && hl.equals(hr)) {
                    baseline.put(path, both(L, R, hl)); // same content, different timestamps
                } else {
                    plan.differ(pair, path, L, R,
                            B != null && B.localSame(L.size, L.mtimeMs),
                            B != null && B.remoteSame(R.size, R.mtimeMs));
                }
            }
            android.util.Log.i(LatencyStats.TAG, "sync " + pair.id + ": hashed " + needHash.size()
                    + " file(s) in " + (now() - hashStart) + " ms");
        }
        long planDone = now();
        long bytesMoved = 0;

        List<String> toPush = plan.push, toPull = plan.pull;
        List<String> toDeleteLocal = plan.deleteLocal, toDeleteRemote = plan.deleteRemote;
        int conflicts = plan.conflicts;

        int total = toPush.size() + toPull.size() + toDeleteLocal.size() + toDeleteRemote.size();
        Set<String> done = DevSyncJobs.readDone(context, job.id);
        snap.totalFiles = total;
        snap.conflicts = conflicts;
        DevSyncJobs.publishNow(snap);

        int synced = 0, failed = 0;

        for (String path : toPush) {
            String key = "push:" + path;
            if (done.contains(key)) { synced++; continue; }
            keepAlive.keepAlive();
            snap.currentPath = path;
            snap.currentDirection = "up";
            DevSyncJobs.publish(snap);
            try {
                DevSyncLocal.Entry L = local.get(path);
                retryOnNewLink(client, () -> pushOne(context, pair, client, path, L));
                bytesMoved += L.size;
                // the daemon stamps the upload with the phone's modified time
                baseline.put(path, new DevSyncJobs.BaselineEntry(L.size, L.mtimeMs, L.size, L.mtimeMs, null));
                synced++;
            } catch (LinkGone e) {
                abort(context, pair, baseline, synced, failed, conflicts);
                return;
            } catch (Exception e) {
                android.util.Log.w("DevSyncRunner", "push failed: " + path, e);
                failed++;
            }
            done.add(key);
            DevSyncJobs.writeDone(context, job.id, done);
            snap.doneFiles = done.size();
            snap.synced = synced;
            snap.failed = failed;
            DevSyncJobs.publish(snap);
        }

        for (String path : toPull) {
            String key = "pull:" + path;
            if (done.contains(key)) { synced++; continue; }
            keepAlive.keepAlive();
            snap.currentPath = path;
            snap.currentDirection = "down";
            DevSyncJobs.publish(snap);
            try {
                DevSyncClient.RemoteEntry R = remote.get(path);
                retryOnNewLink(client, () -> pullOne(context, pair, client, path, R));
                bytesMoved += R.size;
                // the local copy's modified time is "now": record what it actually is
                long ts = now();
                DevSyncLocal.Entry after = DevSyncLocal.stat(context, tree, path);
                LatencyStats.record("sync.pull_stat", now() - ts);
                baseline.put(path, new DevSyncJobs.BaselineEntry(
                        after != null ? after.size : -1, after != null ? after.mtimeMs : 0,
                        R.size, R.mtimeMs, null));
                synced++;
            } catch (LinkGone e) {
                abort(context, pair, baseline, synced, failed, conflicts);
                return;
            } catch (Exception e) {
                android.util.Log.w("DevSyncRunner", "pull failed: " + path, e);
                failed++;
            }
            done.add(key);
            DevSyncJobs.writeDone(context, job.id, done);
            snap.doneFiles = done.size();
            snap.synced = synced;
            snap.failed = failed;
            DevSyncJobs.publish(snap);
        }

        for (String path : toDeleteLocal) {
            String key = "delloc:" + path;
            if (done.contains(key)) continue;
            keepAlive.keepAlive();
            try {
                DevSyncLocal.delete(context, tree, path);
                baseline.remove(path);
            } catch (Exception e) {
                android.util.Log.w("DevSyncRunner", "local delete failed: " + path, e);
                failed++;
            }
            done.add(key);
            DevSyncJobs.writeDone(context, job.id, done);
            snap.doneFiles = done.size();
            DevSyncJobs.publish(snap);
        }

        for (String path : toDeleteRemote) {
            String key = "delrem:" + path;
            if (done.contains(key)) continue;
            keepAlive.keepAlive();
            try {
                retryOnNewLink(client, () -> client.deleteRemote(remotePathFor(pair, path)));
                baseline.remove(path);
            } catch (LinkGone e) {
                abort(context, pair, baseline, synced, failed, conflicts);
                return;
            } catch (Exception e) {
                android.util.Log.w("DevSyncRunner", "remote delete failed: " + path, e);
                failed++;
            }
            done.add(key);
            DevSyncJobs.writeDone(context, job.id, done);
            snap.doneFiles = done.size();
            DevSyncJobs.publish(snap);
        }

        DevSyncJobs.writeBaseline(context, pair.id, baseline);
        long transferMs = now() - planDone;
        android.util.Log.i(LatencyStats.TAG, String.format(java.util.Locale.US,
                "sync %s: %d file(s), %.1f MB in %.1f s (%.2f MB/s) — listing+checks %.1f s, total %.1f s,"
                        + " %d up / %d down / %d unchanged-by-check, %d failed",
                pair.id, toPush.size() + toPull.size(), bytesMoved / 1e6, transferMs / 1000.0,
                transferMs > 0 ? bytesMoved / 1e6 / (transferMs / 1000.0) : 0.0,
                (planDone - runStart) / 1000.0, (now() - runStart) / 1000.0,
                toPush.size(), toPull.size(), allPaths.size() - total, failed));
        pair.lastRunAt = System.currentTimeMillis();
        pair.lastSynced = synced;
        pair.lastFailed = failed;
        pair.lastConflicts = conflicts;
        pair.lastError = null;
        pair.save(context);
        snap.doneFiles = total;
        DevSyncJobs.publishNow(snap);
    }

    interface Transfer { void run() throws IOException; }

    /** The link dropped and didn't come back: stop the run (nothing left can work). */
    private static final class LinkGone extends IOException {
        LinkGone() { super("lost the connection to the PC"); }
    }

    /** Runs one transfer; if the link drops under it, moves to the new connection
     *  and runs it once more — it resumes from its partial data (the daemon's
     *  {@code .partial} for uploads, the local cache file for downloads). */
    private static void retryOnNewLink(DevSyncClient client, Transfer t) throws IOException {
        try {
            t.run();
        } catch (DevSyncClient.LinkLost first) {
            android.util.Log.w("DevSyncRunner", "link dropped mid-transfer, reconnecting");
            if (!client.reconnect()) throw new LinkGone();
            try {
                t.run();
            } catch (DevSyncClient.LinkLost again) {
                if (!client.reconnect()) throw new LinkGone();
                throw again; // this file fails; the link is back for the rest
            }
        }
    }

    /** Ends a run cut short by a lost link: keep what did finish (so the next
     *  run skips it) and say why on the pair's row. */
    private static void abort(Context context, DevSyncPair pair, Map<String, DevSyncJobs.BaselineEntry> baseline,
                              int synced, int failed, int conflicts) {
        android.util.Log.w("DevSyncRunner", "sync " + pair.id + ": connection lost, stopping this run");
        DevSyncJobs.writeBaseline(context, pair.id, baseline);
        pair.lastRunAt = System.currentTimeMillis();
        pair.lastSynced = synced;
        pair.lastFailed = failed + 1;
        pair.lastConflicts = conflicts;
        pair.lastError = "lost the connection to the PC after " + synced + " file(s); the next run continues";
        pair.save(context);
    }

    private static DevSyncJobs.BaselineEntry both(DevSyncLocal.Entry L, DevSyncClient.RemoteEntry R, String hash) {
        return new DevSyncJobs.BaselineEntry(L.size, L.mtimeMs, R.size, R.mtimeMs, hash);
    }

    /** What a run will do, built up path by path. */
    private static final class Plan {
        final List<String> push = new ArrayList<>(), pull = new ArrayList<>();
        final List<String> deleteLocal = new ArrayList<>(), deleteRemote = new ArrayList<>();
        int conflicts;

        /** A path on both sides whose content differs (or couldn't be compared). */
        void differ(DevSyncPair pair, String path, DevSyncLocal.Entry L, DevSyncClient.RemoteEntry R,
                    boolean localSameAsBaseline, boolean remoteSameAsBaseline) {
            if (pair.isMirror()) {
                boolean localChanged = !localSameAsBaseline, remoteChanged = !remoteSameAsBaseline;
                if (localChanged && !remoteChanged) {
                    push.add(path);
                } else if (remoteChanged && !localChanged) {
                    pull.add(path);
                } else {
                    conflicts++;
                    if (L.mtimeMs >= R.mtimeMs) push.add(path); else pull.add(path);
                }
            } else if (DevSyncPair.DIR_PUSH.equals(pair.direction)) {
                push.add(path);
            } else {
                pull.add(path);
            }
        }
    }

    private static String remotePathFor(DevSyncPair pair, String relPath) {
        String root = pair.remotePath;
        if (root.endsWith("/") || root.endsWith("\\")) root = root.substring(0, root.length() - 1);
        return root + "/" + relPath;
    }

    private static void pushOne(Context context, DevSyncPair pair, DevSyncClient client, String relPath, DevSyncLocal.Entry local) throws IOException {
        Uri tree = pair.localTree();
        String remotePath = remotePathFor(pair, relPath);
        long t0 = now();
        long resumeOffset = client.putBegin(remotePath, local.size, local.mtimeMs);
        LatencyStats.record("sync.put_begin_wait", now() - t0); // idle round trip per file
        long t1 = now();
        InputStream opened = DevSyncLocal.open(context, tree, relPath);
        LatencyStats.record("sync.local_find", now() - t1);
        long t2 = now();
        try (InputStream in = opened) {
            if (in == null) throw new IOException("local file vanished: " + relPath);
            skipFully(in, resumeOffset);
            long offset = resumeOffset;
            byte[] buf = new byte[DevProtocol.SYNC_CHUNK_SIZE];
            int n;
            while ((n = in.read(buf)) > 0) {
                byte[] chunk = n == buf.length ? buf : java.util.Arrays.copyOf(buf, n);
                client.putChunk(offset, chunk);
                offset += n;
            }
        }
        long sent = now() - t2;
        long t3 = now();
        boolean ok = client.putEnd();
        LatencyStats.record("sync.put_end_wait", now() - t3); // includes the link draining queued chunks
        recordRate("sync.put_mbps", local.size - resumeOffset, sent + (now() - t3));
        if (!ok) throw new IOException("daemon rejected upload: " + relPath);
    }

    private static void skipFully(InputStream in, long n) throws IOException {
        long remaining = n;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) {
                if (in.read() < 0) break; // EOF — nothing more to skip
                remaining--;
            } else {
                remaining -= skipped;
            }
        }
    }

    /** Downloads into an app-private cache file first (byte-range resume needs a plain
     *  {@code File}, not a DocumentFile — SAF append isn't reliably supported across
     *  providers), then copies the finished file into the destination SAF tree, so a
     *  half-downloaded file is never visible in the user's chosen folder.
     *
     *  <p>Known limitation: the copied-in file's local mtime is "now", not the remote's
     *  original {@code mtime_ms} (SAF gives no reliable way to set it) — a mirror pair in
     *  {@code mtime} detect mode may see that path as locally-changed on its very next run
     *  and push it straight back up once, harmlessly (same content, baseline then matches
     *  and it settles). {@code checksum}/{@code size} detect modes aren't affected. */
    private static void pullOne(Context context, DevSyncPair pair, DevSyncClient client, String relPath, DevSyncClient.RemoteEntry remote) throws IOException {
        File cacheDir = new File(new File(context.getCacheDir(), "dev_sync"), pair.id);
        cacheDir.mkdirs();
        File cache = new File(cacheDir, cacheName(relPath));
        long resumeOffset = cache.exists() ? cache.length() : 0;
        if (resumeOffset > remote.size) { cache.delete(); resumeOffset = 0; }

        long t0 = now();
        try (RandomAccessFile raf = new RandomAccessFile(cache, "rw")) {
            client.getFile(remotePathFor(pair, relPath), resumeOffset, (offset, data) -> {
                raf.seek(offset);
                raf.write(data);
            });
        }
        recordRate("sync.get_mbps", remote.size - resumeOffset, now() - t0);

        long t1 = now();
        OutputStream created = DevSyncLocal.create(context, pair.localTree(), relPath);
        LatencyStats.record("sync.local_create", now() - t1); // find + delete old + mkdirs + create
        long t2 = now();
        try (InputStream in = new java.io.FileInputStream(cache);
             OutputStream out = created) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        recordRate("sync.copy_to_folder_mbps", remote.size, now() - t2); // the second write
        cache.delete();
    }

    private static long now() {
        return android.os.SystemClock.uptimeMillis();
    }

    /** MB/s for one file; skipped for tiny files where it's all noise. */
    private static void recordRate(String name, long bytes, long ms) {
        if (bytes >= 256 * 1024 && ms > 0) LatencyStats.record(name, bytes / 1e6 / (ms / 1000.0));
    }

    private static String cacheName(String relPath) {
        return Integer.toHexString(relPath.hashCode()) + "-" + relPath.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
