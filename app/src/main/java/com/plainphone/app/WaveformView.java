package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * Vertical amplitude bars (0–100 peaks), centred, hard edges.
 *
 * <ul>
 *   <li><b>Live</b> — call {@link #pushLevel} on a tick; a ring buffer scrolls
 *       left, newest bar on the right.</li>
 *   <li><b>Static</b> — {@link #setEnvelope} + {@link #setProgress}; bars left of
 *       the playhead are brighter, drag to seek ({@link #setOnSeek}).</li>
 * </ul>
 */
class WaveformView extends View {

    interface OnSeek {
        void seekTo(float fraction);
    }

    private static final int LIVE_BARS = 56;
    private final Paint dim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lit = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint head = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int[] ring = new int[LIVE_BARS];
    private int ringLen;

    private int[] envelope = new int[0];
    private boolean staticMode;
    private float progress;
    private OnSeek onSeek;

    WaveformView(Context context) {
        super(context);
        dim.setColor(0xFF4A4A4A);
        lit.setColor(0xFFDCDCDC);
        head.setColor(Color.WHITE);
    }

    void pushLevel(int level) {
        staticMode = false;
        if (ringLen < LIVE_BARS) {
            ring[ringLen++] = clamp(level);
        } else {
            System.arraycopy(ring, 1, ring, 0, LIVE_BARS - 1);
            ring[LIVE_BARS - 1] = clamp(level);
        }
        invalidate();
    }

    void setEnvelope(int[] peaks) {
        this.envelope = peaks == null ? new int[0] : peaks;
        this.staticMode = true;
        invalidate();
    }

    void setProgress(float fraction) {
        this.progress = Math.max(0f, Math.min(1f, fraction));
        if (staticMode) invalidate();
    }

    void setOnSeek(OnSeek listener) {
        this.onSeek = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!staticMode || onSeek == null) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                float f = getWidth() == 0 ? 0 : event.getX() / getWidth();
                setProgress(f);
                onSeek.seekTo(Math.max(0f, Math.min(1f, f)));
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        int[] data = staticMode ? envelope : liveData();
        if (data.length == 0) return;

        float slot = w / (float) data.length;
        float barW = Math.max(2f, slot * 0.6f);
        float mid = h / 2f;
        int cut = staticMode ? Math.round(data.length * progress) : data.length;

        for (int i = 0; i < data.length; i++) {
            float cx = i * slot + slot / 2f;
            float half = Math.max(1.5f, (data[i] / 100f) * (h / 2f - 2f));
            canvas.drawRect(cx - barW / 2f, mid - half, cx + barW / 2f, mid + half,
                    i < cut ? lit : dim);
        }

        if (staticMode) {
            float x = w * progress;
            canvas.drawRect(x - 1f, 0, x + 1f, h, head);
        }
    }

    private int[] liveData() {
        int[] out = new int[LIVE_BARS];
        int pad = LIVE_BARS - ringLen;
        for (int i = 0; i < ringLen; i++) out[pad + i] = ring[i];
        return out;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
