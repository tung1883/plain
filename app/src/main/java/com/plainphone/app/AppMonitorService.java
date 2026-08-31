package com.plainphone.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

public class AppMonitorService extends AccessibilityService {

    private enum GateKind { COUNTDOWN, PIN, LOCKOUT, TIME_BLOCK }

    private static final long WARNING_LEAD_MILLIS = 10_000L;

    private static volatile AppMonitorService instance;

    static void lockScreen() {
        AppMonitorService service = instance;
        if (service != null) {
            service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
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

    /** The lock gate was satisfied by PinGateActivity — don't re-prompt when the app returns. */
    static void skipGateFor(String packageName) {
        AppMonitorService service = instance;
        if (service != null) {
            service.cancelPendingTeardown();
            service.endSession();
            service.cancelCountdown();
            service.currentGatedPackage = packageName;
            service.currentGateKind = GateKind.PIN;
            service.sessionPackage = packageName;
            service.sessionStartMillis = SystemClock.elapsedRealtime();
            UsageStore.recordOpen(service, packageName);
        }
    }

    static void skipFlaggedGateFor(String packageName) {
        AppMonitorService service = instance;
        if (service != null) {
            service.cancelPendingTeardown();
            service.endSession();
            service.cancelCountdown();
            service.currentGatedPackage = packageName;
            service.currentGateKind = GateKind.COUNTDOWN;
            service.sessionPackage = packageName;
            service.sessionStartMillis = SystemClock.elapsedRealtime();
            UsageStore.recordOpen(service, packageName);
            // Arm the auto-close budget once and let it keep running across leaving
            // and re-entering the app — a re-open shows the wait screen again but
            // does not hand out a fresh budget.
            if (Config.isBudgetEnabled(service)
                    && (service.budgetRunnable == null
                        || !packageName.equals(service.budgetPackage))) {
                service.startBudgetTimer(packageName);
            }
        }
    }

    static void skipTimeBlockGateFor(String packageName) {
        AppMonitorService service = instance;
        if (service != null) {
            service.cancelPendingTeardown();
            service.cancelCountdown();
            service.currentGatedPackage = null;
            service.currentGateKind = null;
            service.removeOverlay();
        }
    }

    private static final long TEARDOWN_GRACE_MILLIS = 700L;

    private static final long HOME_COOLDOWN_MILLIS = 1500L;

    private static final Set<String> PASSTHROUGH_PACKAGES = new HashSet<>();
    static {
        PASSTHROUGH_PACKAGES.add("com.android.systemui");
        PASSTHROUGH_PACKAGES.add("com.sec.android.app.launcher");
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    private String lastForegroundPackage = null;
    private String lastEventKey = null;
    private String currentGatedPackage = null;
    private GateKind currentGateKind = null;
    private View overlayRoot = null;
    private TextView countdownText = null;
    private Runnable budgetRunnable = null;
    private String budgetPackage = null;
    private Runnable warningRunnable = null;
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
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {

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

        String eventKey = packageName + "/" + className;
        if (eventKey.equals(lastEventKey)) return;
        lastEventKey = eventKey;

        if (PASSTHROUGH_PACKAGES.contains(packageName)) {

            return;
        }

        if (className.equals("android.inputmethodservice.SoftInputWindow")) {

            return;
        }

        if (className.equals("com.android.settings.password.ConfirmDeviceCredentialActivity")) {

            return;
        }

        String previousPackage = lastForegroundPackage;
        lastForegroundPackage = packageName;

        if (packageName.equals(getPackageName())) {

            // Back at the home screen — a locked app the user just left re-locks now,
            // instead of coasting on its unlock grace window.
            if (className.equals(getPackageName() + ".MainActivity")
                    && previousPackage != null
                    && Config.getLockedPackages(this).contains(previousPackage)) {
                Config.clearAppUnlock(this, previousPackage);
            }

            if (className.startsWith(getPackageName() + ".")) {

                boolean wasGated = currentGatedPackage != null;
                cancelPendingTeardown();
                cancelCountdown();
                // Leave budgetRunnable alone — a flagged app's auto-close budget keeps
                // ticking while you're away and is not reset when you go back to it.
                currentGatedPackage = null;
                currentGateKind = null;
                removeOverlay();
                endSession();
                if (wasGated) {
                    homeReachedAt = SystemClock.elapsedRealtime();
                }
            }
            return;
        }

        if (TimeBlockRules.getBlockingBlock(this, packageName) != null) {
            if (SystemClock.elapsedRealtime() - homeReachedAt < HOME_COOLDOWN_MILLIS) return;
            cancelPendingTeardown();
            if (!packageName.equals(currentGatedPackage)) {
                startGate(packageName, GateKind.TIME_BLOCK);
            }
            return;
        }

        boolean locked = Config.getLockedPackages(this).contains(packageName);
        boolean flagged = Config.getFlaggedPackages(this).contains(packageName);

        if (!locked && !flagged) {
            if (currentGatedPackage != null) schedulePendingTeardown();
            return;
        }

        if (SystemClock.elapsedRealtime() - homeReachedAt < HOME_COOLDOWN_MILLIS) return;
        cancelPendingTeardown();

        // A gate for this app is already on screen / its timer running — let it finish.
        if (packageName.equals(currentGatedPackage)) return;

        enforce(packageName);
    }

    /**
     * Apply the two independent gates to a freshly-foregrounded app. They don't know
     * about each other: the <b>lock gate</b> asks for a PIN unless the app is inside
     * its unlock grace window; the <b>flag gate</b> shows the reopen-lockout or the
     * wait countdown and then arms the auto-close budget. An app that is both passes
     * the lock gate first — {@link #showPinEntry}'s success path re-runs enforce so
     * the flag gate follows.
     */
    private void enforce(String packageName) {
        if (Config.getLockedPackages(this).contains(packageName)) {
            if (!Config.isAppRecentlyUnlocked(this, packageName)) {
                launchGate(packageName, PinGateActivity.class);
                return;
            }
            Config.markAppUnlocked(this, packageName);   // refresh grace while in use
        }

        if (Config.getFlaggedPackages(this).contains(packageName)) {
            launchGate(packageName, FlaggedGateActivity.class);
            return;
        }

        // Locked only and already unlocked — nothing to show. Drop a gate left over
        // from a different app.
        if (currentGatedPackage != null && !currentGatedPackage.equals(packageName)) {
            schedulePendingTeardown();
        }
    }

    /**
     * Send the just-opened app to the background and put the gate screen up in its
     * place, so the app is never reachable until the gate is passed — the same as
     * opening a locked/flagged app from Plain's own list. On success the gate
     * activity relaunches the app (and calls back through {@link #skipGateFor} /
     * {@link #skipFlaggedGateFor}).
     */
    private void launchGate(String packageName, Class<?> gateClass) {
        endSession();
        cancelCountdown();
        cancelBudgetTimer();
        removeOverlay();
        currentGatedPackage = packageName;
        sessionPackage = packageName;
        sessionStartMillis = SystemClock.elapsedRealtime();
        UsageStore.recordOpen(this, packageName);

        // Home first so the gated app drops out of view, then the gate on top of the
        // launcher. performGlobalAction is async, so let it settle before startActivity
        // or the gate can flash up and then be covered by the home transition.
        performGlobalAction(GLOBAL_ACTION_HOME);

        Intent gate = new Intent(this, gateClass);
        gate.putExtra("package", packageName);
        gate.putExtra("label", AllAppsUsage.label(getPackageManager(), packageName));
        gate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        handler.postDelayed(() -> startActivity(gate), 250);
    }

    private void startGate(String packageName, GateKind kind) {
        endSession();
        cancelCountdown();
        currentGatedPackage = packageName;
        currentGateKind = kind;
        if (kind == GateKind.LOCKOUT) {

            showLockout(packageName);
            return;
        }
        if (kind == GateKind.TIME_BLOCK) {

            showTimeBlockGate(packageName);
            return;
        }
        sessionPackage = packageName;
        sessionStartMillis = SystemClock.elapsedRealtime();
        UsageStore.recordOpen(this, packageName);
        showCountdown(Config.getWaitSeconds(this), packageName);
    }

    private void showLockout(String packageName) {
        if (!packageName.equals(currentGatedPackage)) return;

        long remainingMillis = Config.getLockoutUntil(this, packageName) - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            removeOverlay();
            currentGateKind = GateKind.COUNTDOWN;
            sessionPackage = packageName;
            sessionStartMillis = SystemClock.elapsedRealtime();
            UsageStore.recordOpen(this, packageName);
            showCountdown(Config.getWaitSeconds(this), packageName);
            return;
        }

        if (overlayRoot == null) {
            addOverlay();
        }
        countdownText.setText("Locked. Try again in " + formatDuration(remainingMillis));

        countdownRunnable = () -> showLockout(packageName);
        handler.postDelayed(countdownRunnable, 1000);
    }

    private void showTimeBlockGate(String packageName) {
        if (!packageName.equals(currentGatedPackage)) return;
        if (overlayRoot == null) {
            addTimeBlockOverlay(packageName);
        }
    }

    private void addTimeBlockOverlay(String packageName) {
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
            closeGatedApp();
        });
        LinearLayout.LayoutParams overrideParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        overrideParams.topMargin = 48;
        root.addView(override, overrideParams);

        Button closeButton = new Button(this);
        closeButton.setText("Close");
        UiKit.style(this, closeButton);
        closeButton.setOnClickListener(v -> closeGatedApp());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        closeParams.topMargin = 24;
        root.addView(closeButton, closeParams);

        root.setFocusableInTouchMode(true);
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                closeGatedApp();
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

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(root, params);
        overlayRoot = root;
    }

