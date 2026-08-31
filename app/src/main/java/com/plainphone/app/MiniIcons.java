package com.plainphone.app;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Tiny hand-drawn line icons — a magnifier and a close cross — for the search field. */
class MiniIcons {

    private MiniIcons() {}

    static Drawable magnifier(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float unit = b.width();
                float cx = b.left + unit * 0.40f;
                float cy = b.top + unit * 0.40f;
                float r = unit * 0.26f;
                canvas.drawCircle(cx, cy, r, p);
                float d = r * 0.7071f;
                canvas.drawLine(cx + d, cy + d, b.left + unit * 0.92f, b.top + unit * 0.92f, p);
            }
        };
    }

    static Drawable cross(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float unit = b.width();
                float lo = unit * 0.28f;
                float hi = unit * 0.72f;
                canvas.drawLine(b.left + lo, b.top + lo, b.left + hi, b.top + hi, p);
                canvas.drawLine(b.left + hi, b.top + lo, b.left + lo, b.top + hi, p);
            }
        };
    }

    private abstract static class LineIcon extends Drawable {
        private final int size;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        LineIcon(int size, int color) {
            this.size = size;
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(Math.max(2f, size * 0.08f));
        }

        abstract void paint(Canvas canvas, Rect bounds, Paint paint);

        @Override
        public void draw(Canvas canvas) {
            paint(canvas, getBounds(), paint);
        }

        @Override
        public int getIntrinsicWidth() {
            return size;
        }

        @Override
        public int getIntrinsicHeight() {
            return size;
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
