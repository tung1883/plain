package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlainSpellCheckerServiceTest {

    @Test
    public void localesMapToOurLanguages() {
        assertEquals("en", PlainSpellCheckerService.languageOf("en_US"));
        assertEquals("vi", PlainSpellCheckerService.languageOf("vi"));
        assertEquals("de", PlainSpellCheckerService.languageOf("de_AT"));
        assertNull(PlainSpellCheckerService.languageOf("ja"));
        assertNull(PlainSpellCheckerService.languageOf(""));
        assertNull(PlainSpellCheckerService.languageOf(null));
    }

    @Test
    public void namesNumbersAndShortWordsAreNotChecked() {
        assertTrue(PlainSpellCheckerService.skip("Tung"));
        assertTrue(PlainSpellCheckerService.skip("ab"));
        assertTrue(PlainSpellCheckerService.skip("abc1"));
        assertTrue(PlainSpellCheckerService.skip("a@b.c"));
        assertFalse(PlainSpellCheckerService.skip("wrod"));
        assertFalse(PlainSpellCheckerService.skip("don't"));
        assertFalse(PlainSpellCheckerService.skip("nguoi"));
    }
}
