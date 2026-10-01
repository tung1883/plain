package com.plainphone.app;

import android.content.Context;

import java.util.Set;

/**
 * Per-app reactions to the foreground app that don't gate anything: greyscale for chosen apps,
 * and switching Plain's own accessibility service off while a banking app is open. Fed by
 * whichever source sees the foreground change (the watcher, or the accessibility service).
 * Both need {@code WRITE_SECURE_SETTINGS}. Main thread only.
 */
final class ForegroundExtras {

    private static final long GATE_PENDING_MILLIS = 60_000L;
    private static final long LAUNCH_PENDING_MILLIS = 8_000L;

    /**
     * A gate (PIN, wait, time block) is up for a blocking app: its screens are Plain's own, so
     * they must not switch accessibility back on before the app has actually been opened.
     */
    private static volatile long gatePendingUntil;

    /**
     * Right after accessibility is switched back on the service takes a few seconds to reconnect
     * and misses app opens meanwhile, so the watcher stays up a little longer to cover the gap.
     */
    private static volatile long keepWatchingUntil;

    /**
     * Bumped each time a gate starts for a blocking app. A gate screen left over from an earlier
     * gate can be destroyed at any moment; it must not cancel the newer gate's state.
     */
    private static volatile int gateGeneration;

    static int gateGeneration() {
        return gateGeneration;
    }

    static void gateClosedWithoutOpening(Context context, int generation) {
        if (generation == gateGeneration) gateCancelled(context);
    }

    static void keepWatching(long millis) {
        keepWatchingUntil = System.currentTimeMillis() + millis;
    }

    private ForegroundExtras() {}

    static void markGatePending(String packageName, Context context) {
        if (Config.isNerdMode(context) && Config.getBankingPackages(context).contains(packageName)) {
            gatePendingUntil = System.currentTimeMillis() + GATE_PENDING_MILLIS;
            gateGeneration++;
        }
    }

    private static boolean gatePending() {
        return System.currentTimeMillis() < gatePendingUntil;
    }

    /**
     * Plain is about to show gate screens for a blocking app it opened itself: switch accessibility
     * off now, so it is well and truly off by the time the PIN has been typed.
     */
    static void beforeOpen(Context context, String packageName) {
        if (Config.isNerdMode(context) && Config.getBankingPackages(context).contains(packageName)) {
            SecureSettings.suspendAccessibility(context);
            gatePendingUntil = System.currentTimeMillis() + GATE_PENDING_MILLIS;
            gateGeneration++;
        }
    }

