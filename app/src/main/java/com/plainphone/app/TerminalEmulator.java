package com.plainphone.app;

import java.nio.charset.StandardCharsets;

/**
 * A practical VT100 / xterm screen model — enough of the escape grammar for
 * {@code bash}, {@code vim}, {@code htop}, {@code tmux} and {@code less}: cursor
 * addressing, the erase ops, SGR colour (16 + 256 + 24-bit truecolor), a scroll region, insert /
 * delete lines and chars, and the alternate screen. Not spec-complete; anything
 * exotic is expected to run inside tmux.
 *
 * <p>Bytes in via {@link #feed}; the grid is read by {@link TerminalView}. One
 * cell = a char plus {@code fg}/{@code bg} palette indices ({@link #DEFAULT} for
 * "use the theme colour") and the bold / inverse flags.
 */
final class TerminalEmulator {

    static final int DEFAULT = -1;
    static final int FLAG_BOLD = 1;
    static final int FLAG_INVERSE = 2;

    static final int SCROLLBACK_MAX = 2000;

    interface Output {
        void write(byte[] bytes);
    }

    /** One line that has scrolled off the top of the primary screen. */
    static final class Line {
        final char[] g;
        final int[] f;
        final int[] b;
        final int[] fl;
        /** Soft-wrapped: the text continues on the next line (autowrap, not a newline). */
        final boolean wrapped;

        Line(char[] g, int[] f, int[] b, int[] fl, boolean wrapped) {
            this.g = g;
            this.f = f;
            this.b = b;
            this.fl = fl;
            this.wrapped = wrapped;
        }
    }

    private final java.util.ArrayList<Line> scrollback = new java.util.ArrayList<>();

    int cols;
    int rows;

    char[][] glyph;
    int[][] fg;
    int[][] bg;
    int[][] flags;
    /** Per screen row: soft-wrapped into the next row (see {@link Line#wrapped}). */
    private boolean[] wrapped;

    int cursorX;
    int cursorY;
    boolean cursorVisible = true;

    private final Output output;

    private char[][] altGlyph;
    private int[][] altFg;
    private int[][] altBg;
    private int[][] altFlags;
    private boolean[] altWrapped;
    private boolean onAlt;
    private int savedX, savedY;

    private int scrollTop;
    private int scrollBottom;
    private boolean autowrap = true;
    private boolean wrapPending;

    private int curFg = DEFAULT;
    private int curBg = DEFAULT;
    private int curFlags = 0;

    // parser
    private int parseState = GROUND;
    private final StringBuilder csi = new StringBuilder();
    private static final int GROUND = 0, ESC = 1, CSI = 2, OSC = 3, CHARSET = 4, OSC_ESC = 5;

    private final StringBuilder utf8 = new StringBuilder();
    private int utf8Remaining;
    private int utf8Value;

    /** Predictive local echo, drawn over this grid (never written into it). */
    final EchoPredictor predictor = new EchoPredictor(this);

    TerminalEmulator(int cols, int rows, Output output) {
        this.output = output;
        resize(cols, rows);
    }

    /**
     * Resize with reflow, like modern terminals (and what ConPTY expects: after
     * a resize it only repaints what it thinks changed, assuming the terminal
     * re-wrapped its own text). Soft-wrapped rows are joined back into logical
     * lines — scrollback included — and re-wrapped to the new width, so text
     * cut off by a narrow (zoomed-in) width comes back when it widens again.
     * The cursor stays on the same character; rows that no longer fit go to
     * scrollback rather than being dropped. The alternate screen (full-screen
     * apps redraw themselves on resize) is just cropped / padded, while the
     * primary screen saved behind it is reflowed.
     */
    synchronized void resize(int newCols, int newRows) {
        if (predictor != null) predictor.invalidate(); // null during the constructor's first resize
        newCols = Math.max(2, newCols);
        newRows = Math.max(2, newRows);
        if (glyph == null) {
            glyph = blankGlyph(newCols, newRows);
            fg = filled(newCols, newRows, DEFAULT);
            bg = filled(newCols, newRows, DEFAULT);
            flags = filled(newCols, newRows, 0);
            wrapped = new boolean[newRows];
        } else if (!onAlt) {
            Screen r = reflow(new Screen(glyph, fg, bg, flags, wrapped, cursorX, cursorY, cols, rows),
                    newCols, newRows);
            glyph = r.g; fg = r.f; bg = r.b; flags = r.fl; wrapped = r.wrap;
            cursorX = r.cx; cursorY = r.cy;
        } else {
            Screen r = reflow(new Screen(altGlyph, altFg, altBg, altFlags, altWrapped, savedX, savedY, cols, rows),
                    newCols, newRows);
            altGlyph = r.g; altFg = r.f; altBg = r.b; altFlags = r.fl; altWrapped = r.wrap;
            savedX = r.cx; savedY = r.cy;
            crop(newCols, newRows);
        }
        cols = newCols;
        rows = newRows;
        scrollTop = 0;
        scrollBottom = rows - 1;
        cursorX = Math.min(cursorX, cols - 1);
        cursorY = Math.min(cursorY, rows - 1);
        wrapPending = false;
    }

