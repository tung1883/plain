package com.plainphone.app;

/** Vault auto-lock timeout (minutes of inactivity before re-lock; stored in seconds). */
public class VaultAutoLockActivity extends StepperActivity {

    private static final int MIN_MINUTES = 1;
    private static final int MAX_MINUTES = 30;

    @Override
    protected String title() {
        return "Vault auto-lock";
    }

    @Override
    protected String stepLabel() {
        return "1 min";
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
        return new int[]{1, 2, 5, 10, 30};
    }

    @Override
    protected int currentValue() {
        return Math.max(MIN_MINUTES, Config.getVaultAutoLockSeconds(this) / 60);
    }

    @Override
    protected void save(int minutes) {
        Config.setVaultAutoLockSeconds(this, minutes * 60);
    }

    @Override
    protected String format(int minutes) {
        return minutes + " min";
    }
}
