package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmojiKeywordsTest {

    @Test
    public void nameStartRanksFirst() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("x", "broken heart\nsad");
        m.put("y", "heart eyes\nlove");
        m.put("z", "red apple\nsweetheart");
        List<String> r = EmojiKeywords.search(m, "heart", 10);
        assertEquals("y", r.get(0));        // name starts with it
        assertEquals("x", r.get(1));        // a name word starts with it
        assertEquals("z", r.get(2));        // only inside a word
    }

    @Test
    public void prefixFindsSeveral() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("a", "heart\nlove");
        m.put("b", "headphone\nmusic");
        m.put("c", "stethoscope\nhealth");
        m.put("d", "cat\nanimal");
        List<String> r = EmojiKeywords.search(m, "hea", 10);
        assertEquals(3, r.size());
        assertTrue(r.contains("a") && r.contains("b") && r.contains("c"));
    }

    @Test
    public void allWordsMustMatchAndAccentsFold() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("a", "red heart\nlove");
        m.put("b", "blue heart\nlove");
        assertEquals(1, EmojiKeywords.search(m, "RED heart", 10).size());
        assertEquals(1, EmojiKeywords.search(m, "réd", 10).size());
    }

    @Test
    public void emptyQueryAndCap() {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < 5; i++) m.put("e" + i, "smile");
        assertTrue(EmojiKeywords.search(m, "  ", 10).isEmpty());
        assertEquals(3, EmojiKeywords.search(m, "smile", 3).size());
    }
}
