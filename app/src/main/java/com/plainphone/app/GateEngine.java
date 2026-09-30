package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.HashSet;
import java.util.Set;

/**
 * Decides what to do when an app comes to the foreground — time-block gate, PIN gate,
 * flagged wait/lockout gate — and owns the timers and usage session behind those gates.
 * It knows nothing about how a foreground change is detected (accessibility events today)
 * or how a gate is drawn: the event source calls {@link #onForeground}, and a
 * {@link Backend} does the going-home and the drawing. Main thread only.
 */
final class GateEngine {

    enum GateKind { PIN, COUNTDOWN, TIME_BLOCK }

    interface Backend {
        /** Send the user to the launcher. */
        void goHome();

        /**
         * Ask for the app-lock PIN for {@code packageName}. Answer via {@link #onPinAccepted} /
         * {@link #cancelPinGate}. {@code overApp}: the app was opened from inside Plain, so its
         * page sits in Plain's own task - put the gate on top instead of going Home, which would
         * tear that page down.
         */
        void showPin(String packageName, boolean overApp);

        /** Show the "unavailable during a time block" gate. Answer via {@link #closeGatedApp}. */
        void showTimeBlock(String packageName, boolean overApp);

        /** Take down whatever gate UI {@link #showPin} / {@link #showTimeBlock} put up. */
        void removeGateUi();
    }

    private static final long WARNING_LEAD_MILLIS = 10_000L;
    private static final long TEARDOWN_GRACE_MILLIS = 700L;
    private static final long HOME_FLASH_DEBOUNCE_MILLIS = 600L;
    private static final long HOME_COOLDOWN_MILLIS = 1500L;

    private static final Set<String> PASSTHROUGH_PACKAGES = new HashSet<>();
    static {
        PASSTHROUGH_PACKAGES.add("com.android.systemui");
        PASSTHROUGH_PACKAGES.add("com.sec.android.app.launcher");
    }

    private static volatile GateEngine instance;

    static GateEngine get(Context context) {
        GateEngine engine = instance;
        if (engine == null) {
            synchronized (GateEngine.class) {
                engine = instance;
                if (engine == null) {
                    engine = new GateEngine(context.getApplicationContext());
                    instance = engine;
                }
            }
        }
        return engine;
    }

    /** Nerd mode: the accessibility service is up and can draw gates over apps. */
    static GateEngine attachNerd(Context context, Backend backend) {
        GateEngine engine = get(context);
        engine.nerdBackend = backend;
        return engine;
    }

    /** Noob mode: the foreground watcher is up and gates are activities. */
    static GateEngine attachNoob(Context context, Backend backend) {
        GateEngine engine = get(context);
        engine.noobBackend = backend;
        return engine;
    }

    static void detachNerd() {
        GateEngine engine = instance;
        if (engine != null) {
            engine.nerdBackend = null;
            engine.sourceGone();
        }
    }

    static void detachNoob() {
        GateEngine engine = instance;
        if (engine != null) {
            engine.noobBackend = null;
            engine.sourceGone();
        }
    }

    /** The lock gate was satisfied by PinGateActivity — don't re-prompt when the app returns. */
    static void skipGateFor(String packageName) {
        GateEngine e = instance;
        if (e != null && e.backend() != null) {
            e.cancelPendingTeardown();
            e.endSession();
            e.currentGatedPackage = packageName;
            e.currentGateKind = GateKind.PIN;
            e.startSession(packageName);
        }
    }

    static void skipFlaggedGateFor(String packageName) {
        GateEngine e = instance;
        if (e != null && e.backend() != null) {
            e.cancelPendingTeardown();
            e.endSession();
            e.currentGatedPackage = packageName;
            e.currentGateKind = GateKind.COUNTDOWN;
            e.startSession(packageName);
            // Arm the auto-close budget once and let it keep running across leaving
            // and re-entering the app — a re-open shows the wait screen again but
            // does not hand out a fresh budget.
            if (Config.isBudgetEnabled(e.ctx)
                    && (e.budgetRunnable == null
                        || !packageName.equals(e.budgetPackage))) {
                e.startBudgetTimer(packageName);
            }
        }
    }

    static void skipTimeBlockGateFor(String packageName) {
        GateEngine e = instance;
        if (e != null && e.backend() != null) {
            e.cancelPendingTeardown();
            e.currentGatedPackage = null;
            e.currentGateKind = null;
            e.backend().removeGateUi();
        }
    }

