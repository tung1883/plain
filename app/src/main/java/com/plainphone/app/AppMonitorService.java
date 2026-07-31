package com.plainphone.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/**
 * Watches foreground app changes. Flagged packages get a blocking countdown
 * overlay before they're usable, then get auto-closed after a time budget.
 */
public class AppMonitorService extends AccessibilityService {

    // Lets MainActivity (a plain Activity, which can't call performGlobalAction itself)
    // reach the running service directly to trigger the lock-screen action.
    private static volatile AppMonitorService instance;

    static void lockScreen() {
        AppMonitorService service = instance;
        if (service != null) {
            service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
    }

    // Grace period before actually tearing down a gate/budget session after
    // seeing a non-flagged package. Covers transient OEM UI bounces (quick
    // settings tile toggles, Recents flings) that briefly report some other
    // package before control returns to the still-open flagged app, without
    // needing to enumerate every such package by name.
    private static final long TEARDOWN_GRACE_MILLIS = 700L;

    // Android appears to briefly resume the most-recently-used app's process shortly
    // after Home is reached, likely to refresh its cached Recents thumbnail. That
    // generates a real WINDOW_STATE_CHANGED event with no actual app visible to the
    // user. A genuine reopen requires physically tapping the app in the list, which
    // never happens this fast, so suppress flagged-app events in this window.
    private static final long HOME_COOLDOWN_MILLIS = 1500L;

    // Transient system UI surfaces (notification shade, Recents/Overview).
    // Foreground-change events for these must not reset gate/grayscale state,
    // otherwise leaving a flagged app to view Recents snaps color back on.
    private static final Set<String> PASSTHROUGH_PACKAGES = new HashSet<>();
    static {
        PASSTHROUGH_PACKAGES.add("com.android.systemui");
        PASSTHROUGH_PACKAGES.add("com.sec.android.app.launcher"); // Samsung Recents/Overview host
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    private String lastForegroundPackage = null;
    private String lastEventKey = null;
    private String currentGatedPackage = null;
    private View overlayRoot = null;
    private TextView countdownText = null;
    private Runnable budgetRunnable = null;
    private Runnable pendingTeardown = null;
    private Runnable countdownRunnable = null;
    private long homeReachedAt = 0L;
    private String sessionPackage = null;
    private long sessionStartMillis = 0L;

    @Override
    protected void onServiceConnected() {
        instance = this;
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
        if (Config.isGrayscaleEnabled(this)) {
            GrayscaleController.enable(this);
        }
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        // Fires when the user disables the service in Settings > Accessibility.
        // Grayscale is a standalone system setting, not tied to this service running,
        // so it must be explicitly turned off here or it would persist forever.
        GrayscaleController.disable(this);
        instance = null;
        endSession();
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;
        String packageName = pkg.toString();
        CharSequence clsSeq = event.getClassName();
        String className = clsSeq == null ? "" : clsSeq.toString();

        // Dedup on (package, class) together, not package alone: our own overlay's
        // self-generated events share our package name but a different class (e.g.
        // LinearLayout) from the real MainActivity arrival. Deduping on package only
        // let the overlay's event silently swallow the real "reached home" event that
        // followed it, leaving currentGatedPackage stuck forever.
        String eventKey = packageName + "/" + className;
        if (eventKey.equals(lastEventKey)) return;
        lastEventKey = eventKey;

        if (PASSTHROUGH_PACKAGES.contains(packageName)) {
            // Recents/notification shade: transient system UI, not a real app switch.
            // Don't update lastForegroundPackage so the budget timer's "did the user
            // actually leave the flagged app" check still sees the real app underneath.
            return;
        }

        lastForegroundPackage = packageName;

        if (packageName.equals(getPackageName())) {
            // Adding our own accessibility overlay window fires a WINDOW_STATE_CHANGED
            // event attributed to our own package, with a raw view class (TextView,
            // LinearLayout) rather than an Activity. Only treat this as genuine
            // navigation into one of our own real screens (Settings, Flagged Apps,
            // MainActivity, etc. all live under this package) — otherwise the overlay
            // tears itself down the instant it appears (visible as flicker), or a real
            // screen like Settings gets silently ignored and never clears a stale gate.
            if (className.startsWith(getPackageName() + ".")) {
                // Only start the ghost-suppression cooldown when we're actually leaving a
                // gated session, not on every incidental glance at the home screen (e.g.
                // the launcher briefly flashing while navigating Recents). Otherwise the
                // cooldown keeps getting pushed forward indefinitely and swallows a
                // genuine quick reopen of the same app.
                boolean wasGated = currentGatedPackage != null;
                cancelPendingTeardown();
                cancelCountdown();
                cancelBudgetTimer();
                currentGatedPackage = null;
                removeOverlay();
                endSession();
                if (wasGated) {
                    homeReachedAt = SystemClock.elapsedRealtime();
                }
            }
            return;
        }

        if (Config.getFlaggedPackages(this).contains(packageName)) {
            if (SystemClock.elapsedRealtime() - homeReachedAt < HOME_COOLDOWN_MILLIS) {
                return;
            }
            cancelPendingTeardown();
            if (!packageName.equals(currentGatedPackage)) {
                startGate(packageName);
            }
        } else if (currentGatedPackage != null) {
            schedulePendingTeardown();
        }
    }

    private void startGate(String packageName) {
        endSession();
        cancelCountdown();
        currentGatedPackage = packageName;
        sessionPackage = packageName;
        sessionStartMillis = SystemClock.elapsedRealtime();
        UsageStore.recordOpen(this, packageName);
        showCountdown(Config.getWaitSeconds(this), packageName);
    }

    private void endSession() {
        if (sessionPackage != null) {
            long elapsed = SystemClock.elapsedRealtime() - sessionStartMillis;
            UsageStore.addUsageMillis(this, sessionPackage, elapsed);
            sessionPackage = null;
        }
    }

    private void showCountdown(int secondsLeft, String packageName) {
        if (!packageName.equals(currentGatedPackage)) return;

        if (secondsLeft <= 0) {
            removeOverlay();
            startBudgetTimer(packageName);
            return;
        }

        if (overlayRoot == null) {
            addOverlay();
        }
        countdownText.setText("Wait " + secondsLeft + "...");

        countdownRunnable = () -> showCountdown(secondsLeft - 1, packageName);
        handler.postDelayed(countdownRunnable, 1000);
    }

    private void cancelCountdown() {
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }

    private void addOverlay() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);

        Typeface georgia = Fonts.georgia(this);

        TextView text = new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setTextSize(28);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(georgia);
        root.addView(text);

        Button closeButton = new Button(this);
        closeButton.setText("Close");
        UiKit.style(this, closeButton);
        closeButton.setOnClickListener(v -> closeGatedApp());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = 48;
        root.addView(closeButton, buttonParams);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                0, // focusable + touchable: blocks interaction with the app underneath
                PixelFormat.OPAQUE);

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(root, params);
        overlayRoot = root;
        countdownText = text;
    }

    private void removeOverlay() {
        if (overlayRoot != null) {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.removeView(overlayRoot);
            overlayRoot = null;
            countdownText = null;
        }
    }

    private void closeGatedApp() {
        String packageName = currentGatedPackage;
        cancelCountdown();
        cancelBudgetTimer();
        removeOverlay();
        performGlobalAction(GLOBAL_ACTION_HOME);
        if (packageName != null) {
            // Don't null out currentGatedPackage immediately: there's a brief race where
            // the app underneath (still resumed until Home fully takes over) flashes back
            // into "foreground" for a moment right after the overlay is removed. Clearing
            // state only via the grace-period teardown (same mechanism as the OEM-bounce
            // fix) means that flash doesn't look like a fresh entry and re-trigger the gate.
            schedulePendingTeardown();
        }
    }

    private void startBudgetTimer(String packageName) {
        cancelBudgetTimer();
        budgetRunnable = () -> {
            if (packageName.equals(lastForegroundPackage)) {
                performGlobalAction(GLOBAL_ACTION_HOME);
            }
        };
        handler.postDelayed(budgetRunnable, Config.getBudgetMinutes(this) * 60 * 1000L);
    }

    private void cancelBudgetTimer() {
        if (budgetRunnable != null) {
            handler.removeCallbacks(budgetRunnable);
            budgetRunnable = null;
        }
    }

    private void schedulePendingTeardown() {
        cancelPendingTeardown();
        pendingTeardown = () -> {
            pendingTeardown = null;
            cancelCountdown();
            cancelBudgetTimer();
            currentGatedPackage = null;
            removeOverlay();
            endSession();
        };
        handler.postDelayed(pendingTeardown, TEARDOWN_GRACE_MILLIS);
    }

    private void cancelPendingTeardown() {
        if (pendingTeardown != null) {
            handler.removeCallbacks(pendingTeardown);
            pendingTeardown = null;
        }
    }

    @Override
    public void onInterrupt() {
        cancelCountdown();
        removeOverlay();
        cancelBudgetTimer();
        endSession();
    }
}
