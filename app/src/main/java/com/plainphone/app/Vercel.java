package com.plainphone.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
        ServiceData.Section deploys = new ServiceData.Section("Recent deployments");
        JSONObject o = Http.getObject(API + "/v6/deployments?limit=20" + team(a), token, HOSTS, null);
        JSONArray arr = o.optJSONArray("deployments");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject d = arr.getJSONObject(i);
                String state = d.optString("state", d.optString("readyState", "")); // READY/ERROR/BUILDING/…
                String project = d.optString("name");
                String branch = branchOf(d);
                Boolean ok = state.isEmpty() ? null : ("READY".equals(state) ? Boolean.TRUE
                        : ("ERROR".equals(state) || "CANCELED".equals(state) ? Boolean.FALSE : null));
                String url = d.optString("inspectorUrl", null);
                if (url == null || url.isEmpty()) {
                    String host = d.optString("url");
                    url = host.isEmpty() ? null : "https://" + host;
                }
                deploys.add(project,
                        state + (branch.isEmpty() ? "" : " · " + branch) + " · "
                                + Fmt.age(d.optLong("created", d.optLong("createdAt", 0))),
                        ok, url);
            }
        }
        out.add(deploys);
        return out;
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
