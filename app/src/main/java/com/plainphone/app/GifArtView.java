package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.os.SystemClock;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

class GifArtView extends View {

    /** ~25 fps — plenty for a pixel-art loop, a quarter of the buffer churn of a per-vsync redraw. */
    private static final long FRAME_MS = 40;

    private Movie movie;
    private long startTime = 0;

    GifArtView(Context context, GifScene scene) {
        super(context);
        setScene(scene);
    }

    void setScene(GifScene scene) {
        startTime = 0;
        try (InputStream in = getContext().getAssets().open("pixel_art/" + scene.assetFileName)) {
            movie = Movie.decodeStream(in);
        } catch (IOException e) {
            movie = null;
        }
        invalidate();
    }

    private boolean animating() {
        return movie != null && isShown() && getWindowVisibility() == VISIBLE;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        if (movie == null || movie.width() <= 0 || movie.height() <= 0) return;

        long now = SystemClock.uptimeMillis();
        if (startTime == 0) startTime = now;
        int duration = movie.duration() > 0 ? movie.duration() : 1000;
        movie.setTime((int) ((now - startTime) % duration));

        float scale = Math.max(getWidth() / (float) movie.width(), getHeight() / (float) movie.height());
        float left = (getWidth() - movie.width() * scale) / 2f;
        float top = (getHeight() - movie.height() * scale) / 2f;

        canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        movie.draw(canvas, 0, 0);
        canvas.restore();

        if (animating()) postInvalidateDelayed(FRAME_MS);
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
}
