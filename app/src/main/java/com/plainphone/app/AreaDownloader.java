package com.plainphone.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Downloads one {@link AreaCatalog.Entry}'s zip (style.json, {@code <id>.pmtiles},
 *  places.json, roads.json, meta.json — see {@code tools/nav}'s packaging step) and unzips it
 *  into {@code filesDir/map/<id>/}, where {@link MapAreas#all} picks it up on its next call.
 *  Unzips into a sibling temp directory first and only swaps it into place on full success, so
 *  a crash or kill mid-download can never leave a half-written area that {@link MapAreas#all}
 *  would try to merge in and choke on. */
final class AreaDownloader {

    private AreaDownloader() {}

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    interface OnProgress {
        void onProgress(int percent);
    }

    interface OnDone {
        void onDone(boolean success);
    }

    static void download(Context context, AreaCatalog.Entry entry, OnProgress onProgress, OnDone onDone) {
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            boolean ok = downloadSync(appContext, entry, onProgress);
            MAIN.post(() -> onDone.onDone(ok));
        }, "AreaDownloader-" + entry.id).start();
    }

    private static boolean downloadSync(Context context, AreaCatalog.Entry entry, OnProgress onProgress) {
        File mapDir = new File(context.getFilesDir(), "map");
        File destDir = new File(mapDir, entry.id);
        File tmpZip = new File(context.getCacheDir(), entry.id + "-download.zip.part");
        File tmpDir = new File(mapDir, entry.id + "-tmp");

        try {
            mapDir.mkdirs();
            downloadZip(entry.zipUrl, tmpZip, onProgress);
            deleteRecursive(tmpDir);
            tmpDir.mkdirs();
            unzip(tmpZip, tmpDir);
            deleteRecursive(destDir);
            if (!tmpDir.renameTo(destDir)) return false;
            return true;
        } catch (IOException e) {
            deleteRecursive(tmpDir);
            return false;
        } finally {
            tmpZip.delete();
        }
    }

    private static void downloadZip(String zipUrl, File dest, OnProgress onProgress) throws IOException {
        URL url = new URL(zipUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true); // GitHub release asset URLs redirect once
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        try {
            int total = conn.getContentLength();
            try (InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[64 * 1024];
                long downloaded = 0;
                int n;
                while ((n = in.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                    downloaded += n;
                    if (total > 0 && onProgress != null) {
                        int pct = (int) (downloaded * 100 / total);
                        MAIN.post(() -> onProgress.onProgress(pct));
                    }
                }
            }
        } finally {
            conn.disconnect();
        }
    }

    private static void unzip(File zipFile, File destDir) throws IOException {
        byte[] buf = new byte[64 * 1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                File outFile = new File(destDir, entry.getName());
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    int n;
                    while ((n = zis.read(buf)) >= 0) fos.write(buf, 0, n);
                }
            }
        }
    }

    /** Remove a downloaded area's folder ({@code assetDir} is {@code map/<name>}, as in {@link MapArea}). */
    static void deleteArea(Context context, MapArea area) {
        if (!area.assetDir.startsWith("map/")) return;   // only ever delete inside filesDir/map/
        deleteRecursive(new File(context.getFilesDir(), area.assetDir));
    }

    private static void deleteRecursive(File file) {
        if (!file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursive(child);
        }
        file.delete();
    }
}
