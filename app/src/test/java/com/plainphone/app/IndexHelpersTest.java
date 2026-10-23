package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.junit.Test;

public class IndexHelpersTest {

    // ------------------------------------------------------------------------ Fold

    @Test
    public void foldForIndexSplitsCjk() {
        assertEquals("读 文 件 夹 test", Fold.forIndex("读文件夹 Test"));
        assertEquals("nguyen van", Fold.forIndex("Nguyễn Văn"));
        assertEquals("に ほ ん go", Fold.forIndex("にほん go"));
        assertEquals("", Fold.forIndex(""));
        assertEquals("한글 안녕", TextMatch.fold("한글 안녕").equals(Fold.forIndex("한글 안녕")) ? "한글 안녕" : "changed");
    }

    // --------------------------------------------------------------------- Chunker

    @Test
    public void shortTextIsOnePassage() {
        List<String> c = Chunker.split("  hello world  ");
        assertEquals(1, c.size());
        assertEquals("hello world", c.get(0));
        assertTrue(Chunker.split("   ").isEmpty());
        assertTrue(Chunker.split(null).isEmpty());
    }

    @Test
    public void longTextSplitsAtParagraphsAndStaysBounded() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) sb.append("Paragraph ").append(i).append(" has some words in it to fill the space nicely. ".repeat(3)).append("\n\n");
        List<String> c = Chunker.split(sb.toString());
        assertTrue(c.size() > 3);
        for (String p : c) assertTrue(p.length() <= Chunker.MAX);
        assertTrue(c.get(0).startsWith("Paragraph 0"));
        assertTrue(c.get(c.size() - 1).contains("Paragraph 39"));
    }

    @Test
    public void oneHugeParagraphIsCutAtSentences() {
        String sentence = "This is a sentence of moderate length. ";
        List<String> c = Chunker.split(sentence.repeat(200));
        assertTrue(c.size() > 3);
        for (String p : c) {
            assertTrue(p.length() <= Chunker.MAX);
            assertTrue("cut at a sentence end: " + p.substring(Math.max(0, p.length() - 5)), p.endsWith("."));
        }
    }

    @Test
    public void textWithoutBreaksIsCutHard() {
        List<String> c = Chunker.split("x".repeat(5000));
        assertTrue(c.size() >= 3);
        for (String p : c) assertTrue(p.length() <= Chunker.MAX);
        int total = 0;
        for (String p : c) total += p.length();
        assertEquals(5000, total);
    }

    // -------------------------------------------------------------------- HtmlText

    @Test
    public void htmlEntitiesAndBlocks() {
        assertEquals("a & b\n\nsecond", HtmlText.strip("<p>a &amp; b</p><p>second</p>"));
        assertEquals("Ä €", HtmlText.strip("&#196; &#x20AC;"));
        assertEquals("x", HtmlText.strip("<!-- hidden --><b>x</b>"));
        assertEquals("", HtmlText.strip(null));
    }

    // -------------------------------------------------------------------- FtsQuery

    private static String fts(String raw) {
        return FtsQuery.build(QueryParser.parse(raw, 0));
    }

    @Test
    public void ftsQueryAndsWordsWithPrefixOnTheLast() {
        assertEquals("\"budget\" \"rep*\"", fts("budget rep"));
        assertEquals("\"nguyen*\"", fts("Nguyễn"));
        assertEquals("\"读\" \"文*\"", fts("读文"));
    }

    @Test
    public void ftsQueryPhrasesAndExclusions() {
        assertEquals("\"project plan\"", fts("\"project plan\" -draft"));
        assertEquals("\"budget\" \"q3 report\"", fts("budget \"Q3 report\""));
        assertEquals("\"x1*\"", fts("x1 -\"old copy\""));
    }

    @Test
    public void ftsQueryNeedsSomethingPositive() {
        assertNull(fts(""));
        assertNull(fts("a"));
        assertNull(fts("-draft"));
        assertNull(fts("pdf:"));
        assertEquals("\"ab*\"", fts("ab"));
    }

    // ------------------------------------------------------------------------ Bm25

    private static int[] mi(int phrases, int rows, int avg, int len, int... hits) {
        int[] out = new int[5 + hits.length];
        out[0] = phrases;
        out[1] = 1;
        out[2] = rows;
        out[3] = avg;
        out[4] = len;
        System.arraycopy(hits, 0, out, 5, hits.length);
        return out;
    }

    @Test
    public void bm25PrefersMoreHitsRarerWordsAndShorterText() {
        // one phrase: {hits in row, hits in all rows, rows with a hit}
        double one = Bm25.score(mi(1, 1000, 100, 100, 1, 50, 20));
        double three = Bm25.score(mi(1, 1000, 100, 100, 3, 50, 20));
        assertTrue(three > one);
        double rare = Bm25.score(mi(1, 1000, 100, 100, 1, 5, 2));
        assertTrue(rare > one);
        double shortRow = Bm25.score(mi(1, 1000, 100, 20, 1, 50, 20));
        double longRow = Bm25.score(mi(1, 1000, 100, 400, 1, 50, 20));
        assertTrue(shortRow > longRow);
        assertTrue(Bm25.score(mi(2, 1000, 100, 100, 1, 50, 20, 1, 50, 20)) > one);
        assertEquals(0.0, Bm25.score(new int[]{1, 1}), 0.0);
    }

    @Test
    public void bm25ParsesAMatchinfoBlob() {
        ByteBuffer b = ByteBuffer.allocate(12).order(ByteOrder.nativeOrder());
        b.putInt(1).putInt(2).putInt(300);
        int[] parsed = Bm25.parse(b.array());
        assertEquals(3, parsed.length);
        assertEquals(300, parsed[2]);
    }

    // -------------------------------------------------------------------- Snippets

    @Test
    public void snippetCentresOnTheMatch() {
        String body = "Lorem ipsum dolor sit amet, ".repeat(10) + "the invoice number is 4711 for March. "
                + "Consectetur adipiscing elit. ".repeat(10);
        String s = Snippets.make(body, TextMatch.prepare("invoice"), 40, 60);
        assertTrue(s, s.contains("invoice number is 4711"));
        assertTrue(s.startsWith("..."));
        assertTrue(s.endsWith("..."));
        assertTrue(s.length() < 140);
    }

    @Test
    public void snippetAtTheStartAndWithoutAMatch() {
        String body = "Invoice 4711 is due soon. Other text follows after that.";
        String s = Snippets.make(body, TextMatch.prepare("invoice"), 40, 60);
        assertTrue(s.startsWith("Invoice 4711"));
        assertFalse(s.startsWith("..."));
        String none = Snippets.make(body, TextMatch.prepare("zzz"), 40, 20);
        assertTrue(none.startsWith("Invoice"));
        assertEquals("", Snippets.make(null, TextMatch.prepare("x"), 10, 10));
    }

    @Test
    public void snippetMatchesWithoutAccentsAndCollapsesWhitespace() {
        String s = Snippets.make("Ông   Nguyễn\n\nVăn đã   đến", TextMatch.prepare("nguyen"), 20, 20);
        assertEquals("Ông Nguyễn Văn đã đến", s);
    }

    // ----------------------------------------------------------------- IndexPolicy

    @Test
    public void indexingWaitsForPowerOrBattery() {
        assertTrue(IndexPolicy.shouldRun(true, 5, false));                       // plugged in: always
        assertTrue(IndexPolicy.shouldRun(true, 5, true));
        assertTrue(IndexPolicy.shouldRun(false, 90, false));                     // on battery, plenty left
        assertTrue(IndexPolicy.shouldRun(false, IndexPolicy.MIN_BATTERY, false));
        assertFalse(IndexPolicy.shouldRun(false, IndexPolicy.MIN_BATTERY - 1, false));   // low: waits
        assertTrue(IndexPolicy.shouldRun(false, IndexPolicy.MIN_BATTERY - 1, true));     // unless allowed
        assertTrue(IndexPolicy.shouldRun(false, 1, true));
    }
}
