package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TextMatchTest {

    private static int lex(String text, String query) {
        return TextMatch.lexicalScore(TextMatch.fold(text), TextMatch.prepare(query));
    }

    @Test
    public void foldStripsAccentsAndPunctuation() {
        assertEquals("nguyen van", TextMatch.fold("Nguyễn Văn"));
        assertEquals("da nang", TextMatch.fold("Đà Nẵng"));
        assertEquals("strasse", TextMatch.fold("Straße"));
        assertEquals("a b c", TextMatch.fold("a--b__c"));
        assertEquals("hello world", TextMatch.fold("  Hello,  World! "));
        assertEquals("", TextMatch.fold(""));
        assertEquals("", TextMatch.fold(" - "));
    }

    @Test
    public void lexicalTiersGoFromExactToLoose() {
        assertEquals(0, lex("calendar", "calendar"));
        assertEquals(1, lex("calendar", "cal"));
        assertEquals(2, lex("my calendar", "calendar"));            // phrase at a word start
        assertEquals(3, lex("mycalendar", "calendar"));             // phrase inside a word
        assertEquals(4, lex("monthly calendar view", "view calendar"));   // all words, each at a word start
        assertEquals(5, lex("xcalendar yview", "view calendar"));   // all words, somewhere inside
        assertEquals(TextMatch.NO_MATCH, lex("calendar", "clock"));
        assertEquals(TextMatch.NO_MATCH, lex("monthly calendar", "view calendar"));
    }

    @Test
    public void emptyQueryMatchesEverythingWeakly() {
        assertTrue(TextMatch.prepare("   ").empty);
        assertEquals(1, TextMatch.lexicalScore("anything", TextMatch.prepare("")));
    }

    @Test
    public void rawTextIsFoldedBeforeScoring() {
        assertEquals(0, TextMatch.score("Nguyễn", TextMatch.prepare("nguyen")));
        assertEquals(0, TextMatch.score("Tài liệu", TextMatch.prepare("tai lieu")));
    }

    @Test
    public void fuzzyMatchesCloseSpellingsOnly() {
        int typo = TextMatch.score("calendar", TextMatch.prepare("calender"));
        assertTrue("typo should still match, got " + typo, typo >= 100 && typo < 140);
        assertEquals(TextMatch.NO_MATCH, TextMatch.score("calendar", TextMatch.prepare("zzzzzz")));
        // a lexical hit always beats a fuzzy one
        assertTrue(TextMatch.score("calendar", TextMatch.prepare("cal")) < typo);
    }

    @Test
    public void keywordsMatchButRankBelowTheTitle() {
        TextMatch.Query network = TextMatch.prepare("network");
        assertEquals(6, TextMatch.score("Wi-Fi", new String[]{"wireless", "network"}, network));
        assertEquals(0, TextMatch.score("Network", new String[]{"net"}, network));
        assertEquals(TextMatch.NO_MATCH, TextMatch.score("Wi-Fi", new String[]{"wireless"}, network));
    }

    @Test
    public void folderAndFileQualifiersNarrowTheKind() {
        TextMatch.Query folder = TextMatch.prepare("folder docs");
        assertEquals(TextMatch.Kind.DIRECTORY, folder.kind);
        assertEquals("docs", folder.withoutQualifier.folded);

        long sig = TextMatch.signatureOf("docs");
        assertEquals(0, TextMatch.score("docs", sig, folder, true));                         // a folder called docs
        assertEquals(TextMatch.NO_MATCH, TextMatch.score("docs", sig, folder, false));       // a file called docs

        TextMatch.Query file = TextMatch.prepare("file docs");
        assertEquals(TextMatch.Kind.FILE, file.kind);
        assertEquals(0, TextMatch.score("docs", sig, file, false));
        assertEquals(TextMatch.NO_MATCH, TextMatch.score("docs", sig, file, true));
    }

    @Test
    public void aloneQualifierWordIsJustText() {
        assertEquals(TextMatch.Kind.ANY, TextMatch.prepare("folder").kind);
        assertEquals(TextMatch.Kind.ANY, TextMatch.prepare("docs").kind);
    }
}
