package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** A thin determinate progress bar: a white fill over a dark track. */
final class ThinBar extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float fraction;

    ThinBar(Context context) {
        super(context);
    }

    /** 0..1. */
    void setFraction(float value) {
        float f = Math.max(0f, Math.min(1f, value));
        if (f != fraction) {
            fraction = f;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), UiKit.dp(getContext(), 6));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight(), r = h / 2f;
        paint.setColor(0xFF1A1A1A);
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, r, r, paint);
        if (fraction > 0f) {
            paint.setColor(0xFFFFFFFF);
            rect.set(0, 0, Math.max(h, w * fraction), h);
            canvas.drawRoundRect(rect, r, r, paint);
        }
    }
}
