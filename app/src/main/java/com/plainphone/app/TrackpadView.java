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
 * <p>Taps are held back {@link #DRAG_GRACE_MS}: a tap-sequence that ends in a
 * press-drag fires <b>no</b> clicks (so a double-tap-drag grabs an item instead
 * of opening it); otherwise the pending taps fire as N clicks in a row, which
 * the OS reads as a single / double / triple click.
 */
final class TrackpadView extends View {

    RemoteScreenView screen;

    /** A full-width pad drag crosses this fraction of the screen. */
    private static final float GAIN = 1.1f;
    /** How long a tap-sequence waits to see if a drag follows. */
    private static final long DRAG_GRACE_MS = 220;

    private final Paint hatch = new Paint();
    private final Paint btnText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint btnLine = new Paint();
    private final float tapSlop;
    private final int longPressTimeout;

    /** Height of the bottom L | R click strip. */
    private final float btnH;
    /** 'L' / 'R' while a strip button is held, else 0. */
    private char btnDown;

    private float lastX, lastY, startX, startY, lastTapX, lastTapY;
    private boolean moved, dragging, twoFinger, armDrag;
    /** Taps waiting to fire; a drag that follows discards them all. */
    private int pendingTaps;

    private final Runnable tapFlush = () -> {
        int n = pendingTaps;
        pendingTaps = 0;
        if (screen != null) for (int i = 0; i < n; i++) screen.tapClick(false);
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
            removeCallbacks(tapFlush);
            pendingTaps = 0;
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

        float density = context.getResources().getDisplayMetrics().density;
        btnH = density * 56f;
        btnText.setColor(0xFF8B8B8B);
        btnText.setTextSize(density * 15f);
        btnText.setTextAlign(Paint.Align.CENTER);
        btnText.setTypeface(Fonts.cascadiaMono(context));
        btnLine.setColor(0xFF262626);
        btnLine.setStrokeWidth(density);
    }

    private boolean inStrip(float y) { return y >= getHeight() - btnH; }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFF0D0D0D);
        int padBottom = (int) (getHeight() - btnH);
        for (int x = -padBottom; x < getWidth(); x += 14) {
            canvas.drawLine(x, 0, x + padBottom, padBottom, hatch);
        }

        // --- L | R click strip ---
        float top = getHeight() - btnH, mid = getWidth() / 2f;
        canvas.drawLine(0, top, getWidth(), top, btnLine);
        canvas.drawLine(mid, top, mid, getHeight(), btnLine);
        if (btnDown != 0) {
            float l = btnDown == 'L' ? 0 : mid, r = btnDown == 'L' ? mid : getWidth();
            Paint hi = new Paint();
            hi.setColor(0xFF1E1E1E);
            canvas.drawRect(l, top, r, getHeight(), hi);
        }
        float baseY = top + btnH / 2f - (btnText.ascent() + btnText.descent()) / 2f;
        canvas.drawText("L", mid / 2f, baseY, btnText);
        canvas.drawText("R", mid + mid / 2f, baseY, btnText);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (screen == null) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (inStrip(e.getY())) {
                    btnDown = e.getX() < getWidth() / 2f ? 'L' : 'R';
                    invalidate();
                    return true;
                }
                btnDown = 0;
                lastX = startX = e.getX();
                lastY = startY = e.getY();
                moved = dragging = twoFinger = false;
                // Any touch that lands while taps are pending might be their drag
                // half; if it turns out to be a tap far away, flush them first.
                armDrag = pendingTaps > 0;
                if (pendingTaps > 0
                        && Math.hypot(startX - lastTapX, startY - lastTapY) >= doubleTapSlop) {
                    removeCallbacks(tapFlush);
                    tapFlush.run();
                    armDrag = false;
                }
                postDelayed(longPress, longPressTimeout);
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                twoFinger = true;
                removeCallbacks(longPress);
                if (dragging) { screen.hold(false); dragging = false; }
                if (pendingTaps > 0) { removeCallbacks(tapFlush); tapFlush.run(); }
                if (e.getPointerCount() >= 2) {
                    pinchDist0 = lastPinchD = Math.max(1, spread(e));
                    pinchMidY0 = lastMidY = (e.getY(0) + e.getY(1)) / 2f;
                    pinchAccum = 0;
                    twoMode = 0;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (btnDown != 0) {
                    char now = inStrip(e.getY()) && (e.getX() < getWidth() / 2f) == (btnDown == 'L')
                            ? btnDown : 0;
                    if (now != btnDown) { btnDown = now; invalidate(); }
                    return true;
                }
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
                    if (armDrag && !dragging) {   // tap(s)-then-drag -> press-drag, discard the clicks
                        removeCallbacks(tapFlush);
                        pendingTaps = 0;
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
                if (btnDown != 0) {
                    if (inStrip(e.getY()) && (e.getX() < getWidth() / 2f) == (btnDown == 'L')) {
                        screen.padClick(btnDown == 'L' ? "l" : "r");
                    }
                    btnDown = 0;
                    invalidate();
                    return true;
                }
                if (dragging) {
                    screen.hold(false);
                    dragging = false;
                    removeCallbacks(tapFlush);
                    pendingTaps = 0;
                } else if (!moved && !twoFinger) {
                    // Queue this tap; it fires (with any siblings) only if no
                    // press-drag follows within the grace window.
                    lastTapX = e.getX();
                    lastTapY = e.getY();
                    pendingTaps++;
                    removeCallbacks(tapFlush);
                    postDelayed(tapFlush, DRAG_GRACE_MS);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(longPress);
                if (dragging) screen.hold(false);
                dragging = false;
                if (btnDown != 0) { btnDown = 0; invalidate(); }
                return true;
        }
        return super.onTouchEvent(e);
    }

    private static float spread(MotionEvent e) {
        return (float) Math.hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1));
    }
}
