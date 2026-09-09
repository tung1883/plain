package com.plainphone.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

/**
 * Remembers which sub-screen was open so that leaving Plain for another app and
 * pressing Home brings you back to it instead of the bare home screen.
 *
 * <p>Plain is the launcher, so {@code MainActivity} is {@code singleTask}: the
 * Home button clears everything above it. That is the right behaviour when you
 * are <em>inside</em> Plain (Home = go home), but wrong when you were in, say,
 * To-do, switched to another app, and hit Home to come back. We tell the two
 * apart by watching the whole process go to the background: if a content screen
 * (not the home grid) was on top when Plain was last backgrounded, the next Home
 * press restores it.
 *
 * <p>All state is process-global and best-effort — a cold start just shows home.
 */
final class Nav {

    private Nav() {}

    /** How long a backgrounded screen stays eligible for restore. */
    private static final long WINDOW_MS = 20 * 60 * 1000;

    private static int started;                 // started-not-stopped activity count
    private static boolean lastResumedWasContent;
    private static boolean backgrounded;        // whole app is in the background
    private static boolean leftFromContent;     // ...and a content screen was on top

    private static Intent lastContentIntent;
    private static String lastContentClass;
    private static long lastContentAt;

    static void install(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity a) {
                started++;
                backgrounded = false;
            }

            @Override public void onActivityResumed(Activity a) {
                boolean content = !(a instanceof MainActivity);
                lastResumedWasContent = content;
                if (content && restorable(a.getClass().getName())) {
                    Intent src = a.getIntent();
                    lastContentIntent = src != null
                            ? new Intent(src).setClass(a, a.getClass())
                            : new Intent(a, a.getClass());
                    lastContentClass = a.getClass().getName();
                    lastContentAt = SystemClock.elapsedRealtime();
                }
                applyLabel(a);
            }

            @Override public void onActivityStopped(Activity a) {
                if (--started <= 0) {
                    started = 0;
                    backgrounded = true;
                    leftFromContent = lastResumedWasContent;
                }
            }

            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    /**
     * True (once) if the Home button that just re-delivered the launcher intent
     * should reopen the last content screen rather than show the home grid.
     */
    static boolean consumeRestore() {
        boolean ok = backgrounded && leftFromContent
                && lastContentIntent != null
                && SystemClock.elapsedRealtime() - lastContentAt < WINDOW_MS
                && restorable(lastContentClass);
        backgrounded = false;
        return ok;
    }

    static Intent restoreIntent() {
        return new Intent(lastContentIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /** Never bounce back into a gate, an overlay, or the idle art. */
    private static boolean restorable(String className) {
        if (className == null) return false;
        return !className.contains("Gate")
                && !className.contains("Onboarding")
                && !className.contains("PluginLockPrompt")
                && !className.contains("PixelScene")
                && !className.contains("ArtSlideshow")
                && !className.contains("TipRotate");
    }

    // --- Recents card labels -------------------------------------------

    private static void applyLabel(Activity a) {
        String label = label(a.getClass().getSimpleName());
        if (label != null) {
            a.setTaskDescription(new ActivityManager.TaskDescription(label));
        }
    }

    /** A friendly Recents-card name for the screens people actually leave open. */
    private static String label(String simpleName) {
        if (simpleName.startsWith("Dev")) return null;   // Dev screens label themselves
        if (simpleName.startsWith("Note")) return "Notes";
        if (simpleName.startsWith("Todo")) return "To-do";
        if (simpleName.startsWith("Record")) return "Recorder";
        if (simpleName.startsWith("Vault")) return "Vault";
        if (simpleName.startsWith("Tips") || simpleName.startsWith("TimeBlock")) return "Settings";
        if (simpleName.startsWith("Art") || simpleName.equals("PhotoCropActivity")) return "Photos";
        if (simpleName.endsWith("SettingsActivity") || simpleName.equals("SettingsActivity")
                || simpleName.equals("StatsActivity")) return "Settings";
        return null;
    }
}
