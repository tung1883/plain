package com.plainphone.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** The keyboard's clipboard history: the last copies, newest first, plus pinned ones. Local only. */
final class ClipboardHistory {

    static final int MAX_UNPINNED = 20;
    private static final String KEY = "clips";
    private static final int MAX_LEN = 5000;

    static final class Item {
        final String text;
        boolean pinned;

        Item(String text, boolean pinned) {
            this.text = text;
            this.pinned = pinned;
        }
    }

    private ClipboardHistory() {}

    /** Pinned first, then newest first. */
    static List<Item> load(Context c) {
        List<Item> all = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(PanelKit.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                all.add(new Item(o.getString("t"), o.optBoolean("p")));
            }
        } catch (JSONException ignored) {
        }
        List<Item> out = new ArrayList<>();
        for (Item i : all) if (i.pinned) out.add(i);
        for (Item i : all) if (!i.pinned) out.add(i);
        return out;
    }

    private static void save(Context c, List<Item> items) {
        JSONArray a = new JSONArray();
        try {
            for (Item i : items) a.put(new JSONObject().put("t", i.text).put("p", i.pinned));
        } catch (JSONException ignored) {
        }
        PanelKit.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    /** Record a copy: moves it to the front if already there, keeps its pin. */
    static void add(Context c, CharSequence raw) {
        if (raw == null) return;
        String text = raw.toString();
        if (text.trim().isEmpty()) return;
        if (text.length() > MAX_LEN) text = text.substring(0, MAX_LEN);
        List<Item> items = load(c);
        boolean pinned = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).text.equals(text)) {
                pinned = items.get(i).pinned;
                items.remove(i);
                break;
            }
        }
        items.add(0, new Item(text, pinned));
        int unpinned = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).pinned) continue;
            if (++unpinned > MAX_UNPINNED) items.remove(i--);
        }
        save(c, items);
    }

    static void togglePin(Context c, String text) {
        List<Item> items = load(c);
        for (Item i : items) if (i.text.equals(text)) i.pinned = !i.pinned;
        save(c, items);
    }

    static void clearUnpinned(Context c) {
        List<Item> items = load(c);
        items.removeIf(i -> !i.pinned);
        save(c, items);
    }
}
