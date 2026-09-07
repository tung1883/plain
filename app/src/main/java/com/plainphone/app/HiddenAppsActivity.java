package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Paint;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HiddenAppsActivity extends Activity {

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

        Typeface georgia = Fonts.current(this);

        listView = new ListView(this);
        listView.setBackgroundColor(Color.BLACK);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        UiKit.screen(this, "Hidden apps", listView);

        labels = new ArrayList<>();
        for (ResolveInfo info : apps) {
            labels.add(info.activityInfo.applicationInfo.loadLabel(pm).toString());
        }

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
                    row = new LinearLayout(HiddenAppsActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setBackground(rowBackground());
                    row.setPadding(48, 30, 48, 30);

                    label = new TextView(HiddenAppsActivity.this);
                    label.setTextColor(Color.WHITE);
                    label.setTextSize(20);
                    label.setTypeface(georgia);
                    label.setGravity(Gravity.START);
                    row.addView(label, new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    mark = new TextView(HiddenAppsActivity.this);
                    mark.setTextSize(18);
                    mark.setTypeface(georgia);
                    mark.setGravity(Gravity.CENTER);
                    row.addView(mark, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                }

                boolean hidden = Config.getHiddenPackages(HiddenAppsActivity.this)
                        .contains(apps.get(position).activityInfo.packageName);
                label.setText(labels.get(position));
                int flags = label.getPaintFlags();
                label.setPaintFlags(hidden ? (flags | Paint.STRIKE_THRU_TEXT_FLAG)
                        : (flags & ~Paint.STRIKE_THRU_TEXT_FLAG));
                mark.setText(hidden ? "[x]" : "[ ]");
                mark.setTextColor(hidden ? Color.WHITE : Color.GRAY);
                return row;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String pkg = apps.get(position).activityInfo.packageName;
            boolean currentlyHidden = Config.getHiddenPackages(this).contains(pkg);

            if (currentlyHidden) {

                Intent intent = new Intent(this, HiddenAppChangeActivity.class);
                intent.putExtra("package", pkg);
                intent.putExtra("label", labels.get(position));
                startActivity(intent);
            } else {

                Set<String> hidden = Config.getHiddenPackages(this);
                hidden.add(pkg);
                Config.setHiddenPackages(this, hidden);
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
