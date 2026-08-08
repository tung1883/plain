package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Shown before a time-block-restricted app is launched, not after — mirrors
 * FlaggedGateActivity/PinGateActivity's pre-launch pattern. Reached only from
 * MainActivity.launchApp(); other launch paths (widgets, notifications, deep
 * links) still go through AppMonitorService's reactive overlay gate.
 */
public class TimeBlockGateActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String packageName = getIntent().getStringExtra("package");
        String blockId = getIntent().getStringExtra("blockId");

        TimeBlock block = TimeBlock.findById(Config.getTimeBlocks(this), blockId);
        String name = block != null ? block.name : "a time block";
        String endTime = block != null ? TimeBlockRules.formatEndTime(this, block) : "";

        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 48, 48, 48);

        TextView text = new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setTextSize(24);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(georgia);
        text.setText("Unavailable during " + name + " until " + endTime);
        root.addView(text);

        Button override = new Button(this);
        override.setText("Override");
        UiKit.style(this, override);
        override.setOnClickListener(v -> {
            Intent intent = new Intent(this, TimeBlockOverridePinActivity.class);
            intent.putExtra("package", packageName);
            intent.putExtra("blockId", blockId);
            startActivity(intent);
            finish();
        });
        LinearLayout.LayoutParams overrideParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        overrideParams.topMargin = 48;
        root.addView(override, overrideParams);

        Button close = new Button(this);
        close.setText("Close");
        UiKit.style(this, close);
        close.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        closeParams.topMargin = 24;
        root.addView(close, closeParams);

        setContentView(root);
    }
}
