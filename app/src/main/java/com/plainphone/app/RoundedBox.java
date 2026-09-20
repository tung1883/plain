package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.LinearLayout;

/** A vertical container that clips its children to a rounded rect via canvas.clipPath in
 *  dispatchDraw, instead of View.setClipToOutline/setOutlineProvider. Some devices' renderers
 *  don't reliably honor the outline-based clip on a plain LinearLayout — children (a
 *  ScrollView's fading edge, a row's own square background) keep painting straight past the
 *  rounded corner regardless of any outline/elevation/window property. Canvas clipping happens
 *  in software during dispatchDraw and doesn't depend on that path at all. */
class RoundedBox extends LinearLayout {
    private final Path clipPath = new Path();
    private final RectF rect = new RectF();
    private float radiusPx;

    RoundedBox(Context c) {
        super(c);
        setOrientation(VERTICAL);
    }

    void setRadiusDp(float dp) {
        radiusPx = UiKit.dp(getContext(), dp);
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW);
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipPath(clipPath);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }
}
