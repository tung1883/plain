package com.plainphone.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Sets a PIN with the app's own keypad ({@link PinPromptView}), two-phase:
 * <i>choose</i> then <i>re-enter</i>. If a PIN is already in place it first asks
 * for the current one. {@code EXTRA_LOCK_ID == "master"} writes the master PIN;
 * any other id writes that lock's custom PIN. Finishes {@code RESULT_OK} on save.
 */
public class PinSetupActivity extends Activity {

    static final String EXTRA_LOCK_ID = "lock_id";
    static final String EXTRA_LOCK_NAME = "lock_name";

    private static final int VERIFY = 0, NEW = 1, CONFIRM = 2;

    private String id;
    private boolean master;
    private String first;
    private int phase;
    private PinPromptView pad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        id = getIntent().getStringExtra(EXTRA_LOCK_ID);
        master = "master".equals(id);
        phase = needsVerify() ? VERIFY : NEW;
        showPad();
    }

    private boolean needsVerify() {
        return master ? Config.isPinSet(this) : Config.hasCustomPin(this, id);
    }

    private void showPad() {
        String prompt = phase == VERIFY ? "Enter your current PIN"
                : phase == NEW ? "Choose a PIN"
                : "Re-enter to confirm";
        boolean submittable = phase != VERIFY;   // verify auto-checks, like a lock gate
        pad = new PinPromptView(this, prompt, true, 6, true, submittable,
                new PinPromptView.Listener() {
                    @Override
                    public void onPin(String pin) {
                        if (phase == VERIFY) onVerifyTyped(pin);
                    }

                    @Override
                    public void onSubmit(String pin) {
                        onSubmitted(pin);
                    }

                    @Override
                    public void onCancel() {
                        finish();
                    }
                });
        pad.disableIdleTimeout();
        setContentView(pad);
    }

    private void onVerifyTyped(String pin) {
        if (pin.length() < 4) return;
        boolean ok = master ? Config.checkLockPin(this, pin) : Config.checkPin(this, id, pin);
        if (ok) {
            phase = NEW;
            showPad();
        } else if (pin.length() >= 6) {
            pad.reject();
        }
    }

    private void onSubmitted(String pin) {
        switch (phase) {
            case NEW:
                first = pin;
                phase = CONFIRM;
                showPad();
                break;
            case CONFIRM:
                if (pin.equals(first)) {
                    if (master) {
                        Config.setLockPin(this, pin);
                    } else {
                        Config.setCustomPin(this, id, pin);
                    }
                    Toast.makeText(this, "PIN saved", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "PINs didn't match", Toast.LENGTH_SHORT).show();
                    first = null;
                    phase = NEW;
                    showPad();
                }
                break;
        }
    }
}
