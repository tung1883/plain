package com.plainphone.app;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dev-sync job facade over the global {@link JobQueue} — the {@code VaultJobs}
 * pattern, applied to file-sync runs. A run is one {@link JobQueue.Job} of
 * type {@link #TYPE_SYNC} carrying the pair id; per-file progress is a
 * "done" checkpoint (see {@link #readDone}/{@link #writeDone}, same shape as
 * {@code VaultJobs.readDoneIds}) so a killed run resumes at the first file
 * not yet in that set, and the daemon's own disk-based partial-file resume
 * (see {@code DevSyncClient.putBegin}/{@code getFile}) picks a part-finished
 * file back up mid-transfer on top of that.
 *
 * <p>The per-pair sync baseline (last known synced state of every file, used
 * to tell a one-sided change from a real mirror conflict) is separate from
 * any one run — it lives in its own file, keyed by pair id, and survives
 * across runs.
 */
final class DevSyncJobs {

    private DevSyncJobs() {}

    static final String TYPE_SYNC = "dev.sync";

    interface Listener { void onDevSyncChanged(); }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    static void addListener(Listener l) { listeners.addIfAbsent(l); }
    static void removeListener(Listener l) { listeners.remove(l); }

    static final class Snapshot {
        String jobId;
        String pairId;
        String currentPath;
        String currentDirection; // "up" / "down" / "same" — for the recent-activity list
        int doneFiles, totalFiles;
        int synced, unchanged, conflicts, failed;
    }

    static volatile Snapshot snapshot;
    private static long lastFanout;

    static void publish(Snapshot s) {
        snapshot = s;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastFanout < 350) return;
        lastFanout = now;
        for (Listener l : listeners) l.onDevSyncChanged();
        JobQueue.publish();
    }

    static void publishNow(Snapshot s) {
        snapshot = s;
        lastFanout = android.os.SystemClock.uptimeMillis();
        for (Listener l : listeners) l.onDevSyncChanged();
        JobQueue.publish();
    }

    static void clearSnapshot() {
        snapshot = null;
        for (Listener l : listeners) l.onDevSyncChanged();
        JobQueue.publish();
    }

    // --- start / query -----------------------------------------------------

    static void enqueue(Context context, DevSyncPair pair) {
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_SYNC)
                .label(pair.label != null && !pair.label.isEmpty() ? pair.label : "Sync")
                .put("pair", pair.id)
                .put("host", pair.hostId));
    }

    static boolean pending(Context context, String pairId) {
        for (JobQueue.Job job : JobQueue.pendingByType(context, TYPE_SYNC)) {
            if (pairId.equals(job.data.get("pair"))) return true;
        }
        return false;
    }

    static void resumeIfPending(Context context) {
        JobQueue.resumeIfPending(context);
    }

    static void finish(Context context, JobQueue.Job job) {
        JobQueue.clear(context, job.id);
    }

    // --- per-run checkpoint: relative paths already transferred this run ---

    static Set<String> readDone(Context context, String jobId) {
        Set<String> out = new LinkedHashSet<>();
        for (String line : JobQueue.readText(context, jobId, "done").split("\n")) {
            if (!line.isEmpty()) out.add(line);
        }
        return out;
    }

    static void writeDone(Context context, String jobId, Set<String> done) {
        StringBuilder sb = new StringBuilder();
        for (String path : done) sb.append(path).append('\n');
        JobQueue.writeText(context, jobId, "done", sb.toString());
    }

    // --- per-pair sync baseline: last known synced state of every file -----

    static final class BaselineEntry {
        long size;
        long mtimeMs;
        String hash; // null unless the pair's detect mode is checksum

        BaselineEntry(long size, long mtimeMs, String hash) {
            this.size = size;
            this.mtimeMs = mtimeMs;
            this.hash = hash;
        }
    }

    static Map<String, BaselineEntry> readBaseline(Context context, String pairId) {
        Map<String, BaselineEntry> out = new LinkedHashMap<>();
        File f = baselineFile(context, pairId);
        if (!f.exists()) return out;
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int n = 0;
            while (n < buf.length) {
                int r = in.read(buf, n, buf.length - n);
                if (r < 0) break;
                n += r;
            }
            JSONObject root = new JSONObject(new String(buf, 0, n, StandardCharsets.UTF_8));
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String path = keys.next();
                JSONObject e = root.getJSONObject(path);
                out.put(path, new BaselineEntry(e.optLong("size", 0), e.optLong("mtime_ms", 0),
                        e.isNull("hash") ? null : e.optString("hash", null)));
            }
        } catch (IOException | JSONException ignored) {
        }
        return out;
    }

    static void writeBaseline(Context context, String pairId, Map<String, BaselineEntry> baseline) {
        JSONObject root = new JSONObject();
        try {
            for (Map.Entry<String, BaselineEntry> e : baseline.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("size", e.getValue().size);
                o.put("mtime_ms", e.getValue().mtimeMs);
                if (e.getValue().hash != null) o.put("hash", e.getValue().hash);
                root.put(e.getKey(), o);
            }
        } catch (JSONException ignored) {
        }
        File f = baselineFile(context, pairId);
        File dir = f.getParentFile();
        if (dir != null) dir.mkdirs();
        File tmp = new File(dir, f.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(root.toString().getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch (IOException e) {
            android.util.Log.w("DevSyncJobs", "baseline write failed", e);
            return;
        }
        if (!tmp.renameTo(f)) tmp.delete();
    }

    static void clearBaseline(Context context, String pairId) {
        baselineFile(context, pairId).delete();
    }

    private static File baselineFile(Context context, String pairId) {
        return new File(new File(context.getFilesDir(), "dev_sync"), pairId + ".json");
    }
}
