package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

/**
 * Renders a little 8-bit-style animated scene as a blocky pixel grid rather than a real
 * image — a bit of texture next to the home screen's menu rows, in keeping with the flat
 * black/white/gray look used everywhere else. The scene itself (what pixels are on) is
 * supplied by a Scene constant; this view just owns the timer, the grid-to-canvas drawing,
 * and a shared ground line every scene sits on.
 */
class PixelArtView extends View {

    static final int COLS = 16;
    static final int CONTENT_ROWS = 18;
    private static final int ROWS = CONTENT_ROWS + 1; // + 1 shared ground row
    private static final int FRAME_MILLIS = 400;

    private static final int[] PALETTE = {
            0, // 0: background — not drawn
            Color.WHITE,
            Color.LTGRAY,
            Color.GRAY,
            Color.DKGRAY,
    };

    private final Paint paint = new Paint();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int[][] grid = new int[CONTENT_ROWS][COLS];
    private Scene scene;
    private int tick = 0;
    private boolean running = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            tick++;
            invalidate();
            if (running) {
                handler.postDelayed(this, FRAME_MILLIS);
            }
        }
    };

    PixelArtView(Context context, Scene scene) {
        super(context);
        this.scene = scene;
    }

    void setScene(Scene scene) {
        this.scene = scene;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        handler.postDelayed(tickRunnable, FRAME_MILLIS);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        running = false;
        handler.removeCallbacks(tickRunnable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Always paint our own black backdrop first, regardless of whatever background
        // color the parent/row has (e.g. a picker row highlighted white when selected) —
        // otherwise WHITE scene pixels become invisible against a white row background.
        canvas.drawColor(Color.BLACK);

        float cellW = getWidth() / (float) COLS;
        float cellH = getHeight() / (float) ROWS;

        for (int[] row : grid) {
            java.util.Arrays.fill(row, 0);
        }
        scene.fillGrid(grid, tick);

        for (int r = 0; r < CONTENT_ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int index = grid[r][c];
                if (index == 0) continue;
                paint.setColor(PALETTE[index]);
                canvas.drawRect(c * cellW, r * cellH, (c + 1) * cellW, (r + 1) * cellH, paint);
            }
        }

        for (int c = 0; c < COLS; c++) {
            paint.setColor(c % 2 == 0 ? Color.WHITE : Color.LTGRAY);
            canvas.drawRect(c * cellW, CONTENT_ROWS * cellH, (c + 1) * cellW, ROWS * cellH, paint);
        }
    }
}
