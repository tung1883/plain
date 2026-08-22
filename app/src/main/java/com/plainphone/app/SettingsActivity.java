package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    /**
     * Class name of a settings screen to open immediately, used when home-screen search
     * deep-links to one. It rides through the friction (and PIN) gates rather than being
     * launched directly, so a searched-for screen still costs the same wait as reaching
     * it by hand — Settings is left underneath so Back lands somewhere sensible.
     */
    static final String EXTRA_DESTINATION = "destination";

    private LinearLayout root;
    private Typeface georgia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        georgia = Fonts.current(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        // The list has outgrown the screen, so it has to scroll — without this the rows
        // past the bottom edge are simply unreachable rather than merely off-screen.
        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroller);

        // Only on a fresh create: after a rotation or a return from the deep-linked screen
        // this Activity is reused, and forwarding again would trap the user in a loop.
        if (savedInstanceState == null) {
            openDeepLinkedScreen(getIntent().getStringExtra(EXTRA_DESTINATION));
        }
    }

    private void openDeepLinkedScreen(String className) {
        if (className == null) return;
        try {
            startActivity(new Intent(this, Class.forName(className)));
        } catch (ClassNotFoundException e) {
            // The catalog in SearchTargets named a screen that no longer exists; showing
            // plain Settings is a fine landing spot, so there's nothing to report.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        render(); // reflect any changes made via dialogs or returning from system Settings
    }

    private void render() {
        root.removeAllViews();

        root.addView(row("Change home app",
                v -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS))));
        root.addView(row("Turn off Accessibility Service",
                v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));

        boolean grayscaleOn = Config.isGrayscaleEnabled(this);
        root.addView(row("Grayscale: " + (grayscaleOn ? "On" : "Off"), v -> toggleGrayscale()));

        root.addView(row("Settings wait time: " + Config.getSettingsWaitSeconds(this) + "s",
                v -> startActivity(new Intent(this, SettingsWaitTimeActivity.class))));

        root.addView(row("Flagged apps",
                v -> startActivity(new Intent(this, FlaggedAppsActivity.class))));

        root.addView(row("Hide apps from app list",
                v -> startActivity(new Intent(this, HiddenAppsActivity.class))));

        root.addView(row("App lock",
                v -> startActivity(new Intent(this, LockedAppsActivity.class))));

        root.addView(row("Time Blocks",
                v -> startActivity(new Intent(this, TimeBlocksActivity.class))));

        root.addView(row("Home screen art",
                v -> startActivity(new Intent(this, PixelSceneActivity.class))));

        root.addView(row("Font: " + Config.getFontChoice(this).label,
                v -> startActivity(new Intent(this, FontActivity.class))));

        root.addView(row("Grant usage access (for all-apps stats)",
                v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));

        boolean fileSearch = Config.isFileSearchEnabled(this);
        root.addView(row("Search files: " + (fileSearch ? "On" : "Off"), v -> {
            Config.setFileSearchEnabled(this, !fileSearch);
            if (!fileSearch && !DeviceSearch.canSearchFiles(this)) {
                requestPermissions(DeviceSearch.filePermissions(), DeviceSearch.REQUEST_FILES);
            }
            render();
        }));

        // Only meaningful where the permission exists as a separate toggle (Android 11+);
        // below that, plain storage permission already allows the filesystem walk.
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            boolean allFiles = FileIndex.canWalk(this);
            root.addView(row("Search folders & all files: " + (allFiles ? "On" : "Off"),
                    v -> DeviceSearch.requestAllFilesAccess(this)));
        }

        boolean webSearch = Config.isWebSearchEnabled(this);
        root.addView(row("Search the web: " + (webSearch ? "On" : "Off"), v -> {
            Config.setWebSearchEnabled(this, !webSearch);
            render();
        }));

        int webTargets = Config.getWebTargets(this).size();
        root.addView(row("My web searches" + (webTargets > 0 ? " (" + webTargets + ")" : ""),
                v -> startActivity(new Intent(this, WebTargetsActivity.class))));

        boolean contactSearch = Config.isContactSearchEnabled(this);
        root.addView(row("Search contacts: " + (contactSearch ? "On" : "Off"), v -> {
            Config.setContactSearchEnabled(this, !contactSearch);
            if (!contactSearch && !DeviceSearch.canSearchContacts(this)) {
                requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS},
                        DeviceSearch.REQUEST_CONTACTS);
            }
            render();
        }));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Turning a search source on here only sticks if its permission was actually
        // granted — otherwise the row would claim "On" for a source that can't read
        // anything. An interrupted request (empty arrays) is left alone.
        if (grantResults.length == 0) return;

        boolean granted = false;
        for (int result : grantResults) {
            if (result == android.content.pm.PackageManager.PERMISSION_GRANTED) granted = true;
        }
        if (granted) return;

        if (requestCode == DeviceSearch.REQUEST_FILES) {
            Config.setFileSearchEnabled(this, false);
        } else if (requestCode == DeviceSearch.REQUEST_CONTACTS) {
            Config.setContactSearchEnabled(this, false);
        }
    }

    private void toggleGrayscale() {
        boolean newValue = !Config.isGrayscaleEnabled(this);
        Config.setGrayscaleEnabled(this, newValue);
        boolean applied = newValue ? GrayscaleController.enable(this) : GrayscaleController.disable(this);
        if (!applied) {
            android.widget.Toast.makeText(this,
                    "Couldn't change grayscale — the permission may have been lost. "
                            + "Run: adb shell pm grant com.plainphone.app "
                            + "android.permission.WRITE_SECURE_SETTINGS",
                    android.widget.Toast.LENGTH_LONG).show();
        }
        render();
    }

    private TextView row(String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20);
        view.setPadding(48, 40, 48, 40);
        view.setGravity(Gravity.START);
        view.setTypeface(georgia);

        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        background.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        view.setBackground(background);

        view.setOnClickListener(listener);
        return view;
    }
}
