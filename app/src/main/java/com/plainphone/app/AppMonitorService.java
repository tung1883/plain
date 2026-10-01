package com.plainphone.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Nerd-mode foreground source and gate backend: feeds window changes to {@link GateEngine}
 * and draws the PIN and time-block gates as accessibility overlays on top of the app.
 */
public class AppMonitorService extends AccessibilityService implements GateEngine.Backend {

    private static volatile AppMonitorService instance;

    static void lockScreen() {
        AppMonitorService service = instance;
        if (service != null) {
            service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
    }

    /** The system still has this service bound (it can take a moment to let go after being switched off). */
    static boolean isBound() {
        return instance != null;
    }

    static boolean isEnabled(android.content.Context context) {
        if (instance != null) return true;

        String enabled = android.provider.Settings.Secure.getString(context.getContentResolver(),
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;

        String component = context.getPackageName() + "/" + AppMonitorService.class.getName();
        for (String service : enabled.split(":")) {
            if (service.equalsIgnoreCase(component)) return true;
        }
        return false;
    }

    private View overlayRoot = null;
    private GateEngine engine = null;

    @Override
    protected void onServiceConnected() {
        instance = this;
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
        engine = GateEngine.attachNerd(this, this);
        ForegroundWatcher.sync(this);
        // Switched back on a moment after a blocking app was reopened? Catch up on it.
        ForegroundExtras.seedFromRecent(this);
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {

        instance = null;
        removeGateUi();
        engine = null;
        GateEngine.detachNerd();
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;
        CharSequence clsSeq = event.getClassName();
        String className = clsSeq == null ? "" : clsSeq.toString();
        ForegroundExtras.onForeground(this, pkg.toString(), className);
        // In Noob mode the foreground watcher drives the gates; this service may still be on.
        GateEngine e = engine;
        if (e != null && Config.isNerdMode(this)) e.onForeground(pkg.toString(), className);
    }

    @Override
    public void goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    /**
     * PIN gate for a locked app, drawn as an accessibility overlay <b>on top of the
     * app</b> — the app is never sent away, so a deep-linked screen (e.g. Settings
     * opened straight to Wi-Fi from a quick tile) is still there when the overlay
     * clears, and no duplicate task is created. On success the overlay is just
     * removed; on cancel we go home.
     */
    @Override
    public void showPin(String packageName, boolean overApp) {
        final PinPromptView[] pad = new PinPromptView[1];
        pad[0] = new PinPromptView(this, new PinPromptView.Listener() {
            @Override
            public void onPin(String pin) {
                if (pin.length() >= 4 && Config.checkPin(AppMonitorService.this, "applock", pin)) {
                    engine.onPinAccepted(packageName);
                } else if (pin.length() >= 6) {
                    pad[0].reject();
                }
            }

            @Override
            public void onCancel() {
                engine.cancelPinGate();
            }
        });
        pad[0].setFocusableInTouchMode(true);
        pad[0].setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                engine.cancelPinGate();
                return true;
            }
            return false;
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                0,
                PixelFormat.OPAQUE);
        addOverlay(pad[0], params);
    }

    @Override
    public void showTimeBlock(String packageName, boolean overApp) {
        if (overlayRoot != null) return;

        TimeBlock block = TimeBlockRules.getBlockingBlock(this, packageName);
        String name = block != null ? block.name : "a time block";
        String endTime = block != null ? TimeBlockRules.formatEndTime(this, block) : "";
        String blockId = block != null ? block.id : null;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 48, 48, 48);

        Typeface georgia = Fonts.current(this);

        TextView text = new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setTextSize(24);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(georgia);
        text.setText("Unavailable during " + name + " until " + endTime);
        root.addView(text);

        Button override = new Button(this);
        override.setText("Override");
        UiKit.style(this, override);
        override.setOnClickListener(v -> {
            Intent intent = new Intent(this, TimeBlockOverridePinActivity.class);
            intent.putExtra("package", packageName);
            intent.putExtra("blockId", blockId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            engine.closeGatedApp();
        });
        LinearLayout.LayoutParams overrideParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        overrideParams.topMargin = 48;
        root.addView(override, overrideParams);

        Button closeButton = new Button(this);
        closeButton.setText("Close");
        UiKit.style(this, closeButton);
        closeButton.setOnClickListener(v -> engine.closeGatedApp());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        closeParams.topMargin = 24;
        root.addView(closeButton, closeParams);

        root.setFocusableInTouchMode(true);
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                engine.closeGatedApp();
                return true;
            }
            return false;
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                0,
                PixelFormat.OPAQUE);

        addOverlay(root, params);
    }

    /** The system refuses overlay windows once it has started unbinding this service. */
    private void addOverlay(View view, WindowManager.LayoutParams params) {
        try {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(view, params);
            overlayRoot = view;
        } catch (RuntimeException e) {
            overlayRoot = null;
        }
    }

    @Override
    public void removeGateUi() {
        if (overlayRoot != null) {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.removeView(overlayRoot);
            overlayRoot = null;
        }
    }

    @Override
    public void onInterrupt() {
        GateEngine e = engine;
        if (e != null) e.onInterrupt();
        else removeGateUi();
    }
}