    /** Alternate screen resize: keep the top-left, pad / cut the rest. */
    private void crop(int newCols, int newRows) {
        char[][] g = blankGlyph(newCols, newRows);
        int[][] f = filled(newCols, newRows, DEFAULT);
        int[][] b = filled(newCols, newRows, DEFAULT);
        int[][] fl = filled(newCols, newRows, 0);
        for (int y = 0; y < Math.min(rows, newRows); y++) {
            int n = Math.min(Math.min(cols, newCols), glyph[y].length);
            System.arraycopy(glyph[y], 0, g[y], 0, n);
            System.arraycopy(fg[y], 0, f[y], 0, n);
            System.arraycopy(bg[y], 0, b[y], 0, n);
            System.arraycopy(flags[y], 0, fl[y], 0, n);
        }
        glyph = g; fg = f; bg = b; flags = fl;
        wrapped = new boolean[newRows];
    }

    /** A screen's rows plus its cursor, in or out of {@link #reflow}. */
    private static final class Screen {
        final char[][] g;
        final int[][] f, b, fl;
        final boolean[] wrap;
        final int cx, cy, cols, rows;

        Screen(char[][] g, int[][] f, int[][] b, int[][] fl, boolean[] wrap, int cx, int cy, int cols, int rows) {
            this.g = g; this.f = f; this.b = b; this.fl = fl; this.wrap = wrap;
            this.cx = cx; this.cy = cy; this.cols = cols; this.rows = rows;
        }
    }

    /** A logical line: the cells of one or more soft-wrapped rows, joined. */
    private static final class Logical {
        char[] g = new char[80];
        int[] f = new int[80], b = new int[80], fl = new int[80];
        int len;

        void add(char c, int fc, int bc, int flc) {
            if (len == g.length) {
                int n = len * 2;
                g = java.util.Arrays.copyOf(g, n);
                f = java.util.Arrays.copyOf(f, n);
                b = java.util.Arrays.copyOf(b, n);
                fl = java.util.Arrays.copyOf(fl, n);
            }
            g[len] = c; f[len] = fc; b[len] = bc; fl[len] = flc;
            len++;
        }
    }

