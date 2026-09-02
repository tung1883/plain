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

class PhotoArtView extends View implements CropArt {

    private static final int MAX_DECODED_SIDE = 1080;

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
    private boolean gray;

    PhotoArtView(Context context, Uri uri, float focusX, float focusY, float zoom) {
        this(context, uri, focusX, focusY, zoom, true);
    }

    PhotoArtView(Context context, Uri uri, float focusX, float focusY, float zoom, boolean gray) {
        super(context);
        this.focusX = focusX;
        this.focusY = focusY;
        this.zoom = zoom;
        this.gray = gray;
        this.bitmap = decodeDownsampled(context, uri);
    }

    public float getFocusX() {
        return focusX;
    }

    public float getFocusY() {
        return focusY;
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    void setGrayscale(boolean g) {
        if (gray != g) { gray = g; invalidate(); }
    }

    public void panBy(float dx, float dy) {
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

    private static float clampSpan(float pos, float frameMinusDrawn) {
        float lo = Math.min(0f, frameMinusDrawn);
        float hi = Math.max(0f, frameMinusDrawn);
        return Math.max(lo, Math.min(pos, hi));
    }

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

            return null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.BLACK);
        if (bitmap == null || getWidth() <= 0 || getHeight() <= 0) return;

        float scale = coverScale() * zoom;
        float drawnW = bitmap.getWidth() * scale;
        float drawnH = bitmap.getHeight() * scale;

        float left = clampSpan(getWidth() / 2f - focusX * drawnW, getWidth() - drawnW);
        float top = clampSpan(getHeight() / 2f - focusY * drawnH, getHeight() - drawnH);

        canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        canvas.drawBitmap(bitmap, 0, 0, gray ? GRAYSCALE_PAINT : null);
        canvas.restore();
    }
}

