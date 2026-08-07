package com.plainphone.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Persisted, user-editable settings: wait time, auto-close budget, grayscale, flagged/hidden/locked apps, lock PIN. */
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

    static int getLockoutMinutes(Context context) {
        return prefs(context).getInt("lockout_minutes", 15);
    }

    static void setLockoutMinutes(Context context, int minutes) {
        prefs(context).edit().putInt("lockout_minutes", minutes).apply();
    }

    static boolean isBudgetEnabled(Context context) {
        return prefs(context).getBoolean("budget_enabled", false);
    }

    static void setBudgetEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("budget_enabled", enabled).apply();
    }

    static boolean isLockoutEnabled(Context context) {
        return prefs(context).getBoolean("lockout_enabled", false);
    }

    static void setLockoutEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("lockout_enabled", enabled).apply();
    }

    /** Epoch millis until which a flagged app is barred from reopening after an auto-close, keyed per package. */
    static long getLockoutUntil(Context context, String packageName) {
        return prefs(context).getLong("lockout_until_" + packageName, 0L);
    }

    static void setLockoutUntil(Context context, String packageName, long untilEpochMillis) {
        prefs(context).edit().putLong("lockout_until_" + packageName, untilEpochMillis).apply();
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

    static Set<String> getHiddenPackages(Context context) {
        Set<String> stored = prefs(context).getStringSet("hidden_packages", null);
        return stored != null ? new HashSet<>(stored) : new HashSet<>();
    }

    static void setHiddenPackages(Context context, Set<String> packages) {
        prefs(context).edit().putStringSet("hidden_packages", packages).apply();
    }

    static Set<String> getLockedPackages(Context context) {
        Set<String> stored = prefs(context).getStringSet("locked_packages", null);
        return stored != null ? new HashSet<>(stored) : new HashSet<>();
    }

    static void setLockedPackages(Context context, Set<String> packages) {
        prefs(context).edit().putStringSet("locked_packages", packages).apply();
    }

    static GifScene getGifScene(Context context) {
        String stored = prefs(context).getString("pixel_gif_scene", GifScene.CITY.name());
        try {
            return GifScene.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return GifScene.CITY;
        }
    }

    static void setGifScene(Context context, GifScene scene) {
        prefs(context).edit().putString("pixel_gif_scene", scene.name()).apply();
    }

    static boolean isPixelArtEnabled(Context context) {
        return prefs(context).getBoolean("pixel_art_enabled", true);
    }

    static void setPixelArtEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("pixel_art_enabled", enabled).apply();
    }

    static FontChoice getFontChoice(Context context) {
        String stored = prefs(context).getString("font_choice", FontChoice.GEORGIA.name());
        try {
            return FontChoice.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return FontChoice.GEORGIA;
        }
    }

    static void setFontChoice(Context context, FontChoice font) {
        prefs(context).edit().putString("font_choice", font.name()).apply();
    }

    static boolean isPinSet(Context context) {
        return prefs(context).getString("lock_pin_hash", null) != null;
    }

    static void setLockPin(Context context, String pin) {
        String salt = generateSalt();
        prefs(context).edit()
                .putString("lock_pin_salt", salt)
                .putString("lock_pin_hash", hashPin(pin, salt))
                .apply();
    }

    static boolean checkLockPin(Context context, String pin) {
        String salt = prefs(context).getString("lock_pin_salt", null);
        String storedHash = prefs(context).getString("lock_pin_hash", null);
        if (salt == null || storedHash == null) return false;
        return storedHash.equals(hashPin(pin, salt));
    }

    private static String generateSalt() {
        byte[] saltBytes = new byte[16];
        new java.security.SecureRandom().nextBytes(saltBytes);
        return android.util.Base64.encodeToString(saltBytes, android.util.Base64.NO_WRAP);
    }

    private static String hashPin(String pin, String salt) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] hashed = digest.digest(pin.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return android.util.Base64.encodeToString(hashed, android.util.Base64.NO_WRAP);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // SHA-256 is guaranteed present on Android
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