    /** Re-wrap scrollback + {@code s} to {@code newCols}; replaces {@link #scrollback}. */
    private Screen reflow(Screen s, int newCols, int newRows) {
        // 1. physical rows -> logical lines
        java.util.ArrayList<Logical> lines = new java.util.ArrayList<>();
        Logical cur = null;
        int topLine = 0, topOff = 0, curLine = 0, curOff = 0;
        int total = scrollback.size() + s.rows;
        for (int i = 0; i < total; i++) {
            char[] g; int[] f, b, fl; boolean wrap;
            if (i < scrollback.size()) {
                Line ln = scrollback.get(i);
                g = ln.g; f = ln.f; b = ln.b; fl = ln.fl; wrap = ln.wrapped;
            } else {
                int y = i - scrollback.size();
                g = s.g[y]; f = s.f[y]; b = s.b[y]; fl = s.fl[y]; wrap = s.wrap[y];
            }
            if (cur == null) {
                cur = new Logical();
                lines.add(cur);
            }
            int y = i - scrollback.size();
            if (y == 0) { topLine = lines.size() - 1; topOff = cur.len; }
            if (y == s.cy) { curLine = lines.size() - 1; curOff = cur.len + s.cx; }
            int w = Math.min(s.cols, g.length);
            int n = w;
            if (!wrap) {
                while (n > 0 && (g[n - 1] == ' ' || g[n - 1] == 0) && b[n - 1] == DEFAULT && fl[n - 1] == 0) n--;
            }
            for (int x = 0; x < n; x++) cur.add(g[x], f[x], b[x], fl[x]);
            if (!wrap) cur = null;
        }
        // blank lines under the cursor are just unused screen, not content
        while (lines.size() - 1 > curLine && lines.get(lines.size() - 1).len == 0) lines.remove(lines.size() - 1);

        // 2. logical lines -> rows of newCols
        java.util.ArrayList<char[]> og = new java.util.ArrayList<>();
        java.util.ArrayList<int[]> of = new java.util.ArrayList<>(), ob = new java.util.ArrayList<>(),
                ofl = new java.util.ArrayList<>();
        java.util.ArrayList<Boolean> ow = new java.util.ArrayList<>();
        int topRow = 0, curRow = 0, curCol = 0;
        for (int li = 0; li < lines.size(); li++) {
            Logical ln = lines.get(li);
            int end = ln.len;
            if (li == curLine) end = Math.max(end, curOff + 1); // room for the cursor
            int row = -1, col = newCols; // col == newCols forces a new row first
            for (int k = 0; k < Math.max(end, 1); k++) {
                boolean wide = k + 1 < ln.len && ln.g[k + 1] == 0 && ln.g[k] != 0;
                if (col >= newCols || (wide && col == newCols - 1)) {
                    if (row >= 0) ow.set(ow.size() - 1, true); // previous row wraps into this one
                    og.add(blankRowG(newCols)); of.add(filledRow(newCols, DEFAULT));
                    ob.add(filledRow(newCols, DEFAULT)); ofl.add(filledRow(newCols, 0));
                    ow.add(false);
                    row = og.size() - 1;
                    col = 0;
                }
                if (li == topLine && k == topOff) topRow = row;
                if (li == curLine && k == curOff) { curRow = row; curCol = col; }
                if (k < ln.len) {
                    og.get(row)[col] = ln.g[k]; of.get(row)[col] = ln.f[k];
                    ob.get(row)[col] = ln.b[k]; ofl.get(row)[col] = ln.fl[k];
                }
                col++;
            }
            if (li == topLine && topOff >= Math.max(end, 1)) topRow = row;
        }

        // 3. split into scrollback + screen: the old screen's first line stays
        //    the screen's top unless the cursor would fall off the bottom
        int top = Math.max(0, Math.max(topRow, curRow - newRows + 1));
        scrollback.clear();
        for (int r = Math.max(0, top - SCROLLBACK_MAX); r < top; r++) {
            scrollback.add(new Line(og.get(r), of.get(r), ob.get(r), ofl.get(r), ow.get(r)));
        }
        char[][] g = blankGlyph(newCols, newRows);
        int[][] f = filled(newCols, newRows, DEFAULT);
        int[][] b = filled(newCols, newRows, DEFAULT);
        int[][] fl = filled(newCols, newRows, 0);
        boolean[] wrap = new boolean[newRows];
        for (int y = 0; y < newRows && top + y < og.size(); y++) {
            g[y] = og.get(top + y); f[y] = of.get(top + y); b[y] = ob.get(top + y);
            fl[y] = ofl.get(top + y); wrap[y] = ow.get(top + y);
        }
        return new Screen(g, f, b, fl, wrap, Math.min(curCol, newCols - 1),
                Math.max(0, Math.min(curRow - top, newRows - 1)), newCols, newRows);
    }

    private static char[] blankRowG(int cols) {
        char[] r = new char[cols];
        java.util.Arrays.fill(r, ' ');
        return r;
    }

    synchronized void feed(byte[] data, int len) {
        for (int i = 0; i < len; i++) {
            handleByte(data[i] & 0xff);
        }
        predictor.onOutput();
    }

    synchronized int scrollbackSize() {
        return scrollback.size();
    }

    synchronized Line scrollbackLine(int i) {
        return (i >= 0 && i < scrollback.size()) ? scrollback.get(i) : null;
    }

