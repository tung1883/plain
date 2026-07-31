package com.plainphone.app;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

/**
 * Toggles Android's built-in system-wide grayscale display filter
 * (the same setting Digital Wellbeing's Bedtime Mode uses).
 * Requires android.permission.WRITE_SECURE_SETTINGS, granted via:
 *   adb shell pm grant <package> android.permission.WRITE_SECURE_SETTINGS
 */
class GrayscaleController {

    private static final String DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled";
    private static final String DALTONIZER_MODE = "accessibility_display_daltonizer";
    private static final int MODE_GRAYSCALE = 0; // simulate monochromacy

    /** Returns whether the write actually succeeded, so callers can surface a failure. */
    static boolean enable(Context context) {
        try {
            ContentResolver resolver = context.getContentResolver();
            Settings.Secure.putInt(resolver, DALTONIZER_MODE, MODE_GRAYSCALE);
            Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 1);
            return true;
        } catch (SecurityException e) {
            // WRITE_SECURE_SETTINGS is granted via adb, not a normal runtime prompt, so it
            // can be missing (e.g. lost across a reinstall) or never granted at all. Onboarding
            // already tells the user this step is optional — the app must not crash over it.
            return false;
        }
    }

    static boolean disable(Context context) {
        try {
            Settings.Secure.putInt(context.getContentResolver(), DALTONIZER_ENABLED, 0);
            return true;
        } catch (SecurityException e) {
            // See enable() — missing permission should never crash the service.
            return false;
        }
    }
}
