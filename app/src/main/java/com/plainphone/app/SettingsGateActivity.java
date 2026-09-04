package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Routes into Settings — straight to {@link SettingsActivity}, or via
 * {@link SettingsPinGateActivity} when the Settings lock is on. No wait: it shows
 * nothing and finishes immediately.
 */
public class SettingsGateActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean gated = Config.isLocksEnabled(this) && Config.isSettingsLockEnabled(this);
        Intent next = new Intent(this,
                gated ? SettingsPinGateActivity.class : SettingsActivity.class);

        String dest = getIntent().getStringExtra(SettingsActivity.EXTRA_DESTINATION);
        if (dest != null) {
            next.putExtra(SettingsActivity.EXTRA_DESTINATION, dest);
        }
        startActivity(next);
        finish();
    }
}
