package com.plainphone.app;

public class BudgetTimeActivity extends StepperActivity {

    private static final int STEP_MINUTES = 1;
    private static final int MIN_MINUTES = 1;
    private static final int MAX_MINUTES = 180;

    @Override
    protected String title() {
        return "Auto-close flagged apps after";
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
        return Config.getBudgetMinutes(this);
    }

    @Override
    protected void save(int value) {
        Config.setBudgetMinutes(this, value);
    }

    @Override
    protected String format(int value) {
        return value + "m";
    }
}

