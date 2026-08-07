package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Wait-time countdown (and reopen-lockout screen, if that app is still cooling down)
 * for a flagged app, shown before the real app is launched — not after, so there's no
 * flicker of the app underneath while AppMonitorService's reactive overlay catches up.
 * Reached only from MainActivity.launchApp(); other launch paths (widgets, notifications,
 * deep links) still go through AppMonitorService's reactive gate, since there's no
 * pre-launch hook for those.
 */
public class FlaggedGateActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String packageName;
    private String label;
    private TextView text;
    private Runnable pending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageName = getIntent().getStringExtra("package");
        label = getIntent().getStringExtra("label");

        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 48, 48, 48);

        text = new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setTextSize(28);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(georgia);
        root.addView(text);

        Button close = new Button(this);
        close.setText("Close");
        UiKit.style(this, close);
        close.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        closeParams.topMargin = 48;
        root.addView(close, closeParams);

        setContentView(root);

        long lockoutUntil = Config.isLockoutEnabled(this) ? Config.getLockoutUntil(this, packageName) : 0L;
        if (lockoutUntil > System.currentTimeMillis()) {
            showLockout(lockoutUntil);
        } else {
            showCountdown(Config.getWaitSeconds(this));
        }
    }

    private void showLockout(long lockoutUntil) {
        long remaining = lockoutUntil - System.currentTimeMillis();
        if (remaining <= 0) {
            showCountdown(Config.getWaitSeconds(this));
            return;
        }
        text.setText("Locked. Try again in " + formatDuration(remaining));
        pending = () -> showLockout(lockoutUntil);
        handler.postDelayed(pending, 1000);
    }

    private void showCountdown(int secondsLeft) {
        if (secondsLeft <= 0) {
            openApp();
            return;
        }
        text.setText("Wait " + secondsLeft + "s before opening " + label);
        pending = () -> showCountdown(secondsLeft - 1);
        handler.postDelayed(pending, 1000);
    }

    private void openApp() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            AppMonitorService.skipFlaggedGateFor(packageName);
            startActivity(launchIntent);
        }
        finish();
    }

    private static String formatDuration(long millis) {
        long totalSeconds = (millis + 999) / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(java.util.Locale.US, "%dm %02ds", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
