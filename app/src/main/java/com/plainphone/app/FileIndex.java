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

class FileIndex {

    private FileIndex() {}

    static class Entry {
        final String name;
        final File file;
        final boolean directory;

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

    interface OnIndexed {
        void onIndexed();
    }

    private static final int MAX_DEPTH = 15;
    private static final int MAX_ENTRIES = 150_000;

    private static final long TTL_MS = 5 * 60 * 1000L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile List<Entry> entries;
    private static volatile long builtAt;
    private static volatile boolean scanning;
    private static volatile OnIndexed listener;

    static boolean canWalk(Context context) {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    static boolean isScanning() {
        return scanning;
    }

    static void setListener(OnIndexed newListener) {
        listener = newListener;
    }

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

                return Integer.compare(a.entry.name.length(), b.entry.name.length());
            }
        });

        return hits.size() > limit ? new ArrayList<>(hits.subList(0, limit)) : hits;
    }

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
                File root = appDir;
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

            if (!visited.add(directory.getAbsolutePath())) return;

            File[] children = directory.listFiles();
            if (children == null) return;

            List<WalkTask> subtasks = new ArrayList<>();
            for (File child : children) {
                if (count.get() >= MAX_ENTRIES) break;

                if (child.getName().startsWith(".")) continue;

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

