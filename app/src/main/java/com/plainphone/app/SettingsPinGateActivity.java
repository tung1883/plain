package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SettingsPinGateActivity extends Activity {

    private PinPromptView pad;
    private boolean submitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pad = new PinPromptView(this, new PinPromptView.Listener() {
            @Override
            public void onPin(String pin) {
                if (submitted) return;
                if (pin.length() >= 4 && Config.checkLockPin(SettingsPinGateActivity.this, pin)) {
                    submitted = true;
                    Intent settings = new Intent(SettingsPinGateActivity.this, SettingsActivity.class);
                    settings.putExtra(SettingsActivity.EXTRA_DESTINATION,
                            getIntent().getStringExtra(SettingsActivity.EXTRA_DESTINATION));
                    startActivity(settings);
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
