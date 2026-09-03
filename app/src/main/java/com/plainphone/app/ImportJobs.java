package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Persistent, resumable background file-imports for the home plugins (Notes,
 * To-do, Rec) — the small sibling of {@link VaultJobs}. Each queued import is a
 * directory under {@code files/import-jobs/}; it exists for exactly as long as
 * the import is unfinished, so a process kill mid-import is picked up and
 * finished on next launch. One import runs at a time, executed by
 * {@link ImportJobService}; the rest queue, oldest first.
 *
 * <p>While a plugin has an import pending it is kept unlocked and the home list
 * shows an inert "Importing…" row (see {@link Lock#importing} and
 * {@code MainActivity}).
 */
final class ImportJobs {

    private ImportJobs() {}

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

    /** The just-finished import, for a one-time toast. Consumed by {@link #takeResult}. */
    private static volatile Result lastResult;

    private static long lastFanout;

    static void publish() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastFanout < 350) return;
        lastFanout = now;
        for (Listener l : listeners) l.onImportJobChanged();
    }

    static void publishNow() {
        lastFanout = android.os.SystemClock.uptimeMillis();
        for (Listener l : listeners) l.onImportJobChanged();
    }

    static void clearSnapshot() {
        snapshot = null;
        lastFanout = 0;
        for (Listener l : listeners) l.onImportJobChanged();
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
        return !pendingJobs(c).isEmpty();
    }

    static boolean pendingForPlugin(Context c, HomeMode plugin) {
        for (Job j : pendingJobs(c)) if (j.plugin == plugin) return true;
        return false;
    }

    /** e.g. "importing 3 files" for the lock-confirm dialog, or null. */
    static String detailForPlugin(Context c, HomeMode plugin) {
        for (Job j : pendingJobs(c)) {
            if (j.plugin == plugin) return "importing " + j.label;
        }
        return null;
    }

    /** Row text for the home list while a plugin's import is pending. */
    static String progressLine(Context c, HomeMode plugin) {
        Snapshot s = snapshot;
        if (s != null && s.plugin == plugin && s.total > 1) {
            return "Importing " + Math.min(s.done + 1, s.total) + " of " + s.total + "…";
        }
        return "Importing…";
    }

    // --- job record --------------------------------------------

    static final class Job {
        String id;
        HomeMode plugin;
        String label;
        List<Uri> uris = new ArrayList<>();
    }

    /** Queue an import. The SAF read grants must already be persisted by the caller. */
    static void start(Context context, HomeMode plugin, List<Uri> uris, String label) {
        if (uris == null || uris.isEmpty()) return;
        File d = newJobDir(context);
        writeLines(new File(d, "job"), "plugin=" + plugin.name(), "label=" + label);
        StringBuilder sb = new StringBuilder();
        for (Uri u : uris) sb.append(u.toString()).append('\n');
        try {
            atomicWrite(new File(d, "uris"), sb.toString());
        } catch (IOException e) {
            android.util.Log.w("ImportJobs", "uris write failed", e);
        }
        kick(context);
    }

    static void resumeIfPending(Context context) {
        if (anyPending(context)) kick(context);
    }

    private static void kick(Context context) {
        context.getApplicationContext().startForegroundService(
                new Intent(context, ImportJobService.class));
    }

    /** Queued imports, oldest first. */
    static List<Job> pendingJobs(Context context) {
        List<Job> out = new ArrayList<>();
        File[] kids = queueDir(context).listFiles();
        if (kids == null) return out;
        java.util.Arrays.sort(kids, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File dir : kids) {
            if (!dir.isDirectory()) continue;
            Job j = readJob(dir);
            if (j != null) {
                j.id = dir.getName();
                out.add(j);
            }
        }
        return out;
    }

    private static Job readJob(File dir) {
        File jobFile = new File(dir, "job");
        File urisFile = new File(dir, "uris");
        if (!jobFile.exists() || !urisFile.exists()) return null;
        Job j = new Job();
        for (String line : readAll(jobFile).split("\n")) {
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String k = line.substring(0, eq), v = line.substring(eq + 1);
            if ("plugin".equals(k)) {
                try { j.plugin = HomeMode.valueOf(v); } catch (Exception ignored) {}
            } else if ("label".equals(k)) {
                j.label = v;
            }
        }
        if (j.plugin == null) return null;
        for (String line : readAll(urisFile).split("\n")) {
            if (!line.isEmpty()) j.uris.add(Uri.parse(line));
        }
        if (j.uris.isEmpty()) return null;
        if (j.label == null) j.label = j.uris.size() + " files";
        return j;
    }

    static Set<String> readDone(Context context, String jobId) {
        Set<String> set = new LinkedHashSet<>();
        File f = new File(new File(queueDir(context), jobId), "done");
        if (!f.exists()) return set;
        for (String line : readAll(f).split("\n")) if (!line.isEmpty()) set.add(line);
        return set;
    }

    static void writeDone(Context context, String jobId, Set<String> done, int added) {
        File dir = new File(queueDir(context), jobId);
        StringBuilder sb = new StringBuilder();
        for (String u : done) sb.append(u).append('\n');
        try {
            atomicWrite(new File(dir, "done"), sb.toString());
            atomicWrite(new File(dir, "added"), Integer.toString(added));
        } catch (IOException e) {
            android.util.Log.w("ImportJobs", "done write failed", e);
        }
    }

    static int readAdded(Context context, String jobId) {
        File f = new File(new File(queueDir(context), jobId), "added");
        if (!f.exists()) return 0;
        try {
            return Integer.parseInt(readAll(f).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static void clearJob(Context context, String jobId) {
        File d = new File(queueDir(context), jobId);
        File[] kids = d.listFiles();
        if (kids != null) for (File f : kids) f.delete();
        d.delete();
    }

    // --- files -------------------------------------------------

    private static File dir(Context context) {
        File d = new File(context.getFilesDir(), "import-jobs");
        d.mkdirs();
        return d;
    }

    private static File queueDir(Context context) {
        return dir(context);
    }

    private static File newJobDir(Context context) {
        long stamp = System.currentTimeMillis();
        File d = new File(queueDir(context), Long.toString(stamp));
        for (int i = 1; d.exists(); i++) d = new File(queueDir(context), stamp + "-" + i);
        d.mkdirs();
        return d;
    }

    private static void writeLines(File f, String... lines) {
        try {
            atomicWrite(f, String.join("\n", lines));
        } catch (IOException e) {
            android.util.Log.w("ImportJobs", "job record write failed", e);
        }
    }

    private static void atomicWrite(File f, String content) throws IOException {
        File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        }
        if (!tmp.renameTo(f)) {
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(content.getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            tmp.delete();
        }
    }

    private static String readAll(File f) {
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int n = 0;
            while (n < buf.length) {
                int r = in.read(buf, n, buf.length - n);
                if (r < 0) break;
                n += r;
            }
            return new String(buf, 0, n, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
