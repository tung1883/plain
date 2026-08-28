package com.plainphone.app;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AllAppsUsage {

    static class Entry {
        String packageName;
        String label;
        long thisWeekMillis;
    }

    static boolean hasUsageAccess(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    static List<Entry> weeklyComparison(Context context, int topN) {
        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager pm = context.getPackageManager();

        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        long weekAgo = cal.getTimeInMillis();

        Map<String, Long> thisWeek = aggregate(usm, weekAgo, now);

        List<Entry> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : thisWeek.entrySet()) {
            Entry entry = new Entry();
            entry.packageName = e.getKey();
            entry.thisWeekMillis = e.getValue();
            try {
                entry.label = pm.getApplicationInfo(entry.packageName, 0).loadLabel(pm).toString();
            } catch (PackageManager.NameNotFoundException ex) {
                entry.label = entry.packageName;
            }
            list.add(entry);
        }

        Collections.sort(list, (a, b) -> Long.compare(b.thisWeekMillis, a.thisWeekMillis));
        if (list.size() > topN) {
            list = list.subList(0, topN);
        }
        return list;
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

