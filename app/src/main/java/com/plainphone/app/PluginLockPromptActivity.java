package com.plainphone.app;

import android.app.Activity;
import android.os.Bundle;

import java.util.EnumSet;

/**
 * Headless, translucent host for the {@link PluginLock} confirmation when the
 * request comes from somewhere that can't show a dialog itself — the
 * "Lock now" notification action on {@link VaultUnlockService}.
 */
public class PluginLockPromptActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PluginLock.requestLock(this, EnumSet.of(HomeMode.VAULT), this::finish);
    }
}
