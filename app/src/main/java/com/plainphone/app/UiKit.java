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