    /**
     * Which gate an app needs, ignoring cooldowns and state: a time block wins, then the
     * app-lock PIN (unless still inside its unlock grace window), then the flagged wait.
     * Null means no gate.
     */
    static GateKind gateFor(boolean timeBlocked, boolean locked, boolean recentlyUnlocked,
                            boolean flagged) {
        if (timeBlocked) return GateKind.TIME_BLOCK;
        if (locked && !recentlyUnlocked) return GateKind.PIN;
        if (flagged) return GateKind.COUNTDOWN;
        return null;
    }

    private final Context ctx;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile Backend nerdBackend;
    private volatile Backend noobBackend;

    private String lastForegroundPackage = null;
    private String lastEventKey = null;
    private String currentGatedPackage = null;
    private GateKind currentGateKind = null;
    private Runnable budgetRunnable = null;
    private String budgetPackage = null;
    private Runnable warningRunnable = null;
    private Runnable pendingTeardown = null;
    private long homeReachedAt = 0L;
    private String sessionPackage = null;
    private long sessionStartMillis = 0L;
    private Runnable pendingGraceClear = null;
    private String pendingGraceClearPackage = null;

    private GateEngine(Context context) {
        this.ctx = context;
    }

    /**
     * The backend for the current mode, or null while that mode's source isn't running. In Nerd
     * mode Plain switches its own accessibility service off while a blocking app is open; gates
     * then fall back to activities, since overlays can no longer be drawn.
     */
    private Backend backend() {
        boolean nerd = Config.isNerdMode(ctx);
        boolean suspended = nerd && Config.isA11ySuspended(ctx);
        // Nerd mode with the service not (yet) connected: the watcher, if up, gates with activities.
        if (nerd && !suspended) return nerdBackend != null ? nerdBackend : noobBackend;
        if (noobBackend != null) return noobBackend;
        return suspended ? new NoobBackend(ctx) : null;
    }

    /** An event source went away. If nothing is left to gate with, close the open session. */
    private void sourceGone() {
        if (nerdBackend == null && noobBackend == null) {
            endSession();
            cancelPendingGraceClear();
        }
    }

    /** One foreground change: the window that just came up, as {@code package} / {@code class}. */
    void onForeground(String packageName, String className) {
        Backend b = backend();
        if (b == null) return;
        if (className == null) className = "";

        String eventKey = packageName + "/" + className;
        if (eventKey.equals(lastEventKey)) return;
        lastEventKey = eventKey;

        if (PASSTHROUGH_PACKAGES.contains(packageName)) return;
        if (className.equals("android.inputmethodservice.SoftInputWindow")) return;
        if (className.equals("com.android.settings.password.ConfirmDeviceCredentialActivity")) return;

        String own = ctx.getPackageName();
        String previousPackage = lastForegroundPackage;
        lastForegroundPackage = packageName;
        boolean overApp = own.equals(previousPackage) && b instanceof NoobBackend;

        // A locked app bounced right back to the foreground after a brief MainActivity
        // sighting below — that was a compositor flash (an in-app transition, e.g. opening
        // a settings sheet), not the user actually leaving, so the grace-clear it queued
        // never fires.
        if (packageName.equals(pendingGraceClearPackage)) {
            cancelPendingGraceClear();
        }

        if (packageName.equals(own)) {

            // Back at the home screen — a locked app the user just left re-locks now,
            // instead of coasting on its unlock grace window. Debounced: a transient
            // window-focus flash through the launcher during an in-app transition (opening
            // a dialog/sheet) reports the same event, so the clear only actually runs if
            // we're still away from that app a moment later.
            if (className.equals(own + ".MainActivity")
                    && previousPackage != null
                    && Config.getLockedPackages(ctx).contains(previousPackage)) {
                schedulePendingGraceClear(previousPackage);
            }

            if (className.startsWith(own + ".")) {

                boolean wasGated = currentGatedPackage != null;
                cancelPendingTeardown();
                // Leave budgetRunnable alone — a flagged app's auto-close budget keeps
                // ticking while you're away and is not reset when you go back to it.
                currentGatedPackage = null;
                currentGateKind = null;
                b.removeGateUi();
                endSession();
                if (wasGated) {
                    homeReachedAt = SystemClock.elapsedRealtime();
                }
            }
            return;
        }

        if (TimeBlockRules.getBlockingBlock(ctx, packageName) != null) {
            if (SystemClock.elapsedRealtime() - homeReachedAt < HOME_COOLDOWN_MILLIS) return;
            cancelPendingTeardown();
            if (!packageName.equals(currentGatedPackage)) {
                startTimeBlockGate(packageName, overApp);
            }
            return;
        }

        boolean locked = isLocked(packageName);
        boolean flagged = Config.getFlaggedPackages(ctx).contains(packageName);

        if (!locked && !flagged) {
            if (currentGatedPackage != null) schedulePendingTeardown();
            return;
        }

        if (SystemClock.elapsedRealtime() - homeReachedAt < HOME_COOLDOWN_MILLIS) return;
        cancelPendingTeardown();

        // A gate for this app is already on screen / its timer running — let it finish.
        if (packageName.equals(currentGatedPackage)) return;

        enforce(packageName, locked, flagged, overApp);
    }

