package com.plainphone.app;

/** When the content index may work (reading documents is heavy: PDFs take a while). */
final class IndexPolicy {

    private IndexPolicy() {}

    /** Below this battery level (percent) indexing waits unless the phone is charging. */
    static final int MIN_BATTERY = 20;

    /**
     * Plugged in: always. On battery: while it is at {@link #MIN_BATTERY} percent or more, and below
     * that only when the user allowed it ("Allow when low battery").
     */
    static boolean shouldRun(boolean charging, int batteryPercent, boolean allowLowBattery) {
        if (charging) return true;
        return batteryPercent >= MIN_BATTERY || allowLowBattery;
    }
}
