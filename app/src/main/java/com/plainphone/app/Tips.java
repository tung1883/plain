package com.plainphone.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * The home-screen "Tip of the day" / "Quote of the day" line. Two plain-text
 * pools (one item per line), each toggleable, each editable and resettable. The
 * strip walks a single merged sequence — enabled tips, then enabled quotes —
 * advancing on every return home and on tap.
 */
class Tips {

    private Tips() {}

    enum Kind { TIP, QUOTE }

    static class Entry {
        final Kind kind;
        final String text;

        Entry(Kind kind, String text) {
            this.kind = kind;
            this.text = text;
        }

        String kicker() {
            return kind == Kind.QUOTE ? "QUOTE OF THE DAY" : "TIP OF THE DAY";
        }
    }

    static final String[] DEFAULT_TIPS = {
            "Turn off recommended apps in your recent-apps screen.",
            "Search files on your phone right from here.",
            "Create a note straight from the search bar.",
            "Long-press a home tab to reorder the sections.",
            "A locked app re-locks the moment you leave it.",
            "Flag an app to add a wait screen before it opens.",
            "Swipe the home list left and right to switch sections.",
    };

    static final String[] DEFAULT_QUOTES = {
            "Attention compounds. Protect the first hour.",
            "Do the hard thing while it is still small.",
            "A quiet phone is a clearer mind.",
            "Boredom is the doorway to your next good idea.",
            "What you give your attention to grows.",
            "The task you keep avoiding is the one that matters.",
    };

    static String defaultTipsText() {
        return String.join("\n", DEFAULT_TIPS);
    }

    static String defaultQuotesText() {
        return String.join("\n", DEFAULT_QUOTES);
    }

    static List<String> tips(Context context) {
        return lines(Config.getTipsText(context), defaultTipsText());
    }

    static List<String> quotes(Context context) {
        return lines(Config.getQuotesText(context), defaultQuotesText());
    }

    /** Merged, ordered pool of whatever is currently enabled. */
    static List<Entry> pool(Context context) {
        List<Entry> out = new ArrayList<>();
        if (Config.isTipsEnabled(context)) {
            for (String t : tips(context)) out.add(new Entry(Kind.TIP, t));
        }
        if (Config.isQuotesEnabled(context)) {
            for (String q : quotes(context)) out.add(new Entry(Kind.QUOTE, q));
        }
        return out;
    }

    /** The item to show now, or null when nothing is enabled / both lists are empty. */
    static Entry current(Context context) {
        List<Entry> pool = pool(context);
        if (pool.isEmpty()) return null;
        return pool.get(Math.floorMod(Config.getTipIndex(context), pool.size()));
    }

    static void advance(Context context) {
        Config.setTipIndex(context, Config.getTipIndex(context) + 1);
        Config.setTipLastAdvance(context, System.currentTimeMillis());
    }

    /**
     * Roll the strip forward by however many rotation intervals have elapsed since
     * the last advance. Returns true if the shown entry changed. No-op when
     * rotation is off ({@code tip_rotate_minutes == 0}).
     */
    static boolean maybeAutoAdvance(Context context) {
        int minutes = Config.getTipRotateMinutes(context);
        if (minutes <= 0) return false;
        long now = System.currentTimeMillis();
        long last = Config.getTipLastAdvance(context);
        if (last <= 0L || last > now) {
            Config.setTipLastAdvance(context, now);
            return false;
        }
        long intervalMs = minutes * 60_000L;
        long elapsed = now - last;
        if (elapsed < intervalMs) return false;
        int steps = (int) Math.min(elapsed / intervalMs, 10_000);
        Config.setTipIndex(context, Config.getTipIndex(context) + steps);
        Config.setTipLastAdvance(context, last + steps * intervalMs);
        return true;
    }

    private static List<String> lines(String stored, String fallback) {
        String text = stored == null || stored.trim().isEmpty() ? fallback : stored;
        List<String> out = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }
}