    boolean onAlt() {
        return onAlt;
    }

    // --- byte handling -------------------------------------------------

    private void handleByte(int b) {
        if (parseState == GROUND && utf8Remaining > 0) {
            if ((b & 0xc0) == 0x80) {
                utf8Value = (utf8Value << 6) | (b & 0x3f);
                if (--utf8Remaining == 0) putCodePoint(utf8Value);
                return;
            }
            utf8Remaining = 0; // malformed — drop the partial char, handle b normally
        }
        switch (parseState) {
            case GROUND: ground(b); break;
            case ESC: esc(b); break;
            case CSI: csiByte(b); break;
            case OSC: osc(b); break;
            case OSC_ESC: // ESC \ ends the string; any other ESC aborts it and starts a new escape
                if (b == '\\') parseState = GROUND; else { parseState = ESC; esc(b); }
                break;
            case CHARSET: parseState = GROUND; break; // consume the set designator
        }
    }

    private void ground(int b) {
        switch (b) {
            case 0x07: return;                       // BEL
            case 0x08: cursorX = Math.max(0, cursorX - 1); wrapPending = false; return; // BS
            case 0x09:                               // HT
                cursorX = Math.min(cols - 1, (cursorX / 8 + 1) * 8);
                return;
            case 0x0a: case 0x0b: case 0x0c:         // LF / VT / FF
                lineFeed();
                return;
            case 0x0d: cursorX = 0; wrapPending = false; return; // CR
            case 0x1b: parseState = ESC; csi.setLength(0); return;
            default:
                if (b < 0x20 || b == 0x7f) return;
                if (b < 0x80) {
                    putCodePoint(b);
                } else if ((b & 0xe0) == 0xc0) {
                    utf8Remaining = 1; utf8Value = b & 0x1f;
                } else if ((b & 0xf0) == 0xe0) {
                    utf8Remaining = 2; utf8Value = b & 0x0f;
                } else if ((b & 0xf8) == 0xf0) {
                    utf8Remaining = 3; utf8Value = b & 0x07;
                }
        }
    }

    private void esc(int b) {
        switch (b) {
            case '[': parseState = CSI; csi.setLength(0); return;
            // OSC, and DCS / SOS / PM / APC: swallow the payload up to BEL / ST
            // instead of printing it (tmux, zellij and ConPTY emit DCS / APC).
            case ']': case 'P': case 'X': case '^': case '_':
                parseState = OSC; csi.setLength(0); return;
            case '(': case ')': case '*': case '+': parseState = CHARSET; return;
            case 'M': reverseLineFeed(); parseState = GROUND; return;
            case '7': savedX = cursorX; savedY = cursorY; parseState = GROUND; return;
            case '8': cursorX = savedX; cursorY = savedY; parseState = GROUND; return;
            case 'c': fullReset(); parseState = GROUND; return;
            case '=': case '>': parseState = GROUND; return;
            default: parseState = GROUND;
        }
    }

    private void osc(int b) {
        if (b == 0x07) { parseState = GROUND; return; }        // BEL terminator
        if (b == 0x1b) { parseState = OSC_ESC; return; }       // ST = ESC \
        // ignore the payload (window title etc.)
    }

    private void csiByte(int b) {
        // ':' is the ITU T.416 sub-parameter separator some programs use for
        // truecolor SGR (e.g. "38:2::R:G:Bm" instead of "38;2;R;G;Bm") — treat
        // it like ';' rather than letting it prematurely terminate the sequence
        // and dump the rest of the escape as garbage onto the screen.
        // Parameter bytes (0x30-0x3F: digits ; : < = > ?) and intermediates
        // (0x20-0x2F) accumulate; only 0x40-0x7E ends the sequence. Anything
        // narrower lets e.g. "ESC[!p" or "ESC[<u" end early and dump the rest
        // of the escape onto the screen as text.
        if (b >= 0x20 && b <= 0x3f) {
            csi.append((char) b);
            return;
        }
        if (b == 0x1b) { parseState = ESC; csi.setLength(0); return; } // aborted sequence
        if (b < 0x20) { ground(b); return; } // C0 controls execute mid-sequence
        if (b <= 0x7e && !hasIntermediate()) dispatchCsi((char) b);
        parseState = GROUND;
    }

