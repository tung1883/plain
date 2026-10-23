package com.plainphone.app;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the user picked from search, for which typing: results they pick often for a query (and
 * recently) rank a little higher next time. Pure Java; {@link PickStore} keeps it on disk.
 */
final class PickHistory {

    interface Clock {
        long now();
    }

    private static final class Entry {
        int count;
        long last;
        final Map<String, Integer> byPrefix = new LinkedHashMap<>();
    }

    static final int MAX_ENTRIES = 500;
    private static final int PREFIX_LEN = 6;
    private static final double HALF_LIFE_DAYS = 14.0;
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private final Map<String, Entry> entries = new HashMap<>();
    private final Clock clock;

    PickHistory(Clock clock) {
        this.clock = clock;
    }

    int size() {
        return entries.size();
    }

    /** The user picked the result {@code key} while the folded query was {@code foldedQuery}. */
    void record(String key, String foldedQuery) {
        if (key == null) return;
        Entry e = entries.get(key);
        if (e == null) {
            e = new Entry();
            entries.put(key, e);
        }
        e.count++;
        e.last = clock.now();
        String p = prefixOf(foldedQuery);
        if (!p.isEmpty()) {
            Integer n = e.byPrefix.get(p);
            e.byPrefix.put(p, n == null ? 1 : n + 1);
            while (e.byPrefix.size() > 8) {
                e.byPrefix.remove(e.byPrefix.keySet().iterator().next());
            }
        }
        prune();
    }

    void clear() {
        entries.clear();
    }

    /**
     * How much to favour {@code key} for this query: 0 when never picked, growing slowly with picks
     * (those for a matching start of the query count most) and fading over weeks.
     */
    double boost(String key, String foldedQuery) {
        Entry e = key == null ? null : entries.get(key);
        if (e == null) return 0;
        int forQuery = 0;
        for (Map.Entry<String, Integer> p : e.byPrefix.entrySet()) {
            if (!foldedQuery.isEmpty() && (foldedQuery.startsWith(p.getKey()) || p.getKey().startsWith(foldedQuery))) {
                forQuery += p.getValue();
            }
        }
        double ageDays = Math.max(0, clock.now() - e.last) / (double) DAY_MS;
        double recency = 0.5 + 0.5 * Math.pow(0.5, ageDays / HALF_LIFE_DAYS);
        return log2(1 + 1.5 * forQuery + 0.5 * e.count) * recency;
    }

    /**
     * Lower {@code score} (lower is better) by the boost without letting a result change tier:
     * name matches (under 100) move at most 2 places, fuzzy ones (100 and over) at most 40 and stay
     * below every name match.
     */
    static int adjust(int score, double boost) {
        if (boost <= 0) return score;
        if (score < 100) return Math.max(0, score - Math.min(2, (int) boost));
        return Math.max(60, score - Math.min(40, (int) (boost * 10)));
    }

    private static String prefixOf(String foldedQuery) {
        if (foldedQuery == null) return "";
        String q = foldedQuery.trim();
        return q.length() > PREFIX_LEN ? q.substring(0, PREFIX_LEN) : q;
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    private void prune() {
        if (entries.size() <= MAX_ENTRIES) return;
        List<Map.Entry<String, Entry>> all = new ArrayList<>(entries.entrySet());
        Collections.sort(all, new Comparator<Map.Entry<String, Entry>>() {
            @Override
            public int compare(Map.Entry<String, Entry> a, Map.Entry<String, Entry> b) {
                return Long.compare(a.getValue().last, b.getValue().last);
            }
        });
        for (int i = 0; i < all.size() - MAX_ENTRIES; i++) entries.remove(all.get(i).getKey());
    }

    // ------------------------------------------------------------------- persistence

    String toJson() {
        try {
            JSONObject root = new JSONObject();
            JSONObject all = new JSONObject();
            for (Map.Entry<String, Entry> e : entries.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("n", e.getValue().count);
                o.put("t", e.getValue().last);
                JSONObject p = new JSONObject();
                for (Map.Entry<String, Integer> q : e.getValue().byPrefix.entrySet()) p.put(q.getKey(), q.getValue());
                o.put("p", p);
                all.put(e.getKey(), o);
            }
            root.put("v", 1);
            root.put("e", all);
            return root.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    /** Read what {@link #toJson} wrote; anything unreadable gives an empty history. */
    static PickHistory fromJson(String json, Clock clock) {
        PickHistory h = new PickHistory(clock);
        if (json == null || json.isEmpty()) return h;
        try {
            JSONObject all = new JSONObject(json).optJSONObject("e");
            if (all == null) return h;
            Iterator<String> keys = all.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject o = all.getJSONObject(key);
                Entry e = new Entry();
                e.count = o.optInt("n", 0);
                e.last = o.optLong("t", 0);
                JSONObject p = o.optJSONObject("p");
                if (p != null) {
                    Iterator<String> pk = p.keys();
                    while (pk.hasNext()) {
                        String prefix = pk.next();
                        e.byPrefix.put(prefix, p.optInt(prefix, 0));
                    }
                }
                if (e.count > 0) h.entries.put(key, e);
            }
        } catch (JSONException ignored) {
            return new PickHistory(clock);
        }
        return h;
    }
}
