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
 *   <li>tap — click where the cursor is</li>
 *   <li>tap then hold-and-drag — press the button and drag an item (no click
 *       lands first, so it never doubles into an "open")</li>
 *   <li>long-press — same press-drag</li>
 *   <li>two-finger slide — scroll (mouse wheel)</li>
 *   <li>two-finger pinch — zoom the mirror above</li>
 * </ul>
 *
 * <p>A tap's click is held back {@link #DRAG_GRACE_MS}: if a press-drag follows
 * it becomes a clean drag; otherwise it fires as a normal click, and two of
 * those in quick succession are a double-click to the OS.
 */
final class TrackpadView extends View {

    RemoteScreenView screen;

    /** A full-width pad drag crosses this fraction of the screen. */
    private static final float GAIN = 1.1f;
    /** How long a tap's click waits to see if a drag follows. */
    private static final long DRAG_GRACE_MS = 220;

    private final Paint hatch = new Paint();
    private final float tapSlop;
    private final int longPressTimeout;

    private float lastX, lastY, startX, startY, lastTapX, lastTapY;
    private boolean moved, dragging, twoFinger, armDrag;
    private boolean clickPending;

    private final Runnable clickFire = () -> {
        clickPending = false;
        if (screen != null) screen.tapClick(false);
    };

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
            removeCallbacks(clickFire);
            clickPending = false;
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
        doubleTapSlop = context.getResources().getDisplayMetrics().density * 24f;
        twoSlop = context.getResources().getDisplayMetrics().density * 26f;
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
                boolean near = Math.hypot(startX - lastTapX, startY - lastTapY) < doubleTapSlop;
                // A tap's click is still pending nearby -> this touch may be the
                // drag half of a tap-then-drag. If it's a tap somewhere else,
                // let that pending click land now.
                armDrag = clickPending && near;
                if (clickPending && !near) {
                    removeCallbacks(clickFire);
                    clickPending = false;
                    screen.tapClick(false);
                }
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
                        if (Math.max(sp, sl) > twoSlop) {
                            twoMode = (sp > sl * 1.5f && sp > twoSlop * 0.6f) ? 1 : 2;
                        }
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
                    if (armDrag && !dragging) {   // tap-then-drag -> clean press-drag, no click
                        removeCallbacks(clickFire);
                        clickPending = false;
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
                    removeCallbacks(clickFire);
                    clickPending = false;
                } else if (!moved && !twoFinger) {
                    // Hold the click briefly: a press-drag may follow (then it's
                    // cancelled). Two of these close together = OS double-click.
                    lastTapX = e.getX();
                    lastTapY = e.getY();
                    removeCallbacks(clickFire);
                    clickPending = true;
                    postDelayed(clickFire, DRAG_GRACE_MS);
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
