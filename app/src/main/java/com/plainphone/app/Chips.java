package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.IntConsumer;

/** A scrolling row of rounded chips; the ones that are on are white. */
final class Chips extends HorizontalScrollView {

    private final LinearLayout row;
    private final IntConsumer onTap;
    private final Typeface typeface;
    private TextView[] views = new TextView[0];

    /** @param onTap gets the index of the chip that was tapped */
    Chips(Context context, Typeface typeface, IntConsumer onTap) {
        super(context);
        this.onTap = onTap;
        this.typeface = typeface;
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(48, UiKit.dp(context, 6), 48, UiKit.dp(context, 10));
        addView(row, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /** Show these labels; {@code on[i]} marks the chips that are active. */
    void set(String[] labels, boolean[] on) {
        Context c = getContext();
        if (views.length != labels.length) {
            row.removeAllViews();
            views = new TextView[labels.length];
            for (int i = 0; i < labels.length; i++) {
                final int index = i;
                TextView t = new TextView(c);
                t.setTextSize(13);
                t.setTypeface(typeface);
                t.setSingleLine(true);
                t.setGravity(Gravity.CENTER);
                t.setPadding(UiKit.dp(c, 12), 0, UiKit.dp(c, 12), 0);
                t.setOnClickListener(v -> onTap.accept(index));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, UiKit.dp(c, 30));
                lp.rightMargin = UiKit.dp(c, 6);
                row.addView(t, lp);
                views[i] = t;
            }
        }
        for (int i = 0; i < labels.length; i++) {
            views[i].setText(labels[i]);
            boolean active = on[i];
            views[i].setTextColor(active ? Color.BLACK : 0xFFB5B5B5);
            views[i].setBackground(active
                    ? UiKit.rounded(c, Color.WHITE, Color.WHITE, 1f, 15)
                    : UiKit.pressable(c, Color.BLACK, 0xFF1A1A1A, 0xFF2C2C2C, 1f, 15));
        }
    }
}
