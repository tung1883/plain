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
import android.graphics.PorterDuff;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
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
    private EditText search;
    private boolean showingHomeReminder = false;
    private boolean homeUiBuilt = false;

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
        pm = getPackageManager();

        SharedPreferences onboardingPrefs = getSharedPreferences("plain", Context.MODE_PRIVATE);
        if (!onboardingPrefs.getBoolean("onboarding_complete", false)) {
            // OnboardingActivity has its own taskAffinity + singleTask, and this flag puts
            // it in its own separate task. Without this, it shares MainActivity's task —
            // and since MainActivity is itself singleTask, every time Home is pressed,
            // Android destroys everything stacked above MainActivity in that task
            // (per singleTask semantics), wiping out onboarding's progress each time.
            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        if (!isDefaultHomeApp()) {
            // Plain was opened via its own launcher icon (not by pressing Home), and it's
            // not currently set as the Home app — e.g. the user picked a different launcher
            // via Settings > Apps > Default apps. Ask them to fix it instead of silently
            // showing a home screen that Home won't actually route to.
            showSetHomeReminder();
            return;
        }

        startLoadingHomeUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (showingHomeReminder && isDefaultHomeApp()) {
            showingHomeReminder = false;
            startLoadingHomeUi();
        } else if (homeUiBuilt) {
            // Apps may have been hidden/unhidden (or uninstalled) while we were away
            // (e.g. via Settings or a long-press action); reflect that on return.
            refreshApps();
        }
    }

    private void refreshApps() {
        new Thread(() -> {
            List<ResolveInfo> loaded = loadLaunchableApps();
            runOnUiThread(() -> {
                allApps = loaded;
                filter(search.getText().toString());
            });
        }).start();
    }

    private boolean isDefaultHomeApp() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && getPackageName().equals(resolveInfo.activityInfo.packageName);
    }

    private void showSetHomeReminder() {
        showingHomeReminder = true;
        Typeface georgia = Fonts.georgia(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 48, 48, 48);

        TextView message = new TextView(this);
        message.setTextColor(Color.WHITE);
        message.setTextSize(18);
        message.setTypeface(georgia);
        message.setGravity(Gravity.CENTER);
        message.setText("Plain isn't set as your Home app right now.");
        root.addView(message);

        Button openHomeSettings = new Button(this);
        openHomeSettings.setText("Open Home app settings");
        UiKit.style(this, openHomeSettings);
        openHomeSettings.setOnClickListener(v ->
                startActivity(new Intent(android.provider.Settings.ACTION_HOME_SETTINGS)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 32;
        root.addView(openHomeSettings, params);

        setContentView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void startLoadingHomeUi() {
        setBlackWallpaperOnce();

        // Querying every launchable app (loadLaunchableApps) can take a noticeable moment,
        // during which the screen would otherwise just look frozen/blank. Show a spinner
        // immediately and do the query off the main thread so it actually gets to render.
        showLoadingSpinner();
        new Thread(() -> {
            List<ResolveInfo> loaded = loadLaunchableApps();
            runOnUiThread(() -> buildHomeUi(loaded));
        }).start();
    }

    private void showLoadingSpinner() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);

        ProgressBar spinner = new ProgressBar(this);
        spinner.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        root.addView(spinner);

        TextView label = new TextView(this);
        label.setText("Loading...");
        label.setTextColor(Color.WHITE);
        label.setTypeface(Fonts.georgia(this));
        label.setTextSize(16);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, 32, 0, 0);
        root.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void buildHomeUi(List<ResolveInfo> loaded) {
        allApps = loaded;
        apps = new ArrayList<>(allApps);
        homeUiBuilt = true;

        Typeface georgia = Fonts.georgia(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        search = new EditText(this);
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

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showAppOptions(apps.get(position));
            return true;
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

    private void showAppOptions(ResolveInfo info) {
        String pkg = info.activityInfo.packageName;
        CharSequence label = labelFor(info);

        // Preinstalled system apps (e.g. Settings) can't be uninstalled — only ones the
        // user installed, or a system app the user has since updated, actually support it.
        // Offering "Uninstall" for the rest would just bounce off a system "can't do that"
        // dialog, so leave it out entirely rather than show a dead-end action.
        int flags = info.activityInfo.applicationInfo.flags;
        boolean uninstallable = (flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
                || (flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;

        List<CharSequence> items = new ArrayList<>();
        if (uninstallable) items.add("Uninstall");
        items.add("Flag app");

        // Intentionally offers only forward actions (uninstall, flag) — no way to
        // un-flag or unhide from here; loosening a restriction goes through Settings
        // and its friction gate instead, so it can't be undone on impulse.
        new android.app.AlertDialog.Builder(this)
                .setTitle(label)
                .setItems(items.toArray(new CharSequence[0]), (dialog, which) -> {
                    if (uninstallable && which == 0) {
                        Intent uninstall = new Intent(Intent.ACTION_DELETE,
                                android.net.Uri.parse("package:" + pkg));
                        startActivity(uninstall);
                    } else {
                        Set<String> flagged = Config.getFlaggedPackages(this);
                        flagged.add(pkg);
                        Config.setFlaggedPackages(this, flagged);
                        android.widget.Toast.makeText(this, label + " flagged",
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
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
        // show up once per alias; keep only the first entry per package. Hidden
        // packages are excluded here too, so they're absent from search as well.
        Set<String> hiddenPackages = Config.getHiddenPackages(this);
        List<ResolveInfo> deduped = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();
        for (ResolveInfo info : resolved) {
            if (hiddenPackages.contains(info.activityInfo.packageName)) continue;
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
