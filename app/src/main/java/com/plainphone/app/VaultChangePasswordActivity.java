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
    private FrameLayout root;
    private PassphraseView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!VaultSession.get().isUnlocked()) {
            finish();
            return;
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        showCurrentGate();
    }

    // --- phase 1: confirm the current password --------------------

    private void showCurrentGate() {
        view = new PassphraseView(this, "Current vault password", false,
                new PassphraseView.Listener() {
            @Override
            public void onPassphrase(char[] passphrase) {
                verifyCurrent(passphrase);
            }

            @Override
            public void onCancel() {
                finish();
            }
        });
        swap(view);
    }

    private void verifyCurrent(char[] passphrase) {
        view.setProgressVerb("Checking");
        view.showBusy(true);
        VaultCrypto.Progress progress = (done, total) ->
                main.post(() -> view.setProgress(done, total));
        new Thread(() -> {
            boolean wrong = false;
            String error = null;
            try {
                VaultFormat.Header header = VaultFormat.readHeader(VaultSession.vaultRoot(this));
                byte[] key = VaultFormat.unwrapMasterKey(header, passphrase, progress);
                VaultCrypto.zeroize(key);
            } catch (VaultFormat.WrongPassphrase e) {
                wrong = true;
            } catch (Exception e) {
                error = "Couldn't read the vault: " + e.getMessage();
            } finally {
                java.util.Arrays.fill(passphrase, '\0');
            }
            boolean isWrong = wrong;
            String message = error;
            main.post(() -> {
                if (isWrong) {
                    view.onAttemptFailed();
                } else if (message != null) {
                    view.reject(message);
                } else {
                    showNewGate();
                }
            });
        }).start();
    }

    // --- phase 2: pick the new password --------------------------

    private void showNewGate() {
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
        swap(view);
    }

    private void apply(char[] newPassphrase) {
        view.showBusy(true);
        view.setProgressVerb("Re-encrypting");
        VaultCrypto.Progress progress = (done, total) ->
                main.post(() -> view.setProgress(done, total));
        new Thread(() -> {
            String error = null;
            byte[] masterKey = VaultSession.get().masterKey();
            try {
                if (masterKey == null) throw new IllegalStateException("vault locked");
                VaultFormat.changePassphrase(VaultSession.vaultRoot(this), masterKey,
                        newPassphrase, progress);
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

    private void swap(PassphraseView next) {
        root.removeAllViews();
        root.addView(next, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!VaultSession.get().isUnlocked()) finish();
    }
}