    /** Intermediate byte present ("ESC[!p", "ESC[2$p"...) — none of those
     *  are implemented, and dispatching them as the bare final byte would
     *  misfire (soft reset as 'p', etc.). DECSCUSR's "ESC[2 q" is harmless. */
    private boolean hasIntermediate() {
        for (int i = 0; i < csi.length(); i++) {
            if (csi.charAt(i) < 0x30) return true;
        }
        return false;
    }

    // --- CSI dispatch -------------------------------------------------

    private void dispatchCsi(char finalByte) {
        char lead = csi.length() > 0 ? csi.charAt(0) : 0;
        boolean priv = lead == '?';
        // "ESC[>...", "ESC[<...", "ESC[=..." are xterm / kitty extensions
        // (modifyOtherKeys, keyboard protocol...) — only DA2 is answered.
        if (lead == '<' || lead == '=' || (lead == '>' && finalByte != 'c')) return;
        String body = (priv || lead == '>') ? csi.substring(1) : csi.toString();
        int[] p = params(body);
        int p0 = p.length > 0 ? p[0] : 0;

        switch (finalByte) {
            case 'A': cursorY = clampY(cursorY - Math.max(1, p0)); wrapPending = false; break;
            case 'B': cursorY = clampY(cursorY + Math.max(1, p0)); wrapPending = false; break;
            case 'C': cursorX = clampX(cursorX + Math.max(1, p0)); wrapPending = false; break;
            case 'D': cursorX = clampX(cursorX - Math.max(1, p0)); wrapPending = false; break;
            case 'E': cursorX = 0; cursorY = clampY(cursorY + Math.max(1, p0)); break;
            case 'F': cursorX = 0; cursorY = clampY(cursorY - Math.max(1, p0)); break;
            case 'G': case '`': cursorX = clampX((p0 == 0 ? 1 : p0) - 1); wrapPending = false; break;
            case 'd': cursorY = clampY((p0 == 0 ? 1 : p0) - 1); wrapPending = false; break;
            case 'H': case 'f': {
                int row = (p.length > 0 && p[0] > 0 ? p[0] : 1) - 1;
                int col = (p.length > 1 && p[1] > 0 ? p[1] : 1) - 1;
                cursorY = clampY(row);
                cursorX = clampX(col);
                wrapPending = false;
                break;
            }
            case 'J': eraseDisplay(p0); break;
            case 'K': eraseLine(p0); break;
            case 'm': applySgr(p); break;
            case 'r':
                scrollTop = (p.length > 0 && p[0] > 0 ? p[0] : 1) - 1;
                scrollBottom = (p.length > 1 && p[1] > 0 ? p[1] : rows) - 1;
                scrollTop = clampY(scrollTop);
                scrollBottom = clampY(scrollBottom);
                cursorX = 0; cursorY = scrollTop;
                break;
            case 'S': scrollUp(Math.max(1, p0)); break;
            case 'T': scrollDown(Math.max(1, p0)); break;
            case 'L': insertLines(Math.max(1, p0)); break;
            case 'M': deleteLines(Math.max(1, p0)); break;
            case '@': insertChars(Math.max(1, p0)); break;
            case 'P': deleteChars(Math.max(1, p0)); break;
            case 'X': eraseChars(Math.max(1, p0)); break;
            case 'h': setMode(body, true); break;
            case 'l': setMode(body, false); break;
            case 's': savedX = cursorX; savedY = cursorY; break;
            case 'u': cursorX = savedX; cursorY = savedY; break;
            case 'n':
                if (output == null) break;
                if (p0 == 6) {
                    output.write(("[" + (cursorY + 1) + ";" + (cursorX + 1) + "R")
                            .getBytes(StandardCharsets.US_ASCII));
                } else if (p0 == 5) {
                    output.write("[0n".getBytes(StandardCharsets.US_ASCII));
                }
                break;
            case 'c':
                // Capability queries: Zellij/tmux probe this on startup and
                // misbehave without a plausible answer.
                if (output != null) {
                    boolean secondary = csi.length() > 0 && csi.charAt(0) == '>';
                    output.write((secondary ? "[>0;95;0c" : "[?1;2c")
                            .getBytes(StandardCharsets.US_ASCII));
                }
                break;
            default: // unhandled — ignore
        }
    }

