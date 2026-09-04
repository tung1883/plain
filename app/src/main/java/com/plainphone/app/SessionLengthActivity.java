package com.plainphone.app;

import android.os.Bundle;

public class SessionLengthActivity extends StepperActivity {

    private static final int STEP_MINUTES = 5;
    private static final int MIN_MINUTES = 5;
    private static final int MAX_MINUTES = 180;
    private static final int DEFAULT_MINUTES = 25;

    private String blockId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        blockId = getIntent().getStringExtra("blockId");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String title() {
        TimeBlock block = TimeBlock.findById(Config.getTimeBlocks(this), blockId);
        return "Start " + (block != null ? block.name : "block") + " now for how long?";
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
    protected String unitLabel() {
        return "min";
    }

    @Override
    protected int[] chips() {
        return new int[]{15, 25, 45, 60, 90};
    }

    @Override
    protected int currentValue() {
        return DEFAULT_MINUTES;
    }

    @Override
    protected void save(int value) {
        Config.setAdhocSession(this, blockId, System.currentTimeMillis() + value * 60_000L);
    }

    @Override
    protected String format(int value) {
        return value + "m";
    }
}

