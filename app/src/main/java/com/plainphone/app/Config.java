package com.plainphone.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Persisted, user-editable settings: wait time, auto-close budget, grayscale, flagged apps. */
class Config {

    private static final String PREFS = "config";

    private static final Set<String> DEFAULT_FLAGGED = new HashSet<>();
    static {
        DEFAULT_FLAGGED.add("com.instagram.android");
        DEFAULT_FLAGGED.add("com.zhiliaoapp.musically"); // TikTok
        DEFAULT_FLAGGED.add("com.google.android.youtube");
        DEFAULT_FLAGGED.add("com.twitter.android");
        DEFAULT_FLAGGED.add("com.whatsapp");
    }

    static int getWaitSeconds(Context context) {
        return prefs(context).getInt("wait_seconds", 10);
    }

    static void setWaitSeconds(Context context, int seconds) {
        prefs(context).edit().putInt("wait_seconds", seconds).apply();
    }

    static int getBudgetMinutes(Context context) {
        return prefs(context).getInt("budget_minutes", 5);
    }

    static void setBudgetMinutes(Context context, int minutes) {
        prefs(context).edit().putInt("budget_minutes", minutes).apply();
    }

    static boolean isGrayscaleEnabled(Context context) {
        return prefs(context).getBoolean("grayscale_enabled", true);
    }

    static void setGrayscaleEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("grayscale_enabled", enabled).apply();
    }

    static Set<String> getFlaggedPackages(Context context) {
        Set<String> stored = prefs(context).getStringSet("flagged_packages", null);
        return stored != null ? new HashSet<>(stored) : new HashSet<>(DEFAULT_FLAGGED);
    }

    static void setFlaggedPackages(Context context, Set<String> packages) {
        prefs(context).edit().putStringSet("flagged_packages", packages).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
