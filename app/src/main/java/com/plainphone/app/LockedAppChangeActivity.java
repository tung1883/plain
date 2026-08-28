package com.plainphone.app;

import android.os.Bundle;

import java.util.Set;

public class LockedAppChangeActivity extends FrictionGateActivity {

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
        return "Unlocking " + label;
    }

    @Override
    protected void onConfirmed() {
        Set<String> locked = Config.getLockedPackages(this);
        locked.remove(packageName);
        Config.setLockedPackages(this, locked);
    }
}