    private void setMode(String body, boolean on) {
        boolean priv = csi.length() > 0 && csi.charAt(0) == '?';
        for (int code : params(body)) {
            if (priv) {
                switch (code) {
                    case 25: cursorVisible = on; break;
                    case 7: autowrap = on; break;
                    case 1049: case 47: case 1047: switchAlt(on); break;
                    default: // 1 (app cursor keys), 2004 (bracketed paste) — ignored
                }
            }
        }
    }

    // --- screen ops -------------------------------------------------

    private void putCodePoint(int cp) {
        if (isZeroWidth(cp)) return; // combining marks, ZWJ, variation selectors
        if (wrapPending && autowrap) {
            wrapped[cursorY] = true;
            cursorX = 0;
            lineFeed();
            wrapPending = false;
        }
        int width = isWide(cp) ? 2 : 1;
        if (width == 2 && cursorX == cols - 1) {
            // doesn't fit in the last column — wrap first, like autowrap does
            // for a normal char; with autowrap off there's nowhere to put the
            // second half, so degrade to narrow rather than overflow the row.
            if (autowrap) {
                wrapped[cursorY] = true;
                cursorX = 0;
                lineFeed();
            } else {
                width = 1;
            }
        }
        char c = cp > 0xffff ? '?' : (char) cp;
        glyph[cursorY][cursorX] = c;
        fg[cursorY][cursorX] = curFg;
        bg[cursorY][cursorX] = curBg;
        flags[cursorY][cursorX] = curFlags;
        if (width == 2) {
            // Continuation cell: glyph 0 is TerminalView's "don't draw" sentinel,
            // but its bg still paints so the wide glyph's background stays solid.
            glyph[cursorY][cursorX + 1] = 0;
            fg[cursorY][cursorX + 1] = curFg;
            bg[cursorY][cursorX + 1] = curBg;
            flags[cursorY][cursorX + 1] = curFlags;
        }
        if (cursorX + width >= cols) {
            cursorX = cols - 1;
            wrapPending = true;
        } else {
            cursorX += width;
        }
    }

    private static boolean isZeroWidth(int cp) {
        return (cp >= 0x0300 && cp <= 0x036F)      // combining diacritics
                || (cp >= 0x200B && cp <= 0x200F)   // ZW space / joiners / marks
                || (cp >= 0xFE00 && cp <= 0xFE0F)   // variation selectors (emoji VS16)
                || (cp >= 0x1F3FB && cp <= 0x1F3FF); // skin-tone modifiers
    }

    /** Coarse East-Asian "Wide"/"Fullwidth" ranges, plus common emoji blocks —
     *  not a complete Unicode width table, but enough that CJK text and emoji
     *  in prompts/status bars (Zellij's, tmux's) don't desync column counts. */
    private static boolean isWide(int cp) {
        return (cp >= 0x1100 && cp <= 0x115F)     // Hangul Jamo
                || (cp >= 0x2E80 && cp <= 0xA4CF && cp != 0x303F) // CJK / Kana / etc.
                || (cp >= 0xAC00 && cp <= 0xD7A3)  // Hangul syllables
                || (cp >= 0xF900 && cp <= 0xFAFF)  // CJK compatibility ideographs
                || (cp >= 0xFF00 && cp <= 0xFF60)  // Fullwidth forms
                || (cp >= 0xFFE0 && cp <= 0xFFE6)
                || (cp >= 0x1F300 && cp <= 0x1FAFF) // emoji blocks
                || (cp >= 0x20000 && cp <= 0x3FFFD); // CJK extension planes
    }

    boolean wrapPending() {
        return wrapPending;
    }

    private void lineFeed() {
        if (cursorY == scrollBottom) {
            scrollUp(1);
        } else if (cursorY < rows - 1) {
            cursorY++;
        }
    }

    private void reverseLineFeed() {
        if (cursorY == scrollTop) {
            scrollDown(1);
        } else if (cursorY > 0) {
            cursorY--;
        }
    }

