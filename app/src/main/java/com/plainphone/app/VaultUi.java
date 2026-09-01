package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared plainphone-style popups for the vault screens (black box, mono, no chrome). */
final class VaultUi {

    private VaultUi() {}

    interface Choice {
        void run();
    }

    static void confirm(Activity host, String title, String message,
                        String okLabel, Choice onOk, String cancelLabel, Choice onCancel) {
        Typeface font = Fonts.current(host);
        LinearLayout box = new LinearLayout(host);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.BLACK);
        box.setBackground(bg);
        box.setPadding(0, 24, 0, 8);

        TextView titleView = new TextView(host);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(18);
        titleView.setTypeface(font);
        titleView.setPadding(48, 8, 48, 16);
        box.addView(titleView);

        if (message != null) {
            TextView body = new TextView(host);
            body.setText(message);
            body.setTextColor(Color.GRAY);
            body.setTextSize(14);
            body.setTypeface(font);
            body.setPadding(48, 0, 48, 16);
            box.addView(body);
        }

        AlertDialog dialog = new AlertDialog.Builder(host).setView(box).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        box.addView(option(host, font, okLabel, () -> {
            dialog.dismiss();
            if (onOk != null) onOk.run();
        }));
        box.addView(option(host, font, cancelLabel, () -> {
            dialog.dismiss();
            if (onCancel != null) onCancel.run();
        }));

        dialog.show();
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (host.getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private static TextView option(Activity host, Typeface font, String label, Choice onClick) {
        TextView view = new TextView(host);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20);
        view.setTypeface(font);
        view.setPadding(48, 28, 48, 28);
        view.setGravity(Gravity.START);
        StateListDrawable rb = new StateListDrawable();
        rb.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        rb.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        view.setBackground(rb);
        view.setOnClickListener(v -> onClick.run());
        return view;
    }
}
