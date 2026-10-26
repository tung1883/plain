package com.plainphone.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * A native PDF reader can bring the whole app down on a damaged file. Every PDF being read is written
 * down first and struck off when done; anything still on the list at the start of the next run is a
 * file that was cut short. One interrupted run can be a plain kill (an update, a force stop), so a file is
 * only skipped from then on when it is found on the list in two runs in a row.
 */
final class PdfCrashGuard {

    private static final String PREFS = "pdf_crash_guard";
    private static final String KEY = "reading";
    private static final String STRUCK = "struck";
    private static final Object LOCK = new Object();

    private PdfCrashGuard() {}

    static void enter(Context context, String path) {
        edit(context, path, true);
    }

    static void leave(Context context, String path) {
        edit(context, path, false);
    }

    /** The files that were being read when the app died in two runs in a row; the list is cleared. */
    static Set<String> takeCrashed(Context context) {
        synchronized (LOCK) {
            SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Set<String> left = new HashSet<>(p.getStringSet(KEY, new HashSet<String>()));
            Set<String> struck = new HashSet<>(p.getStringSet(STRUCK, new HashSet<String>()));
            Set<String> crashed = new HashSet<>(left);
            crashed.retainAll(struck);
            struck.addAll(left);
            struck.removeAll(crashed);
            p.edit().remove(KEY).putStringSet(STRUCK, struck).commit();
            return crashed;
        }
    }

    private static void edit(Context context, String path, boolean add) {
        synchronized (LOCK) {
            SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (!add) {
                Set<String> struck = new HashSet<>(p.getStringSet(STRUCK, new HashSet<String>()));
                if (struck.remove(path)) p.edit().putStringSet(STRUCK, struck).commit();
            }
            Set<String> now = new HashSet<>(p.getStringSet(KEY, new HashSet<String>()));
            // commit, not apply: the entry must be on disk before a crash can happen
            if (add ? now.add(path) : now.remove(path)) p.edit().putStringSet(KEY, now).commit();
        }
    }
}
