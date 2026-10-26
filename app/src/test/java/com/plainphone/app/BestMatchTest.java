package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class BestMatchTest {

    private static SearchResult r(SearchResult.Kind kind, String title, int score) {
        return new SearchResult(kind, title, null, score, () -> {});
    }

    private static Map<SearchResult.Kind, List<SearchResult>> groups(SearchResult... results) {
        Map<SearchResult.Kind, List<SearchResult>> m = new EnumMap<>(SearchResult.Kind.class);
        for (SearchResult x : results) {
            if (!m.containsKey(x.kind)) m.put(x.kind, new ArrayList<SearchResult>());
            m.get(x.kind).add(x);
        }
        return m;
    }

    @Test
    public void takesTheStrongestOfEachGroupAndRemovesThemFromIt() {
        SearchResult calendar = r(SearchResult.Kind.APP, "Calendar", 1);
        SearchResult clock = r(SearchResult.Kind.APP, "Clock", 1);
        SearchResult note = r(SearchResult.Kind.NOTE, "cal notes", 0);
        Map<SearchResult.Kind, List<SearchResult>> g = groups(calendar, clock, note);
        List<SearchResult> best = BestMatch.pick(g);
        assertEquals(2, best.size());
        assertSame(note, best.get(0));                 // lower score first
        assertSame(calendar, best.get(1));             // first of the tied apps
        assertEquals(1, g.get(SearchResult.Kind.APP).size());
        assertSame(clock, g.get(SearchResult.Kind.APP).get(0));
        assertTrue(g.get(SearchResult.Kind.NOTE).isEmpty());
    }

    @Test
    public void atMostThree() {
        Map<SearchResult.Kind, List<SearchResult>> g = groups(
                r(SearchResult.Kind.APP, "a", 0), r(SearchResult.Kind.NOTE, "b", 1),
                r(SearchResult.Kind.FILE, "c", 1), r(SearchResult.Kind.TODO, "d", 2));
        assertEquals(3, BestMatch.pick(g).size());
        assertEquals(1, g.get(SearchResult.Kind.TODO).size());        // the fourth stays in its own group
    }

    @Test
    public void weakFuzzyWebActionAndGuardedRowsNeverQualify() {
        Map<SearchResult.Kind, List<SearchResult>> g = groups(
                r(SearchResult.Kind.APP, "weak", 4),
                r(SearchResult.Kind.NOTE, "fuzzy", 120),
                r(SearchResult.Kind.WEB, "Search the web", 0),
                r(SearchResult.Kind.ACTION, "Call 12345", 0),
                new SearchResult(SearchResult.Kind.PLAIN, "Screen off", null, 0, () -> {}).guarded());
        assertTrue(BestMatch.pick(g).isEmpty());
        assertEquals(1, g.get(SearchResult.Kind.APP).size());
    }

    @Test
    public void nothingWhenOnlyOneGroupHasResults() {
        Map<SearchResult.Kind, List<SearchResult>> g = groups(
                r(SearchResult.Kind.APP, "Calendar", 0), r(SearchResult.Kind.APP, "Camera", 1));
        assertTrue(BestMatch.pick(g).isEmpty());
        assertEquals(2, g.get(SearchResult.Kind.APP).size());
    }

    @Test
    public void webAndActionRowsDoNotCountAsAGroup() {
        Map<SearchResult.Kind, List<SearchResult>> g = groups(
                r(SearchResult.Kind.APP, "Calendar", 0), r(SearchResult.Kind.WEB, "Search the web", 0));
        assertTrue(BestMatch.pick(g).isEmpty());
    }

    @Test
    public void aGroupWithOnlyWeakResultsStillCountsAsFilled() {
        Map<SearchResult.Kind, List<SearchResult>> g = groups(
                r(SearchResult.Kind.APP, "Calendar", 0), r(SearchResult.Kind.NOTE, "loose match", 5));
        List<SearchResult> best = BestMatch.pick(g);
        assertEquals(1, best.size());
        assertEquals("Calendar", best.get(0).title);
    }
}
