package com.plainphone.app;

import android.content.Intent;
import android.os.Bundle;

import java.util.Calendar;
import java.util.List;

public class TimeBlockOverrideActivity extends FrictionGateActivity {

    private String packageName;
    private String blockId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        packageName = getIntent().getStringExtra("package");
        blockId = getIntent().getStringExtra("blockId");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String describeAction() {
        TimeBlock block = TimeBlock.findById(Config.getTimeBlocks(this), blockId);
        return "Ending " + (block != null ? block.name : "this block") + " early";
    }

    @Override
    protected void onConfirmed() {
        boolean isAdhoc = blockId.equals(Config.getAdhocBlockId(this))
                && Config.getAdhocUntil(this) > System.currentTimeMillis();
        if (isAdhoc) {
            Config.clearAdhocSession(this);
        } else {
            List<TimeBlock> blocks = Config.getTimeBlocks(this);
            TimeBlock block = TimeBlock.findById(blocks, blockId);
            if (block != null) {
                Config.setOverrideUntil(this, blockId,
                        TimeBlockRules.activeWindowEndMillis(block, Calendar.getInstance()));
            }
        }

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            AppMonitorService.skipTimeBlockGateFor(packageName);
            startActivity(launchIntent);
        }
    }
}

