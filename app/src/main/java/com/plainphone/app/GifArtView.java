package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.os.SystemClock;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

/**
 * Plays a bundled grayscale GIF (from assets/pixel_art/) scaled to fully cover the view
 * (cropping any excess, not letterboxing). Drives its own animation by continuously
 * invalidating from onDraw, so it only costs anything while actually attached and visible
 * (a GONE view never gets onDraw called).
 */
class GifArtView extends View {

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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        if (movie == null || movie.width() <= 0 || movie.height() <= 0) return;

        long now = SystemClock.uptimeMillis();
        if (startTime == 0) startTime = now;
        int duration = movie.duration() > 0 ? movie.duration() : 1000;
        movie.setTime((int) ((now - startTime) % duration));

        // Cover, not fit: scale up to the larger ratio so the GIF fills the whole view with
        // no black bars, cropping whichever dimension overflows.
        float scale = Math.max(getWidth() / (float) movie.width(), getHeight() / (float) movie.height());
        float left = (getWidth() - movie.width() * scale) / 2f;
        float top = (getHeight() - movie.height() * scale) / 2f;

        canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        movie.draw(canvas, 0, 0);
        canvas.restore();

        invalidate();
    }
}
