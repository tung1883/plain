package com.plainphone.app;

import android.os.Bundle;

import java.util.Set;

public class FlaggedAppChangeActivity extends FrictionGateActivity {

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
        return "Removing " + label;
    }

    @Override
    protected void onConfirmed() {
        Set<String> flagged = Config.getFlaggedPackages(this);
        flagged.remove(packageName);
        Config.setFlaggedPackages(this, flagged);
    }
}

