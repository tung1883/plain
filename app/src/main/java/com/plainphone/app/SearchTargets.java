package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * The searchable catalog of destinations that aren't apps: Plain's own screens and a
 * curated set of phone settings pages. Both are static lists rather than anything
 * discovered at runtime — Android exposes no way to enumerate settings screens, and
 * hand-listing them is what lets each one carry the alternate words people actually
 * type ("wifi" for Wi-Fi, "screen time" for Usage).
 */
class SearchTargets {

    private SearchTargets() {}

    /** A destination plus the alternate words that should find it. */
    private static class Target {
        final String title;
        final String subtitle;
        final String[] keywords;

        Target(String title, String subtitle, String... keywords) {
            this.title = title;
            this.subtitle = subtitle;
            this.keywords = keywords;
        }
    }

    private static class PlainTarget extends Target {
        /**
         * Screens reached from Settings stay behind the settings friction gate here too —
         * search is a faster way to find a screen, never a way around the wait that
         * protects it.
         */
        final Class<? extends Activity> destination;
        final boolean gated;

        PlainTarget(String title, String subtitle, Class<? extends Activity> destination,
                    boolean gated, String... keywords) {
            super(title, subtitle, keywords);
            this.destination = destination;
            this.gated = gated;
        }
    }

    private static class SystemTarget extends Target {
        final String action;

        SystemTarget(String title, String subtitle, String action, String... keywords) {
            super(title, subtitle, keywords);
            this.action = action;
        }
    }

    private static final PlainTarget[] PLAIN = {
            new PlainTarget("Usage", "Screen time for today", StatsActivity.class, false,
                    "stats", "screen time", "statistics", "minutes"),
            new PlainTarget("Time Blocks", "Scheduled app restrictions", TimeBlocksActivity.class, false,
                    "schedule", "focus", "block", "study", "sleep"),
            new PlainTarget("Settings", "All of Plain's settings", SettingsActivity.class, true,
                    "preferences", "options", "config"),
            new PlainTarget("Flagged apps", "Apps that warn and auto-close",
                    FlaggedAppsActivity.class, true, "flag", "distracting", "warn", "auto close"),
            new PlainTarget("App lock", "Require a PIN to open an app",
                    LockedAppsActivity.class, true, "lock", "pin", "password", "protect"),
            new PlainTarget("Hide apps from app list", "Keep apps out of the home list",
                    HiddenAppsActivity.class, true, "hide", "hidden", "remove from list"),
            new PlainTarget("Settings wait time", "How long the settings gate waits",
                    SettingsWaitTimeActivity.class, true, "wait", "delay", "friction", "gate"),
            new PlainTarget("Home screen art", "Pick the pixel art scene",
                    PixelSceneActivity.class, true, "art", "pixel", "animation", "wallpaper", "gif"),
            new PlainTarget("My web searches", "Your own search URLs",
                    WebTargetsActivity.class, true, "web", "search engine", "url", "wiktionary",
                    "dictionary", "wiki"),
            new PlainTarget("Font", "Change the launcher typeface", FontActivity.class, true,
                    "typeface", "typography", "serif", "text"),
            // Grayscale is a toggle inside Settings rather than its own screen, so it lands
            // on Settings itself — searching "grayscale" should still get you to the switch.
            new PlainTarget("Grayscale", "Toggle in Settings", SettingsActivity.class, true,
                    "grayscale", "greyscale", "black and white", "color", "colour", "saturation"),
    };

