package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PinGateActivity extends Activity {

    private String packageName;
    private String label;
    private PinPromptView pad;
    private boolean submitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageName = getIntent().getStringExtra("package");
        label = getIntent().getStringExtra("label");

        pad = new PinPromptView(this, new PinPromptView.Listener() {
            @Override
            public void onPin(String pin) {
                if (submitted) return;
                if (pin.length() >= 4 && Config.checkLockPin(PinGateActivity.this, pin)) {
                    submitted = true;
                    Config.markAppUnlocked(PinGateActivity.this, packageName);
                    Intent launchIntent = openIntent();
                    if (launchIntent != null) {
                        AppMonitorService.skipGateFor(packageName);
                        startActivity(launchIntent);
                    }
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
