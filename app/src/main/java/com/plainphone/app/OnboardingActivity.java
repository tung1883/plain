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
    private static final String STEP_KEY = "step";

    private int step = 0;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            step = savedInstanceState.getInt(STEP_KEY, 0);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(48, 64, 48, 64);
        scroll.addView(content);

        setContentView(scroll);
        renderStep();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STEP_KEY, step);
    }

    private void renderStep() {
        content.removeAllViews();
        Typeface georgia = Fonts.current(this);

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
                body.setText("Plain turns your phone into a dumb-ass brick"
                        + "\n\nSetup takes a few steps");
                addButton("Next", v -> nextStep());
                break;
            case 1:
                title.setText("1. Set as your Home app");
                body.setText("Tap below, choose Plain");
                addButton("Open Home app settings",
                        v -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)));
                addButton("Next", v -> nextStep());
                break;
            case 2:
                title.setText("2. Enable the Accessibility Service");
                body.setText("This lets Plain add a wait screen before addictive apps and lock the "
                        + "screen. Tap below, go to Installed Apps\n\n"
                        + "If it won't turn on: go to Settings → Apps → Plain, tap the ⋮ menu in the "
                        + "top-right corner, then tap \"Allow restricted settings\" — then come back "
                        + "here and try the toggle again.");
                addButton("Open Accessibility settings",
                        v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
                addButton("Next", v -> nextStep());
                break;
            case 3:
                title.setText("3. Everything else is optional");
                body.setText("Plain works from here. Search on the home screen can also look through "
                        + "your files, contacts, and the web — each of those asks for its own "
                        + "permission the first time you actually use it, not now.\n\n"
                        + "Settings → Permissions → App access lists everything Plain can see or "
                        + "control, and what each one is for, whenever you want to check.");
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

