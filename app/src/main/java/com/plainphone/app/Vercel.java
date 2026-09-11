package com.plainphone.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Vercel REST calls for {@link VercelPanel} and {@link DevAccountsActivity}. */
final class Vercel {

    private Vercel() {}

    static final String[] HOSTS = {"api.vercel.com"};
    private static final String API = "https://api.vercel.com";

    private static String team(DevAccount a) {
        return a.teamId() != null ? "&teamId=" + a.teamId() : "";
    }

    /** Projects to offer in the WATCHING list: {@code {id, name}}. */
    static List<String[]> watchCandidates(Context ctx, DevAccount a, String token) throws Exception {
        List<String[]> out = new ArrayList<>();
        JSONObject o = Http.getObject(API + "/v9/projects?limit=100" + team(a), token, HOSTS, null);
        JSONArray arr = o.optJSONArray("projects");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject p = arr.getJSONObject(i);
                out.add(new String[]{p.optString("id"), p.optString("name")});
            }
        }
        return out;
    }

    static List<ServiceData.Section> sections(Context ctx, DevAccount a, String token) throws Exception {
        List<ServiceData.Section> out = new ArrayList<>();

        // Project index (id + name -> project object) — also used to prettify deploys.
        Map<String, JSONObject> byId = new HashMap<>();
        try {
            JSONObject o = Http.getObject(API + "/v9/projects?limit=100" + team(a), token, HOSTS, null);
            JSONArray arr = o.optJSONArray("projects");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject p = arr.getJSONObject(i);
                    byId.put(p.optString("id"), p);
                }
            }
        } catch (Exception ignored) {
        }

        // --- Watched projects: framework + production state ------------------
        if (!a.watch.isEmpty()) {
            ServiceData.Section projects = new ServiceData.Section("Projects");
            for (String id : a.watch) {
                JSONObject p = byId.get(id);
                if (p == null) {
                    String tq = team(a).isEmpty() ? "" : "?" + team(a).substring(1);
                    try {
                        p = Http.getObject(API + "/v9/projects/" + Http.q(id) + tq, token, HOSTS, null);
                    } catch (Exception e) {
                        projects.add(id, "unavailable", Boolean.FALSE, null);
                        continue;
                    }
                }
                String name = p.optString("name", id);
                String framework = p.optString("framework", "");
                if (framework == null || framework.isEmpty() || "null".equals(framework)) framework = "—";
                JSONObject prod = null;
                JSONObject targets = p.optJSONObject("targets");
                if (targets != null) prod = targets.optJSONObject("production");
                String state = prod == null ? "" : prod.optString("readyState", prod.optString("state", ""));
                long created = prod == null ? 0 : prod.optLong("createdAt", prod.optLong("created", 0));
                Boolean ok = state.isEmpty() ? null : ("READY".equals(state) ? Boolean.TRUE
                        : ("ERROR".equals(state) || "CANCELED".equals(state) ? Boolean.FALSE : null));
                StringBuilder sub = new StringBuilder(framework);
                if (!state.isEmpty()) sub.append(" · prod ").append(state);
                if (created > 0) sub.append(" · ").append(Fmt.age(created));
                projects.add(name, sub.toString(), ok, "https://" + name + ".vercel.app");
            }
            if (!projects.rows.isEmpty()) out.add(projects);

            // --- Domains per watched project -------------------------------
            ServiceData.Section domains = new ServiceData.Section("Domains");
            for (String id : a.watch) {
                String name = byId.containsKey(id) ? byId.get(id).optString("name", id) : id;
                try {
                    JSONObject o = Http.getObject(
                            API + "/v9/projects/" + Http.q(id) + "/domains?limit=20" + team(a),
                            token, HOSTS, null);
                    JSONArray arr = o.optJSONArray("domains");
                    if (arr == null) continue;
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject d = arr.getJSONObject(i);
                        boolean verified = d.optBoolean("verified", true);
                        domains.add(d.optString("name"),
                                name + " · " + (verified ? "verified" : "unverified"),
                                verified ? Boolean.TRUE : Boolean.FALSE,
                                "https://" + d.optString("name"));
                    }
                } catch (Exception ignored) {
                }
            }
            if (!domains.rows.isEmpty()) out.add(domains);
        }

        // --- Web Analytics traffic (production, last 30d) -------------------
        // Only token-scoped endpoint that reports request volume; needs Web
        // Analytics enabled on the project (else 4xx). Bandwidth / edge-request
        // / function-invocation / storage-size counters are Observability Plus
        // only and not wired. Uses watched projects, else the whole list.
        ServiceData.Section traffic = new ServiceData.Section("Traffic · 30d");
        long since = System.currentTimeMillis() - 30L * 24 * 3600 * 1000;
        List<String> ids = new ArrayList<>(a.watch.isEmpty() ? byId.keySet() : a.watch);
        int errs = 0;
        for (int i = 0; i < ids.size() && i < 12; i++) {
            String id = ids.get(i);
            String name = byId.containsKey(id) ? byId.get(id).optString("name", id) : id;
            try {
                JSONObject o = Http.getObject(
                        API + "/v1/query/web-analytics/visits/count?projectId=" + Http.q(id)
                                + "&since=" + since + team(a),
                        token, HOSTS, null);
                JSONObject d = o.optJSONObject("data");
                if (d == null) continue;
                long views = d.optLong("pageviews", 0);
                long visitors = d.optLong("visitors", 0);
                traffic.add(name, compact(views) + " views · " + compact(visitors) + " visitors");
            } catch (Exception e) {
                errs++;
            }
        }
        if (traffic.rows.isEmpty() && !ids.isEmpty()) {
            traffic.add(errs > 0 ? "Web Analytics not enabled" : "No traffic data",
                    errs > 0 ? "enable it per-project on vercel.com" : "last 30 days", Boolean.FALSE, null);
        }
        if (!traffic.rows.isEmpty()) out.add(traffic);

        // --- Recent deployments + state tally -------------------------------
        ServiceData.Section deploys = new ServiceData.Section("Recent deployments");
        Map<String, Integer> tally = new LinkedHashMap<>();
        long newest = 0;
        JSONObject o = Http.getObject(API + "/v6/deployments?limit=20" + team(a), token, HOSTS, null);
        JSONArray arr = o.optJSONArray("deployments");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject d = arr.getJSONObject(i);
                String state = d.optString("state", d.optString("readyState", "")); // READY/ERROR/BUILDING/…
                String project = d.optString("name");
                String branch = branchOf(d);
                long when = d.optLong("created", d.optLong("createdAt", 0));
                if (when > newest) newest = when;
                if (!state.isEmpty()) {
                    Integer n = tally.get(state);
                    tally.put(state, n == null ? 1 : n + 1);
                }
                Boolean ok = state.isEmpty() ? null : ("READY".equals(state) ? Boolean.TRUE
                        : ("ERROR".equals(state) || "CANCELED".equals(state) ? Boolean.FALSE : null));
                String url = d.optString("inspectorUrl", null);
                if (url == null || url.isEmpty()) {
                    String host = d.optString("url");
                    url = host.isEmpty() ? null : "https://" + host;
                }
                deploys.add(project,
                        state + (branch.isEmpty() ? "" : " · " + branch) + " · " + Fmt.age(when),
                        ok, url);
            }
        }

        // Summary goes first: counts by state over the pulled window + last deploy.
        if (!tally.isEmpty()) {
            ServiceData.Section summary = new ServiceData.Section("Summary");
            for (Map.Entry<String, Integer> e : tally.entrySet()) {
                summary.add(e.getKey(), e.getValue() + " of " + arr.length());
            }
            if (newest > 0) summary.add("Last deploy", Fmt.age(newest));
            out.add(summary);
        }
        out.add(deploys);
        return out;
    }

    /** 1234 -> "1.2k", 1200000 -> "1.2M". */
    private static String compact(long n) {
        if (n < 1000) return Long.toString(n);
        if (n < 1_000_000) return String.format("%.1fk", n / 1000.0);
        return String.format("%.1fM", n / 1_000_000.0);
    }

    private static String branchOf(JSONObject d) {
        JSONObject meta = d.optJSONObject("meta");
        if (meta != null) {
            String b = meta.optString("githubCommitRef", meta.optString("gitBranch", ""));
            String sha = meta.optString("githubCommitSha", "");
            if (!b.isEmpty()) return sha.length() >= 7 ? b + "@" + sha.substring(0, 7) : b;
        }
        return "";
    }
}
