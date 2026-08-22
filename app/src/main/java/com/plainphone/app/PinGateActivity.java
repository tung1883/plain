package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Asks for the app-lock PIN before a locked app is launched, not after: the real
 * app is only started once the PIN is confirmed, so its screen is never visible
 * without it. Reached only from MainActivity.launchApp(); other launch paths
 * (widgets, notifications, deep links) still go through AppMonitorService's
 * reactive overlay gate, since there's no pre-launch hook for those.
 */
public class PinGateActivity extends Activity {

    private String packageName;
    private String label;
    private EditText pinInput;
    private boolean submitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageName = getIntent().getStringExtra("package");
        label = getIntent().getStringExtra("label");

        // Keyboard should already be up when this screen appears — the user shouldn't
        // have to know to tap the input box first.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

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
        title.setText("Enter PIN to open " + label);
        root.addView(title);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER);

        pinInput = new EditText(this);
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        pinInput.setGravity(Gravity.CENTER);
        pinInput.setTypeface(georgia);
        UiKit.style(this, pinInput);
        pinInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Submit the instant what's typed so far is a correct PIN, rather than
                // waiting for a fixed length or a manual tap — the user just finishes
                // typing and the app opens.
                if (s.length() >= 4 && Config.checkLockPin(PinGateActivity.this, s.toString())) {
                    tryUnlock();
                }
            }
        });
        inputRow.addView(pinInput, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button unlock = new Button(this);
        unlock.setText("Unlock");
        UiKit.style(this, unlock);
        unlock.setOnClickListener(v -> tryUnlock());
        LinearLayout.LayoutParams unlockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        unlockParams.leftMargin = 24;
        inputRow.addView(unlock, unlockParams);

        LinearLayout.LayoutParams inputRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputRowParams.topMargin = 48;
        root.addView(inputRow, inputRowParams);

        Button close = new Button(this);
        close.setText("Close");
        UiKit.style(this, close);
        close.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        closeParams.topMargin = 24;
        root.addView(close, closeParams);

        setContentView(root);

        pinInput.requestFocus();
        pinInput.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(pinInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void tryUnlock() {
        if (submitted) return;

        String pin = pinInput.getText().toString();
        if (!Config.checkLockPin(this, pin)) {
            pinInput.setText("");
            pinInput.requestFocus();
            return;
        }

        submitted = true;
        Intent launchIntent = openIntent();
        if (launchIntent != null) {
            AppMonitorService.skipGateFor(packageName);
            startActivity(launchIntent);
        }
        finish();
    }

    /**
     * What to open once the gate is passed: normally the app's own launcher screen, but an
     * Intent handed over in the OPEN_INTENT extra replaces that. Web search from the home
     * screen sets it, so a gated browser still receives the query rather than opening on a
     * blank start page with what was typed thrown away.
     */
    private Intent openIntent() {
        Intent carried = carriedIntent();
        if (carried != null && carried.resolveActivity(getPackageManager()) != null) {
            return carried;
        }
        return getPackageManager().getLaunchIntentForPackage(packageName);
    }

    private Intent carriedIntent() {
        Intent intent = getIntent();
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            return intent.getParcelableExtra(WebSearch.OPEN_INTENT, Intent.class);
        }
        return intent.getParcelableExtra(WebSearch.OPEN_INTENT);
    }
}
