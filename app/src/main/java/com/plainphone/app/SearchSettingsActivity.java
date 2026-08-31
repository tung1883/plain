package com.plainphone.app;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SearchSettingsActivity extends Activity {

    private LinearLayout root;
    private Typeface georgia;

    private static class Entry {
        final String sortKey;
        final View view;

        Entry(String sortKey, View view) {
            this.sortKey = sortKey;
            this.view = view;
        }
    }

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
        render();
    }

    private void render() {
        georgia = Fonts.current(this);
        root.removeAllViews();

        List<Entry> entries = new ArrayList<>();

        int webTargets = Config.getWebTargets(this).size();
        entries.add(new Entry("My web searches", row(
                "My web searches" + (webTargets > 0 ? " (" + webTargets + ")" : ""),
                v -> startActivity(new android.content.Intent(this, WebTargetsActivity.class)))));

        entries.add(new Entry("Lock search", row(
                "Lock search: " + (Lock.SEARCH.isLocked(this) ? "On" : "Off"),
                v -> Lock.SEARCH.toggleLock(this, this::render))));

        boolean contactSearch = Config.isContactSearchEnabled(this);
        entries.add(new Entry("Search contacts", row(
                "Search contacts: " + (contactSearch ? "On" : "Off"), v -> {
                    Config.setContactSearchEnabled(this, !contactSearch);
                    if (!contactSearch && !DeviceSearch.canSearchContacts(this)) {
                        requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS},
                                DeviceSearch.REQUEST_CONTACTS);
                    }
                    render();
                })));

        boolean fileGranted = FileIndex.canWalk(this);
        boolean fileEnabled = Config.isFileSearchEnabled(this);
        entries.add(new Entry("Search files", row(
                "Search files: " + (fileGranted && fileEnabled ? "On" : "Off"), v -> {
                    if (!fileGranted) {

                        Config.setFileSearchEnabled(this, true);
                        DeviceSearch.requestFullFileAccess(this);
                    } else {
                        Config.setFileSearchEnabled(this, !fileEnabled);
                    }
                    render();
                })));

        boolean webSearch = Config.isWebSearchEnabled(this);
        entries.add(new Entry("Search the web", row(
                "Search the web: " + (webSearch ? "On" : "Off"), v -> {
                    Config.setWebSearchEnabled(this, !webSearch);
                    render();
                })));

        entries.sort(Comparator.comparing(e -> e.sortKey, String.CASE_INSENSITIVE_ORDER));
        for (Entry entry : entries) root.addView(entry.view);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0) return;

        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) granted = true;
        }
        if (granted) return;

        if (requestCode == DeviceSearch.REQUEST_FILES) {
            Config.setFileSearchEnabled(this, false);
        } else if (requestCode == DeviceSearch.REQUEST_CONTACTS) {
            Config.setContactSearchEnabled(this, false);
        }
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

