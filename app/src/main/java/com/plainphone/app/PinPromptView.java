package com.plainphone.app;

import android.content.Context;
import android.content.res.Configuration;
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
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Full-screen numeric entry: a prompt, an entry box, a self-contained number pad
 * and (optionally) a close cross. No system keyboard. {@link Listener#onPin} fires
 * on every change; the host decides when a value is accepted and calls
 * {@link #reject()} otherwise. Used for PIN entry (masked) and the friction-gate
 * maths challenge (plain digits).
 *
 * <p>Laid out as a column (prompt above the keypad) in portrait and as a row (prompt
 * beside the keypad) in landscape, where a column would be taller than the screen.
 * Rebuilt on an orientation flip rather than relying on the host recreating this view,
 * since the one host that can show it over an arbitrary foreground app — an
 * accessibility overlay, see {@code AppMonitorService.showPin} — is never recreated
 * by the system the way an activity is; it just gets resized in place.
 */
class PinPromptView extends FrameLayout {

    interface Listener {
        void onPin(String pin);

        void onCancel();

        /** Fired by the on-screen "OK" key (submittable mode only), at 4-6 digits. */
        default void onSubmit(String pin) {}
    }

    /** Dismiss the gate after this many idle seconds; show the countdown for the last few. */
    private static final int IDLE_SECONDS = 20;
    private static final int COUNTDOWN_FROM = 10;

    private final String prompt;
    private final int maxLen;
    private final boolean masked;
    private final boolean submittable;
    private final boolean showClose;
    private final Listener listener;

    private final StringBuilder pin = new StringBuilder();
    private LinearLayout root;
    private TextView display;
    private TextView okKey;
    private TextView countdown;
    private int builtForOrientation = -1;
    private boolean idleTimeout = true;
    private int idleRemaining = IDLE_SECONDS;
    private final Runnable idleTick = new Runnable() {
        @Override
        public void run() {
            idleRemaining--;
            if (idleRemaining <= 0) {
                listener.onCancel();
                return;
            }
            updateCountdown();
            postDelayed(this, 1000);
        }
    };

    /** PIN mode: masked dots, 6 digits, close cross. */
    PinPromptView(Context context, Listener listener) {
        this(context, "Enter PIN", true, 6, true, false, listener);
    }

    PinPromptView(Context context, String prompt, boolean masked, int maxLen,
                  boolean showClose, Listener listener) {
        this(context, prompt, masked, maxLen, showClose, false, listener);
    }

    /** {@code submittable} adds an on-screen OK key that fires {@link Listener#onSubmit}. */
    PinPromptView(Context context, String prompt, boolean masked, int maxLen,
                  boolean showClose, boolean submittable, Listener listener) {
        super(context);
        this.listener = listener;
        this.prompt = prompt;
        this.masked = masked;
        this.maxLen = maxLen;
        this.showClose = showClose;
        this.submittable = submittable;
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

        rebuild();

        if (showClose) {
            FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(
                    UiKit.backWidth(context), UiKit.backHeight(context),
                    Gravity.TOP | Gravity.START);
            backParams.topMargin = dp(8);
            addView(UiKit.backButton(context, listener::onCancel), backParams);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation != builtForOrientation) rebuild();
    }

    /** (Re)builds the prompt/display/keypad for the current orientation; keeps the pin typed so far. */
    private void rebuild() {
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        builtForOrientation = getResources().getConfiguration().orientation;
        if (root != null) removeView(root);

        Typeface font = Fonts.current(getContext());
        int keySize = landscape ? 60 : 74;
        int keyMargin = landscape ? 4 : 6;
        int keyTextSize = landscape ? 22 : 26;

        LinearLayout info = new LinearLayout(getContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView promptView = new TextView(getContext());
        promptView.setText(prompt);
        promptView.setTextColor(Color.WHITE);
        promptView.setTextSize(20);
        promptView.setTypeface(font);
        promptView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams promptParams = new LinearLayout.LayoutParams(
                dp(landscape ? 200 : 260), LinearLayout.LayoutParams.WRAP_CONTENT);
        promptParams.bottomMargin = dp(landscape ? 20 : 36);
        info.addView(promptView, promptParams);

        display = new TextView(getContext());
        display.setTextColor(Color.WHITE);
        display.setTextSize(24);
        display.setTypeface(font);
        display.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        display.setLetterSpacing(0.3f);
        display.setMinWidth(dp(180));
        display.setMinHeight(dp(56));
        display.setPadding(dp(24), dp(12), dp(24), dp(12));
        display.setBackground(UiKit.rounded(getContext(), Color.BLACK, Color.WHITE, dp(2), UiKit.R_SM));
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        displayParams.bottomMargin = dp(14);
        info.addView(display, displayParams);
        updateDisplay();

        countdown = new TextView(getContext());
        countdown.setTextColor(Color.GRAY);
        countdown.setTextSize(13);
        countdown.setTypeface(font);
        countdown.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams countdownParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(20));
        info.addView(countdown, countdownParams);
        updateCountdown();

        LinearLayout keypad = new LinearLayout(getContext());
        keypad.setOrientation(LinearLayout.VERTICAL);
        keypad.setGravity(Gravity.CENTER_HORIZONTAL);
        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {submittable ? "OK" : "", "0", "←"},
        };
        for (String[] rowKeys : keys) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (String key : rowKeys) {
                row.addView(keyButton(getContext(), font, key, keySize, keyMargin, keyTextSize));
            }
            keypad.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        refreshOk();

        root = new LinearLayout(getContext());
        root.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams rootParams;
        if (landscape) {
            root.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            infoParams.rightMargin = dp(28);
            root.addView(info, infoParams);
            root.addView(keypad, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            rootParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        } else {
            root.setOrientation(LinearLayout.VERTICAL);
            root.addView(info, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams keypadParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            root.addView(keypad, keypadParams);
            rootParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            rootParams.bottomMargin = dp(32);
        }
        // index 0: stay under the close cross, which is only added once, in the constructor.
        addView(root, 0, rootParams);
    }

    private View keyButton(Context context, Typeface font, String label, int size, int margin, int textSize) {
        TextView key = new TextView(context);
        key.setText(label);
        key.setTextColor(Color.WHITE);
        key.setTextSize(textSize);
        key.setTypeface(font);
        key.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(size), dp(size));
        params.setMargins(dp(margin), dp(margin), dp(margin), dp(margin));
        key.setLayoutParams(params);

        if (label.isEmpty()) {
            key.setVisibility(INVISIBLE);
            return key;
        }

        key.setBackground(pressBackground());
        if ("OK".equals(label)) {
            key.setTextSize(textSize - 8);
            okKey = key;
            key.setOnClickListener(v -> {
                if (pin.length() >= 4) listener.onSubmit(pin.toString());
            });
            refreshOk();
        } else if ("←".equals(label)) {
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
                if (pin.length() < maxLen) {
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

    /** Turn off the walk-away auto-dismiss + countdown (e.g. the flagged-app maths challenge). */
    void disableIdleTimeout() {
        idleTimeout = false;
        removeCallbacks(idleTick);
        if (countdown != null) countdown.setText("");
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!idleTimeout) return;
        removeCallbacks(idleTick);
        idleRemaining = IDLE_SECONDS;
        updateCountdown();
        postDelayed(idleTick, 1000);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(idleTick);
    }

    private void updateCountdown() {
        if (countdown == null) return;
        countdown.setText(idleTimeout && idleRemaining <= COUNTDOWN_FROM
                ? "Closing in " + idleRemaining + "s" : "");
    }

    private boolean deleteOne() {
        if (pin.length() == 0) return false;
        pin.deleteCharAt(pin.length() - 1);
        changed();
        return pin.length() > 0;
    }

    private void changed() {
        updateDisplay();
        refreshOk();
        listener.onPin(pin.toString());
    }

    private void refreshOk() {
        if (okKey != null) okKey.setAlpha(pin.length() >= 4 ? 1f : 0.3f);
    }

    void reject() {
        pin.setLength(0);
        updateDisplay();
        refreshOk();
        TranslateAnimation shake = new TranslateAnimation(0, dp(9), 0, 0);
        shake.setDuration(360);
        shake.setInterpolator(new CycleInterpolator(3));
        root.startAnimation(shake);
    }

    private void updateDisplay() {
        if (!masked) {
            display.setText(pin.toString());
            return;
        }
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < pin.length(); i++) dots.append('•');
        display.setText(dots.toString());
    }

    private StateListDrawable pressBackground() {
        GradientDrawable pressed = new GradientDrawable();
        pressed.setShape(GradientDrawable.OVAL);
        pressed.setColor(0xFF262626);   // same soft grey as the list-row pill, see UiKit.pressedFill
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, pressed);
        bg.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        bg.setEnterFadeDuration(120);
        bg.setExitFadeDuration(120);
        return bg;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
