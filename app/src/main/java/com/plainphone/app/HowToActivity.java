package com.plainphone.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** How to grant WRITE_SECURE_SETTINGS: it can only be done from a computer, once, with adb. */
public class HowToActivity extends Activity {

    static final String COMMAND =
            "adb shell pm grant com.plainphone.app android.permission.WRITE_SECURE_SETTINGS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = SettingsUi.scrollScreen(this, "How-to");

        root.addView(step("1. Turn on USB debugging"));
        root.addView(step("2. Plug into a computer"));
        root.addView(step("3. Computer: install adb and run"));

        // The command with a copy icon at its right edge, in one rounded box.
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.BLACK);
        border.setStroke(2, Color.DKGRAY);
        border.setCornerRadius(UiKit.dp(this, UiKit.R_MD));
        box.setBackground(border);

        TextView command = new TextView(this);
        command.setText(COMMAND);
        command.setTextColor(Color.LTGRAY);
        command.setTextSize(12);
        command.setTypeface(android.graphics.Typeface.MONOSPACE);
        command.setTextIsSelectable(true);
        command.setPadding(24, 20, 8, 20);
        box.addView(command, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        ImageView copy = new ImageView(this);
        copy.setImageResource(R.drawable.ic_copy);
        copy.setContentDescription("Copy");
        int pad = UiKit.dp(this, 12);
        copy.setPadding(pad, pad, pad, pad);
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("adb command", COMMAND));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        });
        int size = UiKit.dp(this, 44);
        box.addView(copy, new LinearLayout.LayoutParams(size, size));

        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(48, 4, 48, 0);
        root.addView(box, boxParams);
    }

    private TextView step(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(16);
        view.setTypeface(Fonts.current(this));
        view.setPadding(48, 22, 48, 14);
        return view;
    }
}
