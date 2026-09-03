package com.plainphone.app;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/**
 * The Home section-tab strip. Horizontal touches pass straight through to the
 * tab scroller (switch sections). A clearly VERTICAL drag that starts here is
 * captured and streamed to a listener — Home uses it to fold the header away,
 * following the finger and snapping on release.
 */
class VDragStrip extends LinearLayout {

    interface Listener {
        void onDragStart();

        /** Incremental movement in px since the last call. Up = negative. */
        void onDragBy(float dy);

        /** velocityY in px/s at release (up = negative). */
        void onDragEnd(float velocityY);
    }

    private Listener listener;
    private final int slop;
    private float downX, downY, lastRawY;
    private boolean dragging, started;
    private VelocityTracker vt;

    VDragStrip(Context c) {
        super(c);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
    }

    void setDragListener(Listener l) {
        listener = l;
    }

    private void begin(float rawY) {
        lastRawY = rawY;
        dragging = true;
        if (!started) {
            started = true;
            if (listener != null) listener.onDragStart();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                lastRawY = e.getRawY();
                dragging = false;
                started = false;
                if (vt != null) vt.recycle();
                vt = VelocityTracker.obtain();
                vt.addMovement(e);
                break;
            case MotionEvent.ACTION_MOVE:
                if (vt != null) vt.addMovement(e);
                float dx = Math.abs(e.getX() - downX);
                float dy = Math.abs(e.getY() - downY);
                if (!dragging && dy > slop && dy > dx * 1.4f) {
                    begin(e.getRawY());
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (vt == null) vt = VelocityTracker.obtain();
        vt.addMovement(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                begin(e.getRawY());
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) begin(e.getRawY());
                float d = e.getRawY() - lastRawY;
                lastRawY = e.getRawY();
                if (listener != null) listener.onDragBy(d);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float vy = 0f;
                if (vt != null) {
                    vt.computeCurrentVelocity(1000);
                    vy = vt.getYVelocity();
                    vt.recycle();
                    vt = null;
                }
                if (started && listener != null) listener.onDragEnd(vy);
                dragging = false;
                started = false;
                return true;
        }
        return dragging;
    }
}
