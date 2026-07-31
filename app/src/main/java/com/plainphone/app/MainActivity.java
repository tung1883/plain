package com.plainphone.app;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private PackageManager pm;
    private List<ResolveInfo> allApps;
    private List<ResolveInfo> apps;
    private ArrayAdapter<ResolveInfo> adapter;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // MainActivity is singleTask, so it's reused (not re-created) whenever something
        // sends it a fresh Intent — including OnboardingActivity handing off once setup is
        // done. Without this, onCreate() never re-runs and the real UI never gets built,
        // since the original onCreate() bailed out early while onboarding was incomplete.
        recreate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences onboardingPrefs = getSharedPreferences("plain", Context.MODE_PRIVATE);
        if (!onboardingPrefs.getBoolean("onboarding_complete", false)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            return;
        }

        pm = getPackageManager();
        setBlackWallpaperOnce();

        allApps = loadLaunchableApps();
        apps = new ArrayList<>(allApps);

        Typeface georgia = Fonts.georgia(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        EditText search = new EditText(this);
        search.setHint("Search");
        search.setHintTextColor(Color.GRAY);
        search.setTextColor(Color.WHITE);
        search.setBackgroundColor(Color.BLACK);
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_GO);
        search.setPadding(48, 32, 48, 32);
        search.setTypeface(georgia);
        root.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView screenOffRow = buildRow(georgia, "Turn off screen");
        screenOffRow.setOnClickListener(v -> AppMonitorService.lockScreen());
        root.addView(screenOffRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView statsRow = buildRow(georgia, "Usage stats");
        statsRow.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        root.addView(statsRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView settingsRow = buildRow(georgia, "Settings");
        settingsRow.setOnClickListener(v -> startActivity(new Intent(this, SettingsGateActivity.class)));
        root.addView(settingsRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView listView = new ListView(this);
        listView.setBackgroundColor(Color.BLACK);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        adapter = new ArrayAdapter<ResolveInfo>(this, android.R.layout.simple_list_item_1, apps) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(labelFor(getItem(position)));
                view.setBackground(rowBackground());
                view.setTextColor(Color.WHITE);
                view.setTextSize(20);
                view.setPadding(48, 40, 48, 40);
                view.setGravity(Gravity.START);
                view.setTypeface(georgia);
                return view;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchApp(apps.get(position));
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO && !apps.isEmpty()) {
                launchApp(apps.get(0));
                return true;
            }
            return false;
        });
    }

    private void filter(String query) {
        apps.clear();
        String needle = query.trim().toLowerCase(Locale.getDefault());
        for (ResolveInfo info : allApps) {
            String label = labelFor(info).toString().toLowerCase(Locale.getDefault());
            if (needle.isEmpty() || label.contains(needle)) {
                apps.add(info);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private TextView buildRow(Typeface georgia, String label) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setBackground(rowBackground());
        row.setTextColor(Color.WHITE);
        row.setTextSize(20);
        row.setPadding(48, 40, 48, 40);
        row.setGravity(Gravity.START);
        row.setTypeface(georgia);
        return row;
    }

    private Drawable rowBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        drawable.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return drawable;
    }

    private void launchApp(ResolveInfo info) {
        // Some apps (e.g. Messenger) declare several launcher activity aliases for
        // icon-skin picking, only one of which is enabled at a time. Reconstructing
        // an explicit Intent from a raw ResolveInfo can point at a disabled alias and
        // silently fail. getLaunchIntentForPackage() defers to Android's own logic
        // for resolving whichever one is actually active.
        Intent launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName);
        if (launchIntent != null) {
            startActivity(launchIntent);
        }
    }

    private void setBlackWallpaperOnce() {
        SharedPreferences prefs = getSharedPreferences("plain", Context.MODE_PRIVATE);
        if (prefs.getBoolean("wallpaper_set", false)) return;
        try {
            Bitmap black = Bitmap.createBitmap(2, 2, Bitmap.Config.RGB_565);
            black.eraseColor(Color.BLACK);
            WallpaperManager.getInstance(this).setBitmap(black);
            prefs.edit().putBoolean("wallpaper_set", true).apply();
        } catch (Exception ignored) {
            // Non-critical cosmetic step; don't block launcher startup on it.
        }
    }

    private List<ResolveInfo> loadLaunchableApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);

        // Apps with multiple launcher aliases (icon-skin pickers, etc.) otherwise
        // show up once per alias; keep only the first entry per package.
        List<ResolveInfo> deduped = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();
        for (ResolveInfo info : resolved) {
            if (seenPackages.add(info.activityInfo.packageName)) {
                deduped.add(info);
            }
        }

        Collections.sort(deduped, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return labelFor(a).toString().compareToIgnoreCase(labelFor(b).toString());
            }
        });
        return deduped;
    }

    // Activity/alias-level labels can be overridden per alias (e.g. Messenger's
    // icon-skin aliases), which can shadow unrelated apps sharing that label.
    // The application-level label is the one shown in Settings > Apps and can't
    // be overridden per-alias, so it's the reliable one to display and sort by.
    private CharSequence labelFor(ResolveInfo info) {
        return info.activityInfo.applicationInfo.loadLabel(pm);
    }
}
