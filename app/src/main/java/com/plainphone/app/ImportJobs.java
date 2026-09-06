package com.plainphone.app;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Home-section import facade over the global {@link JobQueue}. Kept as the UI API.
 */
final class ImportJobs {

    private ImportJobs() {}

    static final String TYPE_NOTES = "notes.import";
    static final String TYPE_TODOS = "todos.import";
    static final String TYPE_RECORDER = "recorder.import";

    // --- listeners ------------------------------------------------

    interface Listener { void onImportJobChanged(); }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    static void addListener(Listener l) { listeners.addIfAbsent(l); }
    static void removeListener(Listener l) { listeners.remove(l); }

    // --- live snapshot (UI + notification) ------------------------

    static final class Snapshot {
        HomeMode plugin;
        String label;
        int total;
        volatile int done;
        volatile int added;
    }

    static volatile Snapshot snapshot;

    static final class Result {
        final HomeMode plugin;
        final int added;
        Result(HomeMode plugin, int added) { this.plugin = plugin; this.added = added; }
    }

    private static volatile Result lastResult;
    private static long lastFanout;

    static void publish() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastFanout < 350) return;
        lastFanout = now;
        for (Listener l : listeners) l.onImportJobChanged();
        JobQueue.publish();
    }

    static void publishNow() {
        lastFanout = android.os.SystemClock.uptimeMillis();
        for (Listener l : listeners) l.onImportJobChanged();
        JobQueue.publish();
    }

    static void clearSnapshot() {
        snapshot = null;
        lastFanout = 0;
        for (Listener l : listeners) l.onImportJobChanged();
        JobQueue.publish();
    }

    static void setResult(HomeMode plugin, int added) {
        lastResult = new Result(plugin, added);
    }

    static Result takeResult() {
        Result r = lastResult;
        lastResult = null;
        return r;
    }

    // --- state queries -------------------------------------------

    static boolean anyPending(Context c) {
        migrateLegacy(c);
        return !pendingJobs(c).isEmpty();
    }

    static boolean pendingForPlugin(Context c, HomeMode plugin) {
        migrateLegacy(c);
        for (Job j : pendingJobs(c)) if (j.plugin == plugin) return true;
        return false;
    }

    static String detailForPlugin(Context c, HomeMode plugin) {
        migrateLegacy(c);
        for (Job j : pendingJobs(c)) {
            if (j.plugin == plugin) return "importing " + j.label;
        }
        return null;
    }

    static String progressLine(Context c, HomeMode plugin) {
        Snapshot s = snapshot;
        if (s != null && s.plugin == plugin && s.total > 1) {
            return "Importing " + Math.min(s.done + 1, s.total) + " of " + s.total + "...";
        }
        return "Importing...";
    }

    // --- job record --------------------------------------------

    static final class Job {
        String id;
        HomeMode plugin;
        String label;
        List<Uri> uris = new ArrayList<>();
    }

    static void start(Context context, HomeMode plugin, List<Uri> uris, String label) {
        if (uris == null || uris.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (Uri u : uris) sb.append(u).append('\n');
        JobQueue.enqueue(context, new JobQueue.Spec(typeFor(plugin))
                .label(label)
                .keep(areaFor(plugin))
                .put("plugin", plugin.name())
                .put("label", label)
                .file("uris", sb.toString()));
    }

    static void resumeIfPending(Context context) {
        migrateLegacy(context);
        JobQueue.resumeIfPending(context);
    }

    static boolean isImportType(String type) {
        return TYPE_NOTES.equals(type) || TYPE_TODOS.equals(type) || TYPE_RECORDER.equals(type);
    }

    static List<Job> pendingJobs(Context context) {
        migrateLegacy(context);
        List<Job> out = new ArrayList<>();
        for (JobQueue.Job job : JobQueue.pending(context)) {
            if (!isImportType(job.type)) continue;
            Job j = jobFrom(context, job);
            if (j != null) out.add(j);
        }
        return out;
    }

    static Job jobFrom(Context context, JobQueue.Job job) {
        if (job == null || !isImportType(job.type)) return null;
        Job out = new Job();
        out.id = job.id;
        out.plugin = pluginFor(job.type, job.data.get("plugin"));
        out.label = job.data.get("label");
        if (out.label == null) out.label = job.label;
        for (String line : JobQueue.readText(context, job.id, "uris").split("\n")) {
            if (!line.isEmpty()) out.uris.add(Uri.parse(line));
        }
        if (out.plugin == null || out.uris.isEmpty()) return null;
        if (out.label == null) out.label = out.uris.size() + " files";
        return out;
    }

    static Set<String> readDone(Context context, String jobId) {
        Set<String> set = new LinkedHashSet<>();
        for (String line : JobQueue.readText(context, jobId, "done").split("\n")) {
            if (!line.isEmpty()) set.add(line);
        }
        return set;
    }

    static void writeDone(Context context, String jobId, Set<String> done, int added) {
        StringBuilder sb = new StringBuilder();
        for (String u : done) sb.append(u).append('\n');
        JobQueue.writeText(context, jobId, "done", sb.toString());
        JobQueue.writeText(context, jobId, "added", Integer.toString(added));
    }

    static int readAdded(Context context, String jobId) {
        String raw = JobQueue.readText(context, jobId, "added").trim();
        if (raw.isEmpty()) return 0;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static void clearJob(Context context, String jobId) {
        JobQueue.clear(context, jobId);
    }

    static int importOne(Context c, HomeMode plugin, Uri uri) {
        switch (plugin) {
            case NOTES:    return Notes.importOne(c, uri) ? 1 : 0;
            case RECORDER: return Recorder.importOne(c, uri) ? 1 : 0;
            case TODOS:    return Math.max(0, Todos.importFromFile(c, uri));
            default:       return 0;
        }
    }

    static void migrateLegacy(Context context) {
        File old = new File(context.getFilesDir(), "import-jobs");
        if (!old.exists()) return;
        File[] kids = old.listFiles();
        if (kids != null) {
            java.util.Arrays.sort(kids, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            for (File dir : kids) {
                if (!dir.isDirectory()) continue;
                LegacyJob legacy = readLegacy(dir);
                if (legacy == null) continue;
                JobQueue.Job job = JobQueue.enqueueMigrated(context, new JobQueue.Spec(typeFor(legacy.plugin))
                        .label(legacy.label)
                        .keep(areaFor(legacy.plugin))
                        .put("plugin", legacy.plugin.name())
                        .put("label", legacy.label)
                        .file("uris", legacy.urisText));
                String done = JobQueue.readAll(new File(dir, "done"));
                String added = JobQueue.readAll(new File(dir, "added"));
                if (!done.isEmpty()) JobQueue.writeText(context, job.id, "done", done);
                if (!added.isEmpty()) JobQueue.writeText(context, job.id, "added", added);
            }
        }
        deleteRecursively(old);
    }

    private static final class LegacyJob {
        HomeMode plugin;
        String label;
        String urisText;
    }

    private static LegacyJob readLegacy(File dir) {
        File jobFile = new File(dir, "job");
        File urisFile = new File(dir, "uris");
        if (!jobFile.exists() || !urisFile.exists()) return null;
        LegacyJob out = new LegacyJob();
        for (String line : JobQueue.readAll(jobFile).split("\n")) {
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String k = line.substring(0, eq);
            String v = line.substring(eq + 1);
            if ("plugin".equals(k)) {
                try { out.plugin = HomeMode.valueOf(v); } catch (Exception ignored) {}
            } else if ("label".equals(k)) {
                out.label = v;
            }
        }
        out.urisText = JobQueue.readAll(urisFile);
        if (out.plugin == null || out.urisText.trim().isEmpty()) return null;
        if (out.label == null) out.label = out.urisText.split("\n").length + " files";
        return out;
    }

    private static String typeFor(HomeMode plugin) {
        switch (plugin) {
            case NOTES: return TYPE_NOTES;
            case TODOS: return TYPE_TODOS;
            case RECORDER: return TYPE_RECORDER;
            default: throw new IllegalArgumentException("Unsupported import plugin: " + plugin);
        }
    }

    private static HomeMode pluginFor(String type, String stored) {
        if (stored != null) {
            try { return HomeMode.valueOf(stored); } catch (Exception ignored) {}
        }
        if (TYPE_NOTES.equals(type)) return HomeMode.NOTES;
        if (TYPE_TODOS.equals(type)) return HomeMode.TODOS;
        if (TYPE_RECORDER.equals(type)) return HomeMode.RECORDER;
        return null;
    }

    private static String areaFor(HomeMode plugin) {
        switch (plugin) {
            case NOTES: return JobQueue.AREA_NOTES;
            case TODOS: return JobQueue.AREA_TODOS;
            case RECORDER: return JobQueue.AREA_RECORDER;
            default: return "";
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] kids = file.listFiles();
            if (kids != null) for (File kid : kids) deleteRecursively(kid);
        }
        file.delete();
    }
}