    /** The event source was interrupted (accessibility feedback interrupted). */
    void onInterrupt() {
        if (backend() != null) backend().removeGateUi();
        cancelBudgetTimer();
        endSession();
    }

    /**
     * Apply the two independent gates to a freshly-foregrounded app. They don't know
     * about each other: the <b>lock gate</b> asks for a PIN unless the app is inside
     * its unlock grace window; the <b>flag gate</b> shows the reopen-lockout or the
     * wait countdown and then arms the auto-close budget. An app that is both passes
     * the lock gate first — {@link #onPinAccepted} re-runs the flag gate afterwards.
     */
    private void enforce(String packageName, boolean locked, boolean flagged, boolean overApp) {
        boolean unlocked = locked && Config.isAppRecentlyUnlocked(ctx, packageName);
        GateKind gate = gateFor(false, locked, unlocked, flagged);

        if (gate == GateKind.PIN) {
            beginPinGate(packageName, overApp);
            return;
        }
        if (locked) {
            Config.markAppUnlocked(ctx, packageName);   // refresh grace while in use
        }
        if (gate == GateKind.COUNTDOWN) {
            launchFlaggedGate(packageName, overApp);
            return;
        }

        // Locked only and already unlocked — nothing to show. Drop a gate left over
        // from a different app.
        if (currentGatedPackage != null && !currentGatedPackage.equals(packageName)) {
            schedulePendingTeardown();
        }
    }

    private boolean isLocked(String packageName) {
        return Config.isLocksEnabled(ctx)
                && Config.isApplockEnabled(ctx)
                && Config.getLockedPackages(ctx).contains(packageName);
    }

