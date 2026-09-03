package com.plainphone.app;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.Color;
import java.io.File;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Sectioned settings for the file vault. Reachable from Settings and the vault browser. */
public class VaultSettingsActivity extends Activity {

    private LinearLayout root;
    private FrameLayout stack;
    private Typeface font;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        stack = new FrameLayout(this);
        stack.setBackgroundColor(Color.BLACK);
        stack.addView(scroller, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        UiKit.screen(this, "Vault", stack);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        font = Fonts.current(this);
        root.removeAllViews();

        boolean created = VaultFormat.exists(VaultSession.vaultRoot(this));
        boolean unlocked = VaultSession.get().isUnlocked();

        boolean hidden = Config.isVaultHiddenFromHome(this);
        root.addView(row("Show on home screen: " + (hidden ? "Off" : "On"), v -> {
            Config.setVaultHiddenFromHome(this, !hidden);
            render();
        }));

        if (!created) {
            root.addView(row("Set up the vault", v ->
                    startActivity(new Intent(this, VaultActivity.class))));
            return;
        }

        if (unlocked) {
            root.addView(row("Change password", v ->
                    startActivity(new Intent(this, VaultChangePasswordActivity.class))));
        }
        root.addView(row("Auto-lock: " + formatTimeout(Config.getVaultAutoLockSeconds(this)),
                v -> startActivity(new Intent(this, VaultAutoLockActivity.class))));

        root.addView(row("Change location", v -> pickLocation()));
        if (Config.getVaultLocationPath(this) != null) {
            root.addView(row("Move back to internal", v -> moveBackToInternal()));
        }

        root.addView(row("Open vault", v ->
                startActivity(new Intent(this, VaultActivity.class))));
        if (unlocked) {
            root.addView(row("Lock now", v -> PluginLock.requestLock(this,
                    java.util.EnumSet.of(HomeMode.VAULT), () -> {
                if (!VaultSession.get().isUnlocked()) {
                    Toast.makeText(this, "Vault locked", Toast.LENGTH_SHORT).show();
                }
                render();
            })));
        }

        root.addView(row("Reset vault", v -> confirmReset()));
    }

    private static final int REQ_PICK_VAULT_DIR = 6601;
    private static final int REQ_CONFIRM_RESET = 6602;

    private void confirmReset() {
        VaultUi.confirm(this, "Reset vault?",
                "Deletes every file in the vault.",
                "Continue", this::promptCredentialThenWipe, "Cancel", null);
    }

    private void promptCredentialThenWipe() {
        KeyguardManager km = getSystemService(KeyguardManager.class);
        Intent auth = km == null ? null
                : km.createConfirmDeviceCredentialIntent("Reset the vault",
                        "Confirm it's you before wiping the vault");
        if (auth != null) {
            startActivityForResult(auth, REQ_CONFIRM_RESET);
        } else {
            // No screen lock set on the device — a typed confirmation is the best we can do.
            VaultUi.confirm(this, "No screen lock",
                    "Set a device screen lock for a safer reset, or continue anyway.",
                    "Wipe now", this::doWipe, "Cancel", null);
        }
    }

    private void doWipe() {
        VaultJobs.startReset(this);
        Toast.makeText(this, "Resetting the vault…", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    /**
     * Run a slow filesystem job (wipe / move — hundreds of files) off the main thread
     * behind a blocking overlay. Deleting the vault inline used to ANR ("Waited 10000ms
     * for FocusEvent").
     */
    private void runBlocking(String label, Runnable work, Runnable onDone) {
        if (busy) return;
        busy = true;
        View overlay = busyOverlay(label);
        stack.addView(overlay);
        new Thread(() -> {
            Throwable failure = null;
            try {
                work.run();
            } catch (Throwable t) {
                failure = t;
            }
            final Throwable f = failure;
            runOnUiThread(() -> {
                busy = false;
                stack.removeView(overlay);
                if (isFinishing() || isDestroyed()) return;
                if (f != null) {
                    Toast.makeText(this, "Failed: " + f.getMessage(), Toast.LENGTH_LONG).show();
                } else if (onDone != null) {
                    onDone.run();
                }
                render();
            });
        }, "vault-settings-job").start();
    }

    private View busyOverlay(String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Color.BLACK);
        box.setClickable(true);
        box.addView(UiKit.spinner(this));
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(15);
        text.setTypeface(font);
        text.setPadding(0, 32, 0, 0);
        box.addView(text);
        return box;
    }

    private void pickLocation() {
        if (!VaultLocation.hasStorageAccess()) {
            Toast.makeText(this, "Grant all-files access first", Toast.LENGTH_SHORT).show();
            startActivity(VaultLocation.storageAccessSettingsIntent(this));
            return;
        }
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQ_PICK_VAULT_DIR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CONFIRM_RESET) {
            if (resultCode == RESULT_OK) doWipe();
            return;
        }
        if (requestCode != REQ_PICK_VAULT_DIR || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        File picked = VaultLocation.treeUriToDir(data.getData());
        if (picked == null) {
            Toast.makeText(this, "Only folders on this phone's internal storage work — "
                    + "not SD cards or USB drives", Toast.LENGTH_LONG).show();
            return;
        }
        relocateTo(VaultLocation.vaultDirIn(picked));
    }

    private void relocateTo(File target) {
        relocate(target, target.getAbsolutePath());
    }

    private void moveBackToInternal() {
        relocate(VaultSession.defaultVaultRoot(this), null);
    }

    private void relocate(File target, String newConfigPath) {
        if (VaultJobs.anyPending(this)) {
            Toast.makeText(this, "Finish the running task first", Toast.LENGTH_SHORT).show();
            return;
        }
        File current = VaultSession.vaultRoot(this);
        if (target.equals(current)) return;

        boolean haveVault = VaultFormat.exists(current);
        forceLock();

        Context app = getApplicationContext();
        runBlocking(haveVault ? "Moving the vault…" : "Setting location…",
                () -> {
                    try {
                        if (haveVault) VaultLocation.moveVault(current, target);
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                    Config.setVaultLocationPath(app, newConfigPath);
                    VaultLocation.ensureNoMedia(target);
                },
                () -> Toast.makeText(this, haveVault ? "Vault moved" : "Location set",
                        Toast.LENGTH_SHORT).show());
    }

    private void forceLock() {
        if (VaultSession.get().isUnlocked()) {
            VaultUnlockService.stop(this);
            VaultSession.get().lock(this);
        }
    }

    private static String formatTimeout(int seconds) {
        if (seconds % 60 == 0) return (seconds / 60) + " min";
        return seconds + " s";
    }

    private TextView row(String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20);
        view.setPadding(48, 40, 48, 40);
        view.setGravity(Gravity.START);
        view.setTypeface(font);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        background.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        view.setBackground(background);
        view.setOnClickListener(listener);
        return view;
    }
}
