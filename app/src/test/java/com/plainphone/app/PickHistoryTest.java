package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PickHistoryTest {

    private static final long DAY = 24L * 60 * 60 * 1000;

    private static final class Clock implements PickHistory.Clock {
        long now = 1_000_000_000_000L;

        @Override
        public long now() {
            return now;
        }
    }

    @Test
    public void neverPickedHasNoBoost() {
        PickHistory h = new PickHistory(new Clock());
        assertEquals(0.0, h.boost("app:a", "cal"), 0.0);
        assertEquals(0.0, h.boost(null, "cal"), 0.0);
    }

    @Test
    public void boostGrowsWithPicksForTheSameQuery() {
        PickHistory h = new PickHistory(new Clock());
        h.record("app:cal", "cal");
        double one = h.boost("app:cal", "cal");
        h.record("app:cal", "cal");
        h.record("app:cal", "cal");
        double three = h.boost("app:cal", "cal");
        assertTrue(one > 0);
        assertTrue(three > one);
        // logarithmic: three picks are far from three times one pick
        assertTrue(three < 3 * one);
    }

    @Test
    public void picksForAnotherQueryCountLess() {
        PickHistory h = new PickHistory(new Clock());
        for (int i = 0; i < 3; i++) h.record("app:cal", "cal");
        assertTrue(h.boost("app:cal", "cal") > h.boost("app:cal", "xyz"));
        assertTrue(h.boost("app:cal", "xyz") > 0);              // still counted a little overall
        assertEquals(h.boost("app:cal", "cal"), h.boost("app:cal", "calendar"), 1.0);  // longer query starts the same
        assertTrue(h.boost("app:cal", "ca") > h.boost("app:cal", "xyz"));               // shorter start of it too
    }

    @Test
    public void boostFadesOverWeeks() {
        Clock c = new Clock();
        PickHistory h = new PickHistory(c);
        h.record("note:1", "plan");
        double fresh = h.boost("note:1", "plan");
        c.now += 14 * DAY;
        double later = h.boost("note:1", "plan");
        c.now += 140 * DAY;
        double old = h.boost("note:1", "plan");
        assertTrue(fresh > later);
        assertTrue(later > old);
        assertTrue(old > 0);        // never fades to nothing
    }

    @Test
    public void adjustNeverCrossesTiers() {
        assertEquals(3, PickHistory.adjust(3, 0));
        assertEquals(1, PickHistory.adjust(3, 2.5));            // at most two places
        assertEquals(0, PickHistory.adjust(1, 5));              // floor at exact
        assertEquals(4, PickHistory.adjust(5, 1.0));
        assertEquals(100, PickHistory.adjust(130, 3.0));        // fuzzy: boost * 10
        assertEquals(90, PickHistory.adjust(130, 99));          // capped at 40
        assertEquals(60, PickHistory.adjust(100, 99));          // never below 60, so still under every name match
        assertTrue(PickHistory.adjust(100, 99) > 5);
    }

    @Test
    public void oldestEntriesAreDroppedPastTheCap() {
        Clock c = new Clock();
        PickHistory h = new PickHistory(c);
        for (int i = 0; i < PickHistory.MAX_ENTRIES + 20; i++) {
            c.now += 1000;
            h.record("k" + i, "q");
        }
        assertEquals(PickHistory.MAX_ENTRIES, h.size());
        assertEquals(0.0, h.boost("k0", "q"), 0.0);
        assertTrue(h.boost("k" + (PickHistory.MAX_ENTRIES + 19), "q") > 0);
    }

    @Test
    public void survivesAJsonRoundTrip() {
        Clock c = new Clock();
        PickHistory h = new PickHistory(c);
        h.record("app:cal", "cal");
        h.record("app:cal", "calen");
        h.record("file:/a/b.pdf", "report");
        PickHistory back = PickHistory.fromJson(h.toJson(), c);
        assertEquals(2, back.size());
        assertEquals(h.boost("app:cal", "cal"), back.boost("app:cal", "cal"), 1e-9);
        assertEquals(h.boost("file:/a/b.pdf", "report"), back.boost("file:/a/b.pdf", "report"), 1e-9);
    }

    @Test
    public void badJsonGivesAnEmptyHistory() {
        Clock c = new Clock();
        assertEquals(0, PickHistory.fromJson(null, c).size());
        assertEquals(0, PickHistory.fromJson("", c).size());
        assertEquals(0, PickHistory.fromJson("not json", c).size());
        assertEquals(0, PickHistory.fromJson("{\"v\":1}", c).size());
    }

    @Test
    public void clearForgetsEverything() {
        PickHistory h = new PickHistory(new Clock());
        h.record("a", "q");
        h.clear();
        assertEquals(0, h.size());
        assertEquals(0.0, h.boost("a", "q"), 0.0);
    }
}
