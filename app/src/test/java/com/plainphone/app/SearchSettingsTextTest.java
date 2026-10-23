package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.Test;

public class SearchSettingsTextTest {

    @Test
    public void typeSummaryNamesWhatIsOn() {
        assertEquals("All", SearchSettingsText.typeSummary(Extractors.allGroups()));
        assertEquals("None", SearchSettingsText.typeSummary(new HashSet<String>()));
        assertEquals("Text, PDF", SearchSettingsText.typeSummary(new HashSet<>(Arrays.asList(Extractors.PDF, Extractors.TEXT))));
        assertEquals("Office, E-books", SearchSettingsText.typeSummary(new HashSet<>(Arrays.asList(Extractors.EPUB, Extractors.OFFICE))));
    }

    @Test
    public void skippedPillsListOnlyWhatApplies() {
        Map<String, Integer> m = new HashMap<>();
        m.put("ok", 30);
        m.put("scanned", 3);
        m.put("toobig", 1);
        m.put("locked", 0);
        assertEquals(Arrays.asList("3 scanned", "1 too big"), SearchSettingsText.skippedPills(m));
        m.put("failed", 2);
        m.put("locked", 4);
        assertEquals(Arrays.asList("3 scanned", "1 too big", "4 locked", "2 unreadable"), SearchSettingsText.skippedPills(m));
        assertTrue(SearchSettingsText.skippedPills(new HashMap<String, Integer>()).isEmpty());
    }

    @Test
    public void sizeLineAndFraction() {
        long mb = 1024L * 1024;
        assertEquals("24 MB of 150 MB", SearchSettingsText.sizeLine(24 * mb + 5, 150));
        assertEquals("under 1 MB of 50 MB", SearchSettingsText.sizeLine(1000, 50));
        assertEquals("24 MB of 1 GB", SearchSettingsText.sizeLine(24 * mb, 1000));
        assertEquals("24 MB", SearchSettingsText.sizeLine(24 * mb, 0));      // no limit
        assertEquals("Unlimited", SearchSettingsText.limitLabel(0));
        assertEquals("1 GB", SearchSettingsText.limitLabel(1000));
        assertEquals("500 MB", SearchSettingsText.limitLabel(500));
        assertEquals(0.5f, SearchSettingsText.usedFraction(75 * mb, 150), 1e-6);
        assertEquals(1f, SearchSettingsText.usedFraction(300 * mb, 150), 0f);
        assertEquals(0f, SearchSettingsText.usedFraction(0, 150), 0f);
        assertEquals(0f, SearchSettingsText.usedFraction(10, 0), 0f);
    }

    @Test
    public void lastRunReadsNaturally() {
        long now = 10_000_000_000L, min = 60_000L;
        assertEquals("not run yet", SearchSettingsText.lastRun(0, now));
        assertEquals("last run just now", SearchSettingsText.lastRun(now - 20_000, now));
        assertEquals("last run 3 min ago", SearchSettingsText.lastRun(now - 3 * min, now));
        assertEquals("last run 2 h ago", SearchSettingsText.lastRun(now - 125 * min, now));
        assertEquals("last run 1 day ago", SearchSettingsText.lastRun(now - 25 * 60 * min, now));
        assertEquals("last run 4 days ago", SearchSettingsText.lastRun(now - 4 * 24 * 60 * min, now));
        assertEquals("last run just now", SearchSettingsText.lastRun(now + 5 * min, now));   // clock went back
    }

    @Test
    public void reasonsAreInWords() {
        assertEquals("Scanned pages, there is no text to read", SearchSettingsText.reason("scanned", null, 100));
        assertEquals("Password protected", SearchSettingsText.reason("locked", null, 100));
        assertEquals("Larger than 100 MB, too big to read", SearchSettingsText.reason("toobig", null, 100));
        assertEquals("Could not be read: bad zip", SearchSettingsText.reason("failed", " bad zip ", 0));
        assertEquals("Could not be read", SearchSettingsText.reason("failed", null, 0));
        assertEquals("No text in it", SearchSettingsText.reason("empty", null, 0));
    }

    @Test
    public void progressLine() {
        assertEquals("Reading 12 of 34…", SearchSettingsText.progressLine(12, 34));
    }
}
