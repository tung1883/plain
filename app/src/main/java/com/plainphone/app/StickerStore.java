package com.plainphone.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** The user's own images for the keyboard: stored in the app's private files, newest first. */
final class StickerStore {

    private static final int MAX_SIDE = 512;

    /** Set when an image was just added, so the keyboard can reopen the image tab. */
    static volatile boolean justAdded;

    /** The collection the last add went to, so the tab opens on it; null = leave as is. */
    static volatile String pendingCollection;

    /** The name of the collection that holds images added before collections existed. */
    static final String DEFAULT = "Default";

    private StickerStore() {}

    private static File root(Context c) {
        File d = new File(c.getFilesDir(), "stickers");
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    /** A folder-safe collection name; blank or "Default" means the default collection. */
    static String clean(String name) {
        String n = name == null ? "" : name.replaceAll("[\\/:*?\"<>|.]", "_").trim();
        if (n.length() > 30) n = n.substring(0, 30).trim();
        return n.equalsIgnoreCase(DEFAULT) ? "" : n;
    }

    /** The folder of a collection; the default one is the stickers folder itself. */
    private static File dir(Context c, String collection) {
        String n = clean(collection);
        if (n.isEmpty()) return root(c);
        File d = new File(root(c), n);
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    /** Collection names, Default first, then the rest A to Z. Default is always listed. */
    static List<String> collections(Context c) {
        List<String> out = new ArrayList<>();
        out.add(DEFAULT);
        File[] dirs = root(c).listFiles(File::isDirectory);
        if (dirs == null) return out;
        Arrays.sort(dirs, Comparator.comparing(f -> f.getName().toLowerCase()));
        for (File d : dirs) out.add(d.getName());
        return out;
    }

    /** Delete a collection and every image in it. The default collection is only emptied. */
    static void deleteCollection(Context c, String collection) {
        File d = dir(c, collection);
        File[] files = d.listFiles((x, name) -> name.endsWith(".png"));
        if (files != null) for (File f : files) delete(f);
        if (!d.equals(root(c))) delete(d);
    }

    static List<File> list(Context c, String collection) {
        File[] files = dir(c, collection).listFiles((d, name) -> name.endsWith(".png"));
        List<File> out = new ArrayList<>();
        if (files == null) return out;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        out.addAll(Arrays.asList(files));
        return out;
    }

    /** Decode a picked image, scaled down to at most 512 px on its long side. Null if unreadable. */
    static Bitmap scaledBitmap(Context c, Uri source) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = c.getContentResolver().openInputStream(source)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bmp;
            try (InputStream in = c.getContentResolver().openInputStream(source)) {
                bmp = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bmp == null) return null;
            float scale = Math.min(1f, (float) MAX_SIDE / Math.max(bmp.getWidth(), bmp.getHeight()));
            if (scale < 1f) {
                bmp = Bitmap.createScaledBitmap(bmp, Math.max(1, Math.round(bmp.getWidth() * scale)),
                        Math.max(1, Math.round(bmp.getHeight() * scale)), true);
            }
            return bmp;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Copy the picked image in, scaled down to at most 512 px, as a PNG. */
    static boolean add(Context c, Uri source, String collection) {
        Bitmap bmp = scaledBitmap(c, source);
        return bmp != null && save(c, bmp, collection);
    }

    /** Save a bitmap (already the size you want) as one of the keyboard's own PNG images. */
    static boolean save(Context c, Bitmap bmp, String collection) {
        try {
            File out = new File(dir(c, collection), System.nanoTime() + ".png");   // unique even in a batch
            try (FileOutputStream os = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            justAdded = true;
            pendingCollection = clean(collection).isEmpty() ? DEFAULT : clean(collection);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static void delete(File f) {
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
