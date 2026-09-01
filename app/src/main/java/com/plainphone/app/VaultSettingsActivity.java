package com.plainphone.app;

import android.app.Activity;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Sectioned settings for the file vault. Reachable from Settings and the vault browser. */
public class VaultSettingsActivity extends Activity {

    private LinearLayout root;
    private Typeface font;

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
        setContentView(scroller);
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

        root.addView(sectionHeader("Status"));
        root.addView(readout(!created ? "Not set up"
                : unlocked ? "Unlocked" : "Locked"));

        if (!created) {
            root.addView(row("Set up the vault", v ->
                    startActivity(new Intent(this, VaultActivity.class))));
            return;
        }

        root.addView(sectionHeader("Security"));
        if (unlocked) {
            root.addView(row("Change password", v ->
                    startActivity(new Intent(this, VaultChangePasswordActivity.class))));
        } else {
            root.addView(readout("Unlock the vault to change the password"));
        }
        root.addView(row("Auto-lock: " + formatTimeout(Config.getVaultAutoLockSeconds(this)),
                v -> startActivity(new Intent(this, VaultAutoLockActivity.class))));

        root.addView(sectionHeader("Location"));
        root.addView(readout(VaultLocation.label(this)));
        root.addView(row("Change location", v -> pickLocation()));
        if (Config.getVaultLocationPath(this) != null) {
            root.addView(row("Move back to internal", v -> moveBackToInternal()));
        }

        root.addView(sectionHeader("Session"));
        root.addView(row("Open vault", v ->
                startActivity(new Intent(this, VaultActivity.class))));
        if (unlocked) {
            root.addView(row("Lock now", v -> {
                VaultUnlockService.stop(this);
                VaultSession.get().lock(this);
                Toast.makeText(this, "Vault locked", Toast.LENGTH_SHORT).show();
                render();
            }));
        }
    }

    private static final int REQ_PICK_VAULT_DIR = 6601;

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
        if (requestCode != REQ_PICK_VAULT_DIR || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        File picked = VaultLocation.treeUriToDir(data.getData());
        if (picked == null) {
            Toast.makeText(this, "Can't use that folder — pick one on this phone's storage",
                    Toast.LENGTH_LONG).show();
            return;
        }
        relocateTo(VaultLocation.vaultDirIn(picked));
    }

    private void relocateTo(File target) {
        File current = VaultSession.vaultRoot(this);
        if (target.equals(current)) return;

        boolean haveVault = VaultFormat.exists(current);
        forceLock();

        if (haveVault) {
            try {
                VaultLocation.moveVault(current, target);
            } catch (Exception e) {
                Toast.makeText(this, "Move failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
        }
        Config.setVaultLocationPath(this, target.getAbsolutePath());
        VaultLocation.ensureNoMedia(target);
        Toast.makeText(this, haveVault ? "Vault moved" : "Location set", Toast.LENGTH_SHORT).show();
        render();
    }

    private void moveBackToInternal() {
        relocateTo(VaultSession.defaultVaultRoot(this));
        Config.setVaultLocationPath(this, null);
        render();
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

    private TextView sectionHeader(String label) {
        TextView header = new TextView(this);
        header.setText(label.toUpperCase());
        header.setTextColor(Color.GRAY);
        header.setTextSize(13);
        header.setLetterSpacing(0.15f);
        header.setTypeface(font);
        header.setPadding(48, 36, 48, 12);
        return header;
    }

    private TextView readout(String label) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.GRAY);
        view.setTextSize(15);
        view.setTypeface(font);
        view.setPadding(48, 16, 48, 16);
        return view;
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
