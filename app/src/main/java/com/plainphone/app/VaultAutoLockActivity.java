package com.plainphone.app;

/** Vault auto-lock timeout stepper (seconds of inactivity before re-lock). */
public class VaultAutoLockActivity extends StepperActivity {

    @Override
    protected String title() {
        return "Vault auto-lock";
    }

    @Override
    protected String stepLabel() {
        return "1 min";
    }

    @Override
    protected int step() {
        return 60;
    }

    @Override
    protected int min() {
        return 60;
    }

    @Override
    protected int max() {
        return 1800;
    }

    @Override
    protected int currentValue() {
        return Config.getVaultAutoLockSeconds(this);
    }

    @Override
    protected void save(int value) {
        Config.setVaultAutoLockSeconds(this, value);
    }

    @Override
    protected String format(int value) {
        if (value % 60 == 0) return (value / 60) + " min";
        return value + " s";
    }
}
