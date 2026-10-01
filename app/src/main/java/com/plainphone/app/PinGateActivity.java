package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PinGateActivity extends Activity {

    private String packageName;
    private String label;
    private PinPromptView pad;
    private boolean submitted = false;
    private boolean engineStarted;   // opened by the app watcher, not by a tap in Plain
    private int generation;   // which gate this screen belongs to, see ForegroundExtras
    private boolean onward;   // handed on to the app or the wait gate, so accessibility stays as it is
    private boolean over;   // gating a page already on screen underneath, see NoobBackend

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageName = getIntent().getStringExtra("package");
        label = getIntent().getStringExtra("label");
        over = getIntent().getBooleanExtra("over", false);
        generation = ForegroundExtras.gateGeneration();
        engineStarted = getIntent().getBooleanExtra("engine", false);

        pad = new PinPromptView(this, new PinPromptView.Listener() {
            @Override
            public void onPin(String pin) {
                if (submitted) return;
                if (pin.length() >= 4 && Config.checkPin(PinGateActivity.this, "applock", pin)) {
                    submitted = true;
                    onward = true;
                    Config.markAppUnlocked(PinGateActivity.this, packageName);
                    GateEngine.skipGateFor(packageName);
                    Intent next;
                    if (Config.getFlaggedPackages(PinGateActivity.this).contains(packageName)) {
                        // Also flagged — hand off to the wait gate instead of the app.
                        next = new Intent(PinGateActivity.this, FlaggedGateActivity.class);
                        next.putExtra("package", packageName);
                        next.putExtra("label", label);
                        next.putExtra("over", over);
                        next.putExtra("engine", engineStarted);
                        next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    } else {
                        // Over the app: finishing just uncovers the page that was there.
                        next = over ? null : openIntent();
                    }
                    if (next != null && !FlaggedGateActivity.class.getName().equals(
                            next.getComponent() == null ? null : next.getComponent().getClassName())) {
                        // Stay up until the app has been started: finishing first would let
                        // Plain's home screen show through and then cover the app.
                        android.content.Context app = getApplicationContext();
                        final Intent launch = next;
                        ForegroundExtras.beforeLaunch(app, packageName);
                        ForegroundExtras.afterA11yOff(app, () -> {
                            app.startActivity(launch);
                            finish();
                        });
                        return;
                    }
                    if (next != null) startActivity(next);
                    finish();
                } else if (pin.length() >= 6) {
                    pad.reject();
                }
            }

            @Override
            public void onCancel() {
                // The gated app is right underneath (or behind); leave it.
                if (over || engineStarted) NoobBackend.goHome(PinGateActivity.this);
                finish();
            }
        });
        setContentView(pad);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!onward) ForegroundExtras.gateClosedWithoutOpening(this, generation);
    }

    private Intent openIntent() {
        Intent carried = carriedIntent();
        if (carried != null && carried.resolveActivity(getPackageManager()) != null) {
            return carried;
        }
        return getPackageManager().getLaunchIntentForPackage(packageName);
    }

    private Intent carriedIntent() {
        Intent intent = getIntent();
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            return intent.getParcelableExtra(WebSearch.OPEN_INTENT, Intent.class);
        }
        return intent.getParcelableExtra(WebSearch.OPEN_INTENT);
    }
}
