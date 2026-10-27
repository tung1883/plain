package com.plainphone.app;

import android.app.Activity;
import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** "Inside files": searches the content index and turns the best passages into result rows. */
final class ContentSearch {

    private static final String TAG = "PlainContentIndex";
    private static final int CANDIDATES = 300;
    private static final int KEPT = 150;

    private ContentSearch() {}

    private static final class Group {
        ContentDb.Row best;
        double bestScore;
        int count;
    }

    /** Up to {@code maxFiles} files whose text matches, best first. Call from a background thread. */
    static List<SearchResult> search(Context host, ParsedQuery query, int maxFiles) {
        List<SearchResult> results = new ArrayList<>();
        String fts = FtsQuery.build(query);
        if (fts == null) return results;
        long t0 = System.currentTimeMillis();

        ContentDb db = ContentDb.get(host);
        List<double[]> hits = db.match(fts, CANDIDATES);
        if (hits.isEmpty()) return results;
        Collections.sort(hits, new Comparator<double[]>() {
            @Override
            public int compare(double[] a, double[] b) {
                return Double.compare(b[1], a[1]);
            }
        });
        if (hits.size() > KEPT) hits = new ArrayList<>(hits.subList(0, KEPT));
        List<Long> ids = new ArrayList<>();
        for (double[] h : hits) ids.add((long) h[0]);
        Map<Long, ContentDb.Row> rows = db.rows(ids);

        Map<Long, Group> groups = new LinkedHashMap<>();
        for (double[] h : hits) {
            ContentDb.Row row = rows.get((long) h[0]);
            if (row == null) continue;
            String name = new File(row.path).getName();
            if (!query.acceptsExt(name) || !query.acceptsTime(row.mtime)) continue;
            if (!query.excluded.isEmpty() && query.rejects(Fold.forIndex(row.body))) continue;
            Group g = groups.get(row.docId);
            if (g == null) {
                if (!new File(row.path).exists()) continue;       // deleted since it was indexed
                g = new Group();
                g.best = row;
                g.bestScore = h[1];
                groups.put(row.docId, g);
            }
            g.count++;
        }

        List<Group> ordered = new ArrayList<>(groups.values());
        Collections.sort(ordered, new Comparator<Group>() {
            @Override
            public int compare(Group a, Group b) {
                return Double.compare(b.bestScore, a.bestScore);
            }
        });
        int rank = 0;
        for (Group g : ordered) {
            if (results.size() >= maxFiles) break;
            File file = new File(g.best.path);
            String snippet = Snippets.make(g.best.body, query.core, 60, 110);
            int nameScore = TextMatch.score(file.getName(), query.core);
            int score = Math.max(0, rank - (nameScore != TextMatch.NO_MATCH && nameScore <= 1 ? 3 : 0));
            rank++;

            StringBuilder sub = new StringBuilder(DeviceSearch.describeLocation(file.getParentFile()));
            if (!g.best.locator.isEmpty()) sub.append(" · ").append(g.best.locator);
            if (g.count > 1) sub.append(" · ").append(g.count).append(" matches");
            sub.append('\n').append(snippet);

            ContentHit hit = new ContentHit(file, g.best.locator, snippet, g.count,
                    DeviceSearch.describeLocation(file.getParentFile()));
            results.add(new SearchResult(SearchResult.Kind.CONTENT, file.getName(), sub.toString(), score,
                    () -> { if (host instanceof Activity) DeviceSearch.open((Activity) host, file, false); }, hit).styled(SearchResult.Style.CONTENT));
        }
        android.util.Log.d(TAG, "query " + fts + " -> " + results.size() + " files in "
                + (System.currentTimeMillis() - t0) + " ms");
        return results;
    }
}
