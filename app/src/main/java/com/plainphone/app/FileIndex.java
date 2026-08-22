package com.plainphone.app;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A flat, in-memory list of every file and folder on internal storage and the SD card,
 * walked once in the background and then searched by substring.
 *
 * <p>This exists because MediaStore — what the rest of device search uses — only knows
 * about media (images, video, audio) and has no concept of a folder at all. Finding a PDF,
 * a zip, or a directory by name means walking the filesystem directly, which since Android
 * 11 requires All-files access. Without that permission nothing here can read anything, so
 * callers fall back to MediaStore.
 *
 * <p>Walking tens of thousands of directory entries takes seconds, far too slow to redo on
 * each keystroke, hence the cached index. Every method is safe to call from any thread; the
 * scan itself always runs on its own.
 */
class FileIndex {

    private FileIndex() {}

    /** One indexed path. Directories are indexed too — they're most of what people search for. */
    static class Entry {
        final String name;
        final File file;
        final boolean directory;
        /**
         * Folded name and trigram mask, computed here at scan time rather than per query:
         * folding runs Unicode normalization, which is far too slow to repeat across the
         * whole index on every keystroke.
         */
        final String folded;
        final long signature;

        Entry(File file, boolean directory) {
            this.name = file.getName();
            this.file = file;
            this.directory = directory;
            this.folded = TextMatch.fold(this.name);
            this.signature = TextMatch.signatureOf(this.folded);
        }
    }

    /** Callback fired on the main thread once a scan finishes, so a live search can redraw. */
    interface OnIndexed {
        void onIndexed();
    }

    /**
     * Deep enough for real storage layouts (nested project and backup folders) while still
     * bounding a pathological tree; entries cap total memory at a few MB.
     */
    private static final int MAX_DEPTH = 15;
    private static final int MAX_ENTRIES = 150_000;
    /** How long an index stays trusted before the next search triggers a rescan. */
    private static final long TTL_MS = 5 * 60 * 1000L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile List<Entry> entries;
    private static volatile long builtAt;
    private static volatile boolean scanning;
    private static volatile OnIndexed listener;

