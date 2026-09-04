package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The "Re-lock after" chooser — one dialog reused for the global default and for a
 * per-lock override. Values are seconds; {@link Config} stores them under
 * {@code relock_seconds} / {@code relock_seconds_<id>}.
 */
final class RelockPicker {

    private RelockPicker() {}

    static final int[] SECONDS = {120, 300, 600, 1800};

    static String format(int seconds) {
        if (seconds < 60) return seconds + " sec";
        return (seconds / 60) + " min";
    }

    /**
     * @param id    null / empty for the global default; a lock id for an override.
     * @param name  the lock's display name (only used for a per-lock title).
     */
    static void show(Activity host, String id, String name, Runnable after) {
        boolean perLock = id != null && !id.isEmpty();
        Typeface font = Fonts.current(host);

        LinearLayout box = new LinearLayout(host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Color.BLACK);
        box.setPadding(0, 24, 0, 8);

        TextView title = new TextView(host);
        title.setText("RE-LOCK AFTER");
        title.setTextColor(Color.GRAY);
        title.setTextSize(13);
        title.setLetterSpacing(0.15f);
        title.setTypeface(font);
        title.setPadding(48, 12, 48, 14);
        box.addView(title);

        AlertDialog dialog = new AlertDialog.Builder(host).setView(box).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        int current = perLock ? Config.getRelockSeconds(host, id) : Config.getRelockSeconds(host);
        boolean overridden = perLock && Config.hasRelockOverride(host, id);

        if (perLock) {
            box.addView(option(host, font,
                    "Use the default (" + format(Config.getRelockSeconds(host)) + ")",
                    !overridden, v -> {
                        Config.clearRelock(host, id);
                        dialog.dismiss();
                        if (after != null) after.run();
                    }));
        }

        for (int seconds : SECONDS) {
            boolean selected = (!perLock || overridden) && seconds == current;
            box.addView(option(host, font, format(seconds), selected, v -> {
                if (perLock) {
                    Config.setRelockSeconds(host, id, seconds);
                } else {
                    Config.setRelockSeconds(host, seconds);
                }
                dialog.dismiss();
                if (after != null) after.run();
            }));
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
            p.width = (int) (host.getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(p);
        }
    }

    private static TextView option(Activity host, Typeface font, String label,
                                   boolean selected, View.OnClickListener listener) {
        TextView view = new TextView(host);
        view.setText(label);
        view.setTextColor(selected ? Color.BLACK : Color.WHITE);
        view.setTextSize(16);
        view.setTypeface(font);
        view.setPadding(48, 30, 48, 30);
        view.setGravity(Gravity.START);
        if (selected) {
            view.setBackgroundColor(Color.WHITE);
        } else {
            StateListDrawable bg = new StateListDrawable();
            bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
            bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
            view.setBackground(bg);
        }
        view.setOnClickListener(listener);
        return view;
    }
}
