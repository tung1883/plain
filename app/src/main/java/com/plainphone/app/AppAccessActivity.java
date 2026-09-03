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

public class AppAccessActivity extends Activity {

    private static final int REQUEST_NOTIFICATIONS = 3001;

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
        UiKit.screen(this, "App access", scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        root.removeAllViews();

        root.addView(row("Accessibility service", AppMonitorService.isEnabled(this), null,
                v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));

        root.addView(row("Usage access", AllAppsUsage.hasUsageAccess(this), null,
                v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));

        boolean files = FileIndex.canWalk(this) || DeviceSearch.canSearchFiles(this);
        root.addView(row("Files", files, null,
                v -> DeviceSearch.requestFullFileAccess(this)));

        boolean contacts = DeviceSearch.canSearchContacts(this);
        root.addView(row("Contacts", contacts, null, v -> {
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
            root.addView(row("Notifications", notifications, null, v -> {
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

        root.addView(sectionHeader("Granted at install"));
        root.addView(row("See installed apps", null, null, null));
        root.addView(row("Set wallpaper", null, null, null));
        root.addView(row("Uninstall apps", null, null, null));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        render();
    }

    private TextView sectionHeader(String label) {
        TextView header = new TextView(this);
        header.setText(label.toUpperCase());
        header.setTextColor(Color.GRAY);
        header.setTextSize(13);
        header.setLetterSpacing(0.15f);
        header.setTypeface(font);
        header.setPadding(48, 40, 48, 12);
        return header;
    }

    /** "Label ..... On/Off" on top, a "what it's for" subtitle below. */
    private View row(String label, Boolean on, String subtitle, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(48, 28, 48, 28);

        if (listener != null) {
            StateListDrawable bg = new StateListDrawable();
            bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
            bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
            row.setBackground(bg);
            row.setOnClickListener(listener);
        }

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(font);
        head.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (on != null) {
            TextView value = new TextView(this);
            value.setText(on ? "On" : "Off");
            value.setTextColor(on ? Color.WHITE : Color.GRAY);
            value.setTextSize(16);
            value.setTypeface(font);
            head.addView(value);
        }
        row.addView(head);

        if (subtitle != null) {
            TextView detail = new TextView(this);
            detail.setText(subtitle);
            detail.setTextColor(Color.GRAY);
            detail.setTextSize(13);
            detail.setTypeface(font);
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dp.topMargin = 5;
            row.addView(detail, dp);
        }

        return row;
    }
}
