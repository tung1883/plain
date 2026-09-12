package com.plainphone.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.util.concurrent.atomic.AtomicInteger;

/** Process entry point — wires up {@link Nav} so Plain reopens where you left off. */
public class PlainApp extends Application {

    // Any Activity resumed right now: Android 10+ only lets the app the user is
    // actually looking at read the clipboard, so DevService's phone->PC
    // clipboard poll (DevService.java) checks this before trying.
    private static final AtomicInteger resumedActivities = new AtomicInteger(0);

    static boolean isForeground() {
        return resumedActivities.get() > 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Nav.install(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) { resumedActivities.incrementAndGet(); }
            @Override public void onActivityPaused(Activity a) { resumedActivities.decrementAndGet(); }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }
}
