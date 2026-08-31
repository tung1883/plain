package com.plainphone.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Config {

    private static final String PREFS = "config";

    private static final Set<String> DEFAULT_FLAGGED = new HashSet<>();
    static {
        DEFAULT_FLAGGED.add("com.instagram.android");
        DEFAULT_FLAGGED.add("com.zhiliaoapp.musically");
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

    static int getSettingsWaitSeconds(Context context) {
        return prefs(context).getInt("settings_wait_seconds", 0);
    }

    static void setSettingsWaitSeconds(Context context, int seconds) {
        prefs(context).edit().putInt("settings_wait_seconds", seconds).apply();
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

    static long getLockoutUntil(Context context, String packageName) {
        return prefs(context).getLong("lockout_until_" + packageName, 0L);
    }

    static void setLockoutUntil(Context context, String packageName, long untilEpochMillis) {
        prefs(context).edit().putLong("lockout_until_" + packageName, untilEpochMillis).apply();
    }

    static List<WebTarget> getWebTargets(Context context) {
        String stored = prefs(context).getString("web_targets", null);
        List<WebTarget> targets = new ArrayList<>();
        if (stored == null) return targets;
        try {
            JSONArray arr = new JSONArray(stored);
            for (int i = 0; i < arr.length(); i++) {
                targets.add(WebTarget.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            return new ArrayList<>();
        }
        return targets;
    }

    static void setWebTargets(Context context, List<WebTarget> targets) {
        JSONArray arr = new JSONArray();
        try {
            for (WebTarget target : targets) {
                arr.put(target.toJson());
            }
        } catch (JSONException e) {
            return;
        }
        prefs(context).edit().putString("web_targets", arr.toString()).apply();
    }

    static boolean isFileSearchEnabled(Context context) {
        return prefs(context).getBoolean("file_search_enabled", true);
    }

    static void setFileSearchEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("file_search_enabled", enabled).apply();
    }

    static boolean isContactSearchEnabled(Context context) {
        return prefs(context).getBoolean("contact_search_enabled", true);
    }

    static void setContactSearchEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("contact_search_enabled", enabled).apply();
    }

    static String getSearchEngine(Context context) {
        return prefs(context).getString("search_engine", "https://www.google.com/search?q=%s");
    }

    static void setSearchEngine(Context context, String urlTemplate) {
        prefs(context).edit().putString("search_engine", urlTemplate).apply();
    }

    static boolean isWebSearchEnabled(Context context) {
        return prefs(context).getBoolean("web_search_enabled", true);
    }

    static void setWebSearchEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("web_search_enabled", enabled).apply();
    }

    static Set<String> getCollapsedSections(Context context) {
        Set<String> stored = prefs(context).getStringSet("collapsed_sections", null);
        return stored != null ? new HashSet<>(stored) : new HashSet<>();
    }

    static void setCollapsedSections(Context context, Set<String> sections) {
        prefs(context).edit().putStringSet("collapsed_sections", sections).apply();
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

    static List<String> getPinnedPackages(Context context) {
        String stored = prefs(context).getString("pinned_packages", null);
        List<String> packages = new ArrayList<>();
        if (stored == null) return packages;
        try {
            JSONArray arr = new JSONArray(stored);
            for (int i = 0; i < arr.length(); i++) {
                packages.add(arr.getString(i));
            }
        } catch (JSONException e) {
            return new ArrayList<>();
        }
        return packages;
    }

    static void setPinnedPackages(Context context, List<String> packages) {
        JSONArray arr = new JSONArray();
        for (String pkg : packages) {
            arr.put(pkg);
        }
        prefs(context).edit().putString("pinned_packages", arr.toString()).apply();
    }

    static List<TimeBlock> getTimeBlocks(Context context) {
        String stored = prefs(context).getString("time_blocks", null);
        List<TimeBlock> blocks = new ArrayList<>();
        if (stored == null) return blocks;
        try {
            JSONArray arr = new JSONArray(stored);
            for (int i = 0; i < arr.length(); i++) {
                blocks.add(TimeBlock.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            return new ArrayList<>();
        }
        return blocks;
    }

    static void setTimeBlocks(Context context, List<TimeBlock> blocks) {
        JSONArray arr = new JSONArray();
        for (TimeBlock block : blocks) {
            arr.put(block.toJson());
        }
        prefs(context).edit().putString("time_blocks", arr.toString()).apply();
    }

    static List<Note> getNotes(Context context) {
        String stored = prefs(context).getString("notes", null);
        List<Note> notes = new ArrayList<>();
        if (stored == null) return notes;
        try {
            JSONArray arr = new JSONArray(stored);
            for (int i = 0; i < arr.length(); i++) {
                notes.add(Note.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            return new ArrayList<>();
        }
        return notes;
    }

    static void setNotes(Context context, List<Note> notes) {
        JSONArray arr = new JSONArray();
        for (Note note : notes) {
            arr.put(note.toJson());
        }
        prefs(context).edit().putString("notes", arr.toString()).apply();
    }

    static String getAdhocBlockId(Context context) {
        return prefs(context).getString("adhoc_block_id", null);
    }

    static long getAdhocUntil(Context context) {
        return prefs(context).getLong("adhoc_until", 0L);
    }

    static void setAdhocSession(Context context, String blockId, long untilEpochMillis) {
        prefs(context).edit()
                .putString("adhoc_block_id", blockId)
                .putLong("adhoc_until", untilEpochMillis)
                .apply();
    }

    static void clearAdhocSession(Context context) {
        prefs(context).edit().remove("adhoc_block_id").remove("adhoc_until").apply();
    }

    static long getOverrideUntil(Context context, String blockId) {
        return prefs(context).getLong("override_until_" + blockId, 0L);
    }

    static void setOverrideUntil(Context context, String blockId, long untilEpochMillis) {
        prefs(context).edit().putLong("override_until_" + blockId, untilEpochMillis).apply();
    }

    static GifScene getGifScene(Context context) {
        String stored = prefs(context).getString("pixel_gif_scene", GifScene.CITY.name());
        try {
            return GifScene.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return GifScene.CITY;
        }
    }

    static boolean isPhotoArtSelected(Context context) {
        return "photo".equals(prefs(context).getString("art_source", "gif"));
    }

    static void setPhotoArtSelected(Context context, boolean selected) {
        prefs(context).edit().putString("art_source", selected ? "photo" : "gif").apply();
    }

    static String getArtPhotoUri(Context context) {
        return prefs(context).getString("art_photo_uri", null);
    }

    static void setArtPhotoUri(Context context, String uriString) {
        prefs(context).edit().putString("art_photo_uri", uriString).apply();
    }

    static float getArtPhotoFocusX(Context context) {
        return prefs(context).getFloat("art_photo_focus_x", 0.5f);
    }

    static float getArtPhotoFocusY(Context context) {
        return prefs(context).getFloat("art_photo_focus_y", 0.5f);
    }

    static float getArtPhotoZoom(Context context) {
        return prefs(context).getFloat("art_photo_zoom", 1f);
    }

    static void setArtPhotoCrop(Context context, float focusX, float focusY, float zoom) {
        prefs(context).edit()
                .putFloat("art_photo_focus_x", focusX)
                .putFloat("art_photo_focus_y", focusY)
                .putFloat("art_photo_zoom", zoom)
                .apply();
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

    static HomeMode getHomeMode(Context context) {
        String stored = prefs(context).getString("home_mode", HomeMode.APPS.name());
        try {
            return HomeMode.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return HomeMode.APPS;
        }
    }

    static void setHomeMode(Context context, HomeMode mode) {
        prefs(context).edit().putString("home_mode", mode.name()).apply();
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
            throw new RuntimeException(e);
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

