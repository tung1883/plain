package com.plainphone.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The "Best match" group: the few strongest results across all groups, so the thing you most likely
 * want is on top whatever kind it is. Only strong name matches qualify (exact, prefix, a phrase at a
 * word start, or one that the user's earlier picks have pulled up), never fuzzy ones, web or action
 * rows, or rows that must not be fired from the Go key.
 */
final class BestMatch {

    static final int MAX = 3;
    static final int MAX_SCORE = 2;

    private static final Set<SearchResult.Kind> NEVER = EnumSet.of(SearchResult.Kind.ACTION,
            SearchResult.Kind.WEB, SearchResult.Kind.CONTENT, SearchResult.Kind.BEST, SearchResult.Kind.DEV,
            SearchResult.Kind.WORKSPACE);

    private BestMatch() {}

    /**
     * Take up to {@link #MAX} results out of {@code groups} and return them, best first. Each group
     * gives at most one. Returns nothing (and takes nothing) when fewer than two groups have results,
     * since then the group would only repeat the list below it.
     */
    static List<SearchResult> pick(Map<SearchResult.Kind, List<SearchResult>> groups) {
        int filled = 0;
        List<SearchResult> candidates = new ArrayList<>();
        for (Map.Entry<SearchResult.Kind, List<SearchResult>> e : groups.entrySet()) {
            if (NEVER.contains(e.getKey()) || e.getValue().isEmpty()) continue;
            filled++;
            SearchResult best = null;
            for (SearchResult r : e.getValue()) {
                if (r.guarded || r.score < 0 || r.score > MAX_SCORE) continue;
                if (best == null || r.score < best.score) best = r;
            }
            if (best != null) candidates.add(best);
        }
        if (filled < 2 || candidates.isEmpty()) return new ArrayList<>();

        Collections.sort(candidates, new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult a, SearchResult b) {
                if (a.score != b.score) return Integer.compare(a.score, b.score);
                return Integer.compare(a.kind.ordinal(), b.kind.ordinal());
            }
        });
        List<SearchResult> picked = new ArrayList<>(candidates.subList(0, Math.min(MAX, candidates.size())));
        for (SearchResult r : picked) groups.get(r.kind).remove(r);
        return picked;
    }
}
