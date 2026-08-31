package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

import java.util.List;

/** Minimal vertical column chart: white bars on black, y-axis ticks, x-axis labels. */
class ColumnChartView extends View {

    interface Formatter {
        String format(long value);
    }

    private final float density;
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int TICKS = 4;
    private static final long MINUTE_MS = 60_000L;
    /** "Nice" round durations in minutes for the y-axis tick step. */
    private static final long[] NICE_MINUTES =
            {1, 2, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240, 360, 480, 720, 1440};

    private List<String> labels;
    private long[] values;
    private Formatter formatter = String::valueOf;
    private long axisMax = 1;

    ColumnChartView(Context context, Typeface typeface) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;

        bar.setColor(Color.WHITE);
        axis.setColor(Color.WHITE);
        axis.setStrokeWidth(density);
        grid.setColor(Color.DKGRAY);
        grid.setStrokeWidth(density);
        text.setColor(Color.WHITE);
        text.setTypeface(typeface);
        text.setTextSize(12 * density);

        setPadding(0, (int) (8 * density), 0, 0);
    }

    void setData(List<String> labels, long[] values, Formatter formatter) {
        this.labels = labels;
        this.values = values;
        this.formatter = formatter;
        long max = 1;
        for (long v : values) max = Math.max(max, v);
        this.axisMax = niceAxisMax(max);
        invalidate();
    }

    /** Round up so each of the {@link #TICKS} gridlines lands on a whole, tidy minute value. */
    private static long niceAxisMax(long maxMillis) {
        long rawStepMs = (maxMillis + TICKS - 1) / TICKS;
        long stepMs = NICE_MINUTES[NICE_MINUTES.length - 1] * MINUTE_MS;
        for (long minutes : NICE_MINUTES) {
            if (minutes * MINUTE_MS >= rawStepMs) {
                stepMs = minutes * MINUTE_MS;
                break;
            }
        }
        if (rawStepMs > stepMs) {
            long dayMs = 1440 * MINUTE_MS;
            stepMs = ((rawStepMs + dayMs - 1) / dayMs) * dayMs;
        }
        return stepMs * TICKS;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (values == null || values.length == 0) return;

        float leftAxisWidth = 52 * density;
        float bottomLabelHeight = 22 * density;

        float chartLeft = leftAxisWidth;
        float chartTop = getPaddingTop();
        float chartRight = getWidth();
        float chartBottom = getHeight() - bottomLabelHeight;
        float chartHeight = chartBottom - chartTop;

        text.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= TICKS; i++) {
            float frac = i / (float) TICKS;
            float y = chartBottom - frac * chartHeight;
            canvas.drawLine(chartLeft, y, chartRight, y, i == 0 ? axis : grid);
            canvas.drawText(formatter.format(axisMax * i / TICKS),
                    chartLeft - 6 * density, y + 4 * density, text);
        }

        int n = values.length;
        float slot = (chartRight - chartLeft) / n;
        float barWidth = Math.min(slot * 0.6f, 28 * density);
        text.setTextAlign(Paint.Align.CENTER);

        // Evenly spaced label positions, endpoints included, so they never collide.
        boolean[] showLabel = new boolean[n];
        int wanted = n <= 10 ? n : 6;
        if (wanted <= 1) {
            showLabel[0] = true;
        } else {
            for (int k = 0; k < wanted; k++) {
                showLabel[Math.round(k * (n - 1f) / (wanted - 1))] = true;
            }
        }

        float labelY = chartBottom + 15 * density;
        for (int i = 0; i < n; i++) {
            float cx = chartLeft + slot * (i + 0.5f);
            float h = (values[i] / (float) axisMax) * chartHeight;
            canvas.drawRect(cx - barWidth / 2, chartBottom - h, cx + barWidth / 2, chartBottom, bar);
            if (showLabel[i] && labels != null && i < labels.size()) {
                if (i == 0) {
                    text.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText(labels.get(i), chartLeft, labelY, text);
                } else if (i == n - 1) {
                    text.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(labels.get(i), getWidth() - getPaddingRight(), labelY, text);
                } else {
                    text.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(labels.get(i), cx, labelY, text);
                }
            }
        }
    }
}
