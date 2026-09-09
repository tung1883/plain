package com.plainphone.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The desktop mirror. The pointer is tracked <b>on the phone</b> as a
 * normalised position and every gesture sends {@code input.point} — so the
 * on-screen cursor tracks the finger with zero lag while {@code plaind} follows
 * as fast as messages arrive (and no longer draws its own cursor).
 *
 * <ul>
 *   <li><b>MOVE</b> — drag nudges the cursor, tap clicks at it, two-finger
 *       scrolls, long-press or double-tap-drag holds the button down.</li>
 *   <li><b>VIEW</b> — one-finger pan, two-finger pinch-zoom (1–5×); a tap points
 *       the cursor at that spot without clicking.</li>
 * </ul>
 *
 * In the <b>PAD</b> layout this view is the top screen only (VIEW behaviour +
 * tap-to-point); {@link TrackpadView} below it feeds {@link #nudge} / {@link #tapClick}.
 */
final class RemoteScreenView extends View {

    interface Listener {
        void move(float dx, float dy, float scroll); // scroll only, now
        void click(String button, boolean doubleClick);
        void press(boolean down);
        void point(float nx, float ny);
    }

    enum Layout { WHOLE, PAD }
    enum Mode { MOVE, VIEW }

    Layout layout = Layout.WHOLE;
    private Mode mode = Mode.MOVE;

    /** How much of the screen a full-view drag crosses in MOVE mode. */
    static final float MOVE_GAIN = 1.6f;

    private static final float ZOOM_MIN = 1f, ZOOM_MAX = 5f;
    private float zoom = 1f, panX = 0f, panY = 0f;

    private float curX = 0.5f, curY = 0.5f; // normalised cursor

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint cursorFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path cursor = new Path();
    private final RectF baseRect = new RectF();
    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private Bitmap frame;
    private int sourceW; // real monitor width in px (from the frame's "sw")
    Listener listener;
    Runnable onModeChange;
    /** Fired ~after a zoom gesture settles, so the activity can re-request detail. */
    Runnable onZoomSettle;

    private final float tapSlop;
    private final int longPressTimeout;
    private final int doubleTapTimeout;
    /** Never shrink the drawn pointer below this, so it stays findable. */
    private final float cursorMinPx;
    private final float cursorEdgePx;

    private float lastX, lastY, startX, startY, lastTapX, lastTapY;
    private boolean moved, dragging, twoFinger, secondTap;
    private long lastUpTime;

    /** Second tap only counts as a double if it lands this close to the first. */
    private final float doubleTapSlop;

    private float pinchDist, pinchZoom0, pinchAnchorX, pinchAnchorY;

    private final Runnable longPress = () -> {
        if (!moved && !dragging && !twoFinger && listener != null) {
            dragging = true;
            listener.press(true);
        }
    };

    RemoteScreenView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        // A finger tap on glass jitters several px before lift — use a generous
        // radius so a tap stays a tap (point + click) instead of a tiny nudge.
        tapSlop = context.getResources().getDisplayMetrics().density * 22f;
        longPressTimeout = ViewConfiguration.getLongPressTimeout();
        // Tighter than the system's ~300ms — two deliberate quick taps still
        // double-click, an accidental "tap, look, tap again" does not.
        doubleTapTimeout = Math.min(ViewConfiguration.getDoubleTapTimeout(), 240);
        doubleTapSlop = context.getResources().getDisplayMetrics().density * 24f;
        cursorMinPx = context.getResources().getDisplayMetrics().density * 3f;
        cursorEdgePx = context.getResources().getDisplayMetrics().density * 1f;
        cursorFill.setColor(Color.WHITE);
        cursorEdge.setColor(0xAA000000); // soft halo, not a hard border
        cursorEdge.setStyle(Paint.Style.STROKE);
        cursorEdge.setStrokeJoin(Paint.Join.ROUND);
        cursorEdge.setStrokeCap(Paint.Cap.ROUND);
        buildCursorPath();
    }

    /** Unit pointer (1px tall); scaled to the real cursor's apparent size at draw. */
    private void buildCursorPath() {
        cursor.reset();
        cursor.moveTo(0, 0);
        cursor.lineTo(0, 0.82f);
        cursor.lineTo(0.22f, 0.63f);
        cursor.lineTo(0.38f, 1f);
        cursor.lineTo(0.54f, 0.93f);
        cursor.lineTo(0.37f, 0.57f);
        cursor.lineTo(0.72f, 0.57f);
        cursor.close();
    }

    // --- public control ------------------------------------------------

    boolean drawsOwnCursor() {
        return true;
    }

    Mode mode() {
        return mode;
    }

    void toggleMode() {
        mode = mode == Mode.MOVE ? Mode.VIEW : Mode.MOVE;
        if (mode == Mode.MOVE) resetView();
        if (onModeChange != null) onModeChange.run();
        invalidate();
    }

    float zoom() {
        return zoom;
    }

    void setZoom(float z) {
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, z));
        if (zoom <= ZOOM_MIN + 0.001f) {
            zoom = ZOOM_MIN;
            panX = panY = 0;
        }
        clampPan();
        invalidate();
        maybeNotifyZoom();
    }

    /** The captured source's real pixel width, for cursor sizing. */
    void setSourceSize(int w, int h) {
        sourceW = w;
    }

    private void resetView() {
        zoom = 1f;
        panX = panY = 0f;
        invalidate();
    }

    /** Point the cursor at a normalised spot and tell the daemon. */
    void pointAt(float nx, float ny) {
        curX = clamp01(nx);
        curY = clamp01(ny);
        if (listener != null) listener.point(curX, curY);
        invalidate();
    }

    /** Move the cursor by normalised deltas (from a trackpad drag). */
    void nudge(float dnx, float dny) {
        curX = clamp01(curX + dnx);
        curY = clamp01(curY + dny);
        if (listener != null) listener.point(curX, curY);
        invalidate();
    }

    /** A tap on the dedicated pad = click where the cursor already is. */
    void tapClick(boolean doubleClick) {
        if (listener != null) listener.click("l", doubleClick);
    }

    void scroll(float amount) {
        if (listener != null) listener.move(0, 0, amount);
    }

    void hold(boolean down) {
        if (listener != null) listener.press(down);
    }

    private boolean relativeMode() {
        return layout == Layout.WHOLE && mode == Mode.MOVE;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    // --- frames -----------------------------------------------------

    void setFrame(byte[] jpeg) {
        decoder.execute(() -> {
            Bitmap bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
            if (bmp != null) {
                main.post(() -> {
                    Bitmap old = frame;
                    frame = bmp;
                    boolean aspectChanged = old == null
                            || old.getWidth() * bmp.getHeight() != bmp.getWidth() * old.getHeight();
                    if (old != null) old.recycle();
                    if (aspectLock && aspectChanged) requestLayout();
                    invalidate();
                });
            }
        });
    }

    /** When set, the view measures itself to the frame's aspect — no letterbox. */
    boolean aspectLock;

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        if (!aspectLock || frame == null) {
            super.onMeasure(wSpec, hSpec);
            return;
        }
        int w = MeasureSpec.getSize(wSpec);
        int h = Math.round(w * (float) frame.getHeight() / frame.getWidth());
        int hMax = MeasureSpec.getSize(hSpec);
        if (MeasureSpec.getMode(hSpec) != MeasureSpec.UNSPECIFIED && hMax > 0 && h > hMax) {
            h = hMax;
        }
        setMeasuredDimension(w, h);
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
        // Fit (contain): the whole desktop is always visible; any leftover space
        // is letterboxed. Pinch-zoom then fills it.
        float scale = Math.min((float) getWidth() / frame.getWidth(),
                (float) getHeight() / frame.getHeight());
        float w = frame.getWidth() * scale;
        float h = frame.getHeight() * scale;
        float left = (getWidth() - w) / 2f;
        float top = (getHeight() - h) / 2f;
        baseRect.set(left, top, left + w, top + h);
        clampPan(); // keep pan valid as the view resizes (e.g. keyboard opening)

        canvas.save();
        canvas.translate(panX, panY);
        canvas.scale(zoom, zoom, getWidth() / 2f, getHeight() / 2f);
        canvas.drawBitmap(frame, null, baseRect, paint);

        float cxp = baseRect.left + curX * baseRect.width();
        float cyp = baseRect.top + curY * baseRect.height();
        // Match the real OS pointer's apparent size. The visible Windows arrow
        // is ~20 source-px; a source px shows as (displayedW / sourceW) phone px.
        // Drawn inside the zoom transform, so no extra zoom factor here.
        int srcW = sourceW > 0 ? sourceW : frame.getWidth();
        float size = Math.max(cursorMinPx / Math.max(zoom, 1f),
                20f / srcW * baseRect.width());
        cursorEdge.setStrokeWidth(cursorEdgePx / size / Math.max(zoom, 1f));
        canvas.save();
        canvas.translate(cxp, cyp);
        canvas.scale(size, size);
        canvas.drawPath(cursor, cursorEdge);  // halo under the fill so white stays crisp
        canvas.drawPath(cursor, cursorFill);
        canvas.restore();
        canvas.restore();
    }

    /** View-space touch -> normalised frame coords (unclamped; may fall outside 0..1). */
    private float[] toFrame(float tx, float ty) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float x = (tx - panX - cx) / zoom + cx;
        float y = (ty - panY - cy) / zoom + cy;
        float nx = baseRect.width() == 0 ? 0 : (x - baseRect.left) / baseRect.width();
        float ny = baseRect.height() == 0 ? 0 : (y - baseRect.top) / baseRect.height();
        return new float[]{nx, ny};
    }

    /** Largest pan offset that keeps the (possibly zoomed) image covering the view. */
    private void clampPan() {
        float maxX = Math.max(0f, (baseRect.width() * zoom - getWidth()) / 2f);
        float maxY = Math.max(0f, (baseRect.height() * zoom - getHeight()) / 2f);
        panX = Math.max(-maxX, Math.min(maxX, panX));
        panY = Math.max(-maxY, Math.min(maxY, panY));
    }

    // --- touch ----------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = startX = e.getX();
                lastY = startY = e.getY();
                moved = dragging = twoFinger = false;
                secondTap = relativeMode()
                        && (e.getEventTime() - lastUpTime) < doubleTapTimeout
                        && Math.hypot(startX - lastTapX, startY - lastTapY) < doubleTapSlop;
                if (relativeMode()) postDelayed(longPress, longPressTimeout);
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                twoFinger = true;
                removeCallbacks(longPress);
                if (e.getPointerCount() >= 2) {
                    pinchDist = Math.max(1, dist(e));
                    pinchZoom0 = zoom;
                    float cx = getWidth() / 2f, cy = getHeight() / 2f;
                    float m0x = (e.getX(0) + e.getX(1)) / 2f;
                    float m0y = (e.getY(0) + e.getY(1)) / 2f;
                    // Content point under the fingers — kept fixed as zoom changes.
                    pinchAnchorX = (m0x - cx - panX) / zoom + cx;
                    pinchAnchorY = (m0y - cy - panY) / zoom + cy;
                }
                lastY = e.getY(0);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (twoFinger && e.getPointerCount() >= 2) {
                    if (relativeMode()) {
                        float dy = e.getY(0) - lastY;
                        lastY = e.getY(0);
                        scroll(-dy / 6f);
                    } else {
                        float d = Math.max(1, dist(e));
                        float cx = getWidth() / 2f, cy = getHeight() / 2f;
                        float mx = (e.getX(0) + e.getX(1)) / 2f;
                        float my = (e.getY(0) + e.getY(1)) / 2f;
                        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, pinchZoom0 * d / pinchDist));
                        // Hold the anchor content point under the moving fingers.
                        panX = mx - cx - (pinchAnchorX - cx) * zoom;
                        panY = my - cy - (pinchAnchorY - cy) * zoom;
                        if (zoom <= ZOOM_MIN + 0.001f) {
                            zoom = ZOOM_MIN;
                            panX = panY = 0;
                        }
                        clampPan();
                        invalidate();
                    }
                    moved = true;
                    return true;
                }
                // A lingering finger after a pinch: don't let it lurch into a pan.
                if (twoFinger) {
                    lastX = e.getX();
                    lastY = e.getY();
                    return true;
                }
                float dx = e.getX() - lastX;
                float dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();
                if (!moved && Math.hypot(e.getX() - startX, e.getY() - startY) > tapSlop) {
                    moved = true;
                    removeCallbacks(longPress);
                    if (secondTap && !dragging) {   // double-tap-drag: hold the button
                        dragging = true;
                        hold(true);
                    }
                }
                if (!moved) return true;
                if (relativeMode()) {
                    nudge(dx * MOVE_GAIN / getWidth(), dy * MOVE_GAIN / getWidth());
                } else {
                    panX += dx;
                    panY += dy;
                    clampPan();
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                removeCallbacks(longPress);
                if (dragging) {
                    hold(false);
                    dragging = false;
                    lastUpTime = 0;
                } else if (!moved && !twoFinger) {
                    float[] nf = toFrame(e.getX(), e.getY());
                    boolean onImage = nf[0] >= 0 && nf[0] <= 1 && nf[1] >= 0 && nf[1] <= 1;
                    if (onImage) {
                        pointAt(clamp01(nf[0]), clamp01(nf[1]));
                        listener.click("l", secondTap);   // tap the screen = left-click
                    }
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
                maybeNotifyZoom();
                return true;

            case MotionEvent.ACTION_POINTER_UP:
                clampPan();
                invalidate();
                maybeNotifyZoom();
                return true;

            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(longPress);
                if (dragging) hold(false);
                dragging = false;
                return true;
        }
        return super.onTouchEvent(e);
    }

    private float lastNotifiedZoom = 1f;

    private void maybeNotifyZoom() {
        if (onZoomSettle != null && Math.abs(zoom - lastNotifiedZoom) > 0.15f) {
            lastNotifiedZoom = zoom;
            onZoomSettle.run();
        }
    }

    private static float dist(MotionEvent e) {
        return (float) Math.hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1));
    }
}
