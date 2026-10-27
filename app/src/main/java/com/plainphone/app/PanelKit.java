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

    static final int FG = 0xFFD2D2D2, DIM = 0xFF666666, LINE = 0xFF242424, KEY = 0xFF161616, DOWN = 0xFF2B2B2B;

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
        centerText(t);
        t.setPadding(dp(c, 10), dp(c, 6), dp(c, 10), dp(c, 7));
        styleChip(c, t, on);
        t.setOnClickListener(click);
        return t;
    }

    /**
     * Centre a label in its box. Cascadia Mono's line box is much taller above the baseline than
     * below (win metrics), so with the default font padding text sits low; without it the line is
     * centred on the capitals, and the extra bottom padding set by the callers lifts lowercase to
     * the visual middle.
     */
    static void centerText(TextView t) {
        t.setIncludeFontPadding(false);
        t.setGravity(Gravity.CENTER);
    }

    /** Switch a chip between the white (primary) and dark look. */
    static void styleChip(Context c, TextView t, boolean on) {
        if (on) {
            t.setTextColor(Color.BLACK);
            t.setBackground(UiKit.pressable(c, Color.WHITE, 0xFFBBBBBB, Color.WHITE, 1f, 9));
        } else {
            t.setTextColor(FG);
            t.setBackground(UiKit.pressable(c, KEY, DOWN, LINE, 1f, 9));
        }
    }

    /** A smaller chip for panel headers. */
    static TextView chipSmall(Context c, String text, boolean on, View.OnClickListener click) {
        TextView t = chip(c, text, on, click);
        t.setTextSize(11);
        t.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 5));
        return t;
    }

    /** A quiet text button for panel headers (Clear ...). */
    static TextView link(Context c, String text, View.OnClickListener click) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(11);
        t.setTextColor(DIM);
        t.setTypeface(Fonts.cascadiaMono(c));
        t.setPadding(dp(c, 10), dp(c, 6), dp(c, 10), dp(c, 6));
        t.setOnClickListener(click);
        return t;
    }

    /** A rounded card background used for panel content. */
    static android.graphics.drawable.Drawable card(Context c) {
        return UiKit.rounded(c, KEY, LINE, 1f, 12);
    }

    /** A row of options in one rounded frame; the selected one is white. */
    static android.widget.LinearLayout segmented(Context c, String[] labels, int selected,
                                                 java.util.function.IntConsumer pick) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(c);
        row.setBackground(UiKit.rounded(c, KEY, LINE, 1f, 9));
        row.setPadding(dp(c, 2), dp(c, 2), dp(c, 2), dp(c, 2));
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView t = new TextView(c);
            t.setText(labels[i]);
            t.setTextSize(11.5f);
            t.setTypeface(Fonts.cascadiaMono(c));
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(c, 11), dp(c, 6), dp(c, 11), dp(c, 6));
            boolean on = i == selected;
            t.setTextColor(on ? Color.BLACK : 0xFF8A8A8A);
            if (on) t.setBackground(UiKit.rounded(c, Color.WHITE, Color.WHITE, 0f, 7));
            t.setOnClickListener(v -> pick.accept(idx));
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.leftMargin = dp(c, 4);   // a gap between the options
            row.addView(t, lp);
        }
        return row;
    }

    /** An on/off pill switch; white when on. {@code change} gets the new state. */
    static View toggle(Context c, boolean on, java.util.function.Consumer<Boolean> change) {
        final boolean[] state = {on};
        View v = new View(c) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(Canvas canvas) {
                float w = getWidth(), h = getHeight(), r = h / 2f;
                p.setColor(state[0] ? Color.WHITE : 0xFF2A2A2A);
                canvas.drawRoundRect(0, 0, w, h, r, r, p);
                p.setColor(state[0] ? Color.BLACK : 0xFF777777);
                float cx = state[0] ? w - r : r;
                canvas.drawCircle(cx, r, r - dp(getContext(), 3), p);
            }
        };
        v.setMinimumWidth(dp(c, 34));
        v.setOnClickListener(x -> {
            state[0] = !state[0];
            x.invalidate();
            change.accept(state[0]);
        });
        v.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp(c, 34), dp(c, 20)));
        return v;
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
        private boolean active, fill, tab, tabOn;
        private float iconScale = 0.34f;   // icon half-size as a fraction of the view's short side

        IconView(Context c, int kind) {
            super(c);
            this.kind = kind;
        }

        void setActive(boolean on) {
            active = on;
            setBackground(on ? UiKit.rounded(getContext(), Color.WHITE, Color.WHITE, 0f, 9) : null);
            invalidate();
        }

        /** Tab look: dim icon; the selected tab is white on a key fill with a white underline. */
        void setTab(boolean on) {
            tab = true;
            tabOn = on;
            if (on) {
                android.graphics.drawable.LayerDrawable d = new android.graphics.drawable.LayerDrawable(
                        new android.graphics.drawable.Drawable[]{
                                UiKit.rounded(getContext(), KEY, KEY, 0f, 8),
                                new android.graphics.drawable.ColorDrawable(Color.WHITE)});
                d.setLayerInset(1, dp(getContext(), 10), dp(getContext(), 28), dp(getContext(), 10), 0);
                setBackground(d);
            } else {
                setBackground(null);
            }
            invalidate();
        }

        void setIconScale(float scale) {
            iconScale = scale;
            invalidate();
        }

        void setFill(boolean on) {
            fill = on;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int color = tab ? (tabOn ? Color.WHITE : 0xFF7A7A7A) : active ? Color.BLACK : fill ? Color.WHITE : FG;
            float s = Math.min(getWidth(), getHeight()) * iconScale;
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
