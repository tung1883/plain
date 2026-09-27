package com.plainphone.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class EchoPredictorTest {

    private TerminalEmulator term;
    private EchoPredictor p;
    private long now = 10_000;
    private long seq;

    @Before
    public void setUp() {
        term = new TerminalEmulator(40, 5, null);
        p = term.predictor;
        p.clock = () -> now;
        p.enabled = true;
    }

    private void out(String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        term.feed(b, b.length);
    }

    private boolean key(String s) {
        return p.onInput(s.getBytes(StandardCharsets.UTF_8), ++seq);
    }

    private String shownRow(int y) {
        char[] row = term.glyph[y].clone();
        for (EchoPredictor.Cell c : p.visibleCells()) if (c.y == y) row[c.x] = c.ch;
        return new String(row).replaceAll(" +$", "");
    }

    @Test
    public void typingAtASettledPromptShowsAtOnce() {
        out("$ ");
        now += 500;
        assertTrue(key("l"));
        assertTrue(key("s"));
        assertEquals("$ ls", shownRow(0));
        assertArrayEquals(new int[]{0, 4}, p.visibleCursor());
    }

    @Test
    public void echoPlusAckConfirmsAndClears() {
        out("$ ");
        now += 500;
        key("l");
        out("l");
        p.onAck(seq);
        assertTrue(p.visibleCells().isEmpty());
        assertFalse(p.pending());
        assertTrue(key("s")); // same epoch, still trusted
    }

    @Test
    public void guessIsPendingUntilAckedEvenIfEchoIsLate() {
        out("$ ");
        now += 500;
        key("a");
        key("b");
        out("a");     // only the first echo has arrived...
        p.onAck(1);   // ...and only the first input is acked
        assertEquals("$ ab", shownRow(0)); // 'b' still shown, not judged wrong
    }

    @Test
    public void noEchoAtAllIsWrongOnceAcked() {
        out("$ ");
        now += 500;
        key("a");
        p.onAck(seq); // shell answered, but printed nothing
        assertTrue(p.visibleCells().isEmpty());
        assertFalse(key("b")); // new epoch starts untrusted (just saw a miss)
    }

    @Test
    public void busyScreenStartsTentativeUntilConfirmed() {
        out("$ ");        // output just now: not settled
        assertFalse(key("x"));
        assertTrue(p.visibleCells().isEmpty());
        out("x");
        p.onAck(seq);     // confirmed: the epoch becomes trusted
        assertTrue(key("y"));
    }

    @Test
    public void passwordPromptIsNeverGuessedEagerly() {
        out("Password: ");
        now += 500;
        assertFalse(key("h"));
        assertTrue(p.visibleCells().isEmpty());
    }

    @Test
    public void backspaceAndInsertInsideTheLine() {
        out("$ abc\b\b"); // cursor on 'b'
        now += 500;
        assertTrue(key("X"));
        assertEquals("$ aXbc", shownRow(0));
        assertTrue(key("\u007f"));
        assertEquals("$ abc", shownRow(0));
        assertArrayEquals(new int[]{0, 3}, p.visibleCursor());
    }

    @Test
    public void shiftedTextNeverCountsAsAMiss() {
        out("$ ab\b"); // cursor on 'b'
        now += 500;
        key("X");                    // predicts "aXb"
        out("X  ");                  // shell redrew the tail differently (e.g. grey suggestion)
        out("\b\b");                 // cursor back after 'X'
        p.onAck(seq);
        assertTrue(p.visibleCells().isEmpty());
        assertTrue(key("Y"));        // still trusted: the typed 'X' itself was right
    }

    @Test
    public void enterEndsTheCursorPrediction() {
        out("$ ");
        now += 500;
        key("l");
        assertFalse(key("\r"));
        assertNull(p.visibleCursor());
    }

    @Test
    public void arrowsMoveThePredictedCursor() {
        out("$ abc");
        now += 500;
        assertTrue(key("\u001b[D"));
        assertArrayEquals(new int[]{0, 4}, p.visibleCursor());
        assertTrue(key("\u001b[C"));
        assertTrue(key("\u001b[C")); // already at the end: stays put
        assertArrayEquals(new int[]{0, 5}, p.visibleCursor());
    }

    @Test
    public void unackedGuessesExpire() {
        out("$ ");
        now += 500;
        key("a");
        now += 3000;
        assertTrue(p.expire());
        List<EchoPredictor.Cell> left = p.visibleCells();
        assertTrue(left.isEmpty());
    }
}
