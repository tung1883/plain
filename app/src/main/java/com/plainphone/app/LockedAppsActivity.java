package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LockedAppsActivity extends Activity {

    private PackageManager pm;
    private List<ResolveInfo> apps;
    private List<String> labels;
    private ListView listView;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();
        apps = loadLaunchableApps();

        labels = new ArrayList<>();
        for (ResolveInfo info : apps) {
            labels.add(info.activityInfo.applicationInfo.loadLabel(pm).toString());
        }

        listView = new ListView(this);
        listView.setBackgroundColor(Color.BLACK);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        UiKit.screen(this, "App lock", listView);

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
                    Typeface font = Fonts.current(LockedAppsActivity.this);

                    row = new LinearLayout(LockedAppsActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setBackground(rowBackground());
                    row.setPadding(48, 30, 48, 30);

                    label = new TextView(LockedAppsActivity.this);
                    label.setTextColor(Color.WHITE);
                    label.setTextSize(20);
                    label.setTypeface(font);
                    label.setGravity(Gravity.START);
                    row.addView(label, new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    mark = new TextView(LockedAppsActivity.this);
                    mark.setTextSize(18);
                    mark.setTypeface(font);
                    mark.setGravity(Gravity.CENTER);
                    row.addView(mark, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                }

                String pkg = apps.get(position).activityInfo.packageName;
                label.setText(labels.get(position));
                boolean locked = Config.getLockedPackages(LockedAppsActivity.this).contains(pkg);
                mark.setText(locked ? "[x]" : "[ ]");
                mark.setTextColor(locked ? Color.WHITE : Color.GRAY);
                return row;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            int p = position - listView.getHeaderViewsCount();
            if (p < 0 || p >= apps.size()) return;
            String pkg = apps.get(p).activityInfo.packageName;
            String appLabel = labels.get(p);
            if (Config.getLockedPackages(this).contains(pkg)) {
                startActivity(new Intent(this, LockedAppChangeActivity.class)
                        .putExtra("package", pkg).putExtra("label", appLabel));
            } else {
                Set<String> locked = Config.getLockedPackages(this);
                locked.add(pkg);
                Config.setLockedPackages(this, locked);
                if (!Config.isPinSet(this, "applock")) {
                    android.widget.Toast.makeText(this,
                            "Set a master PIN in Settings → Locker",
                            android.widget.Toast.LENGTH_LONG).show();
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    private Drawable rowBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        drawable.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return drawable;
    }

    private List<ResolveInfo> loadLaunchableApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);

        List<ResolveInfo> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : resolved) {
            if (info.activityInfo.packageName.equals(getPackageName())) continue;
            if (seen.add(info.activityInfo.packageName)) {
                deduped.add(info);
            }
        }

        Collections.sort(deduped, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return a.activityInfo.applicationInfo.loadLabel(pm).toString()
                        .compareToIgnoreCase(b.activityInfo.applicationInfo.loadLabel(pm).toString());
            }
        });
        return deduped;
    }
}

