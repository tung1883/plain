package com.plainphone.app;

import android.os.Bundle;

/**
 * "Re-lock after" — how long a PIN-unlocked lock stays open once you leave it.
 * {@code EXTRA_LOCK_ID} null / empty edits the global default; a lock id edits that
 * lock's override and gets a leading "Use the default" row. Typed / saved in
 * minutes; stored in seconds.
 */
public class RelockTimeActivity extends StepperActivity {

    static final String EXTRA_LOCK_ID = "lock_id";

    private static final int MIN_MINUTES = 2;
    private static final int MAX_MINUTES = 120;
    private static final int[] CHIP_MINUTES = {2, 5, 10, 30};

    private String id;
    private boolean perLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        id = getIntent().getStringExtra(EXTRA_LOCK_ID);
        perLock = id != null && !id.isEmpty();
        super.onCreate(savedInstanceState);
    }

    /** For the hub / detail rows, which display seconds. */
    static String fmt(int seconds) {
        if (seconds < 60) return seconds + " sec";
        return (seconds / 60) + " min";
    }

    @Override
    protected String title() {
        return "Re-lock after";
    }

    @Override
    protected String stepLabel() {
        return "";
    }

    @Override
    protected int step() {
        return 1;
    }

    @Override
    protected int min() {
        return MIN_MINUTES;
    }

    @Override
    protected int max() {
        return MAX_MINUTES;
    }

    @Override
    protected String unitLabel() {
        return "min";
    }

    @Override
    protected int[] chips() {
        return CHIP_MINUTES;
    }

    @Override
    protected int currentValue() {
        int seconds = perLock && Config.hasRelockOverride(this, id)
                ? Config.getRelockSeconds(this, id)
                : Config.getRelockSeconds(this);
        return Math.max(MIN_MINUTES, seconds / 60);
    }

    @Override
    protected void save(int minutes) {
        if (perLock) {
            Config.setRelockSeconds(this, id, minutes * 60);
        } else {
            Config.setRelockSeconds(this, minutes * 60);
        }
    }

    @Override
    protected String format(int minutes) {
        return minutes + " min";
    }

    @Override
    protected String topRowLabel() {
        return perLock ? "Use the default (" + fmt(Config.getRelockSeconds(this)) + ")" : null;
    }

    @Override
    protected boolean topRowActive() {
        return perLock && !Config.hasRelockOverride(this, id);
    }

    @Override
    protected void onTopRow() {
        Config.clearRelock(this, id);
    }
}