    private void scrollUp(int n) {
        predictor.invalidate();
        for (int k = 0; k < n; k++) {
            if (!onAlt && scrollTop == 0) {
                scrollback.add(new Line(glyph[0], fg[0], bg[0], flags[0], wrapped[0]));
                if (scrollback.size() > SCROLLBACK_MAX) scrollback.remove(0);
            }
            for (int y = scrollTop; y < scrollBottom; y++) {
                moveRow(y + 1, y);
            }
            blankRow(scrollBottom);
        }
    }

    private void scrollDown(int n) {
        predictor.invalidate();
        for (int k = 0; k < n; k++) {
            for (int y = scrollBottom; y > scrollTop; y--) {
                moveRow(y - 1, y);
            }
            blankRow(scrollTop);
        }
    }

    private void insertLines(int n) {
        if (cursorY < scrollTop || cursorY > scrollBottom) return;
        predictor.invalidate();
        for (int k = 0; k < n; k++) {
            for (int y = scrollBottom; y > cursorY; y--) {
                moveRow(y - 1, y);
            }
            blankRow(cursorY);
        }
    }

    private void deleteLines(int n) {
        if (cursorY < scrollTop || cursorY > scrollBottom) return;
        predictor.invalidate();
        for (int k = 0; k < n; k++) {
            for (int y = cursorY; y < scrollBottom; y++) {
                moveRow(y + 1, y);
            }
            blankRow(scrollBottom);
        }
    }

    private void insertChars(int n) {
        for (int x = cols - 1; x >= cursorX + n; x--) {
            copyCell(cursorY, x - n, cursorY, x);
        }
        for (int x = cursorX; x < Math.min(cols, cursorX + n); x++) blankCell(cursorY, x);
    }

    private void deleteChars(int n) {
        for (int x = cursorX; x < cols - n; x++) {
            copyCell(cursorY, x + n, cursorY, x);
        }
        for (int x = Math.max(cursorX, cols - n); x < cols; x++) blankCell(cursorY, x);
    }

    private void eraseChars(int n) {
        for (int x = cursorX; x < Math.min(cols, cursorX + n); x++) blankCell(cursorY, x);
    }

    private void eraseDisplay(int mode) {
        if (mode == 0) {
            wrapped[cursorY] = false;
            for (int x = cursorX; x < cols; x++) blankCell(cursorY, x);
            for (int y = cursorY + 1; y < rows; y++) blankRow(y);
        } else if (mode == 1) {
            for (int y = 0; y < cursorY; y++) blankRow(y);
            for (int x = 0; x <= cursorX && x < cols; x++) blankCell(cursorY, x);
        } else {
            for (int y = 0; y < rows; y++) blankRow(y);
        }
    }

    private void eraseLine(int mode) {
        if (mode == 0) {
            wrapped[cursorY] = false;
            for (int x = cursorX; x < cols; x++) blankCell(cursorY, x);
        } else if (mode == 1) {
            for (int x = 0; x <= cursorX && x < cols; x++) blankCell(cursorY, x);
        } else {
            blankRow(cursorY);
        }
    }

    private void applySgr(int[] p) {
        if (p.length == 0) {
            curFg = DEFAULT; curBg = DEFAULT; curFlags = 0;
            return;
        }
        for (int i = 0; i < p.length; i++) {
            int code = p[i];
            if (code == 0) { curFg = DEFAULT; curBg = DEFAULT; curFlags = 0; }
            else if (code == 1) curFlags |= FLAG_BOLD;
            else if (code == 22) curFlags &= ~FLAG_BOLD;
            else if (code == 7) curFlags |= FLAG_INVERSE;
            else if (code == 27) curFlags &= ~FLAG_INVERSE;
            else if (code >= 30 && code <= 37) curFg = code - 30;
            else if (code == 39) curFg = DEFAULT;
            else if (code >= 40 && code <= 47) curBg = code - 40;
            else if (code == 49) curBg = DEFAULT;
            else if (code >= 90 && code <= 97) curFg = code - 90 + 8;
            else if (code >= 100 && code <= 107) curBg = code - 100 + 8;
            else if ((code == 38 || code == 48) && i + 2 < p.length && p[i + 1] == 5) {
                int idx = p[i + 2];
                if (code == 38) curFg = idx; else curBg = idx;
                i += 2;
            } else if ((code == 38 || code == 48) && i + 4 < p.length && p[i + 1] == 2) {
                // 24-bit truecolor: pack as opaque ARGB so it's distinguishable
                // from a 0..255 palette index (which never has the high byte set).
                int rgb = 0xFF000000 | ((p[i + 2] & 0xFF) << 16)
                        | ((p[i + 3] & 0xFF) << 8) | (p[i + 4] & 0xFF);
                if (code == 38) curFg = rgb; else curBg = rgb;
                i += 4;
            }
        }
    }

