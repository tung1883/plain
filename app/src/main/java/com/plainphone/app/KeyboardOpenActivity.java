package com.plainphone.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.io.File;

/**
 * An invisible screen the keyboard starts to open a search result. Opening has to pass the launcher's
 * gates (PIN, time blocks), which need an Activity; the keyboard is a Service. It also asks for the
 * permissions search needs, which a keyboard cannot prompt for itself.
 */
public class KeyboardOpenActivity extends Activity {

    private static final String EXTRA_FILE = "file", EXTRA_INTENT = "intent", EXTRA_WEB = "web",
            EXTRA_ACCESS = "access", EXTRA_SHARE = "share";

    /** The intent that opens {@code hit}, or null when it has nothing to open. */
    static Intent forHit(Context ctx, KbHit hit) {
        Intent go = new Intent(ctx, KeyboardOpenActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        switch (hit.kind) {
            case FILE:
            case CONTENT:
                return go.putExtra(EXTRA_FILE, hit.file.getAbsolutePath());
            case WEB:
            case RECENT:
                return hit.open == null ? null : go.putExtra(EXTRA_INTENT, hit.open).putExtra(EXTRA_WEB, true);
            case LOCKED:
            case NOTE:
            case TODO:
                return hit.open == null ? null : new Intent(hit.open).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            default:
                return hit.open == null ? null : go.putExtra(EXTRA_INTENT, hit.open);
        }
    }

    /** Offer {@code file} to other apps through the share sheet (gated apps left out). */
    static Intent forShare(Context ctx, File file) {
        return new Intent(ctx, KeyboardOpenActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_SHARE, file.getAbsolutePath());
    }

    static Intent forAccess(Context ctx, String access) {
        return new Intent(ctx, KeyboardOpenActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_ACCESS, access);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        if (savedInstanceState != null) {
            finish();
            return;
        }
        Intent in = getIntent();
        String access = in.getStringExtra(EXTRA_ACCESS);
        if (access != null) {
            if (access.equals("contacts")) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 1);
            } else if (android.os.Build.VERSION.SDK_INT >= 30) {
                DeviceSearch.requestFullFileAccess(this);
                finish();
            } else {
                DeviceSearch.requestFullFileAccess(this);   // a permission prompt: finish when answered
            }
            return;
        }
        try {
            String share = in.getStringExtra(EXTRA_SHARE);
            if (share != null) {
                DeviceSearch.shareFile(this, new File(share));
                finish();
                return;
            }
            String path = in.getStringExtra(EXTRA_FILE);
            Intent target = in.getParcelableExtra(EXTRA_INTENT);
            if (path != null) {
                DeviceSearch.open(this, new File(path), false);
            } else if (target != null && in.getBooleanExtra(EXTRA_WEB, false) && target.getPackage() != null) {
                WebSearch.open(this, target.getPackage(), target);
            } else if (target != null) {
                GatedLaunch.start(this, target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Nothing can open this", android.widget.Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
        android.widget.Toast.makeText(this, granted ? "Allowed. Search again." : "Not allowed",
                android.widget.Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
