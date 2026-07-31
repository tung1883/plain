package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

/**
 * Shared 30s-wait + math-check friction gate, used anywhere a change should be
 * hard to make on impulse. Subclasses describe what's being confirmed and what
 * happens once the gate passes; a Back button is always available to bail out.
 */
public abstract class FrictionGateActivity extends Activity {

    private static final int WAIT_SECONDS = 30;

    private LinearLayout content;
    private Typeface georgia;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long mathAnswer;

    protected abstract String describeAction();

    protected abstract void onConfirmed();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        georgia = Fonts.georgia(this);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setBackgroundColor(Color.BLACK);
        content.setPadding(48, 48, 48, 48);
        setContentView(content);

        startCountdown(WAIT_SECONDS);
    }

    private void startCountdown(int secondsLeft) {
        content.removeAllViews();

        TextView title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(georgia);
        title.setGravity(Gravity.CENTER);
        title.setText(describeAction() + "\nWait " + secondsLeft + "s...");
        content.addView(title);

        content.addView(backButton(), backButtonParams());

        if (secondsLeft <= 0) {
            showMathChallenge();
            return;
        }
        handler.postDelayed(() -> startCountdown(secondsLeft - 1), 1000);
    }

    private void showMathChallenge() {
        content.removeAllViews();

        Random random = new Random();
        int a = random.nextInt(90) + 10; // 10-99
        int b = random.nextInt(90) + 10;
        mathAnswer = (long) a * b;

        TextView question = new TextView(this);
        question.setTextColor(Color.WHITE);
        question.setTextSize(24);
        question.setTypeface(georgia);
        question.setGravity(Gravity.CENTER);
        question.setText("What is " + a + " × " + b + "?");
        content.addView(question);

        LinearLayout answerRow = new LinearLayout(this);
        answerRow.setOrientation(LinearLayout.HORIZONTAL);
        answerRow.setGravity(Gravity.CENTER);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setGravity(Gravity.CENTER);
        UiKit.style(this, input);
        answerRow.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button confirm = new Button(this);
        confirm.setText("Confirm");
        UiKit.style(this, confirm);
        confirm.setOnClickListener(v -> checkAnswer(input.getText().toString()));
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        confirmParams.leftMargin = 24;
        answerRow.addView(confirm, confirmParams);

        LinearLayout.LayoutParams answerRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        answerRowParams.topMargin = 48;
        content.addView(answerRow, answerRowParams);

        content.addView(backButton(), backButtonParams());
    }

    private Button backButton() {
        Button back = new Button(this);
        back.setText("Back");
        UiKit.style(this, back);
        back.setOnClickListener(v -> finish());
        return back;
    }

    private LinearLayout.LayoutParams backButtonParams() {
        // Matches the spacing the Close button uses on the countdown overlay
        // in AppMonitorService, so both read as the same kind of control.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 48;
        return params;
    }

    private void checkAnswer(String text) {
        try {
            long value = Long.parseLong(text.trim());
            if (value == mathAnswer) {
                onConfirmed();
                finish();
            } else {
                showMathChallenge(); // wrong answer: fresh question, no extra wait
            }
        } catch (NumberFormatException e) {
            showMathChallenge();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
