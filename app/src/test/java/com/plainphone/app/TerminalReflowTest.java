package com.plainphone.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class TerminalReflowTest {

    private static void out(TerminalEmulator t, String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        t.feed(b, b.length);
    }

    private static String row(TerminalEmulator t, int y) {
        return new String(t.glyph[y]).replaceAll(" +$", "");
    }

    private static String sb(TerminalEmulator t, int i) {
        return new String(t.scrollbackLine(i).g).replaceAll(" +$", "");
    }

    @Test
    public void narrowThenWideRestoresText() {
        // the zoom-in / zoom-out bug: text past the narrow width used to be lost
        TerminalEmulator t = new TerminalEmulator(20, 4, null);
        out(t, "hello wonderful world\r\n$ ");
        t.resize(8, 4);
        t.resize(20, 4);
        assertEquals("hello wonderful worl", row(t, 0));
        assertEquals("d", row(t, 1));
        assertEquals("$", row(t, 2));
        assertEquals(2, t.cursorY);
        assertEquals(2, t.cursorX);
    }

    @Test
    public void narrowingWrapsAndKeepsCursorOnItsCharacter() {
        TerminalEmulator t = new TerminalEmulator(10, 5, null);
        out(t, "abcdefgh\r\n$ xy");
        t.resize(4, 5);
        assertEquals("abcd", row(t, 0));
        assertEquals("efgh", row(t, 1));
        assertEquals("$ xy", row(t, 2));
        assertEquals(3, t.cursorY); // after "xy" = offset 4 = start of the next row
        assertEquals(0, t.cursorX);
    }

    @Test
    public void softWrappedLineJoinsWhenWidened() {
        TerminalEmulator t = new TerminalEmulator(5, 4, null);
        out(t, "0123456789\r\n$ ");
        assertEquals("01234", row(t, 0));
        t.resize(12, 4);
        assertEquals("0123456789", row(t, 0));
        assertEquals("$", row(t, 1));
        assertEquals(1, t.cursorY);
    }

    @Test
    public void rowsThatNoLongerFitGoToScrollbackNotAway() {
        TerminalEmulator t = new TerminalEmulator(10, 4, null);
        out(t, "one\r\ntwo\r\nthree\r\n$ ");
        t.resize(10, 2);
        assertEquals(2, t.scrollbackSize());
        assertEquals("one", sb(t, 0));
        assertEquals("two", sb(t, 1));
        assertEquals("three", row(t, 0));
        assertEquals("$", row(t, 1));
        assertEquals(1, t.cursorY);
    }

    @Test
    public void hardNewlinesStaySeparate() {
        TerminalEmulator t = new TerminalEmulator(10, 4, null);
        out(t, "ab\r\ncd\r\n");
        t.resize(20, 4);
        assertEquals("ab", row(t, 0));
        assertEquals("cd", row(t, 1));
    }

    @Test
    public void primaryScreenSurvivesResizeWhileOnAltScreen() {
        TerminalEmulator t = new TerminalEmulator(10, 4, null);
        out(t, "keep me\r\n$ ");
        out(t, "\u001b[?1049h");  // full-screen app
        out(t, "vim stuff");
        t.resize(6, 4);
        t.resize(10, 4);
        out(t, "\u001b[?1049l");  // back to the shell
        assertEquals("keep me", row(t, 0));
        assertEquals("$", row(t, 1));
        assertEquals(1, t.cursorY);
        assertEquals(2, t.cursorX);
    }
}
