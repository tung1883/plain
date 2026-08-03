package com.plainphone.app;

import android.os.Bundle;

import java.util.Set;

/** Confirms removing one app from the hidden list — friction only applies to loosening it. */
public class HiddenAppChangeActivity extends FrictionGateActivity {

    private String packageName;
    private String label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        packageName = getIntent().getStringExtra("package");
        label = getIntent().getStringExtra("label");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String describeAction() {
        return "Unhiding " + label;
    }

    @Override
    protected void onConfirmed() {
        Set<String> hidden = Config.getHiddenPackages(this);
        hidden.remove(packageName);
        Config.setHiddenPackages(this, hidden);
    }
}
