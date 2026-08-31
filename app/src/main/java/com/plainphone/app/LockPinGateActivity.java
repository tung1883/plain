package com.plainphone.app;

import android.app.Activity;
import android.os.Bundle;

/** PIN entry that unlocks a {@link Lock} for its grace window. Named by {@code EXTRA_LOCK}. */
public class LockPinGateActivity extends Activity {

    private Lock lock;
    private PinPromptView pad;
    private boolean submitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lock = Lock.from(getIntent());

        pad = new PinPromptView(this, new PinPromptView.Listener() {
            @Override
            public void onPin(String pin) {
                if (submitted) return;
                if (pin.length() >= 4 && Config.checkLockPin(LockPinGateActivity.this, pin)) {
                    submitted = true;
                    lock.keepUnlocked(LockPinGateActivity.this);
                    setResult(RESULT_OK);
                    finish();
                } else if (pin.length() >= 6) {
                    pad.reject();
                }
            }

            @Override
            public void onCancel() {
                finish();
            }
        });
        setContentView(pad);
    }
}
