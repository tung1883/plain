package com.plainphone.app;

import android.content.Context;

/** "Screen off": Nerd mode only, via the accessibility service. Noob mode has none (device-admin lockNow() disables fingerprint unlock). */
final class ScreenLock {

    private ScreenLock() {}

    static boolean available(Context context) {
        return Config.isNerdMode(context) && AppMonitorService.isEnabled(context);
    }

    static void lock(Context context) {
        if (available(context)) AppMonitorService.lockScreen();
    }
}
