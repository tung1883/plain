package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;
import android.widget.Toast;

/** Re-wraps the master key under a new password. Only reachable while unlocked. */
public class VaultChangePasswordActivity extends Activity {

    private final Handler main = new Handler(Looper.getMainLooper());
    private PassphraseView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!VaultSession.get().isUnlocked()) {
            finish();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        view = new PassphraseView(this, "New vault password", true,
                new PassphraseView.Listener() {
            @Override
            public void onPassphrase(char[] passphrase) {
                apply(passphrase);
            }

            @Override
            public void onCancel() {
                finish();
            }
        });
        root.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void apply(char[] newPassphrase) {
        view.showBusy(true);
        new Thread(() -> {
            String error = null;
            byte[] masterKey = VaultSession.get().masterKey();
            try {
                if (masterKey == null) throw new IllegalStateException("vault locked");
                VaultFormat.changePassphrase(VaultSession.vaultRoot(this), masterKey, newPassphrase);
            } catch (Exception e) {
                error = "Couldn't change password: " + e.getMessage();
            } finally {
                java.util.Arrays.fill(newPassphrase, '\0');
            }
            String message = error;
            main.post(() -> {
                if (message != null) {
                    view.reject(message);
                    return;
                }
                Toast.makeText(this, "Password changed", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!VaultSession.get().isUnlocked()) finish();
    }
}
