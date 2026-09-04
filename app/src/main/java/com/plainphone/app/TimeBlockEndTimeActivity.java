package com.plainphone.app;

import android.os.Bundle;

import java.util.List;

public class TimeBlockEndTimeActivity extends StepperActivity {

    private static final int STEP_MINUTES = 15;

    private String blockId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        blockId = getIntent().getStringExtra("blockId");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String title() {
        return "End time";
    }

    @Override
    protected boolean listStyle() {
        return true;   // a clock face, not a number to type
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
        return STEP_MINUTES;
    }

    @Override
    protected int max() {
        return 24 * 60;
    }

    @Override
    protected int currentValue() {
        TimeBlock block = TimeBlock.findById(Config.getTimeBlocks(this), blockId);
        return block != null ? block.endMinute : 24 * 60;
    }

    @Override
    protected void save(int value) {
        List<TimeBlock> blocks = Config.getTimeBlocks(this);
        TimeBlock block = TimeBlock.findById(blocks, blockId);
        if (block != null) {
            block.endMinute = value;
            Config.setTimeBlocks(this, blocks);
        }
    }

    @Override
    protected String format(int value) {
        return TimeBlock.formatTime(value % (24 * 60));
    }
}

