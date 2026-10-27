package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the content index ({@link ContentDb}) in step with the files in the chosen folders: new and
 * changed files are read, vanished ones forgotten. The database is the progress, so a stopped run
 * continues where it left off the next time.
 */
final class ContentIndexer {

    private static final String TAG = "PlainContentIndex";
    private static final int MAX_DEPTH = 15;
    private static final long RETRY_FAILED_MS = 24L * 60 * 60 * 1000;
    private static final long RETRY_CRASHED_MS = 6L * 60 * 60 * 1000;
    private static final int WORKERS = 6;
    private static final int PDF_AT_ONCE = 1;

    private ContentIndexer() {}

    interface Control {
        boolean cancelled();

        void progress(int done, int total);
    }

    static final class Result {
        int indexed, removed, total;
        boolean paused, capped;
    }

    private static volatile boolean running;
    private static volatile boolean stopRequested;
    private static volatile int progressDone, progressTotal;
    private static volatile long runStartedAt;
    private static volatile String currentName = "";

    /** Name of the file a worker started reading last ("" when no run is going). */
    static String currentFile() {
        return currentName;
    }

    /** Milliseconds since the current run began (0 when none is running). */
    static long elapsedMs() {
        return running && runStartedAt > 0 ? System.currentTimeMillis() - runStartedAt : 0;
    }

    /** Files read so far in the current run (0 when none is running). */
    static int progressDone() {
        return progressDone;
    }

    /** Files to read in the current run. */
    static int progressTotal() {
        return progressTotal;
    }

    static boolean isRunning() {
        return running;
    }

