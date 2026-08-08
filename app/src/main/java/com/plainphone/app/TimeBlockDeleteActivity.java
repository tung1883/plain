package com.plainphone.app;

import android.os.Bundle;

import java.util.List;

/** Confirms deleting a time block — friction only applies since it removes an active restriction. */
public class TimeBlockDeleteActivity extends FrictionGateActivity {

    private String blockId;
    private String label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        blockId = getIntent().getStringExtra("blockId");
        label = getIntent().getStringExtra("label");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String describeAction() {
        return "Deleting " + label;
    }

    @Override
    protected void onConfirmed() {
        List<TimeBlock> blocks = Config.getTimeBlocks(this);
        TimeBlock block = TimeBlock.findById(blocks, blockId);
        if (block != null) {
            blocks.remove(block);
            Config.setTimeBlocks(this, blocks);
        }
    }
}
