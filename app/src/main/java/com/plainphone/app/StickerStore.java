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

    private StickerStore() {}

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "stickers");
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    static List<File> list(Context c) {
        File[] files = dir(c).listFiles((d, name) -> name.endsWith(".png"));
        List<File> out = new ArrayList<>();
        if (files == null) return out;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        out.addAll(Arrays.asList(files));
        return out;
    }

    /** Copy the picked image in, scaled down to at most 512 px, as a PNG. */
    static boolean add(Context c, Uri source) {
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
            if (bmp == null) return false;
            float scale = Math.min(1f, (float) MAX_SIDE / Math.max(bmp.getWidth(), bmp.getHeight()));
            if (scale < 1f) {
                bmp = Bitmap.createScaledBitmap(bmp, Math.max(1, Math.round(bmp.getWidth() * scale)),
                        Math.max(1, Math.round(bmp.getHeight() * scale)), true);
            }
            File out = new File(dir(c), System.currentTimeMillis() + ".png");
            try (FileOutputStream os = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            justAdded = true;
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    static void delete(File f) {
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
