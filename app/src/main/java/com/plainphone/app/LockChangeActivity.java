package com.plainphone.app;

/** Friction gate to turn a {@link Lock} off. The lock is named by {@code EXTRA_LOCK}. */
public class LockChangeActivity extends FrictionGateActivity {

    private Lock lock() {
        return Lock.from(getIntent());
    }

    @Override
    protected String describeAction() {
        return lock().frictionLabel;
    }

    @Override
    protected void onConfirmed() {
        lock().setLocked(this, false);
    }
}
