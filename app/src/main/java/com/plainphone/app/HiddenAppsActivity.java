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

        Typeface georgia = Fonts.georgia(this);

        listView = new ListView(this);
        listView.setBackgroundColor(Color.BLACK);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        setContentView(listView);

        labels = new ArrayList<>();
        for (ResolveInfo info : apps) {
            labels.add(info.activityInfo.applicationInfo.loadLabel(pm).toString());
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(labels.get(position));
                view.setBackground(rowBackground());
                view.setTextColor(Color.WHITE);
                view.setTextSize(20);
                view.setPadding(48, 32, 48, 32);
                view.setGravity(Gravity.START);
                view.setTypeface(georgia);

                // Hidden apps read as crossed out instead of using a separate checkbox.
                boolean hidden = Config.getHiddenPackages(HiddenAppsActivity.this)
                        .contains(apps.get(position).activityInfo.packageName);
                int flags = view.getPaintFlags();
                view.setPaintFlags(hidden ? (flags | Paint.STRIKE_THRU_TEXT_FLAG)
                        : (flags & ~Paint.STRIKE_THRU_TEXT_FLAG));
                return view;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String pkg = apps.get(position).activityInfo.packageName;
            boolean currentlyHidden = Config.getHiddenPackages(this).contains(pkg);

            if (currentlyHidden) {
                // Unhiding loosens the restriction, so it goes through the friction gate.
                // The crossed-out look only clears once the gate is actually completed.
                Intent intent = new Intent(this, HiddenAppChangeActivity.class);
                intent.putExtra("package", pkg);
                intent.putExtra("label", labels.get(position));
                startActivity(intent);
            } else {
                // Hiding tightens the restriction, so it applies immediately.
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
        adapter.notifyDataSetChanged(); // reflect a change confirmed while we were away
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
