package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Movie;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewTreeObserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class GifArtView extends View implements CropArt {

    /** ~25 fps — plenty for a pixel-art loop, a quarter of the buffer churn of a per-vsync redraw. */
    private static final long FRAME_MS = 40;
    private static final int MAX_BYTES = 25 * 1024 * 1024;

    /** GIF decode is slow (tens of ms each); keep it off the UI thread. */
    private static final ExecutorService DECODE = Executors.newSingleThreadExecutor();
    private int loadGen;

    private static final Paint GRAYSCALE = new Paint();
    static {
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(0f);
        GRAYSCALE.setColorFilter(new ColorMatrixColorFilter(m));
    }

    private Movie movie;
    private boolean grayscale;
    private boolean staticFrame;
    private float focusX = 0.5f, focusY = 0.5f, zoom = 1f;
    private long startTime = 0;

    GifArtView(Context context, GifScene scene) {
        this(context, scene, 0.5f, 0.5f, 1f, true);
    }

    GifArtView(Context context, GifScene scene, float focusX, float focusY, float zoom, boolean gray) {
        super(context);
        this.grayscale = gray;
        this.focusX = focusX;
        this.focusY = focusY;
        this.zoom = zoom;
        setScene(scene);
    }

    /** Draw a single frame and never loop — for list previews, so N views don't all churn the GPU. */
    void setStaticPreview(boolean staticFrame) {
        this.staticFrame = staticFrame;
        invalidate();
    }

    GifArtView(Context context, File gifFile) {
        this(context, gifFile, 0.5f, 0.5f, 1f, true);
    }

    GifArtView(Context context, File gifFile, float focusX, float focusY, float zoom) {
        this(context, gifFile, focusX, focusY, zoom, true);
    }

    GifArtView(Context context, File gifFile, float focusX, float focusY, float zoom, boolean gray) {
        super(context);
        this.grayscale = gray;
        this.focusX = focusX;
        this.focusY = focusY;
        this.zoom = zoom;
        loadAsync(() -> readFile(gifFile));
    }

    public float getFocusX() { return focusX; }
    public float getFocusY() { return focusY; }
    public float getZoom() { return zoom; }
    public void setZoom(float z) { zoom = z; }

    void setGrayscale(boolean g) {
        if (grayscale != g) { grayscale = g; invalidate(); }
    }

    public void panBy(float dx, float dy) {
        if (movie == null || getWidth() <= 0 || getHeight() <= 0) return;
        float scale = coverScale() * zoom;
        focusX = clamp01(focusX - dx / scale / movie.width());
        focusY = clamp01(focusY - dy / scale / movie.height());
    }

    private float coverScale() {
        return Math.max(getWidth() / (float) movie.width(), getHeight() / (float) movie.height());
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(v, 1f));
    }

    /**
     * Position along one axis. {@code frameMinusDrawn = frame - drawn}:
     * negative (image bigger) → clamp to {@code [frameMinusDrawn, 0]} (no gaps);
     * positive (image smaller) → clamp to {@code [0, frameMinusDrawn]} (slides in the frame).
     */
    private static float clampSpan(float pos, float frameMinusDrawn) {
        float lo = Math.min(0f, frameMinusDrawn);
        float hi = Math.max(0f, frameMinusDrawn);
        return Math.max(lo, Math.min(pos, hi));
    }

    void setScene(GifScene scene) {
        loadAsync(() -> {
            try (InputStream in = getContext().getAssets().open("pixel_art/" + scene.assetFileName)) {
                return readAll(in);
            } catch (IOException e) {
                return null;
            }
        });
    }

    private interface Bytes { byte[] get(); }

    /** Read + decode the GIF off the UI thread; drop the result if a newer load started. */
    private void loadAsync(Bytes source) {
        final int gen = ++loadGen;
        movie = null;
        DECODE.execute(() -> {
            byte[] data = source.get();
            Movie m = data != null ? Movie.decodeByteArray(data, 0, data.length) : null;
            post(() -> {
                if (gen != loadGen) return;
                movie = m;
                startTime = 0;
                invalidate();
            });
        });
    }

    private boolean animating() {
        return movie != null && !staticFrame && isShown() && getWindowVisibility() == VISIBLE
                && getLocalVisibleRect(SCRATCH);
    }

    private static final android.graphics.Rect SCRATCH = new android.graphics.Rect();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        if (movie == null || movie.width() <= 0 || movie.height() <= 0) return;

        if (staticFrame) {
            movie.setTime(0);
        } else {
            long now = SystemClock.uptimeMillis();
            if (startTime == 0) startTime = now;
            int duration = movie.duration() > 0 ? movie.duration() : 1000;
            movie.setTime((int) ((now - startTime) % duration));
        }

        float scale = coverScale() * zoom;
        float drawnW = movie.width() * scale;
        float drawnH = movie.height() * scale;
        float left = clampSpan(getWidth() / 2f - focusX * drawnW, getWidth() - drawnW);
        float top = clampSpan(getHeight() / 2f - focusY * drawnH, getHeight() - drawnH);

        // Movie.draw ignores a Paint's colour filter on many devices — desaturate via a layer.
        int layer = grayscale
                ? canvas.saveLayer(0, 0, getWidth(), getHeight(), GRAYSCALE)
                : canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        movie.draw(canvas, 0, 0);
        canvas.restoreToCount(layer);

        if (animating()) postInvalidateDelayed(FRAME_MS);
    }

    /** Restart the draw loop when the view scrolls back into view (visibility events don't fire on scroll). */
    private final ViewTreeObserver.OnScrollChangedListener scrollKick = () -> {
        if (animating()) invalidate();
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnScrollChangedListener(scrollKick);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnScrollChangedListener(scrollKick);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            startTime = 0;
            invalidate();
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && animating()) invalidate();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
            if (out.size() > MAX_BYTES) return null;
        }
        return out.toByteArray();
    }

    private static byte[] readFile(File f) {
        if (f == null || !f.exists() || f.length() > MAX_BYTES) return null;
        try (FileInputStream in = new FileInputStream(f)) {
            return readAll(in);
        } catch (IOException e) {
            return null;
        }
    }
}