    /**
     * Run {@code action} (opening a blocking app) once the accessibility service has really let go.
     * A banking app that starts while the service is still bound can notice it and close itself.
     */
    static void afterA11yOff(Context context, Runnable action) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        if (!Config.isA11ySuspended(context) || !AppMonitorService.isBound()) {
            action.run();
            return;
        }
        long deadline = android.os.SystemClock.elapsedRealtime() + 1200L;
        Runnable[] check = new Runnable[1];
        check[0] = () -> {
            if (!AppMonitorService.isBound()) {
                handler.postDelayed(action, 120L);   // let the system finish tearing it down
            } else if (android.os.SystemClock.elapsedRealtime() >= deadline) {
                action.run();
            } else {
                handler.postDelayed(check[0], 40L);
            }
        };
        handler.post(check[0]);
    }

    /** A gate screen closed without opening the app: nothing is holding accessibility off any more. */
    static void gateCancelled(Context context) {
        gatePendingUntil = 0;
        SecureSettings.resumeAccessibility(context);
    }

    /** Home is showing; switch accessibility back on unless a gate is still on its way to the app. */
    static void resumeIfIdle(Context context) {
        if (!gatePending()) SecureSettings.resumeAccessibility(context);
    }

    /** Whether anything here is configured and could act, so the watcher has a reason to run. */
    static boolean needed(Context context) {
        if (!SecureSettings.canWrite(context)) return false;
        if (Config.isNerdMode(context)) {
            // The accessibility service reports foreground changes itself. The watcher is only
            // needed while Plain has switched that service off for a blocking app (to notice
            // when you leave it), or when the service isn't on at all.
            if (Config.isA11ySuspended(context)) return true;
            if (System.currentTimeMillis() < keepWatchingUntil) return true;
            return !AppMonitorService.isEnabled(context) && tracksMonochromeApps(context);
        }
        return tracksMonochromeApps(context);
    }

    /** Monochrome for chosen apps needs to know what is in front; whole-screen mode doesn't. */
    private static boolean tracksMonochromeApps(Context context) {
        return Config.MONO_APPS.equals(Config.getMonochromeMode(context))
                && !Config.getMonochromePackages(context).isEmpty();
    }

    /** Whole-screen greyscale is meant to stay on: put it back if something switched it off. */
    static void reapplyWholeScreenMonochrome(Context context) {
        if (Config.MONO_ALL.equals(Config.getMonochromeMode(context))) applyMonochromeMode(context);
    }

    /** Apply the monochrome mode right now, after it was changed. */
    static void applyMonochromeMode(Context context) {
        if (!SecureSettings.canWrite(context)) return;
        if (Config.MONO_ALL.equals(Config.getMonochromeMode(context))) {
            SecureSettings.monochromeOn(context);
        } else {
            SecureSettings.monochromeOff(context);   // per-app mode switches it on again as apps open
        }
    }

    static void onForeground(Context context, String packageName, String className) {
        if (packageName == null) return;
        // Shade, keyboard and similar windows aren't the app the user is in.
        if (packageName.equals("com.android.systemui")) return;
        if ("android.inputmethodservice.SoftInputWindow".equals(className)) return;
        // No "same app as last time" shortcut: Home can flip accessibility back on without this
        // ever seeing the launcher, and the next visit to a blocking app must still switch it off.
        // Every call below is a cheap no-op when the state already matches.
        if (!SecureSettings.canWrite(context)) return;
        // Plain's own gate screens for a blocking app: keep accessibility off until it opens.
        if (packageName.equals(context.getPackageName()) && gatePending()) return;

        Set<String> mono = Config.getMonochromePackages(context);
        if (Config.MONO_ALL.equals(Config.getMonochromeMode(context))) {
            SecureSettings.monochromeOn(context);
        } else if (mono.contains(packageName)) {
            SecureSettings.monochromeOn(context);
        } else {
            SecureSettings.monochromeOff(context);
        }

        if (Config.isNerdMode(context)) {
            if (Config.getBankingPackages(context).contains(packageName)) {
                gatePendingUntil = 0;   // the app is up; leaving it switches accessibility back on
                SecureSettings.suspendAccessibility(context);
            } else {
                SecureSettings.resumeAccessibility(context);
            }
        }
    }

    /** Called before Plain itself opens {@code packageName}: switch accessibility off first if it's a banking app. */
    static void beforeLaunch(Context context, String packageName) {
        // The gate is done but the app is not up yet. Plain's own screens closing (Home flashing
        // by) must not switch accessibility back on in that gap, or the app sees it and quits.
        gatePendingUntil = System.currentTimeMillis() + LAUNCH_PENDING_MILLIS;
        if (Config.isNerdMode(context) && Config.getBankingPackages(context).contains(packageName)) {
            SecureSettings.suspendAccessibility(context);
        }
    }

    /**
     * Whatever was in front in the last couple of minutes, for callers that just came up and
     * may have missed the moment it opened (the accessibility service right after being switched
     * back on, the watcher on start). Extras only — nothing gets gated for an app already open.
     */
    @SuppressWarnings("deprecation")
    static void seedFromRecent(Context context) {
        if (!AllAppsUsage.hasUsageAccess(context)) return;
        android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager)
                context.getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        android.app.usage.UsageEvents events = usm.queryEvents(now - 2 * 60_000L, now);
        android.app.usage.UsageEvents.Event e = new android.app.usage.UsageEvents.Event();
        String pkg = null;
        String cls = null;
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            if (e.getEventType() == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                pkg = e.getPackageName();
                cls = e.getClassName();
            }
        }
        if (pkg != null) onForeground(context, pkg, cls);
    }

    /** The watcher is stopping: leave the system the way it was found. */
    static void reset(Context context) {
        if (!Config.MONO_ALL.equals(Config.getMonochromeMode(context))) {
            SecureSettings.monochromeOff(context);
        }
        SecureSettings.resumeAccessibility(context);
    }
}
