package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class TimeBlockEditActivity extends Activity {

    private static final String[] DAY_LABELS = {"S", "M", "T", "W", "T", "F", "S"};

    private String blockId;
    private LinearLayout root;
    private Typeface georgia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        blockId = getIntent().getStringExtra("blockId");
        georgia = Fonts.current(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 48, 48, 48);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TimeBlock block = currentBlock();
        if (block == null) {
            finish();
            return;
        }
        render(block);
    }

    private TimeBlock currentBlock() {
        return TimeBlock.findById(Config.getTimeBlocks(this), blockId);
    }

    private void render(TimeBlock block) {
        root.removeAllViews();

        EditText nameInput = new EditText(this);
        nameInput.setText(block.name);
        nameInput.setTextColor(Color.WHITE);
        nameInput.setTypeface(georgia);
        nameInput.setSingleLine(true);
        UiKit.style(this, nameInput);
        root.addView(nameInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateBlock(b -> b.name = s.toString());
            }
        });

        LinearLayout dayRow = new LinearLayout(this);
        dayRow.setOrientation(LinearLayout.HORIZONTAL);
        dayRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dayRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dayRowParams.topMargin = 32;
        root.addView(dayRow, dayRowParams);
        for (int i = 0; i < 7; i++) {
            int dayBit = i;
            boolean on = (block.daysMask & (1 << dayBit)) != 0;
            TextView day = new TextView(this);
            day.setText(DAY_LABELS[i]);
            day.setTypeface(georgia);
            day.setTextSize(18);
            day.setGravity(Gravity.CENTER);
            day.setPadding(24, 24, 24, 24);
            day.setTextColor(on ? Color.BLACK : Color.WHITE);
            day.setBackgroundColor(on ? Color.WHITE : Color.BLACK);
            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            dayParams.leftMargin = i == 0 ? 0 : 4;
            day.setLayoutParams(dayParams);
            day.setOnClickListener(v -> {
                updateBlock(b -> b.daysMask ^= (1 << dayBit));
                render(currentBlock());
            });
            dayRow.addView(day);
        }

        root.addView(row("Mode: " + (block.mode == TimeBlock.Mode.BLACKOUT ? "Blackout (block these apps)"
                : "Allow-only (block everything else)"), v -> {
            updateBlock(b -> b.mode = b.mode == TimeBlock.Mode.BLACKOUT
                    ? TimeBlock.Mode.ALLOW_ONLY : TimeBlock.Mode.BLACKOUT);
            render(currentBlock());
        }));

        root.addView(row("Start time: " + TimeBlock.formatTime(block.startMinute), v -> {
            Intent intent = new Intent(this, TimeBlockStartTimeActivity.class);
            intent.putExtra("blockId", blockId);
            startActivity(intent);
        }));

        root.addView(row("End time: " + TimeBlock.formatTime(block.endMinute), v -> {
            Intent intent = new Intent(this, TimeBlockEndTimeActivity.class);
            intent.putExtra("blockId", blockId);
            startActivity(intent);
        }));

        root.addView(row("Apps (" + block.packages.size() + ")", v -> {
            Intent intent = new Intent(this, TimeBlockAppsActivity.class);
            intent.putExtra("blockId", blockId);
            startActivity(intent);
        }));

        boolean active = TimeBlockRules.isActiveNow(block, Calendar.getInstance());
        root.addView(row("Enabled: " + (block.enabled ? "On" : "Off"), v -> {
            if (block.enabled && active) {

                Intent intent = new Intent(this, TimeBlockDisableActivity.class);
                intent.putExtra("blockId", blockId);
                startActivity(intent);
            } else {
                updateBlock(b -> b.enabled = !b.enabled);
                render(currentBlock());
            }
        }));

        root.addView(row("Delete", v -> {
            Intent intent = new Intent(this, TimeBlockDeleteActivity.class);
            intent.putExtra("blockId", blockId);
            intent.putExtra("label", block.name);
            startActivity(intent);
        }));
    }

    private interface Mutation {
        void apply(TimeBlock block);
    }

    private void updateBlock(Mutation mutation) {
        List<TimeBlock> blocks = Config.getTimeBlocks(this);
        TimeBlock block = TimeBlock.findById(blocks, blockId);
        if (block == null) return;
        mutation.apply(block);
        Config.setTimeBlocks(this, blocks);
    }

    private TextView row(String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(18);
        view.setPadding(0, 32, 0, 32);
        view.setGravity(Gravity.START);
        view.setTypeface(georgia);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        background.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        view.setBackground(background);
        view.setOnClickListener(listener);
        return view;
    }
}

