package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * A full account of what Plain currently has access to on this phone — everything it can
 * see or control, in one place, rather than scattered one setting at a time. Split into
 * what needed the user's own say-so (Accessibility, usage stats, files, contacts...) and
 * what Android grants automatically at install because the risk is low enough not to ask.
 */
public class AppAccessActivity extends Activity {

    private static final int REQUEST_NOTIFICATIONS = 3001;

    private LinearLayout root;
    private Typeface georgia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        georgia = Fonts.current(this);

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
        render(); // most of these can only change by leaving to a system screen and back
    }

    private void render() {
        root.removeAllViews();

        TextView hint = new TextView(this);
        hint.setText("Everything Plain currently has access to on this phone.");
        hint.setTextColor(Color.GRAY);
        hint.setTextSize(14);
        hint.setTypeface(georgia);
        hint.setPadding(48, 40, 48, 24);
        root.addView(hint);

        root.addView(sectionHeader("Needs your permission"));

        boolean accessibilityOn = AppMonitorService.isEnabled(this);
        root.addView(row("Accessibility Service",
                accessibilityOn ? "Enabled — locking, flagging, and time blocks all depend on this"
                        : "Disabled — app locking, flagging, and time blocks won't work",
                v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));

        boolean usageAccess = AllAppsUsage.hasUsageAccess(this);
        root.addView(row("Usage access", usageAccess ? "Granted" : "Not granted — Usage stats will be incomplete",
                v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));

        boolean fullFiles = FileIndex.canWalk(this);
        boolean mediaFiles = DeviceSearch.canSearchFiles(this);
        root.addView(row("Files",
                fullFiles ? "Full access — folders and every file type"
                        : mediaFiles ? "Media only — photos, video, and audio; no folders"
                        : "Not granted",
                v -> DeviceSearch.requestFullFileAccess(this)));

        boolean contacts = DeviceSearch.canSearchContacts(this);
        root.addView(row("Contacts", contacts ? "Granted" : "Not granted", v -> {
            if (contacts) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            } else {
                requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS},
                        DeviceSearch.REQUEST_CONTACTS);
            }
        }));

        if (Build.VERSION.SDK_INT >= 33) {
            boolean notifications = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
            root.addView(row("Notifications",
                    notifications ? "Granted" : "Not granted — the \"closing soon\" warning won't show",
                    v -> {
                        if (notifications) {
                            Intent open = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                            open.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                            startActivity(open);
                        } else {
                            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                                    REQUEST_NOTIFICATIONS);
                        }
                    }));
        }

        root.addView(sectionHeader("Granted automatically"));

        root.addView(row("See installed apps",
                "Granted at install — how the home screen lists every app", null));
        root.addView(row("Set wallpaper",
                "Granted at install — used once, to set the black background", null));
        root.addView(row("Uninstall apps",
                "Granted at install — powers the Uninstall option on an app's long-press menu",
                null));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        render(); // whatever happened — granted, denied, or dismissed — reflect it
    }

    private TextView sectionHeader(String label) {
        TextView header = new TextView(this);
        header.setText(label.toUpperCase());
        header.setTextColor(Color.GRAY);
        header.setTextSize(13);
        header.setLetterSpacing(0.15f);
        header.setTypeface(georgia);
        header.setPadding(48, 36, 48, 12);
        return header;
    }

    /** listener may be null for an informational row that has nothing to fix or review. */
    private View row(String label, String subtitle, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(48, 28, 48, 28);

        if (listener != null) {
            StateListDrawable background = new StateListDrawable();
            background.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
            background.addState(new int[]{}, new ColorDrawable(Color.BLACK));
            row.setBackground(background);
            row.setOnClickListener(listener);
        }

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.START);
        title.setTypeface(georgia);
        row.addView(title);

        TextView detail = new TextView(this);
        detail.setText(subtitle);
        detail.setTextColor(Color.GRAY);
        detail.setTextSize(13);
        detail.setTypeface(georgia);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = 4;
        row.addView(detail, detailParams);

        return row;
    }
}
