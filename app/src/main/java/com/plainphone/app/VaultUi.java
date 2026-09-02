package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

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

    /**
     * "N task(s) still running" dialog: a scrollable list of {@code lines} (capped
     * at ~3 rows) above {@code labels.length} option buttons. Used by
     * {@link PluginLock} for both the single-plugin and "Lock all" cases.
     */
    static void tasksDialog(Activity host, String title, List<String> lines,
                            String[] labels, Choice[] choices) {
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

        TextView list = new TextView(host);
        list.setText(android.text.TextUtils.join("\n", lines));
        list.setTextColor(Color.GRAY);
        list.setTextSize(14);
        list.setTypeface(font);
        list.setPadding(48, 0, 48, 16);

        int maxH = (int) (host.getResources().getDisplayMetrics().density * 84);
        BoundedScrollView scroller = new BoundedScrollView(host, maxH);
        scroller.addView(list);
        box.addView(scroller);

        AlertDialog dialog = new AlertDialog.Builder(host).setView(box).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        for (int i = 0; i < labels.length; i++) {
            Choice c = choices[i];
            box.addView(option(host, font, labels[i], () -> {
                dialog.dismiss();
                if (c != null) c.run();
            }));
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (host.getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private static class BoundedScrollView extends ScrollView {
        private final int maxHeight;

        BoundedScrollView(Context context, int maxHeight) {
            super(context);
            this.maxHeight = maxHeight;
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            super.onMeasure(widthSpec,
                    MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
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
