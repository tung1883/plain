package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsActivity extends Activity {

    private static final int BAR_HEIGHT = 20;

    private enum Range {
        TODAY("Today", 0, "EEE"),
        WEEK("7 days", -6, "EEE"),
        MONTH("1 month", -29, "d/M");

        final String label;
        final int startOffset;
        final String dayLabelPattern;

        Range(String label, int startOffset, String dayLabelPattern) {
            this.label = label;
            this.startOffset = startOffset;
            this.dayLabelPattern = dayLabelPattern;
        }
    }

    private static class Snapshot {
        List<UsageStore.DayTotal> flaggedDaily;
        List<UsageStore.Entry> flaggedApps;
        boolean hasUsageAccess;
        List<UsageStore.DayTotal> allDaily;
        List<AllAppsUsage.Entry> allApps;
    }

    private ScrollView scroll;
    private Range range = Range.WEEK;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private int generation;

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
        render();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    private LinearLayout newContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(48, 48, 48, 48);
        return content;
    }

    private void render() {
        final int gen = ++generation;
        final Range r = range;
        final Typeface georgia = Fonts.current(this);

        LinearLayout shell = newContent();
        addToggle(shell, georgia, r);
        shell.addView(emptyText(georgia, "Loading…"));
        scroll.removeAllViews();
        scroll.addView(shell);

        io.execute(() -> {
            final Snapshot snap = loadSnapshot(r);
            main.post(() -> {
                if (gen != generation || isFinishing()) return;
                LinearLayout content = newContent();
                addToggle(content, georgia, r);
                bind(content, georgia, r, snap);
                scroll.removeAllViews();
                scroll.addView(content);
            });
        });
    }

    private Snapshot loadSnapshot(Range r) {
        Snapshot s = new Snapshot();
        java.util.Set<String> flagged = Config.getFlaggedPackages(this);

        if (r != Range.TODAY) {
            s.flaggedDaily = UsageStore.dailyTotals(this, flagged, r.startOffset, 0, r.dayLabelPattern);
        }
        s.flaggedApps = UsageStore.rangeUsage(this, flagged, r.startOffset, 0);

        s.hasUsageAccess = AllAppsUsage.hasUsageAccess(this);
        if (s.hasUsageAccess) {
            if (r != Range.TODAY) {
                s.allDaily = AllAppsUsage.dailyTotals(this, r.startOffset, 0, r.dayLabelPattern);
            }
            s.allApps = AllAppsUsage.topApps(this, 12, rangeStartMillis(r), System.currentTimeMillis());
        }
        return s;
    }

    private void bind(LinearLayout content, Typeface georgia, Range r, Snapshot snap) {
        String span = r.label;

        addSectionTitle(content, georgia, "Flagged apps — " + span, 32);
        if (snap.flaggedDaily != null) {
            addDailyChart(content, georgia, snap.flaggedDaily);
        }
        addUsageSection(content, georgia, snap.flaggedApps);

        addSectionTitle(content, georgia, "All apps — " + span, 48);
        if (!snap.hasUsageAccess) {
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
            if (snap.allDaily != null) {
                addDailyChart(content, georgia, snap.allDaily);
            }
            addAllAppsSection(content, georgia, snap.allApps);
        }
    }

    private long rangeStartMillis(Range r) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, r.startOffset);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void addToggle(LinearLayout content, Typeface georgia, Range current) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Range[] values = Range.values();
        for (int i = 0; i < values.length; i++) {
            Range r = values[i];
            boolean selected = r == current;

            TextView tab = new TextView(this);
            tab.setText(r.label);
            tab.setTypeface(georgia);
            tab.setTextSize(15);
            tab.setGravity(android.view.Gravity.CENTER);
            tab.setPadding(0, 20, 0, 20);
            tab.setBackgroundColor(selected ? Color.WHITE : Color.DKGRAY);
            tab.setTextColor(selected ? Color.BLACK : Color.WHITE);
            tab.setOnClickListener(v -> {
                range = r;
                render();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i < values.length - 1) params.rightMargin = 8;
            row.addView(tab, params);
        }

        content.addView(row);
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

    private void addDailyChart(LinearLayout content, Typeface georgia, List<UsageStore.DayTotal> days) {
        long total = 0;
        boolean anyUsage = false;
        long[] values = new long[days.size()];
        java.util.List<String> labels = new java.util.ArrayList<>();
        for (int i = 0; i < days.size(); i++) {
            values[i] = days.get(i).millis;
            labels.add(days.get(i).label);
            total += values[i];
            if (values[i] > 0) anyUsage = true;
        }

        if (!anyUsage) {
            content.addView(emptyText(georgia, "(no usage)"));
            return;
        }

        ColumnChartView chart = new ColumnChartView(this, georgia);
        chart.setData(labels, values, StatsActivity::formatDuration);
        int height = (int) (220 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height);
        params.bottomMargin = 20;
        content.addView(chart, params);
        content.addView(totalText(georgia, total));
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