    /**
     * Whether the filesystem can be walked at all. Android 11 closed direct file access
     * behind the separate "All files access" toggle; before that, plain storage read
     * permission was enough.
     */
    static boolean canWalk(Context context) {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /** True while a scan is in flight, so the UI can say so instead of showing an empty group. */
    static boolean isScanning() {
        return scanning;
    }

    static void setListener(OnIndexed newListener) {
        listener = newListener;
    }

    /**
     * Matching entries, best first. Returns what's currently indexed and kicks off a rescan
     * when the index is missing or stale — so the first search after a cold start comes back
     * empty and fills in via the listener rather than blocking on a multi-second walk.
     */
    static List<Scored> search(Context context, TextMatch.Query query, int limit) {
        ensureFresh(context);

        List<Entry> snapshot = entries;
        List<Scored> hits = new ArrayList<>();
        if (snapshot == null || query.empty) return hits;

        for (Entry entry : snapshot) {
            int score = TextMatch.score(entry.folded, entry.signature, query, entry.directory);
            if (score != TextMatch.NO_MATCH) hits.add(new Scored(entry, score));
        }

        Collections.sort(hits, new Comparator<Scored>() {
            @Override
            public int compare(Scored a, Scored b) {
                if (a.score != b.score) return Integer.compare(a.score, b.score);
                // Among equally good matches the shortest name is the most likely target —
                // "Photos" should beat "Photos from Tokyo 2019 (copy)".
                return Integer.compare(a.entry.name.length(), b.entry.name.length());
            }
        });

        return hits.size() > limit ? new ArrayList<>(hits.subList(0, limit)) : hits;
    }

    /** An entry with the rank it earned for one particular query. */
    static class Scored {
        final Entry entry;
        final int score;

        Scored(Entry entry, int score) {
            this.entry = entry;
            this.score = score;
        }
    }

    private static synchronized void ensureFresh(Context context) {
        if (scanning) return;
        if (entries != null && System.currentTimeMillis() - builtAt < TTL_MS) return;

        scanning = true;
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            List<Entry> scanned = new ArrayList<>();
            long startedAt = System.currentTimeMillis();
            try {
                walkAll(roots(appContext), scanned);
            } catch (Exception ignored) {
                // A storage volume yanked mid-scan (or an OEM quirk) shouldn't lose the
                // entries already collected — serve the partial index instead of none.
            } finally {
                android.util.Log.d("PlainFileIndex", "scan finished in "
                        + (System.currentTimeMillis() - startedAt) + "ms, " + scanned.size() + " entries");
                entries = scanned;
                builtAt = System.currentTimeMillis();
                scanning = false;
            }

            MAIN.post(() -> {
                OnIndexed current = listener;
                if (current != null) current.onIndexed();
            });
        }).start();
    }

    /**
     * Every mounted storage volume's root — internal storage plus any SD card. The
     * StorageManager list is the authoritative one; the fallback walks up from the
     * app-specific directory each volume provides, which sits four levels below its root.
     */
    private static List<File> roots(Context context) {
        List<File> roots = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 30) {
            StorageManager storage = context.getSystemService(StorageManager.class);
            if (storage != null) {
                for (StorageVolume volume : storage.getStorageVolumes()) {
                    File directory = volume.getDirectory();
                    if (directory != null && directory.canRead()) roots.add(directory);
                }
            }
        }

        if (roots.isEmpty()) {
            for (File appDir : context.getExternalFilesDirs(null)) {
                if (appDir == null) continue;
                File root = appDir; // <volume>/Android/data/<package>/files
                for (int i = 0; i < 4 && root != null; i++) {
                    root = root.getParentFile();
                }
                if (root != null && root.canRead()) roots.add(root);
            }
            File primary = Environment.getExternalStorageDirectory();
            if (primary != null && primary.canRead() && !roots.contains(primary)) {
                roots.add(primary);
            }
        }
        return roots;
    }

    /**
     * Walks every root in parallel across a pool sized to the device's core count. A
     * directory listing is I/O, and per-name folding (Unicode normalization, in
     * TextMatch.fold) is real CPU work repeated tens of thousands of times — both overlap
     * cleanly across threads, and ForkJoinPool's work-stealing handles the wildly uneven
     * subtree sizes a real storage layout has (one folder with one file, another with
     * thousands) better than splitting the roots evenly up front would.
     */
    private static void walkAll(List<File> roots, List<Entry> out) {
        if (roots.isEmpty()) return;

        ForkJoinPool pool = new ForkJoinPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
        ConcurrentLinkedQueue<Entry> collected = new ConcurrentLinkedQueue<>();
        Set<String> visited = ConcurrentHashMap.newKeySet();
        AtomicInteger count = new AtomicInteger();

        try {
            List<ForkJoinTask<Void>> top = new ArrayList<>();
            for (File root : roots) {
                top.add(pool.submit(new WalkTask(root, 0, collected, visited, count)));
            }
            for (ForkJoinTask<Void> task : top) task.join();
        } finally {
            pool.shutdown();
        }
        out.addAll(collected);
    }

    private static class WalkTask extends RecursiveAction {
        private final File directory;
        private final int depth;
        private final ConcurrentLinkedQueue<Entry> out;
        private final Set<String> visited;
        private final AtomicInteger count;

        WalkTask(File directory, int depth, ConcurrentLinkedQueue<Entry> out,
                 Set<String> visited, AtomicInteger count) {
            this.directory = directory;
            this.depth = depth;
            this.out = out;
            this.visited = visited;
            this.count = count;
        }

        @Override
        protected void compute() {
            if (depth >= MAX_DEPTH || count.get() >= MAX_ENTRIES) return;
            // A plain absolute-path string, not File.getCanonicalPath() — that resolves
            // symlinks via real filesystem calls, paid on every directory visited, to guard
            // against a loop that MAX_DEPTH already bounds on its own. Storage roots
            // (internal, SD card) essentially never contain symlinks in practice; this still
            // catches the one real risk, a root appearing twice in roots().
            if (!visited.add(directory.getAbsolutePath())) return;

            File[] children = directory.listFiles();
            if (children == null) return; // unreadable — skip quietly

            List<WalkTask> subtasks = new ArrayList<>();
            for (File child : children) {
                if (count.get() >= MAX_ENTRIES) break;
                // Hidden entries are caches and dotfiles nobody searches for by name.
                if (child.getName().startsWith(".")) continue;
                // Android/data and Android/obb are per-app private storage — overwhelmingly
                // cache files (WhatsApp, Telegram, browsers) nobody searches for by name,
                // and easily the majority of a phone's total file count on their own.
                if (isAppPrivateStorage(directory, child)) continue;

                boolean isDirectory = child.isDirectory();
                out.add(new Entry(child, isDirectory));
                count.incrementAndGet();
                if (isDirectory) subtasks.add(new WalkTask(child, depth + 1, out, visited, count));
            }
            invokeAll(subtasks);
        }
    }

    private static boolean isAppPrivateStorage(File parent, File child) {
        return "Android".equals(parent.getName())
                && ("data".equals(child.getName()) || "obb".equals(child.getName()));
    }
}
