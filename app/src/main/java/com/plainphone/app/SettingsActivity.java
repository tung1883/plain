package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private LinearLayout root;
    private Typeface georgia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        georgia = Fonts.georgia(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render(); // reflect any changes made via dialogs or returning from system Settings
    }

    private void render() {
        root.removeAllViews();

        root.addView(row("Change home app",
                v -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS))));
        root.addView(row("Turn off Accessibility Service",
                v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));

        boolean grayscaleOn = Config.isGrayscaleEnabled(this);
        root.addView(row("Grayscale: " + (grayscaleOn ? "On" : "Off"), v -> toggleGrayscale()));

        root.addView(row("Wait time: " + Config.getWaitSeconds(this) + "s",
                v -> startActivity(new Intent(this, WaitTimeActivity.class))));

        root.addView(row("Auto-close after: " + Config.getBudgetMinutes(this) + "m",
                v -> editNumber("Auto-close after (minutes)", Config.getBudgetMinutes(this),
                        value -> Config.setBudgetMinutes(this, value))));

        root.addView(row("Flagged apps",
                v -> startActivity(new Intent(this, FlaggedAppsActivity.class))));

        root.addView(row("Grant usage access (for all-apps stats)",
                v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
    }

    private void toggleGrayscale() {
        boolean newValue = !Config.isGrayscaleEnabled(this);
        Config.setGrayscaleEnabled(this, newValue);
        if (newValue) {
            GrayscaleController.enable(this);
        } else {
            GrayscaleController.disable(this);
        }
        render();
    }

    private interface IntConsumer {
        void accept(int value);
    }

    private void editNumber(String title, int current, IntConsumer onSet) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setTextColor(Color.WHITE);
        input.setBackgroundColor(Color.BLACK);
        input.setTypeface(georgia);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        int value = Integer.parseInt(input.getText().toString().trim());
                        if (value > 0) {
                            onSet.accept(value);
                        }
                    } catch (NumberFormatException ignored) {
                        // leave the setting unchanged on invalid input
                    }
                    render();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private TextView row(String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20);
        view.setPadding(48, 40, 48, 40);
        view.setGravity(Gravity.START);
        view.setTypeface(georgia);

        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        background.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        view.setBackground(background);

        view.setOnClickListener(listener);
        return view;
    }
}
