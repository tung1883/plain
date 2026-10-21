package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class SpellCheckerTest {

    private static SpellChecker checker(String primary, String... mix) {
        SpellChecker sc = new SpellChecker(lang -> {
            switch (lang) {
                case "en": return Arrays.asList("the", "word", "world", "hello", "help", "held", "phone", "keyboard", "people", "there");
                case "de": return Arrays.asList("der", "über", "schön", "mädchen", "für", "können", "größe", "grüße", "aber");
                case "vi": return Arrays.asList("tôi", "người", "việt", "không");
                default: return Collections.emptyList();
            }
        });
        sc.setLanguages(primary, Arrays.asList(mix));
        return sc;
    }

    @Test
    public void knownWordsAndLoneLettersAreNotFlagged() {
        SpellChecker sc = checker("en");
        assertTrue(sc.known("hello"));
        assertTrue(sc.known("Hello"));
        assertTrue(sc.known("a"));
        assertTrue(sc.known("abc123"));
        assertFalse(sc.known("helo"));
    }

    @Test
    public void typoSuggestionsPutTheLikelyWordFirst() {
        SpellChecker sc = checker("en");
        assertEquals("word", sc.suggest("wrod", 3).get(0).word);
        assertEquals("keyboard", sc.suggest("keybaord", 3).get(0).word);
    }

    @Test
    public void accentsAndUmlautSpellingsRestore() {
        SpellChecker sc = checker("de");
        assertEquals("über", sc.suggest("uber", 3).get(0).word);
        assertEquals("über", sc.suggest("ueber", 3).get(0).word);
        assertEquals("mädchen", sc.suggest("maedchen", 3).get(0).word);
        assertEquals("größe", sc.suggest("groesse", 3).get(0).word);
        assertEquals("grüße", sc.suggest("gruesse", 3).get(0).word);
        assertEquals("người", checker("vi").suggest("nguoi", 3).get(0).word);
    }

    @Test
    public void mixedLanguagesAreSearchedAndTagged() {
        SpellChecker sc = checker("en", "de");
        SpellChecker.Hit h = sc.suggest("ueber", 3).get(0);
        assertEquals("über", h.word);
        assertEquals("de", h.lang);
        assertTrue(sc.known("schön"));
        assertFalse(checker("en").known("schön"));
    }

    @Test
    public void prefixHitsAreCompletionsInFrequencyOrder() {
        SpellChecker sc = checker("en");
        List<SpellChecker.Hit> hits = sc.prefix("hel", 4);
        assertEquals("hello", hits.get(0).word);
        assertEquals(3, hits.size());
        assertTrue(sc.prefix("hello", 4).isEmpty());   // a finished word has no completions of itself
    }

    @Test
    public void umlautPrefixMatchesTheAsciiSpelling() {
        SpellChecker sc = checker("de");
        assertEquals("über", sc.prefix("ueb", 3).get(0).word);
        assertEquals("schön", sc.prefix("scho", 3).get(0).word);
    }

    @Test
    public void autoFixOnlyWhenSure() {
        SpellChecker sc = checker("en");
        assertEquals("word", sc.autoFix("wrod").word);
        assertNull(sc.autoFix("hello"));        // known
        assertNull(sc.autoFix("xq"));           // too short and nothing near
        assertNull(sc.autoFix("zzzzzz"));       // nothing near
        assertNotNull(checker("de").autoFix("ueber"));
    }

    @Test
    public void ignoredWordsAreKnown() {
        SpellChecker sc = checker("en");
        sc.ignore("Wrod");
        assertTrue(sc.known("wrod"));
        assertNull(sc.autoFix("wrod"));
    }

    @Test
    public void helpersStripAndTransliterate() {
        assertEquals("nguoi", SpellChecker.strip("Người"));
        assertEquals("d", SpellChecker.strip("đ"));
        assertEquals("ueber", SpellChecker.translit("Über"));
        assertEquals("strasse", SpellChecker.translit("Straße"));
        assertEquals(1, SpellChecker.osa("wrod", "word", 2));
        assertEquals(3, SpellChecker.osa("abcdef", "uvwxyz", 2));
    }
}
