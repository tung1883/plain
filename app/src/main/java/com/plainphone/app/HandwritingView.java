package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import com.google.mlkit.vision.digitalink.Ink;

import java.util.ArrayList;
import java.util.List;

/** A pad that records pen strokes as an ML Kit {@link Ink} and draws them. */
final class HandwritingView extends View {

    interface Listener {
        void onStrokeStart();

        void onStrokeEnd();
    }

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Path> done = new ArrayList<>();
    private Path current;
    private Ink.Builder ink = Ink.builder();
    private Ink.Stroke.Builder stroke;
    private int strokes;
    private Listener listener;

    HandwritingView(Context ctx) {
        super(ctx);
        setBackground(UiKit.rounded(ctx, PanelKit.KEY, PanelKit.LINE, 1f, 8));
        line.setColor(0xFFFFFFFF);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(PanelKit.dp(ctx, 3));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        hint.setColor(PanelKit.DIM);
        hint.setTextAlign(Paint.Align.CENTER);
        hint.setTextSize(PanelKit.dp(ctx, 12));
        hint.setTypeface(Fonts.cascadiaMono(ctx));
    }

    void setListener(Listener l) {
        listener = l;
    }

    boolean isEmpty() {
        return strokes == 0;
    }

    Ink ink() {
        return ink.build();
    }

    void clear() {
        done.clear();
        current = null;
        ink = Ink.builder();
        stroke = null;
        strokes = 0;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX(), y = ev.getY();
        long t = SystemClock.uptimeMillis();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                current = new Path();
                current.moveTo(x, y);
                stroke = Ink.Stroke.builder();
                stroke.addPoint(Ink.Point.create(x, y, t));
                if (listener != null) listener.onStrokeStart();
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (current == null) return true;
                for (int i = 0; i < ev.getHistorySize(); i++) {
                    float hx = ev.getHistoricalX(i), hy = ev.getHistoricalY(i);
                    current.lineTo(hx, hy);
                    stroke.addPoint(Ink.Point.create(hx, hy, ev.getHistoricalEventTime(i)));
                }
                current.lineTo(x, y);
                stroke.addPoint(Ink.Point.create(x, y, t));
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (current == null) return true;
                current.lineTo(x, y);
                stroke.addPoint(Ink.Point.create(x, y, t));
                ink.addStroke(stroke.build());
                done.add(current);
                current = null;
                stroke = null;
                strokes++;
                invalidate();
                if (listener != null) listener.onStrokeEnd();
                return true;
            default:
                return true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (strokes == 0 && current == null) {
            canvas.drawText("write here", getWidth() / 2f, getHeight() / 2f, hint);
        }
        for (Path p : done) canvas.drawPath(p, line);
        if (current != null) canvas.drawPath(current, line);
    }
}
