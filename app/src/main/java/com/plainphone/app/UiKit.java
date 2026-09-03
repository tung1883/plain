package com.plainphone.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

class UiKit {

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** The left inset of body text on plain screens — the arrow lines up with it. */
    static final int BODY_INSET_PX = 48;

    /** One back affordance for every sub-screen: the "←" glyph flush with body text
     *  (the same arrow the PIN keypad uses for backspace). Same size everywhere. */
    static android.widget.TextView backButton(Context c, Runnable onClick) {
        android.widget.TextView b = new android.widget.TextView(c);
        b.setText("←");
        b.setTextColor(Color.WHITE);
        b.setTextSize(24);
        b.setTypeface(Fonts.current(c));
        b.setGravity(android.view.Gravity.CENTER_VERTICAL);
        b.setIncludeFontPadding(false);
        b.setPadding(BODY_INSET_PX - dp(c, 1), 0, dp(c, 12), 0);
        StateListDrawable press = new StateListDrawable();
        press.addState(new int[]{android.R.attr.state_pressed},
                new android.graphics.drawable.ColorDrawable(Color.DKGRAY));
        press.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        b.setBackground(press);
        b.setOnClickListener(v -> onClick.run());
        return b;
    }

    static int backWidth(Context c) { return BODY_INSET_PX + dp(c, 44); }
    static int backHeight(Context c) { return dp(c, 52); }

    /** A "← Title" bar: left arrow (calls {@code onBackPressed()}) + screen title.
     *  The arrow's tip sits at {@link #BODY_INSET_PX}, flush with row text. */
    static android.widget.LinearLayout header(android.app.Activity a, String title) {
        android.widget.LinearLayout bar = new android.widget.LinearLayout(a);
        bar.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        bar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.BLACK);
        bar.setMinimumHeight(dp(a, 56));

        bar.addView(backButton(a, a::onBackPressed),
                new android.widget.LinearLayout.LayoutParams(backWidth(a), backHeight(a)));

        android.widget.TextView t = new android.widget.TextView(a);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(19);
        t.setTypeface(Fonts.current(a));
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setPadding(dp(a, 4), 0, dp(a, 16), 0);
        bar.addView(t, new android.widget.LinearLayout.LayoutParams(
                0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return bar;
    }

    /** Wrap {@code content} under a "← Title" bar + hairline and set it as the content view. */
    static void screen(android.app.Activity a, String title, View content) {
        android.widget.LinearLayout col = new android.widget.LinearLayout(a);
        col.setOrientation(android.widget.LinearLayout.VERTICAL);
        col.setBackgroundColor(Color.BLACK);
        col.addView(header(a, title), new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        View hair = new View(a);
        hair.setBackgroundColor(0xFF1C1C1C);
        col.addView(hair, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1));
        col.addView(content, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        a.setContentView(col);
    }

    /**
     * A white circular indeterminate spinner that actually animates. Built against
     * the Material theme so it uses the thin rotating-arc drawable rather than the
     * flat holo asset the app's legacy {@code Theme.Black} would hand out; tinted
     * white via {@code indeterminateTintList} (a colour filter freezes it).
     */
    static ProgressBar spinner(Context context) {
        ProgressBar bar = new ProgressBar(
                new ContextThemeWrapper(context, android.R.style.Theme_Material),
                null, android.R.attr.progressBarStyleLarge);
        bar.setIndeterminate(true);
        bar.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
        return bar;
    }

    private static final int IMMERSIVE_FLAGS =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    /** Hide the status and navigation bars for a full-screen gate. Call again on focus. */
    static void hideSystemBars(android.app.Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
    }

    static void hideSystemBars(View decorView) {
        decorView.setSystemUiVisibility(IMMERSIVE_FLAGS);
    }

    static void style(Context context, Button button) {
        button.setTextColor(Color.WHITE);
        button.setTypeface(Fonts.current(context));
        button.setAllCaps(false);
        button.setPadding(48, 28, 48, 28);
        button.setBackground(buttonBackground());
    }

    static void style(Context context, EditText input) {
        input.setTextColor(Color.WHITE);
        input.setTypeface(Fonts.current(context));
        input.setPadding(32, 20, 32, 20);
        input.setBackground(inputBackground());
    }

    private static GradientDrawable inputBackground() {
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.BLACK);
        box.setStroke(3, Color.WHITE);
        return box;
    }

    static GradientDrawable frameBorder() {
        GradientDrawable frame = new GradientDrawable();
        frame.setColor(Color.TRANSPARENT);
        frame.setStroke(6, Color.WHITE);
        return frame;
    }

    static GradientDrawable dialogBackground() {
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.BLACK);
        return box;
    }

    static void clearDialogChrome(android.app.AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    Color.TRANSPARENT));
        }
    }

    private static StateListDrawable buttonBackground() {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(Color.BLACK);
        normal.setStroke(3, Color.WHITE);

        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(Color.DKGRAY);
        pressed.setStroke(3, Color.WHITE);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{}, normal);
        return states;
    }
}

