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

    static Drawable folder(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float u = b.width();
                float l = b.left + u * 0.12f;
                float r = b.left + u * 0.88f;
                float top = b.top + u * 0.28f;
                float bot = b.top + u * 0.80f;
                float tabTop = b.top + u * 0.20f;
                float tabRight = b.left + u * 0.44f;
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(l, bot);
                path.lineTo(l, tabTop);
                path.lineTo(tabRight, tabTop);
                path.lineTo(tabRight + u * 0.10f, top);
                path.lineTo(r, top);
                path.lineTo(r, bot);
                path.close();
                canvas.drawPath(path, p);
            }
        };
    }

    static Drawable file(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float u = b.width();
                float l = b.left + u * 0.20f;
                float r = b.left + u * 0.80f;
                float top = b.top + u * 0.12f;
                float bot = b.top + u * 0.88f;
                float fold = u * 0.22f;
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(l, top);
                path.lineTo(r - fold, top);
                path.lineTo(r, top + fold);
                path.lineTo(r, bot);
                path.lineTo(l, bot);
                path.close();
                canvas.drawPath(path, p);
                canvas.drawLine(r - fold, top, r - fold, top + fold, p);
                canvas.drawLine(r - fold, top + fold, r, top + fold, p);
            }
        };
    }

    /** An icon by name, for menus built from data: open, apps, share, copy, folder, call, message, link, check, chevron. */
    static Drawable byName(String name, int sizePx, int color) {
        if (name == null) return null;
        switch (name) {
            case "folder": return folder(sizePx, color);
            case "file": return file(sizePx, color);
            case "open": return open(sizePx, color);
            case "apps": return apps(sizePx, color);
            case "share": return share(sizePx, color);
            case "copy": return copy(sizePx, color);
            case "call": return call(sizePx, color);
            case "message": return message(sizePx, color);
            case "link": return link(sizePx, color);
            case "check": return check(sizePx, color);
            case "chevron": return chevron(sizePx, color);
            default: return null;
        }
    }

    static Drawable open(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float u = b.width();
                android.graphics.Path box = new android.graphics.Path();
                box.moveTo(b.left + u * 0.42f, b.top + u * 0.24f);
                box.lineTo(b.left + u * 0.22f, b.top + u * 0.24f);
                box.lineTo(b.left + u * 0.22f, b.top + u * 0.78f);
                box.lineTo(b.left + u * 0.76f, b.top + u * 0.78f);
                box.lineTo(b.left + u * 0.76f, b.top + u * 0.58f);
                canvas.drawPath(box, p);
                canvas.drawLine(b.left + u * 0.46f, b.top + u * 0.54f, b.left + u * 0.80f, b.top + u * 0.20f, p);
                android.graphics.Path head = new android.graphics.Path();
                head.moveTo(b.left + u * 0.56f, b.top + u * 0.20f);
                head.lineTo(b.left + u * 0.80f, b.top + u * 0.20f);
                head.lineTo(b.left + u * 0.80f, b.top + u * 0.44f);
                canvas.drawPath(head, p);
            }
        };
    }

    static Drawable apps(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float u = b.width();
                float s = u * 0.22f;
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        float l = b.left + u * (0.22f + i * 0.32f), t = b.top + u * (0.22f + j * 0.32f);
                        canvas.drawRoundRect(l, t, l + s, t + s, u * 0.04f, u * 0.04f, p);
                    }
                }
            }
        };
    }

    static Drawable share(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float u = b.width();
                float r = u * 0.09f;
                float x1 = b.left + u * 0.26f, y1 = b.top + u * 0.50f;
                float x2 = b.left + u * 0.74f, y2 = b.top + u * 0.24f;
                float x3 = b.left + u * 0.74f, y3 = b.top + u * 0.76f;
                canvas.drawCircle(x1, y1, r, p);
                canvas.drawCircle(x2, y2, r, p);
                canvas.drawCircle(x3, y3, r, p);
                canvas.drawLine(x1 + r, y1 - r * 0.5f, x2 - r, y2 + r * 0.5f, p);
                canvas.drawLine(x1 + r, y1 + r * 0.5f, x3 - r, y3 - r * 0.5f, p);
            }
        };
    }

    static Drawable copy(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float u = b.width();
                canvas.drawRoundRect(b.left + u * 0.36f, b.top + u * 0.36f, b.left + u * 0.80f, b.top + u * 0.80f, u * 0.07f, u * 0.07f, p);
                android.graphics.Path back = new android.graphics.Path();
                back.moveTo(b.left + u * 0.30f, b.top + u * 0.64f);
                back.lineTo(b.left + u * 0.24f, b.top + u * 0.64f);
                back.lineTo(b.left + u * 0.20f, b.top + u * 0.60f);
                back.lineTo(b.left + u * 0.20f, b.top + u * 0.24f);
                back.lineTo(b.left + u * 0.24f, b.top + u * 0.20f);
                back.lineTo(b.left + u * 0.60f, b.top + u * 0.20f);
                back.lineTo(b.left + u * 0.64f, b.top + u * 0.24f);
                back.lineTo(b.left + u * 0.64f, b.top + u * 0.30f);
                canvas.drawPath(back, p);
            }
        };
    }

    static Drawable call(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float u = b.width();
                android.graphics.Path h = new android.graphics.Path();
                h.moveTo(b.left + u * 0.26f, b.top + u * 0.20f);
                h.lineTo(b.left + u * 0.40f, b.top + u * 0.20f);
                h.lineTo(b.left + u * 0.47f, b.top + u * 0.40f);
                h.lineTo(b.left + u * 0.38f, b.top + u * 0.47f);
                h.quadTo(b.left + u * 0.47f, b.top + u * 0.64f, b.left + u * 0.62f, b.top + u * 0.70f);
                h.lineTo(b.left + u * 0.70f, b.top + u * 0.60f);
                h.lineTo(b.left + u * 0.80f, b.top + u * 0.66f);
                h.lineTo(b.left + u * 0.80f, b.top + u * 0.78f);
                h.quadTo(b.left + u * 0.50f, b.top + u * 0.88f, b.left + u * 0.20f, b.top + u * 0.34f);
                h.close();
                canvas.drawPath(h, p);
            }
        };
    }

    static Drawable message(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float u = b.width();
                android.graphics.Path m = new android.graphics.Path();
                m.moveTo(b.left + u * 0.18f, b.top + u * 0.22f);
                m.lineTo(b.left + u * 0.82f, b.top + u * 0.22f);
                m.lineTo(b.left + u * 0.82f, b.top + u * 0.64f);
                m.lineTo(b.left + u * 0.46f, b.top + u * 0.64f);
                m.lineTo(b.left + u * 0.28f, b.top + u * 0.80f);
                m.lineTo(b.left + u * 0.28f, b.top + u * 0.64f);
                m.lineTo(b.left + u * 0.18f, b.top + u * 0.64f);
                m.close();
                canvas.drawPath(m, p);
            }
        };
    }

    static Drawable link(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float u = b.width();
                canvas.save();
                canvas.rotate(-45f, b.exactCenterX(), b.exactCenterY());
                canvas.drawRoundRect(b.left + u * 0.10f, b.top + u * 0.38f, b.left + u * 0.56f, b.top + u * 0.62f, u * 0.12f, u * 0.12f, p);
                canvas.drawRoundRect(b.left + u * 0.44f, b.top + u * 0.38f, b.left + u * 0.90f, b.top + u * 0.62f, u * 0.12f, u * 0.12f, p);
                canvas.restore();
            }
        };
    }

    static Drawable check(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float u = b.width();
                canvas.drawLine(b.left + u * 0.20f, b.top + u * 0.52f, b.left + u * 0.42f, b.top + u * 0.72f, p);
                canvas.drawLine(b.left + u * 0.42f, b.top + u * 0.72f, b.left + u * 0.82f, b.top + u * 0.28f, p);
            }
        };
    }

    static Drawable chevron(int sizePx, int color) {
        return new LineIcon(sizePx, color) {
            @Override
            void paint(Canvas canvas, Rect b, Paint p) {
                float u = b.width();
                canvas.drawLine(b.left + u * 0.38f, b.top + u * 0.24f, b.left + u * 0.64f, b.top + u * 0.50f, p);
                canvas.drawLine(b.left + u * 0.64f, b.top + u * 0.50f, b.left + u * 0.38f, b.top + u * 0.76f, p);
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