    private void switchAlt(boolean on) {
        if (on == onAlt) return;
        predictor.invalidate();
        if (on) {
            altGlyph = glyph; altFg = fg; altBg = bg; altFlags = flags; altWrapped = wrapped;
            wrapped = new boolean[rows];
            glyph = blankGlyph(cols, rows);
            fg = filled(cols, rows, DEFAULT);
            bg = filled(cols, rows, DEFAULT);
            flags = filled(cols, rows, 0);
            savedX = cursorX; savedY = cursorY;
            cursorX = 0; cursorY = 0;
            onAlt = true;
        } else {
            glyph = altGlyph; fg = altFg; bg = altBg; flags = altFlags; wrapped = altWrapped;
            cursorX = savedX; cursorY = savedY;
            onAlt = false;
        }
        scrollTop = 0;
        scrollBottom = rows - 1;
    }

    /** Wipe the screen, scrollback and parser — for reattaching to a session. */
    synchronized void reset() {
        predictor.invalidate();
        onAlt = false;
        scrollback.clear();
        wrapped = new boolean[rows];
        glyph = blankGlyph(cols, rows);
        fg = filled(cols, rows, DEFAULT);
        bg = filled(cols, rows, DEFAULT);
        flags = filled(cols, rows, 0);
        parseState = GROUND;
        csi.setLength(0);
        fullReset();
    }

    private void fullReset() {
        curFg = DEFAULT; curBg = DEFAULT; curFlags = 0;
        cursorX = 0; cursorY = 0;
        scrollTop = 0; scrollBottom = rows - 1;
        autowrap = true; cursorVisible = true;
        for (int y = 0; y < rows; y++) blankRow(y);
    }

    // --- helpers ----------------------------------------------------

    private int[] params(String body) {
        if (body.isEmpty()) return new int[0];
        String[] parts = body.split("[;:]", -1);
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    private int clampX(int x) { return Math.max(0, Math.min(cols - 1, x)); }
    private int clampY(int y) { return Math.max(0, Math.min(rows - 1, y)); }

    private void copyCell(int sy, int sx, int dy, int dx) {
        glyph[dy][dx] = glyph[sy][sx];
        fg[dy][dx] = fg[sy][sx];
        bg[dy][dx] = bg[sy][sx];
        flags[dy][dx] = flags[sy][sx];
    }

    private void blankCell(int y, int x) {
        glyph[y][x] = ' ';
        fg[y][x] = DEFAULT;
        bg[y][x] = curBg;
        flags[y][x] = 0;
    }

    private void moveRow(int from, int to) {
        glyph[to] = glyph[from]; fg[to] = fg[from]; bg[to] = bg[from]; flags[to] = flags[from];
        wrapped[to] = wrapped[from];
    }

    private void blankRow(int y) {
        wrapped[y] = false;
        glyph[y] = new char[cols];
        java.util.Arrays.fill(glyph[y], ' ');
        fg[y] = filledRow(cols, DEFAULT);
        bg[y] = filledRow(cols, curBg);
        flags[y] = filledRow(cols, 0);
    }

    private static char[][] blankGlyph(int cols, int rows) {
        char[][] g = new char[rows][cols];
        for (char[] row : g) java.util.Arrays.fill(row, ' ');
        return g;
    }

    private static int[][] filled(int cols, int rows, int value) {
        int[][] a = new int[rows][cols];
        if (value != 0) for (int[] row : a) java.util.Arrays.fill(row, value);
        return a;
    }

    private static int[] filledRow(int cols, int value) {
        int[] a = new int[cols];
        if (value != 0) java.util.Arrays.fill(a, value);
        return a;
    }
}
