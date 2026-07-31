package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlaggedAppsActivity extends Activity {

    private PackageManager pm;
    private List<ResolveInfo> apps;
    private List<String> labels;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();
        apps = loadLaunchableApps();

        Typeface georgia = Fonts.georgia(this);

        listView = new ListView(this);
        listView.setBackgroundColor(Color.BLACK);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        setContentView(listView);

        labels = new ArrayList<>();
        for (ResolveInfo info : apps) {
            labels.add(info.activityInfo.applicationInfo.loadLabel(pm).toString());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_multiple_choice, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
                view.setBackgroundColor(Color.BLACK);
                view.setTextColor(Color.WHITE);
                view.setTypeface(georgia);
                view.setPadding(48, 32, 48, 32);
                return view;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String pkg = apps.get(position).activityInfo.packageName;
            boolean currentlyFlagged = Config.getFlaggedPackages(this).contains(pkg);

            if (currentlyFlagged) {
                // Removing loosens the restriction, so it goes through the friction
                // gate. Revert ListView's auto-toggle; the real change only applies
                // if the gate is completed.
                listView.setItemChecked(position, true);
                Intent intent = new Intent(this, FlaggedAppChangeActivity.class);
                intent.putExtra("package", pkg);
                intent.putExtra("label", labels.get(position));
                startActivity(intent);
            } else {
                // Adding tightens the restriction, so it applies immediately.
                Set<String> flagged = Config.getFlaggedPackages(this);
                flagged.add(pkg);
                Config.setFlaggedPackages(this, flagged);
                listView.setItemChecked(position, true);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncChecksToConfig(); // reflect a change confirmed while we were away
    }

    private void syncChecksToConfig() {
        Set<String> flagged = Config.getFlaggedPackages(this);
        for (int i = 0; i < apps.size(); i++) {
            listView.setItemChecked(i, flagged.contains(apps.get(i).activityInfo.packageName));
        }
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
