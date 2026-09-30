package com.plainphone.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** The plain settings-screen look: black, white label with a grey value, grey subtitle. */
final class SettingsUi {

    private SettingsUi() {}

    /** A "← Title" screen with a scrolling column; returns the column to add rows to. */
    static LinearLayout scrollScreen(Activity activity, String title) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        ScrollView scroller = new ScrollView(activity);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(activity, title, scroller);
        return root;
    }

    /**
     * "Label ..... value" with an optional grey subtitle under it. A null listener makes the row
     * inert; {@code dim} greys it out.
     */
    static View row(Context context, String label, String value, String subtitle, boolean dim,
                    View.OnClickListener listener) {
        return row(context, label, value, Color.GRAY, subtitle, dim, listener);
    }

    /** Same, with the value in {@code valueColor} (white for a value that is a live choice). */
    static View row(Context context, String label, String value, int valueColor, String subtitle,
                    boolean dim, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(48, 28, 48, 28);
        if (dim) row.setAlpha(0.4f);

        if (listener != null) {
            StateListDrawable bg = new StateListDrawable();
            bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
            bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
            row.setBackground(bg);
            row.setOnClickListener(listener);
        }

        android.graphics.Typeface font = Fonts.current(context);

        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText(label);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(font);
        head.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (value != null) {
            TextView v = new TextView(context);
            v.setText(value);
            v.setTextColor(valueColor);
            v.setTextSize(16);
            v.setTypeface(font);
            head.addView(v);
        }
        row.addView(head);

        if (subtitle != null) {
            TextView detail = new TextView(context);
            detail.setText(subtitle);
            detail.setTextColor(Color.GRAY);
            detail.setTextSize(13);
            detail.setTypeface(font);
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dp.topMargin = 5;
            row.addView(detail, dp);
        }
        return row;
    }
}
