package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** A 20dp box: a white square with a black tick when checked, an outline when not. Also draws a radio ring. */
final class CheckView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final boolean radio;
    private boolean checked;

    /** @param radio draw a round radio button (ring plus dot) instead of a square check box */
    CheckView(Context context, boolean checked, boolean radio) {
        super(context);
        this.checked = checked;
        this.radio = radio;
    }

    void setChecked(boolean value) {
        checked = value;
        invalidate();
    }

    boolean isChecked() {
        return checked;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int size = UiKit.dp(getContext(), 20);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float stroke = UiKit.dp(getContext(), 1.5f);
        if (radio) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(checked ? 0xFFFFFFFF : 0xFF4A4A4A);
            canvas.drawCircle(w / 2f, w / 2f, w / 2f - stroke / 2f, paint);
            if (checked) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFFFFFFFF);
                canvas.drawCircle(w / 2f, w / 2f, w * 0.25f, paint);
            }
            return;
        }
        float r = UiKit.dp(getContext(), 6);
        rect.set(stroke / 2f, stroke / 2f, w - stroke / 2f, w - stroke / 2f);
        if (checked) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFFFFF);
            canvas.drawRoundRect(rect, r, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(UiKit.dp(getContext(), 2.2f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(0xFF000000);
            android.graphics.Path tick = new android.graphics.Path();
            tick.moveTo(w * 0.25f, w * 0.52f);
            tick.lineTo(w * 0.44f, w * 0.70f);
            tick.lineTo(w * 0.76f, w * 0.32f);
            canvas.drawPath(tick, paint);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(0xFF4A4A4A);
            canvas.drawRoundRect(rect, r, r, paint);
        }
    }
}
