package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class StatsActivity extends Activity {

    private static final int BAR_HEIGHT = 20;

    private ScrollView scroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render(); // reflect usage access granted (or revoked) while we were away
    }

    private void render() {
        Typeface georgia = Fonts.current(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(48, 48, 48, 48);

        addSectionTitle(content, georgia, "Flagged apps — Today", 0);
        addUsageSection(content, georgia,
                UsageStore.rangeUsage(this, Config.getFlaggedPackages(this), 0, 0));

        addSectionTitle(content, georgia, "Flagged apps — Last 7 days", 48);
        addUsageSection(content, georgia,
                UsageStore.rangeUsage(this, Config.getFlaggedPackages(this), -6, 0));

        addSectionTitle(content, georgia, "All apps — This week", 48);

        if (!AllAppsUsage.hasUsageAccess(this)) {
            TextView notice = new TextView(this);
            notice.setTextColor(Color.WHITE);
            notice.setTextSize(16);
            notice.setTypeface(georgia);
            notice.setPadding(0, 16, 0, 16);
            notice.setText("Needs \"Usage access\" permission, granted once in system settings.");
            content.addView(notice);

            Button grant = new Button(this);
            grant.setText("Grant usage access");
            UiKit.style(this, grant);
            grant.setOnClickListener(v ->
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
            content.addView(grant);
        } else {
            List<AllAppsUsage.Entry> entries = AllAppsUsage.weeklyComparison(this, 12);
            addAllAppsSection(content, georgia, entries);
        }

        scroll.removeAllViews();
        scroll.addView(content);
    }

    private void addSectionTitle(LinearLayout content, Typeface georgia, String text, int topMargin) {
        TextView title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(georgia);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = topMargin;
        params.bottomMargin = 16;
        title.setLayoutParams(params);
        title.setText(text);
        content.addView(title);
    }

    private void addUsageSection(LinearLayout content, Typeface georgia, List<UsageStore.Entry> entries) {
        if (entries.isEmpty()) {
            content.addView(emptyText(georgia, "(no usage)"));
            return;
        }

        long max = 1;
        long totalMillis = 0;
        for (UsageStore.Entry e : entries) {
            max = Math.max(max, e.millis);
            totalMillis += e.millis;
        }

        for (UsageStore.Entry e : entries) {
            String caption = e.label + "  " + formatDuration(e.millis) + ", "
                    + e.opens + (e.opens == 1 ? " open" : " opens");
            content.addView(buildBarRow(georgia, caption, e.millis, max));
        }
        content.addView(totalText(georgia, totalMillis));
    }

    private void addAllAppsSection(LinearLayout content, Typeface georgia, List<AllAppsUsage.Entry> entries) {
        if (entries.isEmpty()) {
            content.addView(emptyText(georgia, "(no usage data yet)"));
            return;
        }

        long max = 1;
        long totalMillis = 0;
        for (AllAppsUsage.Entry e : entries) {
            max = Math.max(max, e.thisWeekMillis);
            totalMillis += e.thisWeekMillis;
        }

        for (AllAppsUsage.Entry e : entries) {
            String caption = e.label + "  " + formatDuration(e.thisWeekMillis);
            content.addView(buildBarRow(georgia, caption, e.thisWeekMillis, max));
        }
        content.addView(totalText(georgia, totalMillis));
    }

    /** A label line over a flat black/white bar, its fill width proportional to millis/max. */
    private View buildBarRow(Typeface georgia, String caption, long millis, long max) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = 20;
        row.setLayoutParams(rowParams);

        TextView label = new TextView(this);
        label.setTextColor(Color.WHITE);
        label.setTextSize(15);
        label.setTypeface(georgia);
        label.setText(caption);
        row.addView(label);

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackgroundColor(Color.DKGRAY);
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, BAR_HEIGHT);
        trackParams.topMargin = 8;
        row.addView(track, trackParams);

        long clamped = Math.max(0, Math.min(millis, max));
        View fill = new View(this);
        fill.setBackgroundColor(Color.WHITE);
        track.addView(fill, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, clamped));

        long remainder = max - clamped;
        if (remainder > 0) {
            View spacer = new View(this);
            track.addView(spacer, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, remainder));
        }

        return row;
    }

    private TextView emptyText(Typeface georgia, String text) {
        TextView view = new TextView(this);
        view.setTextColor(Color.WHITE);
        view.setTextSize(15);
        view.setTypeface(georgia);
        view.setText(text);
        return view;
    }

    private TextView totalText(Typeface georgia, long totalMillis) {
        TextView view = new TextView(this);
        view.setTextColor(Color.WHITE);
        view.setTextSize(15);
        view.setTypeface(georgia);
        view.setText("Total: " + formatDuration(totalMillis));
        return view;
    }

    private static String formatDuration(long millis) {
        long totalMinutes = millis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? (hours + "h " + minutes + "m") : (minutes + "m");
    }
}
