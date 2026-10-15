package com.plainphone.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

/** Small shared pieces for the keyboard's clipboard, emoji and translate panels. */
final class PanelKit {

    static final int FG = 0xFFB5B5B5, DIM = 0xFF5A5A5A, LINE = 0xFF242424, KEY = 0xFF121212, DOWN = 0xFF262626;

    private PanelKit() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("kb_panels", Context.MODE_PRIVATE);
    }

    static int dp(Context c, float v) {
        return UiKit.dp(c, v);
    }

    /** A rounded text button; {@code on} draws it white. */
    static TextView chip(Context c, String text, boolean on, View.OnClickListener click) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(12);
        t.setTypeface(Fonts.cascadiaMono(c));
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setPadding(dp(c, 10), dp(c, 6), dp(c, 10), dp(c, 6));
        if (on) {
            t.setTextColor(Color.BLACK);
            t.setBackground(UiKit.pressable(c, Color.WHITE, 0xFFCCCCCC, Color.WHITE, 1f, 8));
        } else {
            t.setTextColor(FG);
            t.setBackground(UiKit.pressable(c, KEY, DOWN, LINE, 1f, 8));
        }
        t.setOnClickListener(click);
        return t;
    }

    /** A dim, letter-spaced caption. */
    static TextView caption(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(10);
        t.setLetterSpacing(0.2f);
        t.setTextColor(DIM);
        t.setTypeface(Fonts.cascadiaMono(c));
        return t;
    }

    /** A view that draws one {@link KeyIcons} icon. */
    static final class IconView extends View {
        private int kind;
        private boolean active, fill;

        IconView(Context c, int kind) {
            super(c);
            this.kind = kind;
        }

        void setActive(boolean on) {
            active = on;
            setBackground(on ? UiKit.rounded(getContext(), Color.WHITE, Color.WHITE, 0f, 8) : null);
            invalidate();
        }

        void setFill(boolean on) {
            fill = on;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int color = active ? Color.BLACK : fill ? Color.WHITE : FG;
            float s = Math.min(getWidth(), getHeight()) * 0.34f;
            if (kind == KeyIcons.KEYS) {
                drawAbc(canvas, color, s);
                return;
            }
            KeyIcons.draw(canvas, kind, getWidth() / 2f, getHeight() / 2f, s, color, fill);
        }

        /** "ABC" as three Cascadia Mono letters: A low on the left, B above C on the right. */
        private void drawAbc(Canvas canvas, int color, float s) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setTypeface(Fonts.cascadiaMono(getContext()));
            p.setColor(color);
            p.setFakeBoldText(true);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            letter(canvas, p, "A", cx - s * 0.6f, cy + s * 0.3f, s * 1.4f);
            letter(canvas, p, "B", cx + s * 0.12f, cy - s * 0.5f, s * 1.4f);
            letter(canvas, p, "C", cx + s * 0.62f, cy + s * 0.6f, s * 1.4f);
        }

        /** Draw one letter centred on (x, y). */
        private static void letter(Canvas canvas, Paint p, String ch, float x, float y, float size) {
            p.setTextSize(size);
            Rect r = new Rect();
            p.getTextBounds(ch, 0, 1, r);
            canvas.drawText(ch, x - r.exactCenterX(), y - r.exactCenterY(), p);
        }
    }
}
