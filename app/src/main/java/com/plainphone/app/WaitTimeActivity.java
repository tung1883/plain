package com.plainphone.app;

public class WaitTimeActivity extends StepperActivity {

    private static final int STEP_SECONDS = 5;
    private static final int MIN_SECONDS = 10;
    private static final int MAX_SECONDS = 120;

    @Override
    protected String title() {
        return "Wait time before opening a flagged app";
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
        return Config.getWaitSeconds(this);
    }

    @Override
    protected void save(int value) {
        Config.setWaitSeconds(this, value);
    }

    @Override
    protected String format(int value) {
        return value + "s";
    }
}

