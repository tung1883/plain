package com.plainphone.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
        android.content.SharedPreferences p = prefs(context);
        // One-shot repair: an earlier build silently flipped this off when the
        // READ_CONTACTS prompt was denied. Undo that once.
        if (!p.getBoolean("contact_search_repair_v1", false)) {
            p.edit().putBoolean("contact_search_repair_v1", true)
                    .putBoolean("contact_search_enabled", true).apply();
        }
        return p.getBoolean("contact_search_enabled", true);
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

    // --- feature locks (see Lock enum): keys "<area>_locked" / "<area>_unlock_until" ---

    static boolean isLocked(Context context, String area) {
        return prefs(context).getBoolean(area + "_locked", false);
    }

    static void setLocked(Context context, String area, boolean locked) {
        prefs(context).edit().putBoolean(area + "_locked", locked).apply();
    }

    static long getUnlockUntil(Context context, String area) {
        return prefs(context).getLong(area + "_unlock_until", 0L);
    }

    static void setUnlockUntil(Context context, String area, long millis) {
        prefs(context).edit().putLong(area + "_unlock_until", millis).apply();
    }

    /** Grace window during which a PIN-unlocked app / feature won't re-prompt. */
    static final long UNLOCK_GRACE_MS = 30_000L;

    static boolean isAppRecentlyUnlocked(Context context, String packageName) {
        return System.currentTimeMillis()
                < prefs(context).getLong("app_unlock_until_" + packageName, 0L);
    }

    /** Expire every per-app unlock grace window, so locked apps re-prompt at once. */
    static void clearAppUnlocks(Context context) {
        SharedPreferences prefs = prefs(context);
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("app_unlock_until_")) editor.remove(key);
        }
        editor.apply();
    }

    static void markAppUnlocked(Context context, String packageName) {
        if (packageName == null) return;
        long until = System.currentTimeMillis() + UNLOCK_GRACE_MS;
        String key = "app_unlock_until_" + packageName;
        if (until - prefs(context).getLong(key, 0L) > 5_000L) {
            prefs(context).edit().putLong(key, until).apply();
        }
    }

    static void clearAppUnlock(Context context, String packageName) {
        if (packageName == null) return;
        prefs(context).edit().remove("app_unlock_until_" + packageName).apply();
    }

    static String getNotesExportTree(Context context) {
        return prefs(context).getString("notes_export_tree", null);
    }

    static int getVaultNoteCount(Context context) {
        return prefs(context).getInt("vault_note_count", 0);
    }

    static void setVaultNoteCount(Context context, int count) {
        prefs(context).edit().putInt("vault_note_count", count).apply();
    }

    // --- voice recorder ---------------------------------------------------

    static List<Recording> getRecordings(Context context) {
        String stored = prefs(context).getString("recordings", null);
        List<Recording> out = new ArrayList<>();
        if (stored == null) return out;
        try {
            JSONArray arr = new JSONArray(stored);
            for (int i = 0; i < arr.length(); i++) {
                out.add(Recording.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            return new ArrayList<>();
        }
        return out;
    }

    static void setRecordings(Context context, List<Recording> recordings) {
        JSONArray arr = new JSONArray();
        for (Recording r : recordings) arr.put(r.toJson());
        prefs(context).edit().putString("recordings", arr.toString()).apply();
    }

    static String getRecorderFormat(Context context) {
        return prefs(context).getString("recorder_format", "m4a");
    }

    static void setRecorderFormat(Context context, String format) {
        prefs(context).edit().putString("recorder_format", format).apply();
    }

    static int getRecorderSampleRate(Context context) {
        return prefs(context).getInt("recorder_sample_rate", 44100);
    }

    static void setRecorderSampleRate(Context context, int rate) {
        prefs(context).edit().putInt("recorder_sample_rate", rate).apply();
    }

    static int getRecordingCount(Context context) {
        return prefs(context).getInt("recording_count", 0);
    }

    // Duration + waveform of vaulted recordings, keyed by vault docId (the audio
    // file carries no duration for PCM WAV / some m4a, has no capture-time
    // envelope, and the encrypted blob can't be inspected without decrypting).
    // { "<docId>": {"d": <durationMs>, "e": "<peaks csv>"} }.
    static JSONObject getVaultRecMeta(Context context) {
        String s = prefs(context).getString("vault_rec_meta", null);
        if (s == null) return new JSONObject();
        try {
            return new JSONObject(s);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private static JSONObject vaultRecEntry(Context context, String docId) {
        Object v = getVaultRecMeta(context).opt(docId);
        if (v instanceof JSONObject) return (JSONObject) v;
        JSONObject o = new JSONObject();
        if (v instanceof Number) {                       // migrate the bare-long form
            try { o.put("d", ((Number) v).longValue()); } catch (JSONException ignored) {}
        }
        return o;
    }

    static long getVaultRecDuration(Context context, String docId) {
        return vaultRecEntry(context, docId).optLong("d", 0L);
    }

    static String getVaultRecEnvelope(Context context, String docId) {
        return vaultRecEntry(context, docId).optString("e", "");
    }

    /** Merge in whichever of duration / envelope is supplied (0 / null = leave). */
    static void setVaultRecMeta(Context context, String docId, long durationMs, String envelope) {
        if (docId == null) return;
        JSONObject root = getVaultRecMeta(context);
        JSONObject entry = vaultRecEntry(context, docId);
        try {
            if (durationMs > 0) entry.put("d", durationMs);
            if (envelope != null && !envelope.isEmpty()) entry.put("e", envelope);
            if (entry.length() == 0) return;
            root.put(docId, entry);
        } catch (JSONException ignored) {
            return;
        }
        prefs(context).edit().putString("vault_rec_meta", root.toString()).apply();
    }

    static void setVaultRecDuration(Context context, String docId, long durationMs) {
        setVaultRecMeta(context, docId, durationMs, null);
    }

    static void removeVaultRecDuration(Context context, String docId) {
        JSONObject root = getVaultRecMeta(context);
        if (!root.has(docId)) return;
        root.remove(docId);
        prefs(context).edit().putString("vault_rec_meta", root.toString()).apply();
    }

    static boolean isRecorderNoiseReduction(Context context) {
        return prefs(context).getBoolean("recorder_noise_reduction", true);
    }

    static void setRecorderNoiseReduction(Context context, boolean on) {
        prefs(context).edit().putBoolean("recorder_noise_reduction", on).apply();
    }

    /** Monotonic counter for auto-naming recordings ("Recording 1", "Recording 2", …). */
    static int getRecorderNextNumber(Context context) {
        return prefs(context).getInt("recorder_next_number", 1);
    }

    static void setRecorderNextNumber(Context context, int n) {
        prefs(context).edit().putInt("recorder_next_number", n).apply();
    }

    static void setRecordingCount(Context context, int count) {
        prefs(context).edit().putInt("recording_count", count).apply();
    }

    static void setNotesExportTree(Context context, String uriString) {
        if (uriString == null) {
            prefs(context).edit().remove("notes_export_tree").apply();
        } else {
            prefs(context).edit().putString("notes_export_tree", uriString).apply();
        }
    }

    static boolean isTipsEnabled(Context context) {
        return prefs(context).getBoolean("tips_enabled", true);
    }

    static void setTipsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("tips_enabled", enabled).apply();
    }

    static boolean isQuotesEnabled(Context context) {
        return prefs(context).getBoolean("quotes_enabled", true);
    }

    static void setQuotesEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("quotes_enabled", enabled).apply();
    }

    /** Minutes between automatic tip/quote rotations; 0 = only advance on tap. */
    static int getTipRotateMinutes(Context context) {
        return prefs(context).getInt("tip_rotate_minutes", 5);
    }

    static void setTipRotateMinutes(Context context, int minutes) {
        prefs(context).edit().putInt("tip_rotate_minutes", minutes).apply();
    }

    static long getTipLastAdvance(Context context) {
        return prefs(context).getLong("tip_last_advance", 0L);
    }

    static void setTipLastAdvance(Context context, long millis) {
        prefs(context).edit().putLong("tip_last_advance", millis).apply();
    }

    static int getTipIndex(Context context) {
        return prefs(context).getInt("tip_index", 0);
    }

    static void setTipIndex(Context context, int index) {
        prefs(context).edit().putInt("tip_index", index).apply();
    }

    /** Null / absent → use the built-in defaults (see Tips). */
    static String getTipsText(Context context) {
        return prefs(context).getString("tips_text", null);
    }

    static void setTipsText(Context context, String text) {
        if (text == null) {
            prefs(context).edit().remove("tips_text").apply();
        } else {
            prefs(context).edit().putString("tips_text", text).apply();
        }
    }

    static String getQuotesText(Context context) {
        return prefs(context).getString("quotes_text", null);
    }

    static void setQuotesText(Context context, String text) {
        if (text == null) {
            prefs(context).edit().remove("quotes_text").apply();
        } else {
            prefs(context).edit().putString("quotes_text", text).apply();
        }
    }

    static String getTodosText(Context context) {
        return prefs(context).getString("todos", "");
    }

    static void setTodosText(Context context, String text) {
        prefs(context).edit().putString("todos", text).apply();
    }

    static String getTodosDoneText(Context context) {
        return prefs(context).getString("todos_done", "");
    }

    static void setTodosDoneText(Context context, String text) {
        prefs(context).edit().putString("todos_done", text).apply();
    }

    static String getTodoFileUri(Context context) {
        return prefs(context).getString("todo_file_uri", null);
    }

    static void setTodoFileUri(Context context, String uriString) {
        if (uriString == null) {
            prefs(context).edit().remove("todo_file_uri").apply();
        } else {
            prefs(context).edit().putString("todo_file_uri", uriString).apply();
        }
    }

    static boolean isTodosShowCompleted(Context context) {
        return prefs(context).getBoolean("todos_show_completed", true);
    }

    static void setTodosShowCompleted(Context context, boolean show) {
        prefs(context).edit().putBoolean("todos_show_completed", show).apply();
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

    static void setGifScene(Context context, GifScene scene) {
        prefs(context).edit().putString("pixel_gif_scene", scene.name()).apply();
    }

    static boolean isAccessWarnEnabled(Context context) {
        return prefs(context).getBoolean("access_warn_enabled", true);
    }

    static void setAccessWarnEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("access_warn_enabled", enabled).apply();
    }

    // --- home art: mode -----------------------------------------

    /** "off" | "on". "on" = show the current item in the selection (1 = static, 2+ = slideshow). */
    static String getArtMode(Context context) {
        migrateArt(context);
        return prefs(context).getString("art_mode", "on");
    }

    static void setArtMode(Context context, String mode) {
        prefs(context).edit().putString("art_mode", mode).apply();
    }

    // --- home art: per-scene crop + colour ("fx,fy,zoom,gray") --

    static float[] getSceneCrop(Context context, GifScene scene) {
        String v = prefs(context).getString("art_scene_crop_" + scene.name(), "0.5,0.5,1,1");
        String[] p = v.split(",");
        try {
            return new float[]{
                    Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]),
                    p.length > 3 ? Float.parseFloat(p[3]) : 1f};
        } catch (Exception e) {
            return new float[]{0.5f, 0.5f, 1f, 1f};
        }
    }

    static void setSceneCrop(Context context, GifScene scene, float fx, float fy, float zoom) {
        float gray = getSceneCrop(context, scene)[3];
        prefs(context).edit().putString("art_scene_crop_" + scene.name(),
                fx + "," + fy + "," + zoom + "," + gray).apply();
    }

    static void setSceneGray(Context context, GifScene scene, boolean gray) {
        float[] c = getSceneCrop(context, scene);
        prefs(context).edit().putString("art_scene_crop_" + scene.name(),
                c[0] + "," + c[1] + "," + c[2] + "," + (gray ? 1 : 0)).apply();
    }

    // --- home art: gallery -------------------------------------

    static String getArtGalleryJson(Context context) {
        return prefs(context).getString("art_gallery", "[]");
    }

    static void setArtGalleryJson(Context context, String json) {
        prefs(context).edit().putString("art_gallery", json).apply();
    }

    static java.util.List<String> getArtSelected(Context context) {
        java.util.List<String> out = new ArrayList<>();
        for (String s : prefs(context).getString("art_gallery_selected", "").split(",")) {
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    static void setArtSelected(Context context, java.util.List<String> ids) {
        prefs(context).edit().putString("art_gallery_selected", String.join(",", ids)).apply();
    }

    // --- home art: slideshow ----------------------------------

    static int getArtSlideshowMinutes(Context context) {
        return prefs(context).getInt("art_slideshow_minutes", 5);
    }

    static void setArtSlideshowMinutes(Context context, int minutes) {
        prefs(context).edit().putInt("art_slideshow_minutes", minutes).apply();
    }

    static int getArtSlideIndex(Context context) {
        return prefs(context).getInt("art_slide_index", 0);
    }

    static void setArtSlideIndex(Context context, int index) {
        prefs(context).edit().putInt("art_slide_index", index).apply();
    }

    static long getArtSlideLastAdvance(Context context) {
        return prefs(context).getLong("art_slide_last_advance", 0L);
    }

    static void setArtSlideLastAdvance(Context context, long millis) {
        prefs(context).edit().putLong("art_slide_last_advance", millis).apply();
    }

    // --- home art: one-shot migration from the single-slot model ---

    static void migrateArt(Context context) {
        android.content.SharedPreferences p = prefs(context);
        if (p.getBoolean("art_migrated", false)) return;
        p.edit().putBoolean("art_migrated", true).apply();

        boolean enabled = p.getBoolean("pixel_art_enabled", true);
        String source = p.getString("art_source", "gif");
        float fx = p.getFloat("art_photo_focus_x", 0.5f);
        float fy = p.getFloat("art_photo_focus_y", 0.5f);
        float zoom = p.getFloat("art_photo_zoom", 1f);

        if (!enabled) {
            p.edit().putString("art_mode", "off").apply();
            return;
        }
        p.edit().putString("art_mode", "on").apply();

        if ("photo".equals(source)) {
            String uri = p.getString("art_photo_uri", null);
            if (uri != null) {
                ArtItem it = ArtGallery.addFromUri(context, android.net.Uri.parse(uri), false);
                if (it != null) {
                    it.fx = fx; it.fy = fy; it.zoom = zoom; it.gray = true;
                    ArtGallery.replace(context, it);
                    p.edit().putString("art_gallery_selected", it.id).apply();
                    return;
                }
            }
        } else if ("customgif".equals(source)) {
            java.io.File old = new java.io.File(context.getFilesDir(), "art_custom.gif");
            if (old.exists()) {
                ArtItem it = ArtGallery.addFromFile(context, old, true, fx, fy, zoom, true);
                old.delete();
                if (it != null) {
                    p.edit().putString("art_gallery_selected", it.id).apply();
                    return;
                }
            }
        }
        // Scenes had no crop before — a scene selection becomes "scene:<NAME>".
        try {
            GifScene s = GifScene.valueOf(p.getString("pixel_gif_scene", GifScene.CITY.name()));
            p.edit().putString("art_gallery_selected", "scene:" + s.name()).apply();
        } catch (Exception ignored) {
        }
    }

    static FontChoice getFontChoice(Context context) {
        String stored = prefs(context).getString("font_choice", FontChoice.CASCADIA_MONO.name());
        try {
            return FontChoice.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return FontChoice.IBM_PLEX_MONO;
        }
    }

    static void setFontChoice(Context context, FontChoice font) {
        prefs(context).edit().putString("font_choice", font.name()).apply();
    }

    static boolean isVaultHiddenFromHome(Context context) {
        return prefs(context).getBoolean("vault_hidden_from_home", false);
    }

    static void setVaultHiddenFromHome(Context context, boolean hidden) {
        prefs(context).edit().putBoolean("vault_hidden_from_home", hidden).apply();
    }

    static java.util.List<HomeMode> getHomeModeOrder(Context context) {
        String stored = prefs(context).getString("home_mode_order", null);
        java.util.List<HomeMode> order = new java.util.ArrayList<>();
        if (stored != null) {
            for (String name : stored.split(",")) {
                try {
                    HomeMode mode = HomeMode.valueOf(name);
                    if (!order.contains(mode)) order.add(mode);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        for (HomeMode mode : HomeMode.values()) {
            if (!order.contains(mode)) order.add(mode);
        }
        if (isVaultHiddenFromHome(context)) order.remove(HomeMode.VAULT);
        return order;
    }

    static void setHomeModeOrder(Context context, java.util.List<HomeMode> order) {
        StringBuilder sb = new StringBuilder();
        for (HomeMode mode : order) {
            if (sb.length() > 0) sb.append(',');
            sb.append(mode.name());
        }
        prefs(context).edit().putString("home_mode_order", sb.toString()).apply();
    }

    static HomeMode getHomeMode(Context context) {
        String stored = prefs(context).getString("home_mode", HomeMode.APPS.name());
        try {
            HomeMode mode = HomeMode.valueOf(stored);
            if (mode == HomeMode.VAULT && isVaultHiddenFromHome(context)) return HomeMode.APPS;
            return mode;
        } catch (IllegalArgumentException e) {
            return HomeMode.APPS;
        }
    }

    static void setHomeMode(Context context, HomeMode mode) {
        prefs(context).edit().putString("home_mode", mode.name()).apply();
    }

    /** Absolute path of the vault directory, or null for the app-private default. */
    static String getVaultLocationPath(Context context) {
        return prefs(context).getString("vault_location_path", null);
    }

    static void setVaultLocationPath(Context context, String path) {
        if (path == null) {
            prefs(context).edit().remove("vault_location_path").apply();
        } else {
            prefs(context).edit().putString("vault_location_path", path).apply();
        }
    }

    /** One of "name" / "size" / "date". */
    static String getVaultSort(Context context) {
        return prefs(context).getString("vault_sort", "name");
    }

    static void setVaultSort(Context context, String sort) {
        prefs(context).edit().putString("vault_sort", sort).apply();
    }

    static boolean isVaultSortDesc(Context context) {
        return prefs(context).getBoolean("vault_sort_desc", false);
    }

    static void setVaultSortDesc(Context context, boolean desc) {
        prefs(context).edit().putBoolean("vault_sort_desc", desc).apply();
    }

    static int getVaultAutoLockSeconds(Context context) {
        return prefs(context).getInt("vault_autolock_seconds", 120);
    }

    static void setVaultAutoLockSeconds(Context context, int seconds) {
        prefs(context).edit().putInt("vault_autolock_seconds", seconds).apply();
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

