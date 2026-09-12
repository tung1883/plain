package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** GitHub-style contribution heatmap: one column per week, one row per weekday (Sun..Sat),
 *  the familiar green scale on the app's black background. */
class ContributionGraphView extends View {

    private static final float CELL_DP = 9f;
    private static final float GAP_DP = 3f;

    private final float density;
    private final int cellPx;
    private final int gapPx;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int[][] grid; // [week][weekday], -1 = no such day

    ContributionGraphView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        cellPx = Math.round(CELL_DP * density);
        gapPx = Math.round(GAP_DP * density);
    }

    /** Pixel width of one week column including its trailing gap — the grid's
     *  natural scroll-snap unit. */
    int stepPx() { return cellPx + gapPx; }

    /** Pixel width of a single day cell, excluding its trailing gap. */
    int cellPx() { return cellPx; }

    void setData(int[][] grid) {
        this.grid = grid;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int weeks = grid == null ? 0 : grid.length;
        int w = weeks == 0 ? 0 : weeks * cellPx + (weeks - 1) * gapPx;
        int h = 7 * cellPx + 6 * gapPx;
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (grid == null || grid.length == 0) return;
        int cell = cellPx;
        int gap = gapPx;
        float corner = 2 * density;

        int max = 1;
        for (int[] week : grid) {
            for (int v : week) if (v > max) max = v;
        }

        for (int w = 0; w < grid.length; w++) {
            for (int d = 0; d < 7; d++) {
                int v = grid[w][d];
                if (v < 0) continue;
                float left = w * (cell + gap);
                float top = d * (cell + gap);
                rect.set(left, top, left + cell, top + cell);
                fill.setColor(colorFor(v, max));
                canvas.drawRoundRect(rect, corner, corner, fill);
            }
        }
    }

    private static int colorFor(int count, int max) {
        if (count <= 0) return 0xFF1B1B1B;
        int level = (int) Math.ceil(count * 4.0 / max);
        level = Math.max(1, Math.min(4, level));
        switch (level) {
            case 1: return 0xFF0E4429;
            case 2: return 0xFF006D32;
            case 3: return 0xFF26A641;
            default: return 0xFF39D353;
        }
    }
}
