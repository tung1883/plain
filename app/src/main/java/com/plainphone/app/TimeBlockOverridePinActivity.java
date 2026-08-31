package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class TimeBlockOverridePinActivity extends Activity {

    private String packageName;
    private String blockId;
    private PinPromptView pad;
    private boolean submitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageName = getIntent().getStringExtra("package");
        blockId = getIntent().getStringExtra("blockId");

        pad = new PinPromptView(this, new PinPromptView.Listener() {
            @Override
            public void onPin(String pin) {
                if (submitted) return;
                if (pin.length() >= 4
                        && Config.checkLockPin(TimeBlockOverridePinActivity.this, pin)) {
                    submitted = true;
                    Intent intent = new Intent(TimeBlockOverridePinActivity.this,
                            TimeBlockOverrideActivity.class);
                    intent.putExtra("package", packageName);
                    intent.putExtra("blockId", blockId);
                    startActivity(intent);
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
