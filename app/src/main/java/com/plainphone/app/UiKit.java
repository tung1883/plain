package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.Button;
import android.widget.EditText;

class UiKit {

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

