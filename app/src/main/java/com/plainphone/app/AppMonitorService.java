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

/**
 * Watches foreground app changes. Flagged packages get a blocking countdown
 * overlay before they're usable, then get auto-closed after a time budget.
 * Locked packages get a blocking PIN-entry overlay instead, and stay gated
 * again every time the user leaves and comes back (no time budget applies).
 */
public class AppMonitorService extends AccessibilityService {

    private enum GateKind { COUNTDOWN, PIN, LOCKOUT, TIME_BLOCK }

    // How far ahead of the auto-close budget timer to fire the warning notification.
    private static final long WARNING_LEAD_MILLIS = 10_000L;

    // Lets MainActivity (a plain Activity, which can't call performGlobalAction itself)
    // reach the running service directly to trigger the lock-screen action.
    private static volatile AppMonitorService instance;

    static void lockScreen() {
        AppMonitorService service = instance;
        if (service != null) {
            service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
    }

    /**
     * Whether the OS currently has this service switched on. Checked via the running-instance
     * field first — the fast, always-correct answer while the service is actually alive — and
     * falls back to reading Android's enabled-services list for the (common) case of checking
     * from a plain Activity, where no instance exists to ask.
     */
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

    // Lets PinGateActivity, which already verified the PIN before launching the real
    // app, pre-arm the gate so the accessibility service's own reactive PIN overlay
    // doesn't prompt a second time the instant it sees that package come to the
    // foreground. Mirrors startGate()'s session bookkeeping, minus actually showing UI.
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

    // Lets FlaggedGateActivity, which already ran the wait-time countdown before launching
    // the real app, pre-arm the gate so the accessibility service's reactive countdown
    // overlay doesn't show a second time the instant it sees that package come to the
    // foreground. Mirrors startGate()'s COUNTDOWN-branch bookkeeping, including arming the
    // budget timer, minus actually showing UI.
    static void skipFlaggedGateFor(String packageName) {
        AppMonitorService service = instance;
        if (service != null) {
            service.cancelPendingTeardown();
            service.endSession();
            service.cancelCountdown();
            service.cancelBudgetTimer();
            service.currentGatedPackage = packageName;
            service.currentGateKind = GateKind.COUNTDOWN;
            service.sessionPackage = packageName;
            service.sessionStartMillis = SystemClock.elapsedRealtime();
            UsageStore.recordOpen(service, packageName);
            if (Config.isBudgetEnabled(service)) {
                service.startBudgetTimer(packageName);
            }
        }
    }

    // Lets TimeBlockOverrideActivity, which just lifted a time-block restriction (or ended
    // an ad-hoc session), clear the stale gate before relaunching the app — unlike
    // skipGateFor()/skipFlaggedGateFor(), this doesn't pre-arm a specific gate kind, since
    // the whole point is that the previously-blocking condition is now gone and something
    // else (locked/flagged, or nothing) may apply once the foreground event is re-evaluated.
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
    private GateKind currentGateKind = null;
    private View overlayRoot = null;
    private TextView countdownText = null;
    private EditText pinInput = null;
    private Runnable budgetRunnable = null;
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

        if (className.equals("android.inputmethodservice.SoftInputWindow")) {
            // The soft keyboard opening for the PIN overlay's EditText fires its own
            // WINDOW_STATE_CHANGED event, attributed to the keyboard app's package. Left
            // unhandled, that reads as "the user left the gated app," and the pending-teardown
            // grace period below removes the PIN overlay mid-entry — flickering the locked app
            // and killing the EditText's IME connection (dismissing the keyboard) with it.
            return;
        }

        if (className.equals("com.android.settings.password.ConfirmDeviceCredentialActivity")) {
            // The system-wide "confirm your device PIN/pattern/biometric" screen, hosted in
            // com.android.settings but launched BY other apps (via
            // KeyguardManager.createConfirmDeviceCredentialIntent()) to verify identity —
            // e.g. an authenticator app falling back to device credentials for its own lock.
            // It's not the user navigating into Settings. If Settings is also a locked
            // package, treating this as "entering Settings" fires our gate mid-flow through
            // the other app's own security check, which then knocks that app's gate loose
            // too and the two ping-pong, forcing the PIN to be re-entered repeatedly.
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
                currentGateKind = null;
                removeOverlay();
                endSession();
                if (wasGated) {
                    homeReachedAt = SystemClock.elapsedRealtime();
                }
            }
            return;
        }

