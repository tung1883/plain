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
        // Once a PIN exists, it protects Settings too — Settings holds the controls to
        // unlock every other locked app, so this gate alone (time + arithmetic) isn't
        // enough on its own once there's a real secret to guard.
        startActivity(new Intent(this, Config.isPinSet(this)
                ? SettingsPinGateActivity.class
                : SettingsActivity.class));
    }
}
