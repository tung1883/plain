package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

/**
 * The remove target shown while a bubble is dragged, in the glass look: a round, frosted-looking
 * button over a soft dark gradient at the bottom of the screen (see {@link #newScrim}). Close to
 * it the button turns red, grows and glows. No text.
 */
final class DismissTargetView extends View {

    /** The view is the button plus room for its glow. */
    static final int SIZE_DP = 76;
    private static final int BUTTON_DP = 54;
    /** How far the view's bottom edge sits above the bottom of the screen, dp. */
    static final int BOTTOM_DP = 100;
    /** How close the bubble's centre must come to the button's centre to be dropped on it, dp. */
    static final int REACH_DP = 40;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;
    private boolean hot;

    DismissTargetView(Context ctx) {
        super(ctx);
        dp = ctx.getResources().getDisplayMetrics().density;
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(dp);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeWidth(2.2f * dp);
        line.setColor(0xFFFFFFFF);
    }

    boolean isHot() {
        return hot;
    }

    void setHot(boolean on) {
        if (hot == on) return;
        hot = on;
        animate().scaleX(on ? 1.22f : 1f).scaleY(on ? 1.22f : 1f).setDuration(120).start();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = BUTTON_DP * dp / 2f;
        if (hot) {   // a soft red glow: rings that fade outwards
            fill.setStyle(Paint.Style.FILL);
            for (int i = 3; i >= 1; i--) {
                fill.setColor((0x1C + (3 - i) * 0x0E) << 24 | 0xFF4D57);
                c.drawCircle(cx, cy, r + i * 3.2f * dp, fill);
            }
        }
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(hot ? 0x8CFF4D57 : 0x1AFFFFFF);
        c.drawCircle(cx, cy, r, fill);
        edge.setColor(hot ? 0xE6FF7880 : 0x40FFFFFF);
        c.drawCircle(cx, cy, r - dp / 2f, edge);
        if (hot) return;   // the bubble is on it: the X goes, the red button stays
        float s = r * 0.3f;
        c.drawLine(cx - s, cy - s, cx + s, cy + s, line);
        c.drawLine(cx - s, cy + s, cx + s, cy - s, line);
    }

    /** The soft dark gradient that rises from the bottom of the screen while a bubble is dragged. */
    static View newScrim(Context ctx) {
        View v = new View(ctx);
        v.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xA6000000, 0x00000000}));
        return v;
    }
}
