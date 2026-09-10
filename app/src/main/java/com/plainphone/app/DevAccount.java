package com.plainphone.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One connected dev service (GitHub / Vercel / Supabase). Mirrors
 * {@link DevHost}: everything here is plain config (a JSON array in
 * {@link Config#getDevAccountsJson}); the access token is a secret kept in
 * {@link SecretStore} keyed {@code "dev_acct_" + id} and fetched only for a request.
 */
final class DevAccount {

    static final String GITHUB = "github";
    static final String VERCEL = "vercel";
    static final String SUPABASE = "supabase";

    final String id;
    final String kind;
    String label;      // resolved handle / username / project name
    String base;       // optional qualifier: Vercel "team=<id>", Supabase project ref
    final List<String> watch = new ArrayList<>();  // repo full-names / project ids

    DevAccount(String id, String kind, String label, String base) {
        this.id = id;
        this.kind = kind;
        this.label = label == null ? "" : label;
        this.base = base == null ? "" : base;
    }

    static String newId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString((int) (Math.random() * 1296), 36);
    }

    String displayKind() {
        switch (kind) {
            case GITHUB: return "GitHub";
            case VERCEL: return "Vercel";
            case SUPABASE: return "Supabase";
            default: return kind;
        }
    }

    boolean isWatched(String key) {
        return watch.contains(key);
    }

    void toggleWatch(String key) {
        if (!watch.remove(key)) watch.add(key);
    }

    /** The Vercel team id parsed out of {@link #base}, or null. */
    String teamId() {
        if (base != null && base.startsWith("team=")) return base.substring(5);
        return null;
    }

    // --- persistence -------------------------------------------------------

    static List<DevAccount> all(Context context) {
        List<DevAccount> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(Config.getDevAccountsJson(context));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                DevAccount a = new DevAccount(o.getString("id"), o.getString("kind"),
                        o.optString("label", ""), o.optString("base", ""));
                JSONArray w = o.optJSONArray("watch");
                if (w != null) {
                    for (int j = 0; j < w.length(); j++) a.watch.add(w.getString(j));
                }
                out.add(a);
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    static List<DevAccount> ofKind(Context context, String kind) {
        List<DevAccount> out = new ArrayList<>();
        for (DevAccount a : all(context)) if (a.kind.equals(kind)) out.add(a);
        return out;
    }

    static DevAccount find(Context context, String id) {
        if (id == null) return null;
        for (DevAccount a : all(context)) if (a.id.equals(id)) return a;
        return null;
    }

    /** Add or replace this account; persist the token when non-null. */
    void save(Context context, String token) {
        List<DevAccount> list = all(context);
        boolean replaced = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) { list.set(i, this); replaced = true; break; }
        }
        if (!replaced) list.add(this);
        writeAll(context, list);
        if (token != null) SecretStore.put(context, tokenKey(), token);
    }

    static void remove(Context context, String id) {
        List<DevAccount> list = all(context);
        list.removeIf(a -> a.id.equals(id));
        writeAll(context, list);
        SecretStore.remove(context, "dev_acct_" + id);
    }

    String token(Context context) {
        return SecretStore.get(context, tokenKey());
    }

    private String tokenKey() {
        return "dev_acct_" + id;
    }

    private static void writeAll(Context context, List<DevAccount> list) {
        JSONArray arr = new JSONArray();
        try {
            for (DevAccount a : list) {
                JSONObject o = new JSONObject();
                o.put("id", a.id);
                o.put("kind", a.kind);
                o.put("label", a.label);
                o.put("base", a.base);
                o.put("watch", new JSONArray(a.watch));
                arr.put(o);
            }
        } catch (JSONException ignored) {
        }
        Config.setDevAccountsJson(context, arr.toString());
    }
}
