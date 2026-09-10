package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The rounded weighted meter bar shared by {@link ProcSurface} and the device surfaces. */
final class Meters {

    private Meters() {}

    /**
     * A {@code LABEL [====----]  47%} row. Weighted fill + spacer, no post-layout
     * callback, so it never renders empty.
     *
     * @param frac      0..1
     * @param fillColor the fill colour (white, grey, or rose for near-full)
     * @param trailing  text shown at the right edge; when null, {@code round(frac*100)+"%"}
     */
    static View bar(Context ctx, Typeface font, String label, double frac, int fillColor,
                    String trailing) {
        final float f = (float) Math.max(0, Math.min(1, frac));
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);

        LinearLayout row = new LinearLayout(ctx);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 4 * d, 0, 4 * d);

        TextView l = new TextView(ctx);
        l.setText(label);
        l.setTextColor(0xFF8B8B8B);
        l.setTextSize(11);
        l.setTypeface(font);
        l.setMinWidth(38 * d);
        row.addView(l);

        LinearLayout track = new LinearLayout(ctx);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(UiKit.rounded(ctx, 0xFF161616, 0, 0f, 4f));
        UiKit.clipRounded(ctx, track, 4f);
        LinearLayout.LayoutParams trackP = new LinearLayout.LayoutParams(0, 8 * d, 1f);
        trackP.rightMargin = 10 * d;

        View fill = new View(ctx);
        fill.setBackgroundColor(fillColor);
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, f));
        if (f < 1f) {
            View spacer = new View(ctx);
            track.addView(spacer, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - f));
        }
        row.addView(track, trackP);

        TextView pct = new TextView(ctx);
        pct.setText(trailing != null ? trailing : (Math.round(frac * 100) + "%"));
        pct.setTextColor(Color.WHITE);
        pct.setTextSize(11);
        pct.setTypeface(font);
        pct.setMinWidth(44 * d);
        pct.setGravity(Gravity.END);
        row.addView(pct);
        return row;
    }

    static View bar(Context ctx, Typeface font, String label, double frac, int fillColor) {
        return bar(ctx, font, label, frac, fillColor, null);
    }

    /** Rose when at/above 80%, otherwise the given colour. */
    static int fillFor(double frac, int normal) {
        return frac >= 0.8 ? 0xFFC88F87 : normal;
    }
}
