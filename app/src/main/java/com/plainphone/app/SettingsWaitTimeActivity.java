package com.plainphone.app;

public class SettingsWaitTimeActivity extends StepperActivity {

    private static final int STEP_SECONDS = 5;
    private static final int MIN_SECONDS = 5;
    private static final int MAX_SECONDS = 60;

    @Override
    protected String title() {
        return "Settings wait time";
    }

    @Override
    protected String stepLabel() {
        return STEP_SECONDS + "s";
    }

    @Override
    protected int step() {
        return STEP_SECONDS;
    }

    @Override
    protected int min() {
        return MIN_SECONDS;
    }

    @Override
    protected int max() {
        return MAX_SECONDS;
    }

    @Override
    protected int currentValue() {
        return Config.getSettingsWaitSeconds(this);
    }

    @Override
    protected void save(int value) {
        Config.setSettingsWaitSeconds(this, value);
    }

    @Override
    protected String format(int value) {
        return value + "s";
    }
}

