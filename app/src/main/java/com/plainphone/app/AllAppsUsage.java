package com.plainphone.app;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class AllAppsUsage {

    static class Entry {
        String packageName;
        String label;
        long thisWeekMillis;
    }

    private static final Map<String, String> LABEL_CACHE = new HashMap<>();

    static String label(PackageManager pm, String pkg) {
        String cached = LABEL_CACHE.get(pkg);
        if (cached != null) return cached;
        String label;
        try {
            label = pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            label = pkg;
        }
        LABEL_CACHE.put(pkg, label);
        return label;
    }

    static boolean hasUsageAccess(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    static List<Entry> topApps(Context context, int topN, long startMillis, long endMillis) {
        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager pm = context.getPackageManager();

        Map<String, Long> totals = aggregate(usm, startMillis, endMillis);

        List<Entry> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : totals.entrySet()) {
            Entry entry = new Entry();
            entry.packageName = e.getKey();
            entry.thisWeekMillis = e.getValue();
            entry.label = label(pm, entry.packageName);
            list.add(entry);
        }

        Collections.sort(list, (a, b) -> Long.compare(b.thisWeekMillis, a.thisWeekMillis));
        if (list.size() > topN) {
            list = new ArrayList<>(list.subList(0, topN));
        }
        return list;
    }

    /** Total foreground time across all apps, one bucket per day, oldest first. */
    static List<UsageStore.DayTotal> dailyTotals(Context context, int startOffset, int endOffset,
                                                 String labelPattern) {
        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        SimpleDateFormat fmt = new SimpleDateFormat(labelPattern, Locale.US);
        long dayMs = 24L * 60 * 60 * 1000;
        int days = endOffset - startOffset + 1;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, startOffset);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long rangeStart = cal.getTimeInMillis();
        long rangeEnd = Math.min(rangeStart + days * dayMs, System.currentTimeMillis());

        long[] buckets = new long[days];
        // One query for the whole range, then bucket each daily record by its day.
        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, rangeStart, rangeEnd);
        if (stats != null) {
            for (UsageStats s : stats) {
                long fg = s.getTotalTimeInForeground();
                if (fg <= 0) continue;
                int idx = (int) ((s.getFirstTimeStamp() - rangeStart) / dayMs);
                if (idx >= 0 && idx < days) buckets[idx] += fg;
            }
        }

        List<UsageStore.DayTotal> out = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            UsageStore.DayTotal dt = new UsageStore.DayTotal();
            dt.label = fmt.format(new java.util.Date(rangeStart + i * dayMs));
            dt.millis = buckets[i];
            out.add(dt);
        }
        return out;
    }

    private static Map<String, Long> aggregate(UsageStatsManager usm, long start, long end) {
        Map<String, Long> totals = new HashMap<>();
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end);
        if (stats != null) {
            for (UsageStats s : stats) {
                if (s.getTotalTimeInForeground() > 0) {
                    totals.merge(s.getPackageName(), s.getTotalTimeInForeground(), Long::sum);
                }
            }
        }
        return totals;
    }
}

