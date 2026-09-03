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

public class FlaggedAppsActivity extends Activity {

    private PackageManager pm;
    private List<ResolveInfo> apps;
    private List<String> labels;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private TextView waitRow;
    private TextView budgetToggleRow;
    private TextView budgetRow;
    private TextView lockoutToggleRow;
    private TextView lockoutRow;

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
        listView.addHeaderView(buildHeader(georgia), null, false);
        UiKit.screen(this, "Flagged apps", listView);

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

                boolean flagged = Config.getFlaggedPackages(FlaggedAppsActivity.this)
                        .contains(apps.get(position).activityInfo.packageName);
                int flags = view.getPaintFlags();
                view.setPaintFlags(flagged ? (flags | Paint.STRIKE_THRU_TEXT_FLAG)
                        : (flags & ~Paint.STRIKE_THRU_TEXT_FLAG));
                return view;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, rawPosition, id) -> {

            int position = rawPosition - listView.getHeaderViewsCount();
            if (position < 0) return;

            String pkg = apps.get(position).activityInfo.packageName;
            boolean currentlyFlagged = Config.getFlaggedPackages(this).contains(pkg);

            if (currentlyFlagged) {

                Intent intent = new Intent(this, FlaggedAppChangeActivity.class);
                intent.putExtra("package", pkg);
                intent.putExtra("label", labels.get(position));
                startActivity(intent);
            } else {

                Set<String> flagged = Config.getFlaggedPackages(this);
                flagged.add(pkg);
                Config.setFlaggedPackages(this, flagged);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        refreshHeader();
    }

    private LinearLayout buildHeader(Typeface georgia) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.BLACK);

        waitRow = row(georgia, v -> startActivity(new Intent(this, WaitTimeActivity.class)));
        header.addView(waitRow);

        budgetToggleRow = row(georgia, v -> {
            Config.setBudgetEnabled(this, !Config.isBudgetEnabled(this));
            refreshHeader();
        });
        header.addView(budgetToggleRow);

        budgetRow = row(georgia, v -> startActivity(new Intent(this, BudgetTimeActivity.class)));
        header.addView(budgetRow);

        lockoutToggleRow = row(georgia, v -> {
            Config.setLockoutEnabled(this, !Config.isLockoutEnabled(this));
            refreshHeader();
        });
        header.addView(lockoutToggleRow);

        lockoutRow = row(georgia, v -> startActivity(new Intent(this, LockoutTimeActivity.class)));
        header.addView(lockoutRow);

        header.addView(divider());

        TextView listTitle = new TextView(this);
        listTitle.setText("List of apps");
        listTitle.setTextColor(Color.GRAY);
        listTitle.setTextSize(14);
        listTitle.setTypeface(georgia);
        listTitle.setPadding(48, 24, 48, 16);
        header.addView(listTitle);

        return header;
    }

    private void refreshHeader() {
        waitRow.setText("Wait time: " + Config.getWaitSeconds(this) + "s");

        boolean budgetOn = Config.isBudgetEnabled(this);
        budgetToggleRow.setText("Auto-close: " + (budgetOn ? "On" : "Off"));
        budgetRow.setText("Auto-close after: " + Config.getBudgetMinutes(this) + "m");
        budgetRow.setVisibility(budgetOn ? View.VISIBLE : View.GONE);

        boolean lockoutOn = Config.isLockoutEnabled(this);
        lockoutToggleRow.setText("Reopen lockout: " + (lockoutOn ? "On" : "Off"));
        lockoutRow.setText("Reopen lockout after: " + Config.getLockoutMinutes(this) + "m");
        lockoutRow.setVisibility(lockoutOn ? View.VISIBLE : View.GONE);
    }

    private TextView row(Typeface georgia, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20);
        view.setPadding(48, 32, 48, 32);
        view.setGravity(Gravity.START);
        view.setTypeface(georgia);
        view.setBackground(rowBackground());
        view.setOnClickListener(listener);
        return view;
    }

    private View divider() {
        View line = new View(this);
        line.setBackgroundColor(Color.DKGRAY);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        params.topMargin = 16;
        line.setLayoutParams(params);
        return line;
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

