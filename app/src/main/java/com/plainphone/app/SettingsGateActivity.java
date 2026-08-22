package com.plainphone.app;

import android.content.Intent;

/** Same friction gate applied before Settings itself, since it holds other escape hatches. */
public class SettingsGateActivity extends FrictionGateActivity {

    @Override
    protected int waitSeconds() {
        return Config.getSettingsWaitSeconds(this);
    }

    @Override
    protected boolean requiresMathChallenge() {
        return false;
    }

    @Override
    protected String describeAction() {
        return "Opening Settings";
    }

    @Override
    protected void onConfirmed() {
        // Once a PIN exists, it protects Settings too — Settings holds the controls to
        // unlock every other locked app, so this gate alone (time + arithmetic) isn't
        // enough on its own once there's a real secret to guard.
        Intent next = new Intent(this, Config.isPinSet(this)
                ? SettingsPinGateActivity.class
                : SettingsActivity.class);
        // Carried through so a screen deep-linked from home search opens once every gate
        // in front of Settings has been passed, not instead of them.
        next.putExtra(SettingsActivity.EXTRA_DESTINATION,
                getIntent().getStringExtra(SettingsActivity.EXTRA_DESTINATION));
        startActivity(next);
    }
}
