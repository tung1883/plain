package com.plainphone.app;

import android.os.SystemClock;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Mosh-style predictive local echo over a {@link TerminalEmulator}.
 *
 * <p>Every outbound keystroke batch carries a sequence number; the daemon
 * reports an <b>echo ack</b> ("input up to N was fed to the shell at least
 * 50 ms before this output") so a guess is judged only against a screen that
 * should already show its echo — pending until then, never "wrong" just
 * because the echo is still in flight.
 *
 * <p>Guesses are an overlay (cells + a predicted cursor) and never touch the
 * grid. Predicted: printable chars (inserted, pushing the rest of the line
 * right), backspace (pulling it left), and ←/→. Anything else — Enter, Tab,
 * control keys, pastes — can't be predicted, so the cursor prediction stops
 * there and the next guesses start a new <b>epoch</b>.
 *
 * <p>An epoch's guesses are shown only once one of them is confirmed, except
 * that a new epoch starting from a <b>settled</b> screen (all input acked, no
 * output for a moment, so the real cursor is authoritative) is trusted
 * straight away — unless the line looks like a password prompt, or the last
 * judged guess was wrong (echo may be off). The first wrong guess drops every
 * guess and starts a new, untrusted epoch.
 *
 * <p>Main thread only, like the emulator's feed.
 */
final class EchoPredictor {

    static final class Cell {
        final int y, x;
        final char ch;
        final long seq;
        final int epoch;
        /** Moved aside by an insert / delete rather than typed: shown, but it
         *  never counts as a hit or a miss (e.g. PowerShell's grey inline
         *  suggestion changes under it on every key). */
        final boolean weak;
        final long at;

        Cell(int y, int x, char ch, long seq, int epoch, boolean weak, long at) {
            this.y = y; this.x = x; this.ch = ch; this.seq = seq; this.epoch = epoch; this.weak = weak;
            this.at = at;
        }
    }

    /** Quiet time after the last output before the screen counts as settled. */
    private static final long SETTLE_MS = 150;
    /** A guess older than this with no ack (link stalled / old daemon) is dropped. */
    private static final long STALE_MS = 2500;
    /** A shown guess pending longer than this gets underlined (mosh's "glitch"). */
    static final long GLITCH_MS = 250;

    private final TerminalEmulator term;
    /** Swappable for unit tests (no Android clock there). */
    java.util.function.LongSupplier clock = SystemClock::uptimeMillis;
    private final List<Cell> cells = new ArrayList<>();

    private boolean cursorActive;
    private int predY, predX;
    private long cursorSeq;
    private int cursorEpoch;

    private int epoch = 1;
    private int confirmedEpoch;
    /** A guess was wrong and none confirmed since: no eager trust (echo may be off). */
    private boolean missed;

    private long lastInputSeq;
    private long ackSeq;
    private long lastOutputAt;

    /** Off when the daemon can't ack echoes — guesses would never be judged. */
    boolean enabled;

    EchoPredictor(TerminalEmulator term) {
        this.term = term;
    }

    // --- input ------------------------------------------------------------

    /** Keystrokes about to go out as {@code seq}. Returns true if every byte
     *  was predicted and the result is visible (for the instant-echo metric). */
    boolean onInput(byte[] data, long seq) {
        lastInputSeq = seq;
        if (!enabled) return false;
        if (term.onAlt()) {
            invalidate();
            return false;
        }
        if (data.length == 1 && (data[0] == 0x7f || data[0] == 0x08)) return backspace(seq);
        if (isCsi(data, 'D')) return left(seq);
        if (isCsi(data, 'C')) return right(seq);
        String s = new String(data, StandardCharsets.UTF_8);
        if (s.codePointCount(0, s.length()) == 1) {
            int cp = s.codePointAt(0);
            if (cp >= 0x20 && cp != 0x7f && cp <= 0xffff) return type((char) cp, seq);
        }
        // Enter, Tab, control keys, escape sequences, pastes, IME multi-char
        // commits: the cursor's next position is anyone's guess.
        becomeTentative();
        return false;
    }

    private boolean type(char c, long seq) {
        if (!begin()) return false;
        if (predX >= term.cols - 1 || isWideOrZero(c)) { // no wrap prediction
            becomeTentative();
            return false;
        }
        int end = Math.min(lastUsed(predY) + 1, term.cols - 1);
        for (int x = end; x > predX; x--) put(predY, x, shown(predY, x - 1), seq, true); // insert
        put(predY, predX, c, seq, false);
        moveCursor(predX + 1, seq);
        return visible();
    }

    private boolean backspace(long seq) {
        if (!begin()) return false;
        if (predX == 0) {
            becomeTentative();
            return false;
        }
        moveCursor(predX - 1, seq);
        int last = lastUsed(predY);
        for (int x = predX; x < last; x++) put(predY, x, shown(predY, x + 1), seq, true); // delete
        if (last >= predX) put(predY, last, ' ', seq, true);
        return visible();
    }

    private boolean left(long seq) {
        if (!begin()) return false;
        if (predX == 0) {
            becomeTentative();
            return false;
        }
        moveCursor(predX - 1, seq);
        return visible();
    }

    private boolean right(long seq) {
        if (!begin()) return false;
        // shells don't move past the end of the typed text
        if (predX <= lastUsed(predY)) moveCursor(predX + 1, seq);
        else moveCursor(predX, seq);
        return visible();
    }

    /** Make sure a cursor prediction exists; start one (and an epoch) from the real cursor. */
    private boolean begin() {
        if (cursorActive) return true;
        if (term.wrapPending()) return false;
        predY = term.cursorY;
        predX = term.cursorX;
        cursorActive = true;
        cursorSeq = lastInputSeq - 1;
        epoch++;
        if (!missed && settled() && !passwordPrompt(predY)) confirmedEpoch = epoch;
        cursorEpoch = epoch;
        return true;
    }

    private void moveCursor(int x, long seq) {
        predX = x;
        cursorSeq = seq;
        cursorEpoch = epoch;
    }

    private void becomeTentative() {
        cursorActive = false;
        epoch++;
    }

    private boolean visible() {
        return epoch <= confirmedEpoch;
    }

    private void put(int y, int x, char ch, long seq, boolean weak) {
        for (Iterator<Cell> it = cells.iterator(); it.hasNext(); ) {
            Cell c = it.next();
            if (c.y == y && c.x == x) it.remove();
        }
        cells.add(new Cell(y, x, ch, seq, epoch, weak, clock.getAsLong()));
    }

    // --- what the user sees ------------------------------------------------

    /** Cell as currently displayed: newest guess there, else the grid. */
    private char shown(int y, int x) {
        for (int i = cells.size() - 1; i >= 0; i--) {
            Cell c = cells.get(i);
            if (c.y == y && c.x == x) return c.ch;
        }
        char g = term.glyph[y][x];
        return g == 0 ? ' ' : g;
    }

    private int lastUsed(int y) {
        for (int x = term.cols - 1; x >= 0; x--) if (shown(y, x) != ' ') return x;
        return -1;
    }

    /** Guesses to draw (confirmed epochs only). */
    List<Cell> visibleCells() {
        List<Cell> out = new ArrayList<>();
        for (Cell c : cells) if (c.epoch <= confirmedEpoch) out.add(c);
        return out;
    }

    /** Predicted cursor {y, x} to draw instead of the real one, or null. */
    int[] visibleCursor() {
        if (!cursorActive || cursorEpoch > confirmedEpoch || cursorSeq <= ackSeq) return null;
        return new int[]{predY, predX};
    }

    boolean pending() {
        return !cells.isEmpty() || (cursorActive && cursorSeq > ackSeq);
    }

    // --- judging -------------------------------------------------------------

    void onOutput() {
        lastOutputAt = clock.getAsLong();
    }

    /** Echo ack from the daemon: judge every guess it covers. */
    void onAck(long seq) {
        if (seq <= ackSeq) return;
        ackSeq = seq;
        judge();
    }

    private void judge() {
        for (Iterator<Cell> it = cells.iterator(); it.hasNext(); ) {
            Cell c = it.next();
            if (c.seq > ackSeq) continue; // its echo may still be on the way
            if (c.y >= term.rows || c.x >= term.cols) {
                wrong();
                return;
            }
            char real = term.glyph[c.y][c.x];
            if (real == 0) real = ' ';
            if (c.weak || c.ch == ' ') {
                it.remove(); // proves nothing either way
            } else if (real == c.ch) {
                it.remove();
                if (c.epoch > confirmedEpoch) confirmedEpoch = c.epoch;
                missed = false;
            } else {
                wrong();
                return;
            }
        }
        // Everything typed so far is acked: the real cursor must be where we
        // said, else the shell did something we didn't model.
        if (cursorActive && ackSeq >= lastInputSeq && cursorSeq <= ackSeq
                && (term.cursorY != predY || term.cursorX != predX)) {
            wrong();
        }
    }

    private void wrong() {
        LatencyStats.record("shell.mispredict", 1);
        missed = true;
        invalidate();
    }

    /** Drop every guess; the next ones start an untrusted epoch. */
    void invalidate() {
        cells.clear();
        becomeTentative();
    }

    /** Periodic: drop guesses that never got an ack. Returns true if anything changed. */
    boolean expire() {
        long now = clock.getAsLong();
        for (Cell c : cells) {
            if (now - c.at > STALE_MS) {
                invalidate();
                return true;
            }
        }
        return false;
    }

    /** Any shown guess waiting longer than {@link #GLITCH_MS}? */
    boolean glitching() {
        long now = clock.getAsLong();
        for (Cell c : cells) if (c.epoch <= confirmedEpoch && now - c.at > GLITCH_MS) return true;
        return false;
    }

    // --- helpers -----------------------------------------------------------

    /** All input acked and no output for a moment: the real cursor is where the next char lands. */
    private boolean settled() {
        return ackSeq >= lastInputSeq - 1 // the keystroke being handled is lastInputSeq itself
                && clock.getAsLong() - lastOutputAt >= SETTLE_MS;
    }

    /** Don't flash a guess where echo is probably off. */
    private boolean passwordPrompt(int y) {
        String line = new String(term.glyph[y]).toLowerCase(java.util.Locale.ROOT);
        return line.contains("password") || line.contains("passphrase") || line.contains("passcode")
                || line.contains("pin:") || line.contains("secret");
    }

    private static boolean isCsi(byte[] d, char fin) {
        return d.length == 3 && d[0] == 0x1b && (d[1] == '[' || d[1] == 'O') && d[2] == fin;
    }

    private static boolean isWideOrZero(char c) {
        return (c >= 0x1100 && c <= 0x115F) || (c >= 0x2E80 && c <= 0xA4CF) || (c >= 0xAC00 && c <= 0xD7A3)
                || (c >= 0xF900 && c <= 0xFAFF) || (c >= 0xFF00 && c <= 0xFF60) || (c >= 0xFFE0 && c <= 0xFFE6)
                || (c >= 0x0300 && c <= 0x036F) || (c >= 0x200B && c <= 0x200F) || (c >= 0xFE00 && c <= 0xFE0F);
    }
}
