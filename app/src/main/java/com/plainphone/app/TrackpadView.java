package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * The dedicated relative trackpad for the Dev screen's "Dedicated pad" layout.
 * Feeds the {@link RemoteScreenView} above it, which owns the cursor:
 *
 * <ul>
 *   <li>drag — nudge the cursor (a full-pad swipe crosses ~the whole screen)</li>
 *   <li>tap — click where the cursor is; second tap = double-click</li>
 *   <li>tap then tap-and-hold-drag — hold the button down while dragging</li>
 *   <li>long-press — same press-drag</li>
 *   <li>two-finger slide — scroll (mouse wheel)</li>
 *   <li>two-finger pinch — zoom the mirror above</li>
 * </ul>
 */
final class TrackpadView extends View {

    RemoteScreenView screen;

    /** A full-width pad drag crosses this fraction of the screen. */
    private static final float GAIN = 1.1f;

    private final Paint hatch = new Paint();
    private final float tapSlop;
    private final int longPressTimeout;
    private final int doubleTapTimeout;

    private float lastX, lastY, startX, startY, lastTapX, lastTapY;
    private boolean moved, dragging, twoFinger, secondTap;
    private long lastUpTime;

    private float pinchDist0, pinchMidY0, lastMidY, lastPinchD, pinchAccum;
    /** 0 = undecided, 1 = pinch-zoom (host), 2 = two-finger scroll. */
    private int twoMode;
    private final float twoSlop;
    /** Spread change (px) that equals one host zoom tick. */
    private final float pinchStep;

    /** Second tap only counts as a double if it lands this close to the first. */
    private final float doubleTapSlop;

    private final Runnable longPress = () -> {
        if (!moved && !dragging && !twoFinger && screen != null) {
            dragging = true;
            screen.hold(true);
        }
    };

    TrackpadView(Context context) {
        super(context);
        setBackgroundColor(0xFF0D0D0D);
        // Fingers on a small pad jitter — a generous tap radius so a tap
        // reliably clicks instead of turning into a tiny drag.
        tapSlop = context.getResources().getDisplayMetrics().density * 22f;
        longPressTimeout = ViewConfiguration.getLongPressTimeout();
        // Tighter than the system's ~300ms — two deliberate quick taps still
        // double-click, an accidental "tap, look, tap again" does not.
        doubleTapTimeout = Math.min(ViewConfiguration.getDoubleTapTimeout(), 240);
        doubleTapSlop = context.getResources().getDisplayMetrics().density * 24f;
        twoSlop = context.getResources().getDisplayMetrics().density * 10f;
        pinchStep = context.getResources().getDisplayMetrics().density * 34f;
        hatch.setColor(0xFF151515);
        hatch.setStrokeWidth(2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFF0D0D0D);
        for (int x = -getHeight(); x < getWidth(); x += 14) {
            canvas.drawLine(x, 0, x + getHeight(), getHeight(), hatch);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (screen == null) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = startX = e.getX();
                lastY = startY = e.getY();
                moved = dragging = twoFinger = false;
                secondTap = (e.getEventTime() - lastUpTime) < doubleTapTimeout
                        && Math.hypot(startX - lastTapX, startY - lastTapY) < doubleTapSlop;
                postDelayed(longPress, longPressTimeout);
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                twoFinger = true;
                removeCallbacks(longPress);
                if (dragging) { screen.hold(false); dragging = false; }
                if (e.getPointerCount() >= 2) {
                    pinchDist0 = lastPinchD = Math.max(1, spread(e));
                    pinchMidY0 = lastMidY = (e.getY(0) + e.getY(1)) / 2f;
                    pinchAccum = 0;
                    twoMode = 0;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (twoFinger && e.getPointerCount() >= 2) {
                    float d = Math.max(1, spread(e));
                    float my = (e.getY(0) + e.getY(1)) / 2f;
                    if (twoMode == 0) {
                        float sp = Math.abs(d - pinchDist0);
                        float sl = Math.abs(my - pinchMidY0);
                        if (Math.max(sp, sl) > twoSlop) twoMode = sp > sl ? 1 : 2;
                    }
                    if (twoMode == 1) {           // pinch -> host zoom (Ctrl+wheel)
                        pinchAccum += d - lastPinchD; // + = fingers spreading = zoom IN
                        while (pinchAccum >= pinchStep) { screen.hostZoom(-1); pinchAccum -= pinchStep; }
                        while (pinchAccum <= -pinchStep) { screen.hostZoom(1); pinchAccum += pinchStep; }
                    } else if (twoMode == 2) {    // slide -> mouse wheel
                        screen.scroll(-(my - lastMidY) / 3f);
                    }
                    lastPinchD = d;
                    lastMidY = my;
                    return true;
                }
                float dx = e.getX() - lastX;
                float dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();
                if (!moved && Math.hypot(e.getX() - startX, e.getY() - startY) > tapSlop) {
                    moved = true;
                    removeCallbacks(longPress);
                    if (secondTap && !dragging) {
                        dragging = true;
                        screen.hold(true);
                    }
                }
                if (moved) {  // only once it's clearly a drag, not a jittery tap
                    screen.nudge(dx * GAIN / getWidth(), dy * GAIN / getWidth());
                }
                return true;

            case MotionEvent.ACTION_UP:
                removeCallbacks(longPress);
                if (dragging) {
                    screen.hold(false);
                    dragging = false;
                    lastUpTime = 0;               // a drag ends no tap chain
                } else if (!moved && !twoFinger) {
                    screen.tapClick(secondTap);
                    if (secondTap) {
                        lastUpTime = 0;          // consumed — no triple-click chain
                    } else {
                        lastUpTime = e.getEventTime();
                        lastTapX = e.getX();
                        lastTapY = e.getY();
                    }
                } else {
                    lastUpTime = 0;
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(longPress);
                if (dragging) screen.hold(false);
                dragging = false;
                return true;
        }
        return super.onTouchEvent(e);
    }

    private static float spread(MotionEvent e) {
        return (float) Math.hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1));
    }
}