    /**
     * Send the just-opened app to the background and put the gate screen up in its
     * place, so the app is never reachable until the gate is passed — the same as
     * opening a locked/flagged app from Plain's own list. On success the gate
     * activity relaunches the app (and calls back through {@link #skipGateFor} /
     * {@link #skipFlaggedGateFor}).
     */
    private void launchFlaggedGate(String packageName, boolean overApp) {
        endSession();
        cancelBudgetTimer();
        backend().removeGateUi();
        currentGatedPackage = packageName;
        startSession(packageName);

        // Home first so the gated app drops out of view, then the gate on top of the
        // launcher. Going home is async, so let it settle before startActivity
        // or the gate can flash up and then be covered by the home transition.
        ForegroundExtras.markGatePending(packageName, ctx);
        Intent gate = new Intent(ctx, FlaggedGateActivity.class);
        gate.putExtra("package", packageName);
        gate.putExtra("label", AllAppsUsage.label(ctx.getPackageManager(), packageName));
        gate.putExtra("over", overApp);
        gate.putExtra("engine", true);
        gate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Noob gates cover the app where it is instead of flashing Home first (an incoming call
        // screen, say); the Nerd backend goes Home through the accessibility service as before.
        if (overApp || backend() instanceof NoobBackend) {
            ctx.startActivity(gate);
            return;
        }
        backend().goHome();
        handler.postDelayed(() -> ctx.startActivity(gate), 250);
    }

    private void beginPinGate(String packageName, boolean overApp) {
        endSession();
        cancelBudgetTimer();
        backend().removeGateUi();
        currentGatedPackage = packageName;
        currentGateKind = GateKind.PIN;
        startSession(packageName);
        ForegroundExtras.markGatePending(packageName, ctx);
        DebugLog.i("PIN gate for " + packageName + " over=" + overApp);   // DEBUG
        backend().showPin(packageName, overApp);
    }

    /** The PIN was right: unlock the app, and go on to the flag gate if it has one. */
    void onPinAccepted(String packageName) {
        Config.markAppUnlocked(ctx, packageName);
        backend().removeGateUi();
        if (Config.getFlaggedPackages(ctx).contains(packageName)) {
            launchFlaggedGate(packageName, false);
        }
    }

    /** The PIN prompt was dismissed: drop the gate and go home. */
    void cancelPinGate() {
        if (backend() != null) backend().removeGateUi();
        currentGatedPackage = null;
        currentGateKind = null;
        endSession();
        if (backend() != null) backend().goHome();
    }

    private void startTimeBlockGate(String packageName, boolean overApp) {
        endSession();
        currentGatedPackage = packageName;
        currentGateKind = GateKind.TIME_BLOCK;
        ForegroundExtras.markGatePending(packageName, ctx);
        backend().showTimeBlock(packageName, overApp);
    }

    /** The user chose Close on a gate: go home and let the gate state clear. */
    void closeGatedApp() {
        String packageName = currentGatedPackage;
        NotificationHelper.cancelClosingSoon(ctx, packageName);
        cancelBudgetTimer();
        if (backend() != null) {
            backend().removeGateUi();
            backend().goHome();
        }
        if (packageName != null) {

            schedulePendingTeardown();
        }
    }

    private void startSession(String packageName) {
        sessionPackage = packageName;
        sessionStartMillis = SystemClock.elapsedRealtime();
        UsageStore.recordOpen(ctx, packageName);
    }

    private void endSession() {
        if (sessionPackage != null) {
            long elapsed = SystemClock.elapsedRealtime() - sessionStartMillis;
            UsageStore.addUsageMillis(ctx, sessionPackage, elapsed);
            sessionPackage = null;
        }
    }

    private void startBudgetTimer(String packageName) {
        cancelBudgetTimer();
        budgetPackage = packageName;
        long budgetMillis = Config.getBudgetMinutes(ctx) * 60 * 1000L;

        long warningDelay = budgetMillis - WARNING_LEAD_MILLIS;
        if (warningDelay > 0) {
            warningRunnable = () -> {
                if (packageName.equals(lastForegroundPackage)) {
                    NotificationHelper.notifyClosingSoon(ctx, packageName);
                }
            };
            handler.postDelayed(warningRunnable, warningDelay);
        }

        budgetRunnable = () -> {
            budgetRunnable = null;
            budgetPackage = null;
            NotificationHelper.cancelClosingSoon(ctx, packageName);
            // The budget is spent — apply the reopen lockout even if the user stepped
            // away just before it expired.
            if (Config.isLockoutEnabled(ctx)) {
                Config.setLockoutUntil(ctx, packageName,
                        System.currentTimeMillis() + Config.getLockoutMinutes(ctx) * 60 * 1000L);
            }
            if (packageName.equals(lastForegroundPackage) && backend() != null) {
                backend().goHome();
            }
        };
        handler.postDelayed(budgetRunnable, budgetMillis);
    }

    private void cancelBudgetTimer() {
        if (budgetRunnable != null) {
            handler.removeCallbacks(budgetRunnable);
            budgetRunnable = null;
        }
        if (budgetPackage != null) NotificationHelper.cancelClosingSoon(ctx, budgetPackage);
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
            // Keep budgetRunnable alive — see the flag-budget note in onForeground.
            currentGatedPackage = null;
            currentGateKind = null;
            if (backend() != null) backend().removeGateUi();
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

    private void schedulePendingGraceClear(String packageName) {
        cancelPendingGraceClear();
        pendingGraceClearPackage = packageName;
        pendingGraceClear = () -> {
            pendingGraceClear = null;
            pendingGraceClearPackage = null;
            Config.clearAppUnlock(ctx, packageName);
        };
        handler.postDelayed(pendingGraceClear, HOME_FLASH_DEBOUNCE_MILLIS);
    }

    private void cancelPendingGraceClear() {
        if (pendingGraceClear != null) {
            handler.removeCallbacks(pendingGraceClear);
            pendingGraceClear = null;
        }
        pendingGraceClearPackage = null;
    }
}