        // A time-block restriction takes precedence over both locked and flagged: it's a
        // deliberate scheduling decision (e.g. an allow-only Study block should restrict
        // even a normally-unlocked app), not an impulse-control gate like the other two.
        if (TimeBlockRules.getBlockingBlock(this, packageName) != null) {
            if (SystemClock.elapsedRealtime() - homeReachedAt < HOME_COOLDOWN_MILLIS) {
                return;
            }
            cancelPendingTeardown();
            if (!packageName.equals(currentGatedPackage)) {
                startGate(packageName, GateKind.TIME_BLOCK);
            }
        } else if (Config.getLockedPackages(this).contains(packageName)) {
            if (SystemClock.elapsedRealtime() - homeReachedAt < HOME_COOLDOWN_MILLIS) {
                return;
            }
            cancelPendingTeardown();
            if (!packageName.equals(currentGatedPackage)) {
                startGate(packageName, GateKind.PIN);
            }
        } else if (Config.getFlaggedPackages(this).contains(packageName)) {
            if (SystemClock.elapsedRealtime() - homeReachedAt < HOME_COOLDOWN_MILLIS) {
                return;
            }
            cancelPendingTeardown();
            if (!packageName.equals(currentGatedPackage)) {
                boolean lockedOut = Config.isLockoutEnabled(this)
                        && Config.getLockoutUntil(this, packageName) > System.currentTimeMillis();
                startGate(packageName, lockedOut ? GateKind.LOCKOUT : GateKind.COUNTDOWN);
            }
        } else if (currentGatedPackage != null) {
            schedulePendingTeardown();
        }
    }

    private void startGate(String packageName, GateKind kind) {
        endSession();
        cancelCountdown();
        currentGatedPackage = packageName;
        currentGateKind = kind;
        if (kind == GateKind.LOCKOUT) {
            // Don't record this as a real open — the user never actually reaches the app.
            showLockout(packageName);
            return;
        }
        if (kind == GateKind.TIME_BLOCK) {
            // Don't record this as a real open — the user never actually reaches the app.
            showTimeBlockGate(packageName);
            return;
        }
        sessionPackage = packageName;
        sessionStartMillis = SystemClock.elapsedRealtime();
        UsageStore.recordOpen(this, packageName);
        if (kind == GateKind.PIN) {
            showPinEntry(packageName);
        } else {
            showCountdown(Config.getWaitSeconds(this), packageName);
        }
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
                0, // focusable + touchable: blocks interaction with the app underneath
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

    private void showPinEntry(String packageName) {
        if (!packageName.equals(currentGatedPackage)) return;
        if (overlayRoot == null) {
            addPinOverlay(packageName);
        }
    }

    private void addPinOverlay(String packageName) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 48, 48, 48);

        Typeface georgia = Fonts.current(this);

        TextView title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(georgia);
        title.setText("Enter PIN");
        root.addView(title);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        input.setGravity(Gravity.CENTER);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        UiKit.style(this, input);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitPin(packageName, input.getText().toString());
                return true;
            }
            return false;
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Submit the instant what's typed so far is a correct PIN, rather than
                // waiting for a fixed length or a manual tap/Done press.
                if (s.length() >= 4 && Config.checkLockPin(AppMonitorService.this, s.toString())) {
                    submitPin(packageName, s.toString());
                }
            }
        });
        inputRow.addView(input, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button unlockButton = new Button(this);
        unlockButton.setText("Unlock");
        UiKit.style(this, unlockButton);
        unlockButton.setOnClickListener(v -> submitPin(packageName, input.getText().toString()));
        LinearLayout.LayoutParams unlockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        unlockParams.leftMargin = 24;
        inputRow.addView(unlockButton, unlockParams);

        LinearLayout.LayoutParams inputRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputRowParams.topMargin = 48;
        root.addView(inputRow, inputRowParams);

        Button closeButton = new Button(this);
        closeButton.setText("Close");
        UiKit.style(this, closeButton);
        closeButton.setOnClickListener(v -> closeGatedApp());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        closeParams.topMargin = 24;
        root.addView(closeButton, closeParams);

        // The overlay is focusable/touchable (flags=0 below), but Back doesn't
        // route through an accessibility overlay's window the way it does an
        // Activity's — wire it explicitly so it can't be used to peek at the
        // locked app underneath instead of going Home like Close does.
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
                0, // focusable + touchable: blocks interaction with the app underneath
                PixelFormat.OPAQUE);
        // Keyboard should already be up when this overlay appears — the user shouldn't
        // have to know to tap the input box first.
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(root, params);
        overlayRoot = root;
        pinInput = input;
        root.requestFocus();
        input.requestFocus();
        input.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void submitPin(String packageName, String pin) {
        if (Config.checkLockPin(this, pin)) {
            removeOverlay();
            // currentGatedPackage/currentGateKind are left alone: the gate stays
            // "armed" so leaving and coming back to this app re-shows the PIN
            // prompt, matching the flagged-app gate's teardown-driven re-arming.
        } else if (pinInput != null) {
            pinInput.setText("");
            pinInput.requestFocus();
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
            pinInput = null;
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
            if (packageName.equals(lastForegroundPackage)) {
                if (Config.isLockoutEnabled(this)) {
                    Config.setLockoutUntil(this, packageName,
                            System.currentTimeMillis() + Config.getLockoutMinutes(this) * 60 * 1000L);
                }
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
            cancelBudgetTimer();
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
