package com.plainphone.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.view.View;

import java.io.InputStream;

/**
 * Draws a user-picked photo scaled to cover the view — the same "fill the frame, crop
 * whatever overflows" idea GifArtView uses for a GIF — but offset by a stored focus point
 * and zoom instead of always centering, since a photo (unlike the bundled art) usually
 * needs a specific part of it chosen rather than its geometric center.
 *
 * <p>Static (no self-driven animation loop, unlike GifArtView), and mutable: PhotoCropActivity
 * drives this same class live during editing by changing focusX/focusY/zoom directly and
 * redrawing, so what's shown while cropping is exactly what gets saved — there's no separate
 * formula to keep in sync between "how it's edited" and "how it's finally rendered".
 */
class PhotoArtView extends View {

    /**
     * Longest side a decoded bitmap is allowed to be — comfortably more than any frame this
     * app ever shows a photo in, while keeping a multi-megapixel camera photo from sitting
     * in memory at full resolution for no visual benefit.
     */
    private static final int MAX_DECODED_SIDE = 1080;

    /**
     * Desaturates the photo so it matches the bundled art's black-and-white look instead
     * of clashing with it in full color — a Paint filter applied at draw time rather than
     * converting the decoded Bitmap itself, since it costs nothing extra per frame and
     * never needs to touch the (already memory-capped) decoded pixels.
     */
    private static final Paint GRAYSCALE_PAINT = new Paint();
    static {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);
        GRAYSCALE_PAINT.setColorFilter(new ColorMatrixColorFilter(matrix));
    }

    private final Bitmap bitmap;
    private float focusX;
    private float focusY;
    private float zoom;

    PhotoArtView(Context context, Uri uri, float focusX, float focusY, float zoom) {
        super(context);
        this.focusX = focusX;
        this.focusY = focusY;
        this.zoom = zoom;
        this.bitmap = decodeDownsampled(context, uri);
    }

    float getFocusX() {
        return focusX;
    }

    float getFocusY() {
        return focusY;
    }

    float getZoom() {
        return zoom;
    }

    void setZoom(float zoom) {
        this.zoom = zoom;
    }

    /**
     * Shifts the visible crop window by a drag of (dx, dy) screen pixels. Dragging right
     * should make the photo follow the finger — revealing more of what's to its left — so
     * the stored focus point moves the opposite way from the drag.
     */
    void panBy(float dx, float dy) {
        if (bitmap == null || getWidth() <= 0 || getHeight() <= 0) return;
        float scale = coverScale() * zoom;
        focusX = clamp01(focusX - dx / scale / bitmap.getWidth());
        focusY = clamp01(focusY - dy / scale / bitmap.getHeight());
    }

    private float coverScale() {
        return Math.max(getWidth() / (float) bitmap.getWidth(), getHeight() / (float) bitmap.getHeight());
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(v, 1f));
    }

    /** Reads the image's real dimensions first, then decodes at whatever scale keeps its
     *  longest side under the cap — avoids ever holding a full-resolution camera photo. */
    private Bitmap decodeDownsampled(Context context, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }

            int sample = 1;
            while (bounds.outWidth / sample > MAX_DECODED_SIDE
                    || bounds.outHeight / sample > MAX_DECODED_SIDE) {
                sample *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, options);
            }
        } catch (Exception e) {
            // A revoked persistable permission (the photo's source app was uninstalled,
            // or storage was moved) shouldn't crash the home screen — draw nothing instead.
            return null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Dark gray, not black: once zoom can go below "cover" (see panBy/setZoom), the
        // photo no longer necessarily fills the frame, and gray reads as "empty space"
        // instead of looking like part of a dark photo.
        canvas.drawColor(Color.DKGRAY);
        if (bitmap == null || getWidth() <= 0 || getHeight() <= 0) return;

        float scale = coverScale() * zoom;
        float drawnW = bitmap.getWidth() * scale;
        float drawnH = bitmap.getHeight() * scale;

        // Below "cover" scale the bitmap no longer spans the view in that dimension, so
        // there's no edge-to-edge range to clamp within — center it against the gray
        // background instead. At or above "cover", clamp the pan the usual way so no edge
        // of the bitmap ever falls short of the view's edge.
        float left = drawnW >= getWidth()
                ? Math.min(0f, Math.max(getWidth() / 2f - focusX * drawnW, getWidth() - drawnW))
                : (getWidth() - drawnW) / 2f;
        float top = drawnH >= getHeight()
                ? Math.min(0f, Math.max(getHeight() / 2f - focusY * drawnH, getHeight() - drawnH))
                : (getHeight() - drawnH) / 2f;

        canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        canvas.drawBitmap(bitmap, 0, 0, GRAYSCALE_PAINT);
        canvas.restore();
    }
}