    private static final SystemTarget[] SYSTEM = {
            new SystemTarget("Wi-Fi", null, Settings.ACTION_WIFI_SETTINGS,
                    "wifi", "wireless", "internet", "network"),
            new SystemTarget("Bluetooth", null, Settings.ACTION_BLUETOOTH_SETTINGS,
                    "pair", "headphones", "earbuds"),
            new SystemTarget("Mobile network", null, Settings.ACTION_DATA_ROAMING_SETTINGS,
                    "cellular", "data", "sim", "roaming", "carrier"),
            new SystemTarget("Airplane mode", null, Settings.ACTION_AIRPLANE_MODE_SETTINGS,
                    "flight", "plane", "offline"),
            new SystemTarget("Display", null, Settings.ACTION_DISPLAY_SETTINGS,
                    "brightness", "screen", "dark mode", "font size", "timeout"),
            new SystemTarget("Sound", null, Settings.ACTION_SOUND_SETTINGS,
                    "volume", "ringtone", "silent", "vibrate", "audio"),
            new SystemTarget("Battery", null, Settings.ACTION_BATTERY_SAVER_SETTINGS,
                    "power", "charge", "saver"),
            new SystemTarget("Apps", null, Settings.ACTION_APPLICATION_SETTINGS,
                    "applications", "uninstall", "permissions", "default apps"),
            new SystemTarget("Storage", null, Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
                    "space", "free up", "memory", "disk"),
            new SystemTarget("Location", null, Settings.ACTION_LOCATION_SOURCE_SETTINGS,
                    "gps", "maps"),
            new SystemTarget("Security", null, Settings.ACTION_SECURITY_SETTINGS,
                    "lock screen", "fingerprint", "password", "pin"),
            new SystemTarget("Accessibility", null, Settings.ACTION_ACCESSIBILITY_SETTINGS,
                    "talkback", "accessibility service"),
            new SystemTarget("Date & time", null, Settings.ACTION_DATE_SETTINGS,
                    "clock", "timezone", "24 hour"),
            new SystemTarget("Language & input", null, Settings.ACTION_LOCALE_SETTINGS,
                    "language", "locale", "translate"),
            new SystemTarget("Keyboard", null, Settings.ACTION_INPUT_METHOD_SETTINGS,
                    "input method", "typing", "autocorrect"),
            new SystemTarget("Accounts", null, Settings.ACTION_SYNC_SETTINGS,
                    "google account", "sync", "sign in"),
            new SystemTarget("Default home app", null, Settings.ACTION_HOME_SETTINGS,
                    "launcher", "home screen", "default launcher"),
            new SystemTarget("Usage access", "Needed for all-apps stats",
                    Settings.ACTION_USAGE_ACCESS_SETTINGS, "permission", "usage stats"),
            new SystemTarget("Developer options", null, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
                    "adb", "usb debugging", "developer"),
            new SystemTarget("About phone", null, Settings.ACTION_DEVICE_INFO_SETTINGS,
                    "android version", "model", "serial", "build"),
            new SystemTarget("All settings", null, Settings.ACTION_SETTINGS,
                    "system settings", "android settings"),
    };

    /** Plain's own screens matching the query. */
    static List<SearchResult> plain(Activity host, TextMatch.Query query) {
        List<SearchResult> results = new ArrayList<>();

        // Not a screen, so it isn't in the table: an action that runs and stays put.
        int screenOff = TextMatch.score("Screen off",
                new String[]{"lock", "sleep", "turn off", "display off"}, query);
        if (screenOff != TextMatch.NO_MATCH) {
            results.add(new SearchResult(SearchResult.Kind.PLAIN, "Screen off", null,
                    screenOff, AppMonitorService::lockScreen));
        }

        for (PlainTarget target : PLAIN) {
            int score = TextMatch.score(target.title, target.keywords, query);
            if (score == TextMatch.NO_MATCH) continue;
            results.add(new SearchResult(SearchResult.Kind.PLAIN, target.title, target.subtitle,
                    score, () -> host.startActivity(openIntent(host, target))));
        }
        return results;
    }

    private static Intent openIntent(Activity host, PlainTarget target) {
        if (!target.gated) return new Intent(host, target.destination);

        Intent gate = new Intent(host, SettingsGateActivity.class);
        if (target.destination != SettingsActivity.class) {
            gate.putExtra(SettingsActivity.EXTRA_DESTINATION, target.destination.getName());
        }
        return gate;
    }

    /** Phone settings pages matching the query, skipping any this device can't open. */
    static List<SearchResult> system(Activity host, TextMatch.Query query) {
        List<SearchResult> results = new ArrayList<>();
        for (SystemTarget target : SYSTEM) {
            int score = TextMatch.score(target.title, target.keywords, query);
            if (score == TextMatch.NO_MATCH) continue;

            Intent intent = new Intent(target.action);
            // Not every Settings.ACTION_* screen exists on every device or OEM skin;
            // showing a row that dies with ActivityNotFoundException is worse than
            // leaving it out.
            if (intent.resolveActivity(host.getPackageManager()) == null) continue;

            results.add(new SearchResult(SearchResult.Kind.SYSTEM, target.title, target.subtitle,
                    score, () -> host.startActivity(intent)));
        }
        return results;
    }

}
