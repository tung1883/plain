package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** GitHub-style contribution heatmap: one column per week, one row per weekday (Sun..Sat),
 *  monochrome to match the app's other charts (see {@link ColumnChartView}). */
class ContributionGraphView extends View {

    private static final float CELL_DP = 10f;
    private static final float GAP_DP = 3f;

    private final float density;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int[][] grid; // [week][weekday], -1 = no such day

    ContributionGraphView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
    }

    void setData(int[][] grid) {
        this.grid = grid;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int cell = Math.round(CELL_DP * density);
        int gap = Math.round(GAP_DP * density);
        int weeks = grid == null ? 0 : grid.length;
        int w = weeks == 0 ? 0 : weeks * cell + (weeks - 1) * gap;
        int h = 7 * cell + 6 * gap;
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (grid == null || grid.length == 0) return;
        int cell = Math.round(CELL_DP * density);
        int gap = Math.round(GAP_DP * density);
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
        if (count <= 0) return 0xFF262626;
        int level = (int) Math.ceil(count * 4.0 / max);
        level = Math.max(1, Math.min(4, level));
        switch (level) {
            case 1: return 0xFF585858;
            case 2: return 0xFF8A8A8A;
            case 3: return 0xFFC4C4C4;
            default: return Color.WHITE;
        }
    }
}
