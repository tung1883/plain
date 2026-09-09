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

        Line(char[] g, int[] f, int[] b, int[] fl) {
            this.g = g;
            this.f = f;
            this.b = b;
            this.fl = fl;
        }
    }

    private final java.util.ArrayList<Line> scrollback = new java.util.ArrayList<>();

    int cols;
    int rows;

    char[][] glyph;
    int[][] fg;
    int[][] bg;
    int[][] flags;

    int cursorX;
    int cursorY;
    boolean cursorVisible = true;

    private final Output output;

    private char[][] altGlyph;
    private int[][] altFg;
    private int[][] altBg;
    private int[][] altFlags;
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
    private static final int GROUND = 0, ESC = 1, CSI = 2, OSC = 3, CHARSET = 4;

    private final StringBuilder utf8 = new StringBuilder();
    private int utf8Remaining;
    private int utf8Value;

    TerminalEmulator(int cols, int rows, Output output) {
        this.output = output;
        resize(cols, rows);
    }

    synchronized void resize(int newCols, int newRows) {
        newCols = Math.max(2, newCols);
        newRows = Math.max(2, newRows);
        char[][] g = blankGlyph(newCols, newRows);
        int[][] f = filled(newCols, newRows, DEFAULT);
        int[][] b = filled(newCols, newRows, DEFAULT);
        int[][] fl = filled(newCols, newRows, 0);
        if (glyph != null) {
            for (int y = 0; y < Math.min(rows, newRows); y++) {
                for (int x = 0; x < Math.min(cols, newCols); x++) {
                    g[y][x] = glyph[y][x];
                    f[y][x] = fg[y][x];
                    b[y][x] = bg[y][x];
                    fl[y][x] = flags[y][x];
                }
            }
        }
        glyph = g; fg = f; bg = b; flags = fl;
        cols = newCols;
        rows = newRows;
        scrollTop = 0;
        scrollBottom = rows - 1;
        cursorX = Math.min(cursorX, cols - 1);
        cursorY = Math.min(cursorY, rows - 1);
        if (onAlt) {
            altGlyph = blankGlyph(cols, rows);
            altFg = filled(cols, rows, DEFAULT);
            altBg = filled(cols, rows, DEFAULT);
            altFlags = filled(cols, rows, 0);
        }
    }

    synchronized void feed(byte[] data, int len) {
        for (int i = 0; i < len; i++) {
            handleByte(data[i] & 0xff);
        }
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
            utf8Remaining = 0; // malformed — fall through
        }
        switch (parseState) {
            case GROUND: ground(b); break;
            case ESC: esc(b); break;
            case CSI: csiByte(b); break;
            case OSC: osc(b); break;
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
                if (b < 0x20) return;
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
            case ']': parseState = OSC; csi.setLength(0); return;
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
        if (b == 0x1b) { parseState = ESC; return; }           // ST starts with ESC \
        if (b == '\\' && parseState == ESC) { parseState = GROUND; return; }
        // ignore the payload (window title etc.)
    }

    private void csiByte(int b) {
        if ((b >= '0' && b <= '9') || b == ';' || b == '?' || b == ' ' || b == '>') {
            csi.append((char) b);
            return;
        }
        dispatchCsi((char) b);
        parseState = GROUND;
    }

    // --- CSI dispatch -------------------------------------------------

    private void dispatchCsi(char finalByte) {
        boolean priv = csi.length() > 0 && csi.charAt(0) == '?';
        String body = priv ? csi.substring(1) : csi.toString();
        int[] p = params(body);
        int p0 = p.length > 0 ? p[0] : 0;

        switch (finalByte) {
            case 'A': cursorY = clampY(cursorY - Math.max(1, p0)); break;
            case 'B': cursorY = clampY(cursorY + Math.max(1, p0)); break;
            case 'C': cursorX = clampX(cursorX + Math.max(1, p0)); wrapPending = false; break;
            case 'D': cursorX = clampX(cursorX - Math.max(1, p0)); wrapPending = false; break;
            case 'E': cursorX = 0; cursorY = clampY(cursorY + Math.max(1, p0)); break;
            case 'F': cursorX = 0; cursorY = clampY(cursorY - Math.max(1, p0)); break;
            case 'G': case '`': cursorX = clampX((p0 == 0 ? 1 : p0) - 1); wrapPending = false; break;
            case 'd': cursorY = clampY((p0 == 0 ? 1 : p0) - 1); break;
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
                if (p0 == 6 && output != null) {
                    output.write(("[" + (cursorY + 1) + ";" + (cursorX + 1) + "R")
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
        if (wrapPending && autowrap) {
            cursorX = 0;
            lineFeed();
            wrapPending = false;
        }
        char c = cp > 0xffff ? '?' : (char) cp;
        glyph[cursorY][cursorX] = c;
        fg[cursorY][cursorX] = curFg;
        bg[cursorY][cursorX] = curBg;
        flags[cursorY][cursorX] = curFlags;
        if (cursorX == cols - 1) {
            wrapPending = true;
        } else {
            cursorX++;
        }
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
        for (int k = 0; k < n; k++) {
            if (!onAlt && scrollTop == 0) {
                scrollback.add(new Line(glyph[0], fg[0], bg[0], flags[0]));
                if (scrollback.size() > SCROLLBACK_MAX) scrollback.remove(0);
            }
            for (int y = scrollTop; y < scrollBottom; y++) {
                glyph[y] = glyph[y + 1]; fg[y] = fg[y + 1]; bg[y] = bg[y + 1]; flags[y] = flags[y + 1];
            }
            blankRow(scrollBottom);
        }
    }

    private void scrollDown(int n) {
        for (int k = 0; k < n; k++) {
            for (int y = scrollBottom; y > scrollTop; y--) {
                glyph[y] = glyph[y - 1]; fg[y] = fg[y - 1]; bg[y] = bg[y - 1]; flags[y] = flags[y - 1];
            }
            blankRow(scrollTop);
        }
    }

    private void insertLines(int n) {
        if (cursorY < scrollTop || cursorY > scrollBottom) return;
        for (int k = 0; k < n; k++) {
            for (int y = scrollBottom; y > cursorY; y--) {
                glyph[y] = glyph[y - 1]; fg[y] = fg[y - 1]; bg[y] = bg[y - 1]; flags[y] = flags[y - 1];
            }
            blankRow(cursorY);
        }
    }

    private void deleteLines(int n) {
        if (cursorY < scrollTop || cursorY > scrollBottom) return;
        for (int k = 0; k < n; k++) {
            for (int y = cursorY; y < scrollBottom; y++) {
                glyph[y] = glyph[y + 1]; fg[y] = fg[y + 1]; bg[y] = bg[y + 1]; flags[y] = flags[y + 1];
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
        if (on) {
            altGlyph = glyph; altFg = fg; altBg = bg; altFlags = flags;
            glyph = blankGlyph(cols, rows);
            fg = filled(cols, rows, DEFAULT);
            bg = filled(cols, rows, DEFAULT);
            flags = filled(cols, rows, 0);
            savedX = cursorX; savedY = cursorY;
            cursorX = 0; cursorY = 0;
            onAlt = true;
        } else {
            glyph = altGlyph; fg = altFg; bg = altBg; flags = altFlags;
            cursorX = savedX; cursorY = savedY;
            onAlt = false;
        }
        scrollTop = 0;
        scrollBottom = rows - 1;
    }

    /** Wipe the screen, scrollback and parser — for reattaching to a session. */
    synchronized void reset() {
        onAlt = false;
        scrollback.clear();
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
        String[] parts = body.split(";", -1);
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

    private void blankRow(int y) {
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
