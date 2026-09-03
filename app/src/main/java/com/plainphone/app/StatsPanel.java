package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The screen-time panel: a range toggle, daily bar charts and per-app bars for
 * flagged apps and (with usage access) all apps. Hosted full-screen by
 * {@link StatsActivity} and inline by the home screen's Stats section.
 */
class StatsPanel {

    private static final int BAR_HEIGHT = 20;

    private static class Snapshot {
        List<UsageStore.DayTotal> flaggedDaily;
        List<UsageStore.Entry> flaggedApps;
        boolean hasUsageAccess;
        List<UsageStore.DayTotal> allDaily;
        List<AllAppsUsage.Entry> allApps;
    }

    private final Activity host;
    private final ScrollView scroll;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private int generation;
    private StatsActivity.Range range = StatsActivity.Range.WEEK;

    StatsPanel(Activity host) {
        this.host = host;
        this.scroll = new ScrollView(host);
        this.scroll.setBackgroundColor(Color.BLACK);
    }

    View view() {
        return scroll;
    }

    StatsActivity.Range range() {
        return range;
    }

    void setRange(StatsActivity.Range r) {
        range = r;
        render();
    }

    void shutdown() {
        io.shutdownNow();
    }

    private LinearLayout newContent() {
        LinearLayout content = new LinearLayout(host);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(48, 48, 48, 48);
        return content;
    }

    void render() {
        if (io.isShutdown() || host.isDestroyed()) return;   // host tore down mid-load
        final int gen = ++generation;
        final StatsActivity.Range r = range;
        final Typeface georgia = Fonts.current(host);

        LinearLayout shell = newContent();
        addToggle(shell, georgia, r);
        TextView loading = new TextView(host);
        loading.setText("Loading…");
        loading.setTextColor(Color.GRAY);
        loading.setTextSize(14);
        loading.setTypeface(georgia);
        LinearLayout.LayoutParams loadingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingParams.topMargin = 36;
        shell.addView(loading, loadingParams);
        scroll.removeAllViews();
        scroll.addView(shell);

        io.execute(() -> {
            final Snapshot snap = loadSnapshot(r);
            main.post(() -> {
                if (gen != generation || host.isFinishing()) return;
                LinearLayout content = newContent();
                addToggle(content, georgia, r);
                bind(content, georgia, r, snap);
                scroll.removeAllViews();
                scroll.addView(content);
            });
        });
    }

    private Snapshot loadSnapshot(StatsActivity.Range r) {
        Snapshot s = new Snapshot();
        java.util.Set<String> flagged = Config.getFlaggedPackages(host);
        boolean today = r == StatsActivity.Range.TODAY;

        s.hasUsageAccess = AllAppsUsage.hasUsageAccess(host);

        if (s.hasUsageAccess) {
            // Flagged apps read from the same system source as the All-apps section,
            // so the two agree. Opens still come from Plain's own tally.
            long start = StatsActivity.rangeStartMillis(r);
            long end = System.currentTimeMillis();
            java.util.Map<String, Long> sysMillis =
                    AllAppsUsage.foregroundMillis(host, flagged, start, end);
            java.util.Map<String, Integer> opens = new java.util.HashMap<>();
            for (UsageStore.Entry e : UsageStore.rangeUsage(host, flagged, r.startOffset, 0)) {
                if (e.opens > 0) opens.put(e.packageName, e.opens);
            }

            java.util.Set<String> pkgs = new java.util.TreeSet<>(sysMillis.keySet());
            pkgs.addAll(opens.keySet());
            List<UsageStore.Entry> merged = new ArrayList<>();
            for (String pkg : pkgs) {
                UsageStore.Entry e = new UsageStore.Entry();
                e.packageName = pkg;
                e.label = UsageStore.friendlyName(host, pkg);
                e.millis = sysMillis.getOrDefault(pkg, 0L);
                e.opens = opens.getOrDefault(pkg, 0);
                merged.add(e);
            }
            merged.sort((a, b) -> Long.compare(b.millis, a.millis));
            s.flaggedApps = merged;
            if (!today) {
                s.flaggedDaily = AllAppsUsage.dailyTotals(host, flagged,
                        r.startOffset, 0, r.dayLabelPattern);
            }

            if (!today) {
                s.allDaily = AllAppsUsage.dailyTotals(host, r.startOffset, 0, r.dayLabelPattern);
            }
            s.allApps = AllAppsUsage.topApps(host, 12, start, end);
        } else {
            // No usage access — fall back to Plain's own accessibility-based tally.
            if (!today) {
                s.flaggedDaily = UsageStore.dailyTotals(host, flagged,
                        r.startOffset, 0, r.dayLabelPattern);
            }
            s.flaggedApps = UsageStore.rangeUsage(host, flagged, r.startOffset, 0);
        }
        return s;
    }

