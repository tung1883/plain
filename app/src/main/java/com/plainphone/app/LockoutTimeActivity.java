package com.plainphone.app;

public class LockoutTimeActivity extends StepperActivity {

    private static final int STEP_MINUTES = 5;
    private static final int MIN_MINUTES = 5;
    private static final int MAX_MINUTES = 480;

    @Override
    protected String title() {
        return "Reopen lockout after auto-close";
    }

    @Override
    protected String stepLabel() {
        return STEP_MINUTES + "m";
    }

    @Override
    protected int step() {
        return STEP_MINUTES;
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
    protected int currentValue() {
        return Config.getLockoutMinutes(this);
    }

    @Override
    protected void save(int value) {
        Config.setLockoutMinutes(this, value);
    }

    @Override
    protected String format(int value) {
        return value + "m";
    }
}

