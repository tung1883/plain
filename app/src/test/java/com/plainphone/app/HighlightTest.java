package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class HighlightTest {

    private static List<int[]> ranges(String text, String query) {
        return Highlight.ranges(text, TextMatch.prepare(query));
    }

    private static String marked(String text, String query) {
        StringBuilder sb = new StringBuilder();
        int at = 0;
        for (int[] r : ranges(text, query)) {
            sb.append(text, at, r[0]).append('[').append(text, r[0], r[1]).append(']');
            at = r[1];
        }
        return sb.append(text.substring(at)).toString();
    }

    @Test
    public void foldedTextMatchesTextMatchFold() {
        for (String s : new String[]{"Nguyễn Văn", "Straße 12", "  a--b  ", "Đà Nẵng!", "한글 test", "日本語 text", "x"}) {
            assertEquals(s, TextMatch.fold(s), Highlight.foldWithMap(s).text);
        }
    }

    @Test
    public void marksMatchedWordsInTheOriginalText() {
        assertEquals("[Cal]endar view", marked("Calendar view", "cal"));
        assertEquals("my [Calendar] [View]", marked("my Calendar View", "view calendar"));
        assertEquals("nothing here", marked("nothing here", "zzz"));
    }

    @Test
    public void accentsAreMatchedWithoutAccentsAndMarkedWithThem() {
        assertEquals("Ông [Nguyễn] Văn", marked("Ông Nguyễn Văn", "nguyen"));
        assertEquals("[Tài] [liệu]", marked("Tài liệu", "tai lieu"));
        assertEquals("Fuß[ball]", marked("Fußball", "ball"));
    }

    @Test
    public void sharpSAndExpandingCharactersCoverTheWholeLetter() {
        assertEquals("[Straße] 12", marked("Straße 12", "strasse"));
        assertEquals("a[ß]b", marked("aßb", "ss"));
    }

    @Test
    public void overlappingAndRepeatedMatchesMerge() {
        assertEquals("[banana]", marked("banana", "ban ana nan"));
        assertEquals("[a] b [a] b [a]", marked("a b a b a", "a"));
    }

    @Test
    public void emptyQueryOrTextMarksNothing() {
        assertTrue(ranges("hello", "").isEmpty());
        assertTrue(ranges("", "x").isEmpty());
        assertTrue(Highlight.ranges(null, TextMatch.prepare("x")).isEmpty());
        assertTrue(Highlight.ranges("abc", null).isEmpty());
    }

    @Test
    public void aFolderQualifierWordIsNotMarked() {
        assertEquals("[docs] folder", marked("docs folder", "folder docs"));
    }

    @Test
    public void cjkAndSurrogateText() {
        assertEquals("读[文件]夹", marked("读文件夹", "文件"));
        assertEquals("😀 [smile]", marked("😀 smile", "smile"));
    }
}
