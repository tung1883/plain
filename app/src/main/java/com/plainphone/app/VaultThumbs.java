package com.plainphone.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Decrypts vault images in the background and hands back a downscaled thumbnail.
 * Bitmaps are held in a small RAM cache that is dropped on lock — plaintext pixels
 * never touch disk here (the DocumentsProvider path has its own temp handling).
 */
final class VaultThumbs {

    private VaultThumbs() {}

    /** Above this blob size a thumbnail is skipped — decoding happens fully in RAM. */
    private static final long MAX_BLOB_BYTES = 25L * 1024 * 1024;

    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    interface Callback {
        void onThumb(Bitmap bitmap);
    }

    static Bitmap cached(String docId) {
        return CACHE.get(docId);
    }

    static void clear() {
        CACHE.evictAll();
    }

    /** {@code targetPx} is the desired max edge in pixels. Callback runs on the main thread. */
    static void load(Context context, String docId, long blobSize, int targetPx, Callback callback) {
        Bitmap hit = CACHE.get(docId);
        if (hit != null) {
            callback.onThumb(hit);
            return;
        }
        if (blobSize > MAX_BLOB_BYTES) {
            callback.onThumb(null);
            return;
        }
        Context app = context.getApplicationContext();
        POOL.execute(() -> {
            Bitmap bitmap = decode(app, docId, targetPx);
            if (bitmap != null) CACHE.put(docId, bitmap);
            MAIN.post(() -> callback.onThumb(bitmap));
        });
    }

    private static Bitmap decode(Context context, String docId, int targetPx) {
        try {
            byte[] plain = VaultStore.decryptToMemory(context, docId);

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(plain, 0, plain.length, bounds);

            int sample = 1;
            int longest = Math.max(bounds.outWidth, bounds.outHeight);
            while (longest / sample > targetPx * 2) sample *= 2;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(plain, 0, plain.length, opts);
        } catch (Throwable t) {
            return null;
        }
    }
}
