package com.plainphone.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Supabase Management API calls for {@link SupabasePanel} and {@link DevAccountsActivity}. */
final class Supabase {

    private Supabase() {}

    static final String[] HOSTS = {"api.supabase.com"};
    private static final String API = "https://api.supabase.com";

    /** Look up a project name by ref/id for a token (used at add time). */
    static String projectName(String token, String ref) throws Exception {
        JSONArray arr = Http.getArray(API + "/v1/projects", token, HOSTS, null);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject p = arr.getJSONObject(i);
            if (ref.equals(p.optString("id")) || ref.equals(p.optString("ref"))) {
                return p.optString("name");
            }
        }
        return null;
    }

    static List<ServiceData.Section> sections(Context ctx, DevAccount a, String token) throws Exception {
        String ref = a.base;
        List<ServiceData.Section> out = new ArrayList<>();

        ServiceData.Section health = new ServiceData.Section("Health");
        try {
            JSONObject p = Http.getObject(API + "/v1/projects/" + ref, token, HOSTS, null);
            String status = p.optString("status");
            health.add("Status", status, "ACTIVE_HEALTHY".equals(status), null);
            health.add("Region", p.optString("region"));
            long created = millis(p.optString("created_at"));
            if (created > 0) health.add("Created", Fmt.age(created));
        } catch (Exception e) {
            health.add("Status", "unavailable", Boolean.FALSE, null);
        }
        out.add(health);

        // SQL-backed sections — each best-effort, hidden on failure or scope error.
        ServiceData.Section db = new ServiceData.Section("Database");
        sqlPairs(db, token, ref,
                "select 'Size' as k, pg_size_pretty(pg_database_size(current_database())) as v"
                        + " union all select 'Connections',"
                        + " (select count(*)::text from pg_stat_activity) || ' / ' ||"
                        + " (select setting from pg_settings where name = 'max_connections')");
        if (!db.rows.isEmpty()) out.add(db);

        ServiceData.Section tables = new ServiceData.Section("Tables");
        sqlRows(tables, token, ref,
                "select relname as name, n_live_tup as n from pg_stat_user_tables"
                        + " order by n_live_tup desc limit 20", "name", "n", " rows");
        if (!tables.rows.isEmpty()) out.add(tables);

        ServiceData.Section buckets = new ServiceData.Section("Storage buckets");
        sqlRows(buckets, token, ref,
                "select name, case when public then 'public' else 'private' end as vis"
                        + " from storage.buckets order by name", "name", "vis", "");
        if (!buckets.rows.isEmpty()) out.add(buckets);

        try {
            JSONArray fns = Http.getArray(API + "/v1/projects/" + ref + "/functions", token, HOSTS, null);
            if (fns.length() > 0) {
                ServiceData.Section s = new ServiceData.Section("Edge functions");
                for (int i = 0; i < fns.length(); i++) {
                    JSONObject f = fns.getJSONObject(i);
                    s.add(f.optString("slug", f.optString("name")), f.optString("status", ""));
                }
                out.add(s);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static JSONArray runSql(String token, String ref, String sql) throws Exception {
        JSONObject body = new JSONObject();
        body.put("query", sql);
        return Http.postArray(API + "/v1/projects/" + ref + "/database/query", token, HOSTS, body, null);
    }

    private static void sqlPairs(ServiceData.Section sec, String token, String ref, String sql) {
        try {
            JSONArray rows = runSql(token, ref, sql);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject o = rows.getJSONObject(i);
                sec.add(o.optString("k"), o.optString("v"));
            }
        } catch (Exception ignored) {
        }
    }

    private static void sqlRows(ServiceData.Section sec, String token, String ref, String sql,
                                String keyCol, String valCol, String suffix) {
        try {
            JSONArray rows = runSql(token, ref, sql);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject o = rows.getJSONObject(i);
                sec.add(o.optString(keyCol), o.optString(valCol) + suffix);
            }
        } catch (Exception ignored) {
        }
    }

    private static long millis(String iso) {
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }
}
