package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * Holds one child (the list) and lets a horizontal drag move it with the finger,
 * committing to the next/previous section on release. Vertical drags pass straight
 * through to the child so it scrolls normally.
 */
class SwipeSwitcher extends FrameLayout {

    interface Handler {
        /** dir: +1 = next section, -1 = previous. */
        boolean canGo(int dir);

        /** Swap the child's content to the section `dir` away. No animation. */
        void switchSection(int dir);
    }

    private static final DecelerateInterpolator DECEL = new DecelerateInterpolator();
    private static final int OUT_MS = 140;
    private static final int IN_MS = 200;
    private static final int SNAP_MS = 160;
    private static final float COMMIT_FRACTION = 0.15f;

    private final int slop;
    private final int flingVelocity;
    private Handler handler;
    private VelocityTracker velocity;
    private float downX, downY, dragDx;
    private boolean dragging;
    private boolean animating;

    SwipeSwitcher(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        ViewConfiguration vc = ViewConfiguration.get(context);
        slop = vc.getScaledTouchSlop();
        flingVelocity = vc.getScaledMinimumFlingVelocity() * 2;
    }

    void setHandler(Handler handler) {
        this.handler = handler;
    }

    private View content() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (animating || handler == null) return false;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                dragging = false;
                dragDx = 0;
                if (velocity != null) velocity.recycle();
                velocity = VelocityTracker.obtain();
                velocity.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                if (velocity != null) velocity.addMovement(ev);
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.3f) {
                    dragging = true;
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        View content = content();
        if (content == null || handler == null) return false;

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE: {
                if (!dragging) return true;
                if (velocity != null) velocity.addMovement(ev);
                float dx = ev.getX() - downX;
                if (!handler.canGo(dx < 0 ? 1 : -1)) dx *= 0.22f;   // rubber-band at the ends
                dragDx = dx;
                content.setTranslationX(dx);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (!dragging) return false;
                dragging = false;
                float vx = 0f;
                if (velocity != null) {
                    velocity.addMovement(ev);
                    velocity.computeCurrentVelocity(1000);
                    vx = velocity.getXVelocity();
                    velocity.recycle();
                    velocity = null;
                }
                float width = Math.max(getWidth(), 1);
                int dir = dragDx < 0 ? 1 : -1;
                boolean flung = Math.abs(vx) > flingVelocity
                        && (vx < 0 ? 1 : -1) == dir
                        && Math.abs(dragDx) > slop;
                if (handler.canGo(dir)
                        && (flung || Math.abs(dragDx) > width * COMMIT_FRACTION)) {
                    commit(dir);
                } else {
                    content.animate().translationX(0f).setDuration(SNAP_MS)
                            .setInterpolator(DECEL).start();
                }
                return true;
            }
        }
        return false;
    }

    /** Programmatic switch (tab tap): full slide in `dir`. */
    void performSwitch(int dir) {
        if (animating || handler == null || !handler.canGo(dir)) return;
        commit(dir);
    }

    private void commit(int dir) {
        View content = content();
        if (content == null) return;
        animating = true;
        float width = Math.max(getWidth(), 1);
        content.animate().translationX(-dir * width).setDuration(OUT_MS).setInterpolator(DECEL)
                .withEndAction(() -> {
                    handler.switchSection(dir);
                    content.setTranslationX(dir * width);
                    content.animate().translationX(0f).setDuration(IN_MS).setInterpolator(DECEL)
                            .withEndAction(() -> animating = false).start();
                }).start();
    }
}
