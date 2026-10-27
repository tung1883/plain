package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KeyboardSearchTest {

    @Test
    public void recentMovesToFrontWithoutRepeats() {
        List<String> r = KeyboardSearch.pushRecent(Arrays.asList("a", "b", "c"), "B", 8);
        assertEquals(Arrays.asList("B", "a", "c"), r);
    }

    @Test
    public void recentIsCapped() {
        List<String> r = Collections.emptyList();
        for (int i = 0; i < 12; i++) r = KeyboardSearch.pushRecent(r, "q" + i, KeyboardSearch.MAX_RECENT);
        assertEquals(8, r.size());
        assertEquals("q11", r.get(0));
    }

    @Test
    public void blankQueryLeavesRecentAlone() {
        assertEquals(Arrays.asList("a"), KeyboardSearch.pushRecent(Arrays.asList("a"), "  ", 8));
    }

    @Test
    public void snippetLosesEllipses() {
        assertEquals("the invoice total", KeyboardSearch.cleanSnippet("...the invoice total..."));
        assertEquals("the invoice total", KeyboardSearch.cleanSnippet("…the invoice total…"));
        assertEquals("", KeyboardSearch.cleanSnippet(null));
    }

    @Test
    public void sizes() {
        assertEquals("512 B", KeyboardSearch.size(512));
        assertEquals("1.5 KB", KeyboardSearch.size(1536));
        assertEquals("12 MB", KeyboardSearch.size(12L * 1024 * 1024));
        assertEquals("Internal", KeyboardSearch.fileSubtitle("Internal", -1));
    }

    @Test
    public void passwordFieldsAreSecure() {
        int text = InputType.TYPE_CLASS_TEXT;
        assertTrue(InputKinds.isSecure(text | InputType.TYPE_TEXT_VARIATION_PASSWORD, 0));
        assertTrue(InputKinds.isSecure(text | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, 0));
        assertTrue(InputKinds.isSecure(text | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, 0));
        assertTrue(InputKinds.isSecure(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD, 0));
        assertFalse(InputKinds.isSecure(text, 0));
        assertFalse(InputKinds.isSecure(text | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 0));
        assertFalse(InputKinds.isSecure(InputType.TYPE_CLASS_NUMBER, 0));
    }

    @Test
    public void noLearningFlag() {
        assertTrue(InputKinds.noLearning(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING));
        assertFalse(InputKinds.noLearning(0));
    }
}
