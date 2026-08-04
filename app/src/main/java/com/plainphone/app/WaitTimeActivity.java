package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WaitTimeActivity extends Activity {

    private static final int STEP_SECONDS = 5;
    private static final int MIN_SECONDS = 10;
    private static final int MAX_SECONDS = 120;

    private int seconds;
    private TextView valueText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        seconds = Config.getWaitSeconds(this);

        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(georgia);
        title.setGravity(Gravity.CENTER);
        title.setText("Wait time before opening a flagged app");
        root.addView(title);

        valueText = new TextView(this);
        valueText.setTextColor(Color.WHITE);
        valueText.setTextSize(36);
        valueText.setTypeface(georgia);
        valueText.setGravity(Gravity.CENTER);
        valueText.setPadding(0, 32, 0, 32);
        root.addView(valueText);

        LinearLayout stepperRow = new LinearLayout(this);
        stepperRow.setOrientation(LinearLayout.HORIZONTAL);
        stepperRow.setGravity(Gravity.CENTER);

        Button minus = new Button(this);
        minus.setText("- " + STEP_SECONDS + "s");
        UiKit.style(this, minus);
        minus.setOnClickListener(v -> adjust(-STEP_SECONDS));
        stepperRow.addView(minus);

        Button plus = new Button(this);
        plus.setText("+ " + STEP_SECONDS + "s");
        UiKit.style(this, plus);
        plus.setOnClickListener(v -> adjust(STEP_SECONDS));
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        plusParams.leftMargin = 24;
        stepperRow.addView(plus, plusParams);

        root.addView(stepperRow);

        Button save = new Button(this);
        save.setText("Save");
        UiKit.style(this, save);
        save.setOnClickListener(v -> {
            Config.setWaitSeconds(this, seconds);
            finish();
        });
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        saveParams.topMargin = 48;
        root.addView(save, saveParams);

        setContentView(root);
        updateValueText();
    }

    private void adjust(int delta) {
        seconds = Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds + delta));
        updateValueText();
    }

    private void updateValueText() {
        valueText.setText(seconds + "s");
    }
}