    private void bind(LinearLayout content, Typeface georgia, StatsActivity.Range r, Snapshot snap) {
        String span = r.label;

        addSectionTitle(content, georgia, "Flagged apps — " + span, 32);
        if (snap.flaggedApps.isEmpty() && !hasDailyUsage(snap.flaggedDaily)) {
            content.addView(emptyText(georgia, "(no usage)"));
        } else {
            if (snap.flaggedDaily != null) {
                addDailyChart(content, georgia, snap.flaggedDaily);
            }
            addUsageSection(content, georgia, snap.flaggedApps);
        }

        addSectionTitle(content, georgia, "All apps — " + span, 48);
        if (!snap.hasUsageAccess) {
            TextView notice = new TextView(host);
            notice.setTextColor(Color.WHITE);
            notice.setTextSize(16);
            notice.setTypeface(georgia);
            notice.setPadding(0, 16, 0, 16);
            notice.setText("Needs \"Usage access\" permission, granted once in system settings.");
            content.addView(notice);

            Button grant = new Button(host);
            grant.setText("Grant usage access");
            UiKit.style(host, grant);
            grant.setOnClickListener(v ->
                    host.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
            content.addView(grant);
        } else {
            if (snap.allApps.isEmpty() && !hasDailyUsage(snap.allDaily)) {
                content.addView(emptyText(georgia, "(no usage data yet)"));
            } else {
                if (snap.allDaily != null) {
                    addDailyChart(content, georgia, snap.allDaily);
                }
                addAllAppsSection(content, georgia, snap.allApps);
            }
        }
    }

    private void addToggle(LinearLayout content, Typeface georgia, StatsActivity.Range current) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);

        StatsActivity.Range[] values = StatsActivity.Range.values();
        for (int i = 0; i < values.length; i++) {
            StatsActivity.Range r = values[i];
            boolean selected = r == current;

            TextView tab = new TextView(host);
            tab.setText(r.label);
            tab.setTypeface(georgia);
            tab.setTextSize(15);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(0, 20, 0, 20);
            tab.setBackgroundColor(selected ? Color.WHITE : Color.DKGRAY);
            tab.setTextColor(selected ? Color.BLACK : Color.WHITE);
            tab.setOnClickListener(v -> setRange(r));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i < values.length - 1) params.rightMargin = 8;
            row.addView(tab, params);
        }

        content.addView(row);
    }

    private void addSectionTitle(LinearLayout content, Typeface georgia, String text, int topMargin) {
        TextView title = new TextView(host);
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
            String caption = e.label + "  " + StatsActivity.formatDuration(e.millis) + ", "
                    + e.opens + (e.opens == 1 ? " open" : " opens");
            content.addView(buildBarRow(georgia, caption, e.millis, max));
        }
        content.addView(totalText(georgia, totalMillis));
    }

    private static boolean hasDailyUsage(List<UsageStore.DayTotal> days) {
        if (days == null) return false;
        for (UsageStore.DayTotal d : days) if (d.millis > 0) return true;
        return false;
    }

    private void addDailyChart(LinearLayout content, Typeface georgia, List<UsageStore.DayTotal> days) {
        long total = 0;
        boolean anyUsage = false;
        long[] values = new long[days.size()];
        List<String> labels = new ArrayList<>();
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

        ColumnChartView chart = new ColumnChartView(host, georgia);
        chart.setData(labels, values, StatsActivity::formatDuration);
        int height = (int) (220 * host.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height);
        params.bottomMargin = 20;
        content.addView(chart, params);
        content.addView(totalText(georgia, total));
    }

    private void addAllAppsSection(LinearLayout content, Typeface georgia,
                                   List<AllAppsUsage.Entry> entries) {
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
            String caption = e.label + "  " + StatsActivity.formatDuration(e.thisWeekMillis);
            content.addView(buildBarRow(georgia, caption, e.thisWeekMillis, max));
        }
        content.addView(totalText(georgia, totalMillis));
    }

    private View buildBarRow(Typeface georgia, String caption, long millis, long max) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = 20;
        row.setLayoutParams(rowParams);

        TextView label = new TextView(host);
        label.setTextColor(Color.WHITE);
        label.setTextSize(15);
        label.setTypeface(georgia);
        label.setText(caption);
        row.addView(label);

        LinearLayout track = new LinearLayout(host);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackgroundColor(Color.DKGRAY);
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, BAR_HEIGHT);
        trackParams.topMargin = 8;
        row.addView(track, trackParams);

        long clamped = Math.max(0, Math.min(millis, max));
        View fill = new View(host);
        fill.setBackgroundColor(Color.WHITE);
        track.addView(fill, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, clamped));

        long remainder = max - clamped;
        if (remainder > 0) {
            View spacer = new View(host);
            track.addView(spacer, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, remainder));
        }

        return row;
    }

    private TextView emptyText(Typeface georgia, String text) {
        TextView view = new TextView(host);
        view.setTextColor(Color.WHITE);
        view.setTextSize(15);
        view.setTypeface(georgia);
        view.setText(text);
        return view;
    }

    private TextView totalText(Typeface georgia, long totalMillis) {
        TextView view = new TextView(host);
        view.setTextColor(Color.WHITE);
        view.setTextSize(15);
        view.setTypeface(georgia);
        view.setText("Total: " + StatsActivity.formatDuration(totalMillis));
        return view;
    }
}
