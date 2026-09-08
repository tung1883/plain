package com.plainphone.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shows the {@code screen.frame} JPEGs from {@code plaind} and turns touches
 * into relative pointer moves, taps, two-finger scroll and press-drag — the same
 * gestures the phone-mouse web client uses. Right-click is a toggle the host
 * screen supplies.
 */
final class RemoteScreenView extends View {

    interface Listener {
        void move(float dx, float dy, float scroll);
        void click(String button, boolean doubleClick);
        void press(boolean down);
    }

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect dst = new Rect();
    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private Bitmap frame;
    Listener listener;
    boolean rightClickArmed;

    private final int slop;
    private final int tapTimeout;
    private final int longPressTimeout;

    private float lastX, lastY;
    private float startX, startY;
    private long downTime;
    private boolean moved;
    private boolean dragging;
    private boolean twoFinger;
    private long lastTapUp;

    private final Runnable longPress = () -> {
        if (!moved && !dragging && listener != null) {
            dragging = true;
            listener.press(true);
        }
    };

    RemoteScreenView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        ViewConfiguration vc = ViewConfiguration.get(context);
        slop = vc.getScaledTouchSlop();
        tapTimeout = ViewConfiguration.getTapTimeout();
        longPressTimeout = ViewConfiguration.getLongPressTimeout();
    }

    void setFrame(byte[] jpeg) {
        decoder.execute(() -> {
            Bitmap bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
            if (bmp != null) {
                main.post(() -> {
                    Bitmap old = frame;
                    frame = bmp;
                    if (old != null) old.recycle();
                    invalidate();
                });
            }
        });
    }

    void release() {
        decoder.shutdownNow();
        if (frame != null) {
            frame.recycle();
            frame = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        if (frame == null) return;
        float scale = Math.min((float) getWidth() / frame.getWidth(),
                (float) getHeight() / frame.getHeight());
        int w = Math.round(frame.getWidth() * scale);
        int h = Math.round(frame.getHeight() * scale);
        int left = (getWidth() - w) / 2;
        int top = (getHeight() - h) / 2;
        dst.set(left, top, left + w, top + h);
        canvas.drawBitmap(frame, null, dst, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = startX = event.getX();
                lastY = startY = event.getY();
                downTime = event.getEventTime();
                moved = dragging = twoFinger = false;
                postDelayed(longPress, longPressTimeout);
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                twoFinger = true;
                removeCallbacks(longPress);
                lastY = event.getY(0);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (twoFinger && event.getPointerCount() >= 2) {
                    float dy = event.getY(0) - lastY;
                    lastY = event.getY(0);
                    if (listener != null) listener.move(0, 0, -dy / 6f);
                    return true;
                }
                float dx = event.getX() - lastX;
                float dy = event.getY() - lastY;
                lastX = event.getX();
                lastY = event.getY();
                if (!moved && Math.hypot(event.getX() - startX, event.getY() - startY) > slop) {
                    moved = true;
                    removeCallbacks(longPress);
                }
                if ((moved || dragging) && listener != null) listener.move(dx, dy, 0);
                return true;

            case MotionEvent.ACTION_UP:
                removeCallbacks(longPress);
                if (dragging) {
                    if (listener != null) listener.press(false);
                    dragging = false;
                } else if (!moved && !twoFinger && listener != null) {
                    long now = event.getEventTime();
                    boolean isDouble = now - lastTapUp < 280;
                    lastTapUp = now;
                    String button = rightClickArmed ? "r" : "l";
                    rightClickArmed = false;
                    listener.click(button, isDouble);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(longPress);
                if (dragging && listener != null) listener.press(false);
                dragging = false;
                return true;
        }
        return super.onTouchEvent(event);
    }
}
