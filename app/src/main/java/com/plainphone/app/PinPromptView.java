package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Full-screen PIN entry: a prompt, a row of dots, a self-contained number pad and
 * a close cross. No system keyboard. {@link Listener#onPin} fires on every change;
 * the host decides when a value is accepted and calls {@link #reject()} otherwise.
 */
class PinPromptView extends FrameLayout {

    interface Listener {
        void onPin(String pin);

        void onCancel();
    }

    private static final int MAX_LEN = 6;

    private final StringBuilder pin = new StringBuilder();
    private final LinearLayout column;
    private final TextView display;
    private final Listener listener;

    PinPromptView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setBackgroundColor(Color.BLACK);

        if (context instanceof android.app.Activity) {
            android.view.Window window = ((android.app.Activity) context).getWindow();
            // No text input here — keep any keyboard from a previous screen out, and
            // stop it panning/resizing this window off-centre if it lingers.
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            // Full-screen: hide the status and navigation bars while entering a PIN.
            UiKit.hideSystemBars((android.app.Activity) context);
        }

        Typeface font = Fonts.current(context);

        column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView promptView = new TextView(context);
        promptView.setText("Enter PIN");
        promptView.setTextColor(Color.WHITE);
        promptView.setTextSize(20);
        promptView.setTypeface(font);
        promptView.setGravity(Gravity.CENTER);
        promptView.setPadding(dp(32), 0, dp(32), 0);
        LinearLayout.LayoutParams promptParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        promptParams.bottomMargin = dp(36);
        column.addView(promptView, promptParams);

        display = new TextView(context);
        display.setTextColor(Color.WHITE);
        display.setTextSize(24);
        display.setTypeface(font);
        display.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        display.setLetterSpacing(0.3f);
        display.setMinWidth(dp(180));
        display.setMinHeight(dp(56));
        display.setPadding(dp(24), dp(12), dp(24), dp(12));
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.BLACK);
        box.setStroke(dp(2), Color.WHITE);
        display.setBackground(box);
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        displayParams.bottomMargin = dp(44);
        column.addView(display, displayParams);
        updateDisplay();

        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"", "0", "←"},
        };
        for (String[] rowKeys : keys) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (String key : rowKeys) {
                row.addView(keyButton(context, font, key));
            }
            column.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        columnParams.bottomMargin = dp(32);
        addView(column, columnParams);

        ImageView close = new ImageView(context);
        close.setImageDrawable(MiniIcons.cross(dp(20), Color.WHITE));
        close.setScaleType(ImageView.ScaleType.CENTER);
        close.setBackground(pressBackground());
        close.setOnClickListener(v -> listener.onCancel());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                dp(48), dp(48), Gravity.TOP | Gravity.END);
        closeParams.topMargin = dp(12);
        closeParams.rightMargin = dp(8);
        addView(close, closeParams);
    }

    private View keyButton(Context context, Typeface font, String label) {
        TextView key = new TextView(context);
        key.setText(label);
        key.setTextColor(Color.WHITE);
        key.setTextSize(26);
        key.setTypeface(font);
        key.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(74), dp(74));
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        key.setLayoutParams(params);

        if (label.isEmpty()) {
            key.setVisibility(INVISIBLE);
            return key;
        }

        key.setBackground(pressBackground());
        if ("←".equals(label)) {
            key.setOnClickListener(v -> deleteOne());
            final Runnable[] repeat = new Runnable[1];
            key.setOnLongClickListener(v -> {
                repeat[0] = new Runnable() {
                    @Override
                    public void run() {
                        if (deleteOne()) postDelayed(this, 80);
                    }
                };
                post(repeat[0]);
                return true;
            });
            key.setOnTouchListener((v, event) -> {
                int action = event.getActionMasked();
                if ((action == android.view.MotionEvent.ACTION_UP
                        || action == android.view.MotionEvent.ACTION_CANCEL)
                        && repeat[0] != null) {
                    removeCallbacks(repeat[0]);
                    repeat[0] = null;
                }
                return false;
            });
        } else {
            key.setOnClickListener(v -> {
                if (pin.length() < MAX_LEN) {
                    pin.append(label);
                    changed();
                }
            });
        }
        return key;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) UiKit.hideSystemBars(this);
    }

    private boolean deleteOne() {
        if (pin.length() == 0) return false;
        pin.deleteCharAt(pin.length() - 1);
        changed();
        return pin.length() > 0;
    }

    private void changed() {
        updateDisplay();
        listener.onPin(pin.toString());
    }

    void reject() {
        pin.setLength(0);
        updateDisplay();
        TranslateAnimation shake = new TranslateAnimation(0, dp(9), 0, 0);
        shake.setDuration(360);
        shake.setInterpolator(new CycleInterpolator(3));
        column.startAnimation(shake);
    }

    private void updateDisplay() {
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < pin.length(); i++) masked.append('•');
        display.setText(masked.toString());
    }

    private StateListDrawable pressBackground() {
        GradientDrawable pressed = new GradientDrawable();
        pressed.setShape(GradientDrawable.OVAL);
        pressed.setColor(Color.DKGRAY);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, pressed);
        bg.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        return bg;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