    /** Stops the current run (if any) and waits for it to end; the files in flight are finished first. */
    static void stopAndWait() {
        stopRequested = true;
        try {
            long until = System.currentTimeMillis() + 120_000;
            while (running && System.currentTimeMillis() < until) Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void allowRuns() {
        stopRequested = false;
    }

    /** Index what changed. Blocks; call from a background thread. */
    static synchronized Result run(Context context, Control control) {
        Context app = context.getApplicationContext();
        Result result = new Result();
        running = true;
        runStartedAt = System.currentTimeMillis();
        currentName = "";
        try {
            PdfExtractor.install(app);
            ContentDb db = ContentDb.get(app);
            // files that took the app down last time are not read again
            for (String path : PdfCrashGuard.takeCrashed(app)) {
                File crashed = new File(path);
                db.replace(path, crashed.length(), crashed.lastModified(), "pdf", null, "crashed", "crashed the PDF reader", new ArrayList<ExtractRunner.Passage>());
            }
            Set<String> groups = Config.getContentTypes(app);
            Collected collected = collect(app, groups);
            List<File> found = collected.files;

            Map<String, ContentDb.Doc> known = db.allDocs();
            Set<String> present = new HashSet<>();
            for (File f : found) present.add(f.getAbsolutePath());
            // when a chosen place cannot be reached (an SD card taken out) its files must stay in the index
            if (collected.complete) {
                for (Map.Entry<String, ContentDb.Doc> e : known.entrySet()) {
                    if (!present.contains(e.getKey())) {
                        db.deleteDoc(e.getValue().id);
                        result.removed++;
                    }
                }
            }

            final long now = System.currentTimeMillis();
            List<File> todo = new ArrayList<>();
            for (File f : found) {
                ContentDb.Doc d = known.get(f.getAbsolutePath());
                boolean same = d != null && d.size == f.length() && d.mtime == f.lastModified();
                if (!same || (d.status.equals("failed") && now - d.indexedAt > RETRY_FAILED_MS) || (d.status.equals("crashed") && now - d.indexedAt > RETRY_CRASHED_MS)) todo.add(f);
            }
            // cheap files first, so something is searchable soon; PDFs last
            Collections.sort(todo, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    int ra = rank(a), rb = rank(b);
                    return ra != rb ? Integer.compare(ra, rb) : Long.compare(a.length(), b.length());
                }
            });
            result.total = todo.size();
            progressDone = 0;
            progressTotal = todo.size();
            int maxMb = Config.getContentMaxMb(app);
            long capBytes = maxMb <= 0 ? Long.MAX_VALUE : maxMb * 1024L * 1024L;
            boolean allowLow = Config.isContentAllowLowBattery(app);

            // Files are read by a few worker threads at once (writing to the database is serialised by SQLite).
            // PDFs take the most time and memory, so only PDF_AT_ONCE of them run together.
            final int total = todo.size();
            final java.util.concurrent.atomic.AtomicInteger started = new java.util.concurrent.atomic.AtomicInteger();
            final java.util.concurrent.atomic.AtomicInteger finished = new java.util.concurrent.atomic.AtomicInteger();
            final java.util.concurrent.atomic.AtomicBoolean halted = new java.util.concurrent.atomic.AtomicBoolean();
            final java.util.concurrent.atomic.AtomicBoolean paused = new java.util.concurrent.atomic.AtomicBoolean();
            final java.util.concurrent.atomic.AtomicBoolean capped = new java.util.concurrent.atomic.AtomicBoolean();
            final java.util.concurrent.Semaphore pdfSlots = new java.util.concurrent.Semaphore(PDF_AT_ONCE);
            final long cap = capBytes;
            java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(WORKERS, r -> {
                Thread t = new Thread(() -> {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                    r.run();
                }, "content-index");
                t.setDaemon(true);
                return t;
            });
            for (final File f : todo) {
                pool.execute(() -> {
                    if (halted.get()) return;
                    if (stopRequested || Config.isContentUserPaused(app) || control.cancelled()) {
                        paused.set(true);
                        halted.set(true);
                        return;
                    }
                    if (started.getAndIncrement() % 5 == 0 && !policyAllows(app, allowLow)) {
                        paused.set(true);
                        halted.set(true);
                        return;
                    }
                    if (db.sizeBytes() > cap) {
                        capped.set(true);
                        halted.set(true);
                        return;
                    }
                    boolean pdf = Extractors.PDF.equals(Extractors.groupOf(PlainTextExtractor.extensionOf(f.getName())));
                    if (pdf) pdfSlots.acquireUninterruptibly();
                    try {
                        if (halted.get()) return;
                        if (!indexOne(app, db, f, control)) {
                            paused.set(true);
                            halted.set(true);
                            return;
                        }
                    } finally {
                        if (pdf) pdfSlots.release();
                    }
                    int n = finished.incrementAndGet();
                    progressDone = n;
                    control.progress(n, total);
                });
            }
            pool.shutdown();
            try {
                while (!pool.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    // wait; the workers stop themselves when the run is cancelled
                }
            } catch (InterruptedException e) {
                halted.set(true);
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            result.indexed = finished.get();
            result.paused = paused.get();
            if (Config.isContentUserPaused(app)) {
                if (result.indexed >= result.total) {
                    Config.setContentUserPaused(app, false);     // paused on the last file: nothing is left to resume
                } else {
                    Config.setContentPausedAt(app, result.indexed, result.total,
                            todo.get(Math.min(started.get(), todo.size() - 1)).getName());
                }
            }
            result.capped = capped.get();
            Config.setContentCapped(app, result.capped);
            Config.setContentLastRun(app, System.currentTimeMillis());
            android.util.Log.d(TAG, "run finished: " + result.indexed + "/" + result.total + " indexed, "
                    + result.removed + " removed, paused=" + result.paused + ", capped=" + result.capped);
        } finally {
            running = false;
            runStartedAt = 0;
            currentName = "";
            progressDone = 0;
            progressTotal = 0;
        }
        return result;
    }

    /** False when the run was cancelled while reading this file (nothing was stored for it). */
    private static boolean indexOne(Context appContext, ContentDb db, File f, Control control) {
        String name = f.getName();
        String ext = PlainTextExtractor.extensionOf(name);
        String group = Extractors.groupOf(ext);
        String path = f.getAbsolutePath();
        long size = f.length(), mtime = f.lastModified();
        if (group == null) return true;
        if (size > Extractors.sizeLimit(group)) {
            db.replace(path, size, mtime, ext, null, "toobig", null, new ArrayList<ExtractRunner.Passage>());
            return true;
        }
        TextExtractor extractor = Extractors.forExt(ext);
        if (extractor == null) return true;      // e.g. PDF support not available: leave it for later
        long start = System.currentTimeMillis();
        currentName = name;
        boolean pdf = Extractors.PDF.equals(group);
        if (pdf) PdfCrashGuard.enter(appContext, path);
        ExtractRunner.Result r;
        try {
            r = ExtractRunner.run(extractor, f, ExtractRunner.MAX_CHARS, control::cancelled);
        } finally {
            if (pdf) PdfCrashGuard.leave(appContext, path);
        }
        if (control.cancelled()) return false;
        db.replace(path, size, mtime, ext, r.title != null ? r.title : name, r.status, r.error, r.passages);
        long took = System.currentTimeMillis() - start;
        if (took > 3000) android.util.Log.d(TAG, "slow file (" + took + " ms): " + path + " -> " + r.status);
        return true;
    }

    private static int rank(File f) {
        String group = Extractors.groupOf(PlainTextExtractor.extensionOf(f.getName()));
        if (Extractors.TEXT.equals(group)) return 0;
        if (Extractors.PDF.equals(group)) return 2;
        return 1;
    }

    // ------------------------------------------------------------------ gathering

    /** The indexable files in the chosen places, and whether every place could be reached. */
    static final class Collected {
        final List<File> files = new ArrayList<>();
        boolean complete = true;
    }

    /** Every indexable file in the chosen locations (see {@link StorageLocations}). */
    static Collected collect(Context context, Set<String> groups) {
        Collected out = new Collected();
        List<StorageLocations.Volume> volumes = Volumes.list(context);
        Set<String> seen = new java.util.HashSet<>();
        List<File> found = new ArrayList<>();
        for (String stored : Config.getContentFolders(context)) {
            File root = StorageLocations.resolve(StorageLocations.parse(stored), volumes);
            if (root == null) {
                out.complete = false;          // a card that is not there: do not forget what it held
                continue;
            }
            if (root.isDirectory()) walk(root, groups, found);
        }
        for (File f : found) if (seen.add(f.getAbsolutePath())) out.files.add(f);
        return out;
    }

    private static void walk(File root, Set<String> groups, List<File> out) {
        Deque<File> dirs = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();
        dirs.push(root);
        depths.push(0);
        while (!dirs.isEmpty()) {
            File dir = dirs.pop();
            int depth = depths.pop();
            File[] list = dir.listFiles();
            if (list == null) continue;
            for (File f : list) {
                String name = f.getName();
                if (name.startsWith(".")) continue;
                if (java.nio.file.Files.isSymbolicLink(f.toPath())) continue;
                if (f.isDirectory()) {
                    if (depth < MAX_DEPTH && !isAppPrivate(f)) {
                        dirs.push(f);
                        depths.push(depth + 1);
                    }
                } else if (f.isFile() && f.length() > 0) {
                    String group = Extractors.groupOf(PlainTextExtractor.extensionOf(name));
                    if (group != null && groups.contains(group)) out.add(f);
                }
            }
        }
    }

    private static boolean isAppPrivate(File dir) {
        String p = dir.getAbsolutePath();
        return p.endsWith("/Android/data") || p.endsWith("/Android/obb");
    }

    // ---------------------------------------------------------------------- power

    static boolean policyAllows(Context context, boolean allowLowBattery) {
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return true;
        boolean charging = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
        int scale = Math.max(1, battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        return IndexPolicy.shouldRun(charging, level * 100 / scale, allowLowBattery);
    }
}
