package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class StepperActivity extends Activity {

    private int value;
    private TextView valueText;

    protected abstract String title();

    protected abstract String stepLabel();

    protected abstract int step();

    protected abstract int min();

    protected abstract int max();

    protected abstract int currentValue();

    protected abstract void save(int value);

    protected abstract String format(int value);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        value = currentValue();

        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 48, 48, 48);

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
        minus.setText("- " + stepLabel());
        UiKit.style(this, minus);
        minus.setOnClickListener(v -> adjust(-step()));
        stepperRow.addView(minus);

        Button plus = new Button(this);
        plus.setText("+ " + stepLabel());
        UiKit.style(this, plus);
        plus.setOnClickListener(v -> adjust(step()));
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        plusParams.leftMargin = 24;
        stepperRow.addView(plus, plusParams);

        root.addView(stepperRow);

        Button save = new Button(this);
        save.setText("Save");
        UiKit.style(this, save);
        save.setOnClickListener(v -> {
            save(value);
            finish();
        });
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        saveParams.topMargin = 48;
        root.addView(save, saveParams);

        UiKit.screen(this, title(), root);
        updateValueText();
    }

    private void adjust(int delta) {
        value = Math.max(min(), Math.min(max(), value + delta));
        updateValueText();
    }

    private void updateValueText() {
        valueText.setText(format(value));
    }
}

