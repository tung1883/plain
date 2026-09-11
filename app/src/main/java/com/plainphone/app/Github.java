package com.plainphone.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** GitHub REST calls for {@link GithubPanel} and {@link DevAccountsActivity}. */
final class Github {

    private Github() {}

    static final String[] HOSTS = {"api.github.com"};
    private static final String API = "https://api.github.com";

    static Map<String, String> headers() {
        Map<String, String> h = new HashMap<>();
        h.put("Accept", "application/vnd.github+json");
        h.put("X-GitHub-Api-Version", "2022-11-28");
        return h;
    }

    /** Repos to offer in the WATCHING list: {@code {full_name, full_name}}. */
    static List<String[]> watchCandidates(Context ctx, DevAccount a, String token) throws Exception {
        List<String[]> out = new ArrayList<>();
        JSONArray arr = Http.getArray(API + "/user/repos?per_page=100&sort=pushed",
                token, HOSTS, headers());
        for (int i = 0; i < arr.length(); i++) {
            String full = arr.getJSONObject(i).optString("full_name");
            if (!full.isEmpty()) out.add(new String[]{full, full});
        }
        return out;
    }

    static List<ServiceData.Section> sections(Context ctx, DevAccount a, String token) throws Exception {
        String login = a.label;
        List<ServiceData.Section> out = new ArrayList<>();

        out.add(searchSection("My open PRs",
                "is:open is:pr author:" + login, token));
        out.add(searchSection("Review requested",
                "is:open is:pr review-requested:" + login, token));
        out.add(searchSection("Assigned issues",
                "is:open is:issue assignee:" + login, token));

        if (!a.watch.isEmpty()) {
            ServiceData.Section actions = new ServiceData.Section("Actions");
            for (String repo : a.watch) {
                try {
                    JSONObject o = Http.getObject(
                            API + "/repos/" + repo + "/actions/runs?per_page=1",
                            token, HOSTS, headers());
                    JSONArray runs = o.optJSONArray("workflow_runs");
                    if (runs == null || runs.length() == 0) {
                        actions.add(repo, "no runs");
                        continue;
                    }
                    JSONObject run = runs.getJSONObject(0);
                    String status = run.optString("status");
                    String concl = run.optString("conclusion", "");
                    boolean done = "completed".equals(status);
                    boolean ok = "success".equals(concl);
                    String state = done ? concl : status;
                    actions.add(repo,
                            run.optString("name", "run") + " · " + state + " · "
                                    + Fmt.age(millis(run.optString("updated_at"))),
                            done ? ok : null,
                            run.optString("html_url", null));
                } catch (Exception e) {
                    actions.add(repo, "unavailable");
                }
            }
            out.add(actions);
        }
        return out;
    }

    private static ServiceData.Section searchSection(String header, String q, String token)
            throws Exception {
        ServiceData.Section sec = new ServiceData.Section(header);
        JSONObject o = Http.getObject(
                API + "/search/issues?per_page=20&q=" + Http.q(q), token, HOSTS, headers());
        JSONArray items = o.optJSONArray("items");
        if (items == null) return sec;
        for (int i = 0; i < items.length(); i++) {
            JSONObject it = items.getJSONObject(i);
            String repo = repoOf(it.optString("repository_url"));
            sec.add(it.optString("title"),
                    repo + " #" + it.optInt("number") + " · "
                            + Fmt.age(millis(it.optString("updated_at"))),
                    null, it.optString("html_url", null));
        }
        return sec;
    }

    /** One year of contribution counts, laid out like the github.com heatmap. */
    static final class Contributions {
        final int total;
        final int[][] grid; // [week][weekday 0=Sun..6=Sat], -1 = no such day

        Contributions(int total, int[][] grid) {
            this.total = total;
            this.grid = grid;
        }
    }

    private static final String GRAPHQL_CONTRIBUTIONS = "query { viewer { contributionsCollection { "
            + "contributionCalendar { totalContributions weeks { contributionDays { "
            + "contributionCount weekday } } } } } }";

    static Contributions contributions(String token) throws Exception {
        JSONObject body = new JSONObject().put("query", GRAPHQL_CONTRIBUTIONS);
        JSONObject resp = Http.postObject(API + "/graphql", token, HOSTS, body, headers());
        JSONObject cal = resp.getJSONObject("data").getJSONObject("viewer")
                .getJSONObject("contributionsCollection").getJSONObject("contributionCalendar");
        JSONArray weeks = cal.getJSONArray("weeks");

        int[][] grid = new int[weeks.length()][7];
        for (int[] row : grid) Arrays.fill(row, -1);
        for (int w = 0; w < weeks.length(); w++) {
            JSONArray days = weeks.getJSONObject(w).getJSONArray("contributionDays");
            for (int d = 0; d < days.length(); d++) {
                JSONObject day = days.getJSONObject(d);
                int weekday = day.optInt("weekday", -1);
                if (weekday >= 0 && weekday < 7) grid[w][weekday] = day.optInt("contributionCount");
            }
        }
        return new Contributions(cal.optInt("totalContributions"), grid);
    }

    private static String repoOf(String repositoryUrl) {
        // https://api.github.com/repos/owner/name  ->  owner/name
        int at = repositoryUrl.indexOf("/repos/");
        return at >= 0 ? repositoryUrl.substring(at + 7) : repositoryUrl;
    }

    private static long millis(String iso) {
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }
}
