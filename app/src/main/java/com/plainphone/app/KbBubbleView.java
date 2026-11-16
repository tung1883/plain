package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;

/**
 * The keyboard's bubble as its own overlay window, for when Android hides the keyboard (its field
 * lost focus). The keyboard window is gone then, so the bubble has to live outside it: the
 * accessibility service draws it. Drag it and it snaps to the nearer edge (the place is saved and
 * shared with the in-keyboard bubble); a tap runs {@code onTap}.
 */
final class KbBubbleView extends View {

    private static final int SIZE_DP = 48, EDGE_DP = 8;

    private final WindowManager wm;
    private final WindowManager.LayoutParams lp;
    private final Runnable onTap;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;
    private float downX, downY;
    private int left0, top0;
    private boolean moved;

    KbBubbleView(Context ctx, WindowManager wm, Runnable onTap) {
        super(ctx);
        this.wm = wm;
        this.onTap = onTap;
        dp = ctx.getResources().getDisplayMetrics().density;
        setElevation(6 * dp);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                outline.setOval(0, 0, v.getWidth(), v.getHeight());
            }
        });
        int size = Math.round(SIZE_DP * dp);
        lp = new WindowManager.LayoutParams(size, size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        applySavedPlace();
    }

    /** Add the bubble to the screen; false when the system refuses (the service is unbinding). */
    boolean show() {
        try {
            wm.addView(this, lp);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    void remove() {
        try {
            wm.removeView(this);
        } catch (RuntimeException ignored) {
            // not attached
        }
    }

    private DisplayMetrics screen() {
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        return dm;
    }

    private int topBound() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : Math.round(24 * dp);
    }

    private void applySavedPlace() {
        DisplayMetrics dm = screen();
        Context ctx = getContext();
        int size = lp.width, top = topBound();
        lp.x = Math.round(FloatGeometry.bubbleLeft(Config.isKbBubbleRight(ctx), dm.widthPixels, size, EDGE_DP * dp));
        lp.y = Math.round(top + FloatGeometry.posFromFraction(Config.getKbBubbleY(ctx), dm.heightPixels - top, size));
    }

    @Override
    protected void onDraw(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        c.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 2f, paint);
        KeyIcons.draw(c, KeyIcons.KEYBOARD, getWidth() / 2f, getHeight() / 2f, getWidth() * 0.27f, Color.BLACK, false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        DisplayMetrics dm = screen();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getRawX();
                downY = e.getRawY();
                left0 = lp.x;
                top0 = lp.y;
                moved = false;
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                if (!moved && Math.hypot(dx, dy) > 6 * dp) moved = true;
                if (!moved) return true;
                lp.x = Math.round(FloatGeometry.keepInside(left0 + dx, 0, dm.widthPixels, lp.width, 0));
                lp.y = Math.round(FloatGeometry.keepInside(top0 + dy, topBound(), dm.heightPixels, lp.height, EDGE_DP * dp));
                wm.updateViewLayout(this, lp);
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (!moved) {
                    onTap.run();
                    return true;
                }
                int top = topBound();
                boolean right = FloatGeometry.bubbleOnRight(lp.x + lp.width / 2f, dm.widthPixels);
                float fy = FloatGeometry.fractionFromPos(lp.y - top, dm.heightPixels - top, lp.height);
                Config.setKbBubble(getContext(), right, fy);
                applySavedPlace();
                wm.updateViewLayout(this, lp);
                return true;
            }
            default:
                return true;
        }
    }
}
