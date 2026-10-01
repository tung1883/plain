package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pick apps from the launcher list with [x] / [ ] rows — for banking apps or monochrome apps. */
public class AppPickerActivity extends Activity {

    static final String EXTRA_KIND = "kind";
    static final String KIND_BANKING = "banking";
    static final String KIND_MONOCHROME = "monochrome";

    private boolean banking;
    private List<ResolveInfo> apps;
    private List<String> labels;
    private ArrayAdapter<String> adapter;
    private ListView listView;
    private TextView wholeScreenRow;
    private TextView specificAppsRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        banking = KIND_BANKING.equals(getIntent().getStringExtra(EXTRA_KIND));
        PackageManager pm = getPackageManager();
        apps = loadLaunchableApps(pm);
        Typeface font = Fonts.current(this);

        labels = new ArrayList<>();
        for (ResolveInfo info : apps) {
            labels.add(info.activityInfo.applicationInfo.loadLabel(pm).toString());
        }

        listView = new ListView(this);
        listView.setBackgroundColor(Color.BLACK);
        listView.setDivider(null);
        listView.setDividerHeight(0);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Color.BLACK);

        if (banking) {
            TextView guidance = new TextView(this);
            guidance.setText("For these apps, Plain will:\n"
                    + "\u2022 Turn off accessibility service temporarily\n"
                    + "\u2022 Turn it back later");
            guidance.setTextColor(Color.GRAY);
            guidance.setTextSize(13);
            guidance.setTypeface(font);
            guidance.setLineSpacing(0f, 1.6f);
            guidance.setPadding(48, 32, 48, 28);
            screen.addView(guidance);
        }

        if (!banking) {
            wholeScreenRow = modeRow(font, Config.MONO_ALL);
            specificAppsRow = modeRow(font, Config.MONO_APPS);
            screen.addView(wholeScreenRow);
            screen.addView(specificAppsRow);
            refreshMode();
        }
        screen.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        UiKit.screen(this, banking ? "Blocking apps" : "Monochrome", screen);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout row;
                TextView label;
                TextView mark;
                if (convertView instanceof LinearLayout) {
                    row = (LinearLayout) convertView;
                    label = (TextView) row.getChildAt(0);
                    mark = (TextView) row.getChildAt(1);
                } else {
                    row = new LinearLayout(AppPickerActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setBackground(rowBackground());
                    row.setPadding(48, 30, 48, 30);

                    label = new TextView(AppPickerActivity.this);
                    label.setTextColor(Color.WHITE);
                    label.setTextSize(20);
                    label.setTypeface(font);
                    row.addView(label, new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    mark = new TextView(AppPickerActivity.this);
                    mark.setTextSize(18);
                    mark.setTypeface(font);
                    row.addView(mark, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                }
                boolean picked = current().contains(apps.get(position).activityInfo.packageName);
                label.setText(labels.get(position));
                mark.setText(picked ? "[x]" : "[ ]");
                mark.setTextColor(picked ? Color.WHITE : Color.GRAY);
                return row;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String pkg = apps.get(position).activityInfo.packageName;
            Set<String> set = current();
            if (!set.remove(pkg)) set.add(pkg);
            save(set);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        ForegroundWatcher.sync(this);
    }

    private TextView modeRow(Typeface font, String mode) {
        TextView row = new TextView(this);
        row.setTextColor(Color.WHITE);
        row.setTextSize(20);
        row.setTypeface(font);
        row.setPadding(48, 24, 48, 24);
        row.setBackground(rowBackground());
        row.setOnClickListener(v -> {
            Config.setMonochromeMode(this, mode);
            ForegroundExtras.applyMonochromeMode(this);
            refreshMode();
        });
        return row;
    }

    /** "Whole screen" hides the app list: it doesn't depend on which app is open. */
    private void refreshMode() {
        boolean whole = Config.MONO_ALL.equals(Config.getMonochromeMode(this));
        wholeScreenRow.setText((whole ? "\u25CF " : "\u25CB ") + "Whole screen");
        specificAppsRow.setText((whole ? "\u25CB " : "\u25CF ") + "Specific apps");
        listView.setVisibility(whole ? View.GONE : View.VISIBLE);
    }

    private Set<String> current() {
        return banking ? Config.getBankingPackages(this) : Config.getMonochromePackages(this);
    }

    private void save(Set<String> set) {
        if (banking) Config.setBankingPackages(this, set);
        else Config.setMonochromePackages(this, set);
    }

    private Drawable rowBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, UiKit.pressedFill());
        drawable.setEnterFadeDuration(120);
        drawable.setExitFadeDuration(120);
        drawable.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return drawable;
    }

    private List<ResolveInfo> loadLaunchableApps(PackageManager pm) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);

        List<ResolveInfo> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : resolved) {
            if (info.activityInfo.packageName.equals(getPackageName())) continue;
            if (seen.add(info.activityInfo.packageName)) deduped.add(info);
        }
        Collections.sort(deduped, (a, b) -> a.activityInfo.applicationInfo.loadLabel(pm).toString()
                .compareToIgnoreCase(b.activityInfo.applicationInfo.loadLabel(pm).toString()));
        return deduped;
    }
}
