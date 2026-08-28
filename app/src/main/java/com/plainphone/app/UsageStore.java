package com.plainphone.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

class UsageStore {

    private static final String PREFS = "usage";
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);

    static class Entry {
        String label;
        long millis;
        int opens;
    }

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

    static List<Entry> rangeUsage(Context context, Set<String> packages, int startOffset, int endOffset) {
        SharedPreferences prefs = prefs(context);
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

        List<Entry> entries = new ArrayList<>();
        for (String pkg : involved) {
            Entry entry = new Entry();
            entry.label = friendlyName(pkg);
            entry.millis = millisTotals.getOrDefault(pkg, 0L);
            entry.opens = openTotals.getOrDefault(pkg, 0);
            entries.add(entry);
        }
        Collections.sort(entries, (a, b) -> Long.compare(b.millis, a.millis));
        return entries;
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

