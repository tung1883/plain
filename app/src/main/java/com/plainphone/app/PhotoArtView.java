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

class PhotoArtView extends View {

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

        canvas.drawColor(Color.DKGRAY);
        if (bitmap == null || getWidth() <= 0 || getHeight() <= 0) return;

        float scale = coverScale() * zoom;
        float drawnW = bitmap.getWidth() * scale;
        float drawnH = bitmap.getHeight() * scale;

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

