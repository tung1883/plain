package com.plainphone.app;

import android.os.Bundle;

import java.util.List;

public class TimeBlockDisableActivity extends FrictionGateActivity {

    private String blockId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        blockId = getIntent().getStringExtra("blockId");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String describeAction() {
        TimeBlock block = TimeBlock.findById(Config.getTimeBlocks(this), blockId);
        return "Disabling " + (block != null ? block.name : "this block");
    }

    @Override
    protected void onConfirmed() {
        List<TimeBlock> blocks = Config.getTimeBlocks(this);
        TimeBlock block = TimeBlock.findById(blocks, blockId);
        if (block != null) {
            block.enabled = false;
            Config.setTimeBlocks(this, blocks);
        }
    }
}

