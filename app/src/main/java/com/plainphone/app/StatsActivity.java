package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class StatsActivity extends Activity {

    private static final int BAR_MAX_CHARS = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Typeface georgia = Fonts.georgia(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(48, 48, 48, 48);

        TextView flaggedReport = new TextView(this);
        flaggedReport.setTextColor(Color.WHITE);
        flaggedReport.setTextSize(18);
        flaggedReport.setTypeface(georgia);
        flaggedReport.setText(UsageStore.buildReport(this, Config.getFlaggedPackages(this)));
        content.addView(flaggedReport);

        TextView allAppsTitle = new TextView(this);
        allAppsTitle.setTextColor(Color.WHITE);
        allAppsTitle.setTextSize(18);
        allAppsTitle.setTypeface(georgia);
        allAppsTitle.setPadding(0, 48, 0, 0);
        allAppsTitle.setText("All apps: this week vs last week");
        content.addView(allAppsTitle);

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
            TextView allAppsReport = new TextView(this);
            allAppsReport.setTextColor(Color.WHITE);
            allAppsReport.setTextSize(16);
            allAppsReport.setTypeface(georgia);
            allAppsReport.setPadding(0, 16, 0, 0);
            allAppsReport.setText(buildAllAppsReport());
            content.addView(allAppsReport);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.addView(content);
        setContentView(scroll);
    }

    private String buildAllAppsReport() {
        List<AllAppsUsage.Entry> entries = AllAppsUsage.weeklyComparison(this, 12);
        if (entries.isEmpty()) {
            return "  (no usage data yet)";
        }

        long max = 1;
        for (AllAppsUsage.Entry e : entries) {
            max = Math.max(max, e.thisWeekMillis);
        }

        StringBuilder sb = new StringBuilder();
        for (AllAppsUsage.Entry e : entries) {
            int barLength = (int) (e.thisWeekMillis * BAR_MAX_CHARS / max);
            String bar = repeat('█', barLength);
            sb.append(e.label).append('\n')
                    .append("  ").append(bar).append(' ').append(formatDuration(e.thisWeekMillis))
                    .append(" (last week: ").append(formatDuration(e.lastWeekMillis)).append(")\n");
        }
        return sb.toString();
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(c);
        return sb.toString();
    }

    private static String formatDuration(long millis) {
        long totalMinutes = millis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? (hours + "h " + minutes + "m") : (minutes + "m");
    }
}