    private static String formatDuration(long millis) {
        long totalSeconds = (millis + 999) / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(java.util.Locale.US, "%dm %02ds", minutes, seconds);
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
            if (Config.isBudgetEnabled(this)) {
                startBudgetTimer(packageName);
            }
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

        Typeface georgia = Fonts.current(this);

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
                0,
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

            schedulePendingTeardown();
        }
    }

    private void startBudgetTimer(String packageName) {
        cancelBudgetTimer();
        budgetPackage = packageName;
        long budgetMillis = Config.getBudgetMinutes(this) * 60 * 1000L;

        long warningDelay = budgetMillis - WARNING_LEAD_MILLIS;
        if (warningDelay > 0) {
            warningRunnable = () -> {
                if (packageName.equals(lastForegroundPackage)) {
                    NotificationHelper.notifyClosingSoon(this, packageName);
                }
            };
            handler.postDelayed(warningRunnable, warningDelay);
        }

        budgetRunnable = () -> {
            budgetRunnable = null;
            budgetPackage = null;
            // The budget is spent — apply the reopen lockout even if the user stepped
            // away just before it expired.
            if (Config.isLockoutEnabled(this)) {
                Config.setLockoutUntil(this, packageName,
                        System.currentTimeMillis() + Config.getLockoutMinutes(this) * 60 * 1000L);
            }
            if (packageName.equals(lastForegroundPackage)) {
                performGlobalAction(GLOBAL_ACTION_HOME);
            }
        };
        handler.postDelayed(budgetRunnable, budgetMillis);
    }

    private void cancelBudgetTimer() {
        if (budgetRunnable != null) {
            handler.removeCallbacks(budgetRunnable);
            budgetRunnable = null;
        }
        budgetPackage = null;
        if (warningRunnable != null) {
            handler.removeCallbacks(warningRunnable);
            warningRunnable = null;
        }
    }

    private void schedulePendingTeardown() {
        cancelPendingTeardown();
        pendingTeardown = () -> {
            pendingTeardown = null;
            cancelCountdown();
            // Keep budgetRunnable alive — see the flag-budget note in onAccessibilityEvent.
            currentGatedPackage = null;
            currentGateKind = null;
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

