package com.plainphone.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * The few system settings Plain writes once {@code WRITE_SECURE_SETTINGS} has been granted with
 * adb (see {@link HowToActivity}): its own accessibility switch, and the colour-correction
 * setting used for monochrome.
 */
final class SecureSettings {

    private static final String DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled";
    private static final String DALTONIZER_TYPE = "accessibility_display_daltonizer";
    private static final int MONOCHROMACY = 0;
    private static final int DEFAULT_TYPE = 12;   // what Android uses while the setting is unset

    private SecureSettings() {}

    static boolean canWrite(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // --- accessibility switch ---

    /** Turn Plain's accessibility service off (banking app in front). Returns true if it did. */
    static boolean suspendAccessibility(Context context) {
        if (!canWrite(context)) return false;
        String list = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String pkg = context.getPackageName();
        String updated = removeComponent(list, pkg, AppMonitorService.class.getName());
        if (updated.equals(list == null ? "" : list)) return false;
        Config.setA11ySuspended(context, true);
        Settings.Secure.putString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updated);
        ForegroundWatcher.sync(context);   // now the watcher has to notice when the app is left
        return true;
    }

    /** Undo {@link #suspendAccessibility}. No-op unless Plain itself switched the service off. */
    static void resumeAccessibility(Context context) {
        if (!Config.isA11ySuspended(context)) return;
        if (canWrite(context)) {
            String list = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    addComponent(list, context.getPackageName(), AppMonitorService.class.getName()));
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        }
        Config.setA11ySuspended(context, false);
        ForegroundExtras.keepWatching(10_000L);
        ForegroundWatcher.sync(context);
    }

    /** {@code list} is the colon-separated enabled-services setting; drops our entry, either spelling. */
    static String removeComponent(String list, String pkg, String cls) {
        List<String> kept = new ArrayList<>();
        if (list != null) {
            for (String entry : list.split(":")) {
                if (entry.isEmpty() || isComponent(entry, pkg, cls)) continue;
                kept.add(entry);
            }
        }
        return String.join(":", kept);
    }

    static String addComponent(String list, String pkg, String cls) {
        List<String> all = new ArrayList<>();
        boolean present = false;
        if (list != null) {
            for (String entry : list.split(":")) {
                if (entry.isEmpty()) continue;
                if (isComponent(entry, pkg, cls)) present = true;
                all.add(entry);
            }
        }
        if (!present) all.add(pkg + "/" + cls);
        return String.join(":", all);
    }

    private static boolean isComponent(String entry, String pkg, String cls) {
        String shortForm = cls.startsWith(pkg + ".") ? cls.substring(pkg.length()) : cls;
        return entry.equalsIgnoreCase(pkg + "/" + cls) || entry.equalsIgnoreCase(pkg + "/" + shortForm);
    }

    // --- monochrome ---

    /** Whether the system colour correction is greyscale right now, whoever set it. */
    static boolean monochromeActive(Context context) {
        android.content.ContentResolver cr = context.getContentResolver();
        return Settings.Secure.getInt(cr, DALTONIZER_ENABLED, 0) == 1
                && Settings.Secure.getInt(cr, DALTONIZER_TYPE, DEFAULT_TYPE) == MONOCHROMACY;
    }

    /**
     * Switch the system colour correction to greyscale, remembering what it was. Safe to repeat:
     * the setting is shared with the developer option "Simulate color space", so turning
     * developer options off resets it behind our back, and calling this again puts it back.
     */
    static void monochromeOn(Context context) {
        if (!canWrite(context)) return;
        android.content.ContentResolver cr = context.getContentResolver();
        if (Config.getMonoSaved(context) == null) {
            int enabled = Settings.Secure.getInt(cr, DALTONIZER_ENABLED, 0);
            int type = Settings.Secure.getInt(cr, DALTONIZER_TYPE, DEFAULT_TYPE);
            Config.setMonoSaved(context, enabled + "," + type);
        } else if (monochromeActive(context)) {
            return;
        }
        Settings.Secure.putInt(cr, DALTONIZER_TYPE, MONOCHROMACY);
        Settings.Secure.putInt(cr, DALTONIZER_ENABLED, 1);
    }

    /** Put the colour correction back the way it was before {@link #monochromeOn}. */
    static void monochromeOff(Context context) {
        String saved = Config.getMonoSaved(context);
        if (saved == null) return;
        if (canWrite(context)) {
            String[] parts = saved.split(",");
            int enabled = 0;
            int type = DEFAULT_TYPE;
            try {
                enabled = Integer.parseInt(parts[0]);
                type = Integer.parseInt(parts[1]);
            } catch (RuntimeException ignored) {
                // fall back to off
            }
            android.content.ContentResolver cr = context.getContentResolver();
            Settings.Secure.putInt(cr, DALTONIZER_TYPE, type);
            Settings.Secure.putInt(cr, DALTONIZER_ENABLED, enabled);
        }
        Config.setMonoSaved(context, null);
    }
}
