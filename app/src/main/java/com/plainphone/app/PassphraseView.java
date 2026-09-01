package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Full-screen passphrase gate for the vault. A real password field (no number
 * pad), {@code FLAG_SECURE}, personalised-learning off, no clipboard. The host
 * validates and calls {@link #reject} / {@link #showBusy} as needed.
 */
class PassphraseView extends FrameLayout {

    interface Listener {
        void onPassphrase(char[] passphrase);

        void onCancel();
    }

    private final Listener listener;
    private final EditText input;
    private final TextView error;
    private final ProgressBar spinner;

    PassphraseView(Context context, String prompt, boolean creating, Listener listener) {
        super(context);
        this.listener = listener;
        setBackgroundColor(Color.BLACK);

        if (context instanceof android.app.Activity) {
            android.view.Window window = ((android.app.Activity) context).getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
            UiKit.hideSystemBars((android.app.Activity) context);
        }

        Typeface font = Fonts.current(context);

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(dp(40), 0, dp(40), 0);

        TextView promptView = new TextView(context);
        promptView.setText(prompt);
        promptView.setTextColor(Color.WHITE);
        promptView.setTextSize(20);
        promptView.setTypeface(font);
        promptView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams promptParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        promptParams.bottomMargin = dp(28);
        column.addView(promptView, promptParams);

        input = new EditText(context);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setHint("Passphrase");
        input.setTypeface(font);
        input.setTextSize(18);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE
                | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setPadding(dp(20), dp(16), dp(20), dp(16));
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.BLACK);
        box.setStroke(dp(2), Color.WHITE);
        input.setBackground(box);
        input.setLongClickable(false);
        input.setTextIsSelectable(false);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                dp(280), LinearLayout.LayoutParams.WRAP_CONTENT);
        column.addView(input, inputParams);

        error = new TextView(context);
        error.setTextColor(Color.GRAY);
        error.setTextSize(14);
        error.setTypeface(font);
        error.setGravity(Gravity.CENTER);
        error.setPadding(0, dp(16), 0, 0);
        column.addView(error);

        spinner = new ProgressBar(context);
        spinner.getIndeterminateDrawable().setColorFilter(Color.WHITE,
                android.graphics.PorterDuff.Mode.SRC_IN);
        spinner.setVisibility(GONE);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        spinnerParams.topMargin = dp(24);
        column.addView(spinner, spinnerParams);

        TextView action = new TextView(context);
        action.setText(creating ? "Create vault" : "Unlock");
        action.setTextColor(Color.WHITE);
        action.setTextSize(16);
        action.setTypeface(font);
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(32), dp(20), dp(32), dp(20));
        GradientDrawable btn = new GradientDrawable();
        btn.setColor(Color.BLACK);
        btn.setStroke(dp(2), Color.WHITE);
        action.setBackground(btn);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        actionParams.topMargin = dp(28);
        action.setOnClickListener(v -> submit());
        column.addView(action, actionParams);

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });

        FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        addView(column, columnParams);
    }

    private void submit() {
        int len = input.getText().length();
        if (len == 0) return;
        char[] chars = new char[len];
        input.getText().getChars(0, len, chars, 0);
        listener.onPassphrase(chars);
    }

    void showBusy(boolean busy) {
        spinner.setVisibility(busy ? VISIBLE : GONE);
        input.setEnabled(!busy);
        if (busy) error.setText("");
    }

    void reject(String message) {
        showBusy(false);
        error.setText(message);
        input.setText("");
    }

    int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
