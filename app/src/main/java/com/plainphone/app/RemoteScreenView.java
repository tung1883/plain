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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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
        void zoom(float ticks); // Ctrl+wheel on the host (trackpad pinch)
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
    private final java.util.concurrent.atomic.AtomicBoolean decoding =
            new java.util.concurrent.atomic.AtomicBoolean();

    private Bitmap frame;
    private int sourceW; // real monitor width in px (from the frame's "sw")
    Listener listener;
    Runnable onModeChange;
    /** Fired ~after a zoom gesture settles, so the activity can re-request detail. */
    Runnable onZoomSettle;

    private static final long DRAG_GRACE_MS = 220;

    private final float tapSlop;
    private final int longPressTimeout;
    /** Never shrink the drawn pointer below this, so it stays findable. */
    private final float cursorMinPx;
    private final float cursorEdgePx;

    // --- minimap: the whole desktop, tiny, with a white box for what the view shows ---
    private static final long MAP_HOLD_MS = 1600, MAP_FADE_MS = 350;
    private final float dp;
    private final Paint mapBack = new Paint();
    private final Paint mapBmp = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint mapEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mapView = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mapDot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mapRect = new RectF();
    private boolean mapEnabled = true;
    private boolean onMap;
    /** uptimeMillis until which the minimap stays fully visible. */
    private long mapUntil;

    private float lastX, lastY, startX, startY, lastTapX, lastTapY;
    private boolean moved, dragging, twoFinger, armDrag;
    /** Taps waiting to fire; a press-drag that follows discards them all. */
    private int pendingTaps;

    /** A far-away tap flushes the pending ones instead of joining them. */
    private final float doubleTapSlop;

    private final Runnable tapFlush = () -> {
        int n = pendingTaps;
        pendingTaps = 0;
        if (listener != null) for (int i = 0; i < n; i++) listener.click("l", false);
    };

    private float pinchDist, pinchZoom0, pinchAnchorX, pinchAnchorY, pinchMidX, pinchMidY, lastMidY;
    /** 0 = undecided, 1 = pinch-zoom, 2 = two-finger scroll (mouse wheel). */
    private int twoMode;
    private final float twoSlop;

    private final Runnable longPress = () -> {
        if (!moved && !dragging && !twoFinger && listener != null) {
            removeCallbacks(tapFlush);
            pendingTaps = 0;
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
        doubleTapSlop = context.getResources().getDisplayMetrics().density * 24f;
        twoSlop = context.getResources().getDisplayMetrics().density * 26f;
        cursorMinPx = context.getResources().getDisplayMetrics().density * 3f;
        cursorEdgePx = context.getResources().getDisplayMetrics().density * 1f;
        cursorFill.setColor(Color.WHITE);
        cursorEdge.setColor(0xAA000000); // soft halo, not a hard border
        cursorEdge.setStyle(Paint.Style.STROKE);
        cursorEdge.setStrokeJoin(Paint.Join.ROUND);
        cursorEdge.setStrokeCap(Paint.Cap.ROUND);
        buildCursorPath();

        dp = context.getResources().getDisplayMetrics().density;
        mapBack.setColor(Color.BLACK);
        mapEdge.setStyle(Paint.Style.STROKE);
        mapEdge.setStrokeWidth(dp);
        mapEdge.setColor(Color.WHITE);
        mapView.setStyle(Paint.Style.STROKE);
        mapView.setStrokeWidth(dp * 1.5f);
        mapView.setColor(Color.WHITE);
        mapDot.setColor(0xFFFF5A5A);
    }

    /** Show the minimap (or never, when turned off). It hides itself a moment after the last use. */
    void setMinimapEnabled(boolean on) {
        mapEnabled = on;
        if (on) touchMap(); else invalidate();
    }

    /** Start at this zoom (1 = the whole desktop fits) instead of fit-to-view. */
    void setInitialZoom(float z) {
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, z));
        lastNotifiedZoom = zoom;
        panX = panY = 0f;
        invalidate();
    }

    private void touchMap() {
        if (!mapEnabled) return;
        mapUntil = android.os.SystemClock.uptimeMillis() + MAP_HOLD_MS;
        invalidate();
    }

    /** 0 = hidden, 1 = fully shown; fades out after the hold time. Needs a zoomed-in view. */
    private float mapAlpha() {
        if (!mapEnabled || frame == null || zoom <= 1.02f) return 0f;
        if (onMap) return 1f;
        long over = android.os.SystemClock.uptimeMillis() - mapUntil;
        if (over <= 0) return 1f;
        return over >= MAP_FADE_MS ? 0f : 1f - over / (float) MAP_FADE_MS;
    }

    /** Where the base (fit) rectangle sits in the view. */
    private boolean layoutBase() {
        if (frame == null || getWidth() == 0 || getHeight() == 0) return false;
        float scale = Math.min((float) getWidth() / frame.getWidth(),
                (float) getHeight() / frame.getHeight());
        float w = frame.getWidth() * scale;
        float h = frame.getHeight() * scale;
        float left = (getWidth() - w) / 2f;
        float top = (getHeight() - h) / 2f;
        baseRect.set(left, top, left + w, top + h);
        return true;
    }

    /** Pan just enough to keep the pointer inside the middle of the view. */
    private void followCursor() {
        if (zoom <= 1.001f || !layoutBase()) return;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float x = baseRect.left + curX * baseRect.width();
        float y = baseRect.top + curY * baseRect.height();
        float sx = cx + (x - cx) * zoom + panX;
        float sy = cy + (y - cy) * zoom + panY;
        float mx = getWidth() * 0.28f, my = getHeight() * 0.28f;
        if (sx < mx) panX += mx - sx;
        else if (sx > getWidth() - mx) panX -= sx - (getWidth() - mx);
        if (sy < my) panY += my - sy;
        else if (sy > getHeight() - my) panY -= sy - (getHeight() - my);
        clampPan();
    }

    /** Centre the view on the spot of the desktop that a touch on the minimap points at. */
    private void jumpFromMap(float tx, float ty) {
        if (!layoutBase()) return;
        float nx = clamp01((tx - mapRect.left) / mapRect.width());
        float ny = clamp01((ty - mapRect.top) / mapRect.height());
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float x = baseRect.left + nx * baseRect.width();
        float y = baseRect.top + ny * baseRect.height();
        panX = -(x - cx) * zoom;
        panY = -(y - cy) * zoom;
        clampPan();
        // The pointer goes to the middle of the view as it ended up (not to the touch itself), so
        // when the view stops at an edge of the desktop the pointer stops with it.
        float bx = cx - panX / zoom, by = cy - panY / zoom;
        curX = clamp01((bx - baseRect.left) / baseRect.width());
        curY = clamp01((by - baseRect.top) / baseRect.height());
        if (listener != null) listener.point(curX, curY);
        touchMap();
    }

    /**
     * Pan the view by (dx, dy) screen pixels, and move the pointer with it: it stays at the same
     * spot of the screen, so it travels across the desktop as the view does.
     */
    private void panWithPointer(float dx, float dy) {
        if (!layoutBase()) return;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float sx = cx + (baseRect.left + curX * baseRect.width() - cx) * zoom + panX;
        float sy = cy + (baseRect.top + curY * baseRect.height() - cy) * zoom + panY;
        panX += dx;
        panY += dy;
        clampPan();
        float bx = cx + (sx - panX - cx) / zoom;
        float by = cy + (sy - panY - cy) / zoom;
        curX = clamp01((bx - baseRect.left) / baseRect.width());
        curY = clamp01((by - baseRect.top) / baseRect.height());
        if (listener != null) listener.point(curX, curY);
    }

    private void drawMinimap(Canvas canvas) {
        float a = mapAlpha();
        if (a <= 0f || !layoutBase()) return;
        float w = dp * 96f, h = w * frame.getHeight() / frame.getWidth(), m = dp * 6f;
        mapRect.set(getWidth() - w - m, m, getWidth() - m, m + h);

        // the pointer would be under it: let it show through
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float px = cx + (baseRect.left + curX * baseRect.width() - cx) * zoom + panX;
        float py = cy + (baseRect.top + curY * baseRect.height() - cy) * zoom + panY;
        float pad = dp * 18f;
        if (px > mapRect.left - pad && px < mapRect.right + pad
                && py > mapRect.top - pad && py < mapRect.bottom + pad) {
            a *= 0.3f;
        }
        int al = (int) (255 * a);
        mapBack.setAlpha(al);
        mapBmp.setAlpha(al);
        mapEdge.setAlpha(al);
        mapView.setAlpha(al);
        mapDot.setAlpha(al);

        canvas.drawRect(mapRect, mapBack);
        canvas.drawBitmap(frame, null, mapRect, mapBmp);
        // the part of the desktop the view shows, as a white box
        float x0 = (-panX - cx) / zoom + cx, y0 = (-panY - cy) / zoom + cy;
        float x1 = (getWidth() - panX - cx) / zoom + cx, y1 = (getHeight() - panY - cy) / zoom + cy;
        float l = mapRect.left + clamp01((x0 - baseRect.left) / baseRect.width()) * w;
        float r = mapRect.left + clamp01((x1 - baseRect.left) / baseRect.width()) * w;
        float t = mapRect.top + clamp01((y0 - baseRect.top) / baseRect.height()) * h;
        float b = mapRect.top + clamp01((y1 - baseRect.top) / baseRect.height()) * h;
        canvas.drawRect(l, t, r, b, mapView);
        canvas.drawCircle(mapRect.left + curX * w, mapRect.top + curY * h, dp * 2f, mapDot);
        canvas.drawRect(mapRect, mapEdge);

        // keep drawing while it fades, and once more when the hold runs out
        long now = android.os.SystemClock.uptimeMillis();
        if (now > mapUntil && !onMap) postInvalidateOnAnimation();
        else if (!onMap) postInvalidateDelayed(mapUntil - now + 16);
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
        touchMap();
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
        followCursor();
        invalidate();
    }

    /** Move the cursor by normalised deltas (from a trackpad drag). */
    void nudge(float dnx, float dny) {
        // With the view zoomed in, a swipe should move the pointer about as far as it moves on
        // screen, not as far across the whole desktop.
        float k = layout == Layout.PAD ? 1f / Math.max(1f, zoom) : 1f;
        curX = clamp01(curX + dnx * k);
        curY = clamp01(curY + dny * k);
        if (listener != null) listener.point(curX, curY);
        followCursor();
        touchMap();
        invalidate();
    }

    /** A tap on the dedicated pad = click where the cursor already is. */
    void tapClick(boolean doubleClick) {
        if (listener != null) listener.click("l", doubleClick);
    }

    /** Click a specific button ("l" / "r" / "m") where the cursor already is. */
    void padClick(String button) {
        if (listener != null) listener.click(button, false);
    }

    void scroll(float amount) {
        if (listener != null) listener.move(0, 0, amount);
    }

    /** Trackpad pinch: send a host zoom gesture (Ctrl+wheel), not a view zoom. */
    void hostZoom(float ticks) {
        if (listener != null) listener.zoom(ticks);
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

    /** One changed rectangle of a partial update, at (x, y) in frame pixels. */
    static final class Tile {
        final int x, y;
        final byte[] jpeg;

        Tile(int x, int y, byte[] jpeg) {
            this.x = x; this.y = y; this.jpeg = jpeg;
        }
    }

    /** A full frame ({@code tiles == null}) or a partial update, as received. */
    private static final class Update {
        final byte[] full;
        final int w, h;
        final List<Tile> tiles;
        final long arrived = android.os.SystemClock.uptimeMillis();

        Update(byte[] full, int w, int h, List<Tile> tiles) {
            this.full = full; this.w = w; this.h = h; this.tiles = tiles;
        }
    }

    /** Received, not yet decoded, in order. Partial updates build on each other
     *  so they're never skipped; a full frame supersedes everything before it. */
    private final ArrayDeque<Update> pending = new ArrayDeque<>();

    void setFrame(byte[] jpeg) {
        enqueue(new Update(jpeg, 0, 0, null));
    }

    void setTiles(int w, int h, List<Tile> tiles) {
        enqueue(new Update(null, w, h, tiles));
    }

    private void enqueue(Update u) {
        if (decoder.isShutdown()) return; // a late frame after release()
        synchronized (pending) {
            if (u.tiles == null) {
                if (!pending.isEmpty()) LatencyStats.record("screen.dropped", pending.size());
                pending.clear();
            }
            pending.add(u);
        }
        kickDecoder();
    }

    private void kickDecoder() {
        synchronized (pending) {
            if (pending.isEmpty()) return;
        }
        if (decoder.isShutdown() || !decoding.compareAndSet(false, true)) return;
        try {
            decoder.execute(this::drainDecode);
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            decoding.set(false);
        }
    }

    private void drainDecode() {
        while (true) {
            Update u;
            synchronized (pending) {
                u = pending.poll();
            }
            if (u == null) break;
            long t0 = android.os.SystemClock.uptimeMillis();
            if (u.tiles == null) {
                Bitmap bmp = decodeFull(u.full);
                LatencyStats.record("screen.decode", android.os.SystemClock.uptimeMillis() - t0);
                if (bmp != null) onDecoded(bmp, u.arrived);
            } else {
                List<Bitmap> parts = new ArrayList<>(u.tiles.size());
                for (Tile t : u.tiles) parts.add(BitmapFactory.decodeByteArray(t.jpeg, 0, t.jpeg.length));
                LatencyStats.record("screen.decode_tiles", android.os.SystemClock.uptimeMillis() - t0);
                onTilesDecoded(u, parts);
            }
        }
        decoding.set(false);
        kickDecoder(); // an update may have arrived in the gap above
    }

    /** Spare frame-sized bitmap to decode the next full frame into, instead of
     *  allocating ~4 MB per frame (the GC pauses behind the decode spikes). */
    private final AtomicReference<Bitmap> spare = new AtomicReference<>();
    /** Frame swapped out on the main thread; becomes {@link #spare} once a
     *  draw no longer references it. */
    private Bitmap retired;

    private Bitmap decodeFull(byte[] jpeg) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inMutable = true; // partial updates paint into it
        Bitmap reuse = spare.getAndSet(null);
        if (reuse != null && !reuse.isRecycled()) {
            o.inBitmap = reuse;
            try {
                Bitmap b = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, o);
                if (b != null) return b;
            } catch (IllegalArgumentException tooSmall) {
                reuse.recycle(); // frame grew: fall through to a fresh bitmap
            }
            o.inBitmap = null;
        }
        return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, o);
    }

    private void onDecoded(Bitmap bmp, long arrived) {
        main.post(() -> {
            // frame off the socket -> handed to the view (decode + main-thread wait)
            LatencyStats.record("screen.to_view", android.os.SystemClock.uptimeMillis() - arrived);
            Bitmap old = frame;
            frame = bmp;
            boolean aspectChanged = old == null
                    || old.getWidth() * bmp.getHeight() != bmp.getWidth() * old.getHeight();
            if (retired != null) retired.recycle();
            retired = old;
            if (aspectLock && aspectChanged) requestLayout();
            invalidate();
        });
    }

    private void onTilesDecoded(Update u, List<Bitmap> parts) {
        main.post(() -> {
            Bitmap f = frame;
            // Only valid on top of the frame they were diffed against.
            if (f != null && f.isMutable() && f.getWidth() == u.w && f.getHeight() == u.h) {
                Canvas c = new Canvas(f);
                for (int i = 0; i < parts.size(); i++) {
                    Bitmap b = parts.get(i);
                    if (b != null) c.drawBitmap(b, u.tiles.get(i).x, u.tiles.get(i).y, null);
                }
                LatencyStats.record("screen.to_view", android.os.SystemClock.uptimeMillis() - u.arrived);
                invalidate();
            }
            for (Bitmap b : parts) if (b != null) b.recycle();
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
        if (retired != null) {
            retired.recycle();
            retired = null;
        }
        Bitmap s = spare.getAndSet(null);
        if (s != null) s.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        if (frame == null) return;
        // Fit (contain): the whole desktop is visible at zoom 1; any leftover space
        // is letterboxed. Zooming in then fills the view.
        layoutBase();
        clampPan(); // keep pan valid as the view resizes (e.g. keyboard opening)

        canvas.save();
        canvas.translate(panX, panY);
        canvas.scale(zoom, zoom, getWidth() / 2f, getHeight() / 2f);
        canvas.drawBitmap(frame, null, baseRect, paint);
        if (retired != null) {
            // this draw no longer uses the swapped-out frame: decode into it next
            Bitmap old = spare.getAndSet(retired);
            if (old != null) old.recycle();
            retired = null;
        }

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
        drawMinimap(canvas);
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
        // A touch that starts on the minimap moves the view to where it points, and keeps doing
        // so while the finger drags across it.
        int act = e.getActionMasked();
        if (onMap || (act == MotionEvent.ACTION_DOWN && mapAlpha() > 0f
                && mapRect.contains(e.getX(), e.getY()))) {
            if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
                onMap = false;
                touchMap();
            } else {
                onMap = true;
                jumpFromMap(e.getX(), e.getY());
                invalidate();
            }
            return true;
        }
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = startX = e.getX();
                lastY = startY = e.getY();
                moved = dragging = twoFinger = false;
                armDrag = false;
                if (relativeMode()) {
                    armDrag = pendingTaps > 0;
                    if (pendingTaps > 0
                            && Math.hypot(startX - lastTapX, startY - lastTapY) >= doubleTapSlop) {
                        removeCallbacks(tapFlush);
                        tapFlush.run();
                        armDrag = false;
                    }
                    postDelayed(longPress, longPressTimeout);
                }
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                twoFinger = true;
                removeCallbacks(longPress);
                if (e.getPointerCount() >= 2) {
                    pinchDist = Math.max(1, dist(e));
                    pinchZoom0 = zoom;
                    twoMode = 0;
                    float cx = getWidth() / 2f, cy = getHeight() / 2f;
                    pinchMidX = (e.getX(0) + e.getX(1)) / 2f;
                    pinchMidY = (e.getY(0) + e.getY(1)) / 2f;
                    lastMidY = pinchMidY;
                    // Content point under the fingers — kept fixed as zoom changes.
                    pinchAnchorX = (pinchMidX - cx - panX) / zoom + cx;
                    pinchAnchorY = (pinchMidY - cy - panY) / zoom + cy;
                }
                lastY = e.getY(0);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (twoFinger && e.getPointerCount() >= 2) {
                    moved = true;
                    float d = Math.max(1, dist(e));
                    float cx = getWidth() / 2f, cy = getHeight() / 2f;
                    float mx = (e.getX(0) + e.getX(1)) / 2f;
                    float my = (e.getY(0) + e.getY(1)) / 2f;
                    // Classify once, after enough travel to tell them apart:
                    // pinch only wins if the spread change clearly dominates the
                    // slide — otherwise it's a scroll (the common case).
                    if (twoMode == 0) {
                        float spread = Math.abs(d - pinchDist);
                        float slide = (float) Math.hypot(mx - pinchMidX, my - pinchMidY);
                        if (Math.max(spread, slide) > twoSlop) {
                            twoMode = (spread > slide * 1.5f && spread > twoSlop * 0.6f) ? 1 : 2;
                        }
                    }
                    if (twoMode == 1) { // pinch-zoom, anchored under the fingers
                        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, pinchZoom0 * d / pinchDist));
                        panX = mx - cx - (pinchAnchorX - cx) * zoom;
                        panY = my - cy - (pinchAnchorY - cy) * zoom;
                        if (zoom <= ZOOM_MIN + 0.001f) {
                            zoom = ZOOM_MIN;
                            panX = panY = 0;
                        }
                        clampPan();
                        touchMap();
                        invalidate();
                    } else if (twoMode == 2) { // two-finger drag = mouse wheel
                        scroll(-(my - lastMidY) / 3f);
                        lastMidY = my;
                    }
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
                    if (armDrag && !dragging) {   // tap(s)-then-drag -> press-drag, discard the clicks
                        removeCallbacks(tapFlush);
                        pendingTaps = 0;
                        dragging = true;
                        hold(true);
                    }
                }
                if (!moved) return true;
                if (relativeMode()) {
                    nudge(dx * MOVE_GAIN / getWidth(), dy * MOVE_GAIN / getWidth());
                } else {
                    if (layout == Layout.PAD) {
                        panWithPointer(dx, dy);
                    } else {
                        panX += dx;
                        panY += dy;
                        clampPan();
                    }
                    touchMap();
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                removeCallbacks(longPress);
                if (dragging) {
                    hold(false);
                    dragging = false;
                    removeCallbacks(tapFlush);
                    pendingTaps = 0;
                } else if (!moved && !twoFinger) {
                    float[] nf = toFrame(e.getX(), e.getY());
                    boolean onImage = nf[0] >= 0 && nf[0] <= 1 && nf[1] >= 0 && nf[1] <= 1;
                    if (onImage) {
                        pointAt(clamp01(nf[0]), clamp01(nf[1]));  // move the cursor now
                        if (relativeMode()) {
                            // Queue the tap; fires only if no press-drag follows.
                            lastTapX = e.getX();
                            lastTapY = e.getY();
                            pendingTaps++;
                            removeCallbacks(tapFlush);
                            postDelayed(tapFlush, DRAG_GRACE_MS);
                        } else {
                            listener.click("l", false); // VIEW/PAD: point + click now
                        }
                    }
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
