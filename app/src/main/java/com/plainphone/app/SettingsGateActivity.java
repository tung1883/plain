package com.plainphone.app;

import android.content.Intent;

/** Same friction gate applied before Settings itself, since it holds other escape hatches. */
public class SettingsGateActivity extends FrictionGateActivity {

    @Override
    protected String describeAction() {
        return "Opening Settings";
    }

    @Override
    protected void onConfirmed() {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}
