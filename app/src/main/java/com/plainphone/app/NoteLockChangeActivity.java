package com.plainphone.app;

public class NoteLockChangeActivity extends FrictionGateActivity {

    @Override
    protected String describeAction() {
        return "Unlocking Notes";
    }

    @Override
    protected void onConfirmed() {
        Config.setNotesLocked(this, false);
    }
}
