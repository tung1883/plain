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

    static void enable(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Settings.Secure.putInt(resolver, DALTONIZER_MODE, MODE_GRAYSCALE);
        Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 1);
    }

    static void disable(Context context) {
        Settings.Secure.putInt(context.getContentResolver(), DALTONIZER_ENABLED, 0);
    }
}
