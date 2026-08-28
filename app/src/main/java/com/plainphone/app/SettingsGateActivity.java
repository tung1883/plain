package com.plainphone.app;

import android.content.Intent;

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

        Intent next = new Intent(this, Config.isPinSet(this)
                ? SettingsPinGateActivity.class
                : SettingsActivity.class);

        next.putExtra(SettingsActivity.EXTRA_DESTINATION,
                getIntent().getStringExtra(SettingsActivity.EXTRA_DESTINATION));
        startActivity(next);
    }
}

