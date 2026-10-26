package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import org.junit.Test;

public class QueryParserTest {

    private static final long NOW = 1_700_000_000_000L;

    private static ParsedQuery p(String q) {
        return QueryParser.parse(q, NOW);
    }

    @Test
    public void plainWordsPassThrough() {
        ParsedQuery q = p("  hello   world ");
        assertEquals("hello world", q.text);
        assertFalse(q.hasOperators());
        assertTrue(q.kinds.isEmpty());
        assertTrue(q.wants(SearchResult.Kind.NOTE));
        assertEquals("hello world", q.core.folded);
    }

    @Test
    public void emptyQuery() {
        ParsedQuery q = p("");
        assertEquals("", q.text);
        assertTrue(q.core.empty);
        assertFalse(q.hasOperators());
        assertTrue(QueryParser.parse(null, NOW).core.empty);
    }

    @Test
    public void quotedPhraseIsKeptWhole() {
        ParsedQuery q = p("meeting \"Project Plan\" notes");
        assertEquals(Arrays.asList("project plan"), q.phrases);
        assertEquals("meeting Project Plan notes", q.text);
        assertTrue(q.hasOperators());
    }

    @Test
    public void unclosedQuoteStillCounts() {
        ParsedQuery q = p("\"half open");
        assertEquals(Arrays.asList("half open"), q.phrases);
    }

    @Test
    public void minusExcludesWordsAndPhrases() {
        ParsedQuery q = p("invoice -draft -\"old copy\"");
        assertEquals("invoice", q.text);
        assertEquals(Arrays.asList("draft", "old copy"), q.excluded);
        assertTrue(q.rejects("final draft v2"));
        assertTrue(q.rejects("an old copy of it"));
        assertFalse(q.rejects("final invoice"));
    }

    @Test
    public void aLoneOrEmbeddedMinusIsText() {
        assertEquals("- wi-fi", p("- wi-fi").text);
        assertTrue(p("wi-fi").excluded.isEmpty());
    }

    @Test
    public void kindOperatorsChooseGroups() {
        ParsedQuery q = p("note:meeting todo:call");
        assertEquals("meeting call", q.text);
        assertTrue(q.wants(SearchResult.Kind.NOTE));
        assertTrue(q.wants(SearchResult.Kind.TODO));
        assertFalse(q.wants(SearchResult.Kind.APP));
        ParsedQuery f = p("file:report");
        assertTrue(f.wants(SearchResult.Kind.FILE));
        assertTrue(f.wants(SearchResult.Kind.CONTENT));
        assertFalse(f.wants(SearchResult.Kind.NOTE));
        assertTrue(p("content:budget").wants(SearchResult.Kind.CONTENT));
        assertFalse(p("content:budget").wants(SearchResult.Kind.FILE));
    }

    @Test
    public void operatorWithoutTextOnlyFilters() {
        ParsedQuery q = p("app:");
        assertEquals("", q.text);
        assertTrue(q.wants(SearchResult.Kind.APP));
        assertFalse(q.wants(SearchResult.Kind.WEB));
    }

    @Test
    public void extensionFilters() {
        ParsedQuery q = p("pdf:invoice ext:.DOCX");
        assertEquals("invoice", q.text);
        assertTrue(q.exts.contains("pdf"));
        assertTrue(q.exts.contains("docx"));
        assertTrue(q.acceptsExt("Invoice 2024.PDF"));
        assertTrue(q.acceptsExt("a.docx"));
        assertFalse(q.acceptsExt("a.txt"));
        assertFalse(q.acceptsExt("noext"));
        // an extension filter only makes sense for files
        assertTrue(q.wants(SearchResult.Kind.FILE));
        assertTrue(q.wants(SearchResult.Kind.CONTENT));
        assertFalse(q.wants(SearchResult.Kind.APP));
        assertTrue(p("anything").acceptsExt("whatever"));
    }

    @Test
    public void dateFilters() {
        ParsedQuery today = p("report in:today");
        assertEquals("report", today.text);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(NOW);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        assertEquals(c.getTimeInMillis(), today.since);
        assertTrue(today.acceptsTime(NOW));
        assertFalse(today.acceptsTime(c.getTimeInMillis() - 1));
        assertTrue(p("x in:week").since < today.since);
        assertTrue(p("x in:year").since < p("x in:month").since);
        assertTrue(p("x").acceptsTime(0));
    }

    @Test
    public void unknownPrefixesUrlsAndTimesStayText() {
        assertEquals("foo:bar", p("foo:bar").text);
        assertEquals("https://example.com/a", p("https://example.com/a").text);
        assertEquals("meet 10:30", p("meet 10:30").text);
        assertEquals("in:someday", p("in:someday").text);
        assertFalse(p("foo:bar").hasOperators());
    }

    @Test
    public void commandWords() {
        ParsedQuery todo = p("todo buy milk");
        assertEquals("todo", todo.command);
        assertEquals("buy milk", todo.commandText);
        ParsedQuery note = p("Note   an idea");
        assertEquals("note", note.command);
        assertEquals("an idea", note.commandText);
        assertNull(p("todo").command);                 // nothing after it: it is just a search
        assertNull(p("buy todo list").command);
        assertNull(p("\"todo list\" x").command);
    }
}
