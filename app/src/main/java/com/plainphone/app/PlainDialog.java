package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.Consumer;

/**
 * The black bordered popup (as {@link VaultUi} draws it) whose body can be rebuilt while it is
 * open: a title, then whatever {@link #setBody} is given. Used where a popup changes as you tap in it.
 */
final class PlainDialog {

    private final Activity host;
    private final LinearLayout box;
    private final AlertDialog dialog;
    private final Typeface font;
    private final int firstBody;     // 1 when there is a title above the body, else 0

    private PlainDialog(Activity host, String title) {
        this.host = host;
        font = Fonts.current(host);
        box = new LinearLayout(host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(UiKit.dialogBackground(host));
        UiKit.clipRounded(host, box, UiKit.R_MD);
        box.setPadding(2, 24, 2, UiKit.dp(host, UiKit.R_MD));
        if (title != null) box.addView(UiKit.dialogTitle(host, title));
        firstBody = title == null ? 0 : 1;
        FrameLayout scrim = UiKit.wrapScrim(host, box, 0.85f);
        dialog = new AlertDialog.Builder(host, R.style.Theme_PlainPhone_RoundedDialog).setView(scrim).create();
    }

    /** Opens the popup; {@code fill} builds its first body. */
    static PlainDialog show(Activity host, String title, Consumer<PlainDialog> fill, Runnable onDismiss) {
        PlainDialog d = new PlainDialog(host, title);
        fill.accept(d);
        if (onDismiss != null) d.dialog.setOnDismissListener(x -> onDismiss.run());
        UiKit.finishCentered(d.dialog, (FrameLayout) d.box.getParent());
        return d;
    }

    /** Replaces everything under the title. */
    void setBody(View... views) {
        box.removeViews(firstBody, box.getChildCount() - firstBody);
        for (View v : views) box.addView(v);
    }

    void dismiss() {
        dialog.dismiss();
    }

    /** A full-width row: label on the left, an optional small state on the right. */
    View option(String label, String state, int stateColor, Runnable click) {
        LinearLayout row = new LinearLayout(host);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(48, 28, 48, 28);
        row.setBackground(rowBackground());

        TextView l = new TextView(host);
        l.setText(label);
        l.setTextColor(Color.WHITE);
        l.setTextSize(20);
        l.setTypeface(font);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (state != null) {
            TextView s = new TextView(host);
            s.setText(state);
            s.setTextColor(stateColor);
            s.setTextSize(14);
            s.setTypeface(font);
            row.addView(s);
        }
        row.setOnClickListener(v -> click.run());
        return row;
    }

    static StateListDrawable rowBackground() {
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, UiKit.pressedFill());
        bg.setEnterFadeDuration(120);
        bg.setExitFadeDuration(120);
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return bg;
    }
}
