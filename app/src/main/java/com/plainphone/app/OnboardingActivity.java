package com.plainphone.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class OnboardingActivity extends Activity {

    private static final int STEP_COUNT = 4;
    private int step = 0;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(48, 64, 48, 64);
        scroll.addView(content);

        setContentView(scroll);
        renderStep();
    }

    private void renderStep() {
        content.removeAllViews();
        Typeface georgia = Fonts.georgia(this);

        TextView title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(georgia);
        title.setPadding(0, 0, 0, 32);
        content.addView(title);

        TextView body = new TextView(this);
        body.setTextColor(Color.WHITE);
        body.setTextSize(16);
        body.setTypeface(georgia);
        body.setPadding(0, 0, 0, 48);
        content.addView(body);

        switch (step) {
            case 0:
                title.setText("Welcome to Plain");
                body.setText("Plain turns your phone into a simple, distraction-reduced device: "
                        + "a plain black-and-white home screen, a wait before opening flagged apps, "
                        + "and usage stats.\n\nSetup takes three steps.");
                addButton("Next", v -> nextStep());
                break;
            case 1:
                title.setText("1. Set as your Home app");
                body.setText("Tap below, choose Plain, and select 'Always' or 'Set as default'.");
                addButton("Open Home app settings",
                        v -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)));
                addButton("Next", v -> nextStep());
                break;
            case 2:
                title.setText("2. Enable the Accessibility Service");
                body.setText("This lets Plain add a wait screen before flagged apps, apply grayscale, "
                        + "and lock the screen. Tap below, find 'Plain' in the list, and turn it on.");
                addButton("Open Accessibility settings",
                        v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
                addButton("Next", v -> nextStep());
                break;
            case 3:
                title.setText("3. Enable grayscale (optional, needs a computer)");
                body.setText("Grayscale uses a permission Android won't grant through a button. "
                        + "Connect your phone to a computer with adb and run:\n\n"
                        + "adb shell pm grant com.plainphone.app android.permission.WRITE_SECURE_SETTINGS\n\n"
                        + "Everything else works fine without this — you can do it later.");
                addButton("Done", v -> finishOnboarding());
                break;
            default:
                break;
        }

        if (step > 0) {
            addButton("Back", v -> previousStep());
        }
    }

    private void addButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        UiKit.style(this, button);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 24;
        content.addView(button, params);
    }

    private void nextStep() {
        step = Math.min(step + 1, STEP_COUNT - 1);
        renderStep();
    }

    private void previousStep() {
        step = Math.max(step - 1, 0);
        renderStep();
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences("plain", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_complete", true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
