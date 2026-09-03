package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

public abstract class FrictionGateActivity extends Activity {

    private static final int DEFAULT_WAIT_SECONDS = 30;

    private LinearLayout countdownView;
    private TextView countdownText;
    private Typeface font;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long mathAnswer;
    private PinPromptView mathPad;

    protected abstract String describeAction();

    protected abstract void onConfirmed();

    protected int waitSeconds() {
        return DEFAULT_WAIT_SECONDS;
    }

    protected boolean requiresMathChallenge() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);

        if (waitSeconds() <= 0 && !requiresMathChallenge()) {
            onConfirmed();
            finish();
            return;
        }

        countdownView = new LinearLayout(this);
        countdownView.setOrientation(LinearLayout.VERTICAL);
        countdownView.setGravity(Gravity.CENTER);
        countdownView.setBackgroundColor(Color.BLACK);
        countdownView.setPadding(48, 48, 48, 48);

        countdownText = new TextView(this);
        countdownText.setTextColor(Color.WHITE);
        countdownText.setTextSize(20);
        countdownText.setTypeface(font);
        countdownText.setGravity(Gravity.CENTER);
        countdownView.addView(countdownText);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.addView(countdownView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(
                UiKit.backWidth(this), UiKit.backHeight(this), Gravity.TOP | Gravity.START);
        backParams.topMargin = (int) (getResources().getDisplayMetrics().density * 8);
        root.addView(UiKit.backButton(this, this::finish), backParams);

        setContentView(root);
        UiKit.hideSystemBars(this);

        startCountdown(waitSeconds());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) UiKit.hideSystemBars(this);
    }

    private void startCountdown(int secondsLeft) {
        if (secondsLeft <= 0) {
            if (requiresMathChallenge()) {
                showMathChallenge();
            } else {
                onConfirmed();
                finish();
            }
            return;
        }
        countdownText.setText("Wait " + secondsLeft + "s...");
        handler.postDelayed(() -> startCountdown(secondsLeft - 1), 1000);
    }

    private void showMathChallenge() {
        Random random = new Random();
        int a = random.nextInt(90) + 10;
        int b = random.nextInt(90) + 10;
        mathAnswer = (long) a * b;
        int answerDigits = Long.toString(mathAnswer).length();

        mathPad = new PinPromptView(this, "What is " + a + " × " + b + "?",
                false, answerDigits + 1, true, new PinPromptView.Listener() {
            @Override
            public void onPin(String value) {
                if (value.isEmpty()) return;
                long entered;
                try {
                    entered = Long.parseLong(value);
                } catch (NumberFormatException e) {
                    return;
                }
                if (entered == mathAnswer) {
                    onConfirmed();
                    finish();
                } else if (value.length() >= answerDigits) {
                    mathPad.reject();
                }
            }

            @Override
            public void onCancel() {
                finish();
            }
        });
        mathPad.disableIdleTimeout();
        setContentView(mathPad);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
