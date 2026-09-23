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
 */
final class DevSyncRunner {

    private DevSyncRunner() {}

    interface KeepAlive { void keepAlive(); }

    static void run(Context context, JobQueue.Job job, DevSyncPair pair, DevSyncClient client, KeepAlive keepAlive) {
        Uri tree = pair.localTree();
        boolean hash = true; // always compare mtime + size + checksum, no user-chosen mode anymore

        List<DevSyncLocal.Entry> localList;
        List<DevSyncClient.RemoteEntry> remoteList;
        try {
            localList = DevSyncLocal.walk(context, tree, hash);
            remoteList = client.syncList(pair.remotePath, hash);
        } catch (Exception e) {
            android.util.Log.w("DevSyncRunner", "listing failed for pair " + pair.id, e);
            pair.lastRunAt = System.currentTimeMillis();
            pair.lastFailed = 1;
            pair.lastSynced = 0;
            pair.lastConflicts = 0;
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

        List<String> toPush = new ArrayList<>();
        List<String> toPull = new ArrayList<>();
        List<String> toDeleteLocal = new ArrayList<>();
        List<String> toDeleteRemote = new ArrayList<>();
        int conflicts = 0;

        for (String path : allPaths) {
            DevSyncLocal.Entry L = local.get(path);
            DevSyncClient.RemoteEntry R = remote.get(path);
            DevSyncJobs.BaselineEntry B = baseline.get(path);

            if (L != null && R != null) {
                if (sameByMode(pair, L, R)) continue;
                if (pair.isMirror()) {
                    boolean localChanged = B == null || !localMatchesBaseline(pair, L, B);
                    boolean remoteChanged = B == null || !remoteMatchesBaseline(pair, R, B);
                    if (localChanged && !remoteChanged) {
                        toPush.add(path);
                    } else if (remoteChanged && !localChanged) {
                        toPull.add(path);
                    } else {
                        conflicts++;
                        if (L.mtimeMs >= R.mtimeMs) toPush.add(path); else toPull.add(path);
                    }
                } else if (DevSyncPair.DIR_PUSH.equals(pair.direction)) {
                    toPush.add(path);
                } else {
                    toPull.add(path);
                }
            } else if (L != null) {
                if (DevSyncPair.DIR_PULL.equals(pair.direction)) continue;
                if (pair.isMirror() && B != null) {
                    if (pair.deletesPropagate()) toDeleteLocal.add(path);
                } else {
                    toPush.add(path);
                }
            } else if (R != null) {
                if (DevSyncPair.DIR_PUSH.equals(pair.direction)) continue;
                if (pair.isMirror() && B != null) {
                    if (pair.deletesPropagate()) toDeleteRemote.add(path);
                } else {
                    toPull.add(path);
                }
            }
            // else: gone from both sides — just falls out of the baseline below.
        }

        int total = toPush.size() + toPull.size() + toDeleteLocal.size() + toDeleteRemote.size();
        Set<String> done = DevSyncJobs.readDone(context, job.id);
        DevSyncJobs.Snapshot snap = new DevSyncJobs.Snapshot();
        snap.jobId = job.id;
        snap.pairId = pair.id;
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
                pushOne(context, pair, client, path, L);
                baseline.put(path, new DevSyncJobs.BaselineEntry(L.size, L.mtimeMs, L.sha256));
                synced++;
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
                pullOne(context, pair, client, path, R);
                baseline.put(path, new DevSyncJobs.BaselineEntry(R.size, R.mtimeMs, R.sha256));
                synced++;
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
                client.deleteRemote(remotePathFor(pair, path));
                baseline.remove(path);
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
        pair.lastRunAt = System.currentTimeMillis();
        pair.lastSynced = synced;
        pair.lastFailed = failed;
        pair.lastConflicts = conflicts;
        pair.save(context);
        snap.doneFiles = total;
        DevSyncJobs.publishNow(snap);
    }

    /** A file is unchanged only when modified time, size, AND checksum all agree —
     *  no user-chosen mode anymore, just the strictest combination of all three. */
    private static boolean sameByMode(DevSyncPair pair, DevSyncLocal.Entry L, DevSyncClient.RemoteEntry R) {
        return Math.abs(L.mtimeMs - R.mtimeMs) <= 1000 && L.size == R.size
                && L.sha256 != null && L.sha256.equals(R.sha256);
    }

    private static boolean localMatchesBaseline(DevSyncPair pair, DevSyncLocal.Entry L, DevSyncJobs.BaselineEntry B) {
        return Math.abs(L.mtimeMs - B.mtimeMs) <= 1000 && L.size == B.size
                && L.sha256 != null && L.sha256.equals(B.hash);
    }

    private static boolean remoteMatchesBaseline(DevSyncPair pair, DevSyncClient.RemoteEntry R, DevSyncJobs.BaselineEntry B) {
        return Math.abs(R.mtimeMs - B.mtimeMs) <= 1000 && R.size == B.size
                && R.sha256 != null && R.sha256.equals(B.hash);
    }

    private static String remotePathFor(DevSyncPair pair, String relPath) {
        String root = pair.remotePath;
        if (root.endsWith("/") || root.endsWith("\\")) root = root.substring(0, root.length() - 1);
        return root + "/" + relPath;
    }

    private static void pushOne(Context context, DevSyncPair pair, DevSyncClient client, String relPath, DevSyncLocal.Entry local) throws IOException {
        Uri tree = pair.localTree();
        String remotePath = remotePathFor(pair, relPath);
        long resumeOffset = client.putBegin(remotePath, local.size, local.mtimeMs);
        try (InputStream in = DevSyncLocal.open(context, tree, relPath)) {
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
        if (!client.putEnd()) throw new IOException("daemon rejected upload: " + relPath);
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

        try (RandomAccessFile raf = new RandomAccessFile(cache, "rw")) {
            client.getFile(remotePathFor(pair, relPath), resumeOffset, (offset, data) -> {
                raf.seek(offset);
                raf.write(data);
            });
        }

        try (InputStream in = new java.io.FileInputStream(cache);
             OutputStream out = DevSyncLocal.create(context, pair.localTree(), relPath)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        cache.delete();
    }

    private static String cacheName(String relPath) {
        return Integer.toHexString(relPath.hashCode()) + "-" + relPath.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
