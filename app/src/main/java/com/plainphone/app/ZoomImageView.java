package com.plainphone.app;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/** An image that fits its view, pinch-zooms up to 6x, pans while zoomed and double-taps between fit and 2.5x. */
final class ZoomImageView extends ImageView {

    private static final float MAX_ZOOM = 6f, DOUBLE_TAP_ZOOM = 2.5f;

    private final Matrix matrix = new Matrix();
    private final float[] values = new float[9];
    private final RectF rect = new RectF();
    private final ScaleGestureDetector scaler;
    private final GestureDetector gestures;
    private float fit = 1f;

    ZoomImageView(Context ctx) {
        super(ctx);
        setScaleType(ScaleType.MATRIX);
        scaler = new ScaleGestureDetector(ctx, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                float cur = scale();
                float target = Math.max(fit, Math.min(fit * MAX_ZOOM, cur * d.getScaleFactor()));
                float f = target / cur;
                matrix.postScale(f, f, d.getFocusX(), d.getFocusY());
                apply();
                return true;
            }
        });
        gestures = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent a, MotionEvent b, float dx, float dy) {
                matrix.postTranslate(-dx, -dy);
                apply();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (scale() > fit * 1.05f) {
                    reset();
                } else {
                    float f = fit * DOUBLE_TAP_ZOOM / scale();
                    matrix.postScale(f, f, e.getX(), e.getY());
                    apply();
                }
                return true;
            }
        });
    }

    @Override
    public void setImageBitmap(android.graphics.Bitmap bm) {
        super.setImageBitmap(bm);
        reset();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        reset();
    }

    private float scale() {
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    /** Fit the whole picture in the view, centered. */
    private void reset() {
        Drawable d = getDrawable();
        if (d == null || getWidth() == 0 || getHeight() == 0) return;
        float dw = d.getIntrinsicWidth(), dh = d.getIntrinsicHeight();
        fit = Math.min(getWidth() / dw, getHeight() / dh);
        matrix.reset();
        matrix.postScale(fit, fit);
        matrix.postTranslate((getWidth() - dw * fit) / 2f, (getHeight() - dh * fit) / 2f);
        setImageMatrix(matrix);
    }

    /** Keep the picture covering the view, or centered on an axis where it is smaller. */
    private void apply() {
        Drawable d = getDrawable();
        if (d == null) return;
        rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        matrix.mapRect(rect);
        float dx = 0, dy = 0;
        if (rect.width() <= getWidth()) dx = (getWidth() - rect.width()) / 2f - rect.left;
        else if (rect.left > 0) dx = -rect.left;
        else if (rect.right < getWidth()) dx = getWidth() - rect.right;
        if (rect.height() <= getHeight()) dy = (getHeight() - rect.height()) / 2f - rect.top;
        else if (rect.top > 0) dy = -rect.top;
        else if (rect.bottom < getHeight()) dy = getHeight() - rect.bottom;
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        getParent().requestDisallowInterceptTouchEvent(true);
        scaler.onTouchEvent(ev);
        if (!scaler.isInProgress()) gestures.onTouchEvent(ev);
        return true;
    }
}
