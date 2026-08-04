package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.Button;
import android.widget.EditText;

/** Flat black/white button and input styling, consistent with the rest of the plain UI. */
class UiKit {

    static void style(Context context, Button button) {
        button.setTextColor(Color.WHITE);
        button.setTypeface(Fonts.georgia(context));
        button.setAllCaps(false);
        button.setPadding(48, 28, 48, 28);
        button.setBackground(buttonBackground());
    }

    static void style(Context context, EditText input) {
        input.setTextColor(Color.WHITE);
        input.setTypeface(Fonts.georgia(context));
        input.setPadding(32, 20, 32, 20);
        input.setBackground(inputBackground());
    }

    private static GradientDrawable inputBackground() {
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.BLACK);
        box.setStroke(3, Color.WHITE);
        return box;
    }

    /**
     * A border-only drawable (transparent center) — meant to sit as a View's foreground so
     * it draws on top of full-bleed content instead of being painted over by it, unlike a
     * background drawable which draws first and gets covered.
     */
    static GradientDrawable frameBorder() {
        GradientDrawable frame = new GradientDrawable();
        frame.setColor(Color.TRANSPARENT);
        frame.setStroke(6, Color.WHITE);
        return frame;
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
