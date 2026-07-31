package com.plainphone.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Plain per-day open counts and foreground time for flagged apps, kept in SharedPreferences. */
class UsageStore {

    private static final String PREFS = "usage";
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);

    static void recordOpen(Context context, String packageName) {
        String key = "opens_" + today() + "_" + packageName;
        SharedPreferences prefs = prefs(context);
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply();
    }

    static void addUsageMillis(Context context, String packageName, long millis) {
        if (millis <= 0) return;
        String key = "millis_" + today() + "_" + packageName;
        SharedPreferences prefs = prefs(context);
        prefs.edit().putLong(key, prefs.getLong(key, 0L) + millis).apply();
    }

    static String buildReport(Context context, Set<String> packages) {
        SharedPreferences prefs = prefs(context);
        StringBuilder sb = new StringBuilder();
        sb.append("Flagged apps\n\n");
        sb.append("Today\n");
        appendRange(sb, prefs, packages, 0, 0);
        sb.append("\nLast 7 days\n");
        appendRange(sb, prefs, packages, -6, 0);
        return sb.toString();
    }

    private static void appendRange(StringBuilder sb, SharedPreferences prefs,
            Set<String> packages, int startOffset, int endOffset) {
        Map<String, Long> millisTotals = new TreeMap<>();
        Map<String, Integer> openTotals = new TreeMap<>();

        for (int offset = startOffset; offset <= endOffset; offset++) {
            String day = dayOffset(offset);
            for (String pkg : packages) {
                long millis = prefs.getLong("millis_" + day + "_" + pkg, 0L);
                int opens = prefs.getInt("opens_" + day + "_" + pkg, 0);
                if (millis > 0) millisTotals.merge(pkg, millis, Long::sum);
                if (opens > 0) openTotals.merge(pkg, opens, Integer::sum);
            }
        }

        Set<String> involved = new TreeSet<>();
        involved.addAll(millisTotals.keySet());
        involved.addAll(openTotals.keySet());

        if (involved.isEmpty()) {
            sb.append("  (no usage)\n");
            return;
        }

        long totalMillis = 0;
        for (String pkg : involved) {
            long millis = millisTotals.getOrDefault(pkg, 0L);
            int opens = openTotals.getOrDefault(pkg, 0);
            totalMillis += millis;
            sb.append("  ").append(friendlyName(pkg)).append(": ")
                    .append(formatDuration(millis)).append(", ")
                    .append(opens).append(opens == 1 ? " open\n" : " opens\n");
        }
        sb.append("  Total: ").append(formatDuration(totalMillis)).append('\n');
    }

    private static String friendlyName(String pkg) {
        switch (pkg) {
            case "com.instagram.android": return "Instagram";
            case "com.zhiliaoapp.musically": return "TikTok";
            case "com.google.android.youtube": return "YouTube";
            case "com.twitter.android": return "Twitter";
            case "com.whatsapp": return "WhatsApp";
            default: return pkg;
        }
    }

    private static String formatDuration(long millis) {
        long totalMinutes = millis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? (hours + "h " + minutes + "m") : (minutes + "m");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String today() {
        return DAY_FORMAT.format(new Date());
    }

    private static String dayOffset(int offsetDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, offsetDays);
        return DAY_FORMAT.format(cal.getTime());
    }
}
