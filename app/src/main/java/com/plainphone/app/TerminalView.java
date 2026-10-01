package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.nio.charset.StandardCharsets;

/**
 * Draws a {@link TerminalEmulator} grid in a monospace face and turns key and
 * IME input into the bytes a PTY expects. Sizes its column / row count to the
 * view and reports it through {@link #onResize} so the caller can send
 * {@code pty.resize}.
 *
 * <p>Pinch changes the text size (like Termux): the grid re-lays out, the
 * emulator reflows its text to the new width, and the size is remembered
 * ({@link Config#getDevTermFontSp}) for the next shell.
 */
final class TerminalView extends View {

    /** Outbound bytes, numbered so the daemon's echo acks can refer to them. */
    interface OnInput { void bytes(byte[] data, long seq); }
    /** Smoothed round trip to the daemon in ms, or negative if unknown. */
    interface Rtt { double ms(); }
    interface OnResize { void size(int cols, int rows); }

    private static final int DEFAULT_FG = 0xFFD2D2D2;
    private static final int DEFAULT_BG = 0xFF000000;

    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint();
    private final int[] palette = xterm256();

    private TerminalEmulator term;
    private float charW;
    private float charH;
    private float baseline;
    private int cols = 80;
    private int rows = 24;

    OnInput onInput;
    OnResize onResize;
    Rtt rtt;
    private long nextSeq = 1;
    /** Guesses drawn at all (mosh: only once the round trip is noticeable). */
    private boolean showGuesses = true;
    private boolean ctrlArmed, altArmed, shiftArmed;
    /** Fired when the sticky modifiers auto-clear after a keystroke. */
    Runnable onModsCleared;

    /** Lines scrolled up into history; 0 = following the live bottom. */
    private int scrollLines;
    private final int touchSlop;
    private float downY, lastY, scrollAccum;
    private boolean touchMoved;

    private final float density;
    private float fontSp;
    private static final float FONT_MIN = 4f, FONT_MAX = 26f;
    private final ScaleGestureDetector scaleDetector;

    TerminalView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackgroundColor(DEFAULT_BG);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        density = context.getResources().getDisplayMetrics().scaledDensity;

        text.setTypeface(Fonts.cascadiaMono(context));
        fontSp = Config.getDevTermFontSp(context);
        applyFont();

        term = new TerminalEmulator(cols, rows, this::emit);

        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector d) {
                        float next = Math.max(FONT_MIN, Math.min(FONT_MAX, fontSp * d.getScaleFactor()));
                        if (Math.abs(next - fontSp) < 0.1f) return false;
                        fontSp = next;
                        applyFont();
                        invalidate();
                        // Only the paint scale updates live; the grid reflow (and the
                        // pty.resize it sends) waits until the pinch settles, same as
                        // onSizeChanged — a resize per tick floods the remote shell with
                        // dozens of different sizes and corrupts anything that redraws
                        // its own layout on resize (e.g. a TUI status/divider line).
                        removeCallbacks(settle);
                        postDelayed(settle, 180);
                        return true;
                    }

                    @Override
                    public void onScaleEnd(ScaleGestureDetector d) {
                        Config.setDevTermFontSp(getContext(), fontSp);
                    }
                });
    }

    private void applyFont() {
        text.setTextSize(density * fontSp);
        Paint.FontMetrics fm = text.getFontMetrics();
        charW = text.measureText("M");
        charH = fm.bottom - fm.top;
        baseline = -fm.top;
    }

    /** Recompute cols/rows for the current view size + font and push a resize. */
    private void remeasure() {
        int w = getWidth() - getPaddingLeft() - getPaddingRight(), h = getHeight();
        if (w <= 0 || h == 0) return;
        int newCols = Math.max(20, (int) (w / charW));
        int newRows = Math.max(6, (int) (h / charH));
        if (newCols != cols || newRows != rows) {
            cols = newCols;
            rows = newRows;
            scrollLines = 0;
            term.resize(cols, rows);
            if (onResize != null) onResize.size(cols, rows);
        }
    }

    /** Clear everything — used when reattaching to a persistent shell before its replay. */
    void reset() {
        term.reset();
        scrollLines = 0;
        removeCallbacks(sweep);
        sweeping = false;
        postInvalidate();
    }

    void feed(byte[] data, int len) {
        int before = term.scrollbackSize();
        term.feed(data, len);
        if (scrollLines > 0) {
            // keep the same history visible as new lines push in
            scrollLines = Math.min(term.scrollbackSize(), scrollLines + term.scrollbackSize() - before);
        }
        postInvalidate();
    }

    /** Bring the soft keyboard back — a tap on the terminal, or the key-bar button. */
    void showKeyboard() {
        requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
    }

    int cols() { return cols; }
    int rows() { return rows; }

    /** Arm a sticky modifier for the next keystroke (the key-bar chips). */
    void armCtrl(boolean armed) { ctrlArmed = armed; }
    void armAlt(boolean armed) { altArmed = armed; }
    void armShift(boolean armed) { shiftArmed = armed; }

    boolean ctrlArmed() { return ctrlArmed; }
    boolean altArmed() { return altArmed; }
    boolean shiftArmed() { return shiftArmed; }

    private void clearMods() {
        if (ctrlArmed || altArmed || shiftArmed) {
            ctrlArmed = altArmed = shiftArmed = false;
            if (onModsCleared != null) onModsCleared.run();
        }
    }

    /** xterm modifier parameter: 1 + shift(1) + alt(2) + ctrl(4). */
    private int modParam() {
        return 1 + (shiftArmed ? 1 : 0) + (altArmed ? 2 : 0) + (ctrlArmed ? 4 : 0);
    }

    /** An arrow / nav key from the bar, honouring the armed modifiers. */
    void barArrow(char dir) {
        int m = modParam();
        sendBytes(m == 1 ? esc("[" + dir) : esc("[1;" + m + dir));
        clearMods();
    }

    /** A fixed control byte sequence from the bar; Alt prefixes it with ESC. */
    void barKey(byte[] base) {
        if (altArmed) {
            byte[] out = new byte[base.length + 1];
            out[0] = 0x1b;
            System.arraycopy(base, 0, out, 1, base.length);
            base = out;
        }
        sendBytes(base);
        clearMods();
    }

    /** The daemon can ack echoes; without that, guesses could never be judged. */
    void setEchoAck(boolean on) {
        term.predictor.enabled = on;
        if (!on) term.predictor.invalidate();
    }

    /** Echo ack: the daemon fed input up to {@code seq} to the shell and the
     *  output since then has arrived. */
    void ack(long seq) {
        term.predictor.onAck(seq);
        postInvalidate();
    }

    /** User keystrokes: predicted locally (where possible) as they go out. */
    void sendBytes(byte[] data) {
        send(data, true);
    }

    private void send(byte[] data, boolean user) {
        if (data == null || data.length == 0 || onInput == null) return;
        if (scrollLines != 0) scrollLines = 0; // snap to the live bottom on any input
        long seq = nextSeq++;
        if (user) {
            boolean instant = term.predictor.onInput(data, seq) && showGuesses();
            if (data[0] >= 0x20 && data[0] != 0x7f && data.length <= 4) {
                // share of plain keystrokes drawn before their echo (avg of 0/1)
                LatencyStats.record("shell.instant", instant ? 1 : 0);
            }
            if (!sweeping && term.predictor.pending()) {
                sweeping = true;
                postDelayed(sweep, SWEEP_MS);
            }
        }
        onInput.bytes(data, seq);
        postInvalidate();
    }

    /** Mosh's display rule: guesses only once the round trip is noticeable
     *  (on above 30 ms, off below 20 ms — hysteresis so it doesn't flap). */
    private boolean showGuesses() {
        double ms = rtt != null ? rtt.ms() : -1;
        if (ms < 0) return showGuesses;
        if (ms > 30) showGuesses = true;
        else if (ms < 20) showGuesses = false;
        return showGuesses;
    }

    private boolean sweeping;
    private static final long SWEEP_MS = 100;

    /** While guesses are pending: expire ones that never got an ack, and
     *  redraw so the slow-guess underline appears on time. Stops itself. */
    private final Runnable sweep = new Runnable() {
        @Override public void run() {
            term.predictor.expire();
            invalidate();
            if (term.predictor.pending()) postDelayed(this, SWEEP_MS);
            else sweeping = false;
        }
    };

    void sendString(String s) {
        sendBytes(s.getBytes(StandardCharsets.UTF_8));
    }

    private void emit(byte[] data) {
        // replies the emulator generates itself (e.g. cursor-position report)
        send(data, false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // The view resizes 2-3 times right after open (keyboard slides up, key
        // bar appears). Debounce so the shell reflows once, when it settles —
        // otherwise the content visibly jumps / "zooms".
        removeCallbacks(settle);
        postDelayed(settle, 180);
    }

    private final Runnable settle = this::remeasure;

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(DEFAULT_BG);
        boolean focused = isFocused();
        int sb = term.scrollbackSize();
        int off = Math.min(scrollLines, sb);
        final float left = getPaddingLeft();
        boolean show = off == 0 && showGuesses();
        java.util.List<EchoPredictor.Cell> guesses = show
                ? term.predictor.visibleCells() : java.util.Collections.emptyList();
        // With a cursor prediction, the cursor is drawn there instead.
        int[] guessCursor = show ? term.predictor.visibleCursor() : null;

        for (int y = 0; y < rows; y++) {
            int virt = sb - off + y;
            char[] gRow;
            int[] fRow, bRow, flRow;
            int gy = -1; // grid row when this line is live, else -1
            if (virt < sb) {
                TerminalEmulator.Line ln = term.scrollbackLine(virt);
                if (ln == null) continue;
                gRow = ln.g; fRow = ln.f; bRow = ln.b; flRow = ln.fl;
            } else {
                gy = virt - sb;
                if (gy < 0 || gy >= term.rows) continue;
                gRow = term.glyph[gy]; fRow = term.fg[gy]; bRow = term.bg[gy]; flRow = term.flags[gy];
            }
            float top = y * charH;
            int w = Math.min(term.cols, gRow.length);
            for (int x = 0; x < w; x++) {
                int cflags = flRow[x];
                boolean inverse = (cflags & TerminalEmulator.FLAG_INVERSE) != 0;
                boolean bold = (cflags & TerminalEmulator.FLAG_BOLD) != 0;
                int fgc = resolve(fRow[x], true, bold);
                int bgc = resolve(bRow[x], false, false);
                if (inverse) {
                    int t = fgc; fgc = bgc; bgc = t;
                }
                boolean cursorHere = focused && term.cursorVisible && off == 0 && gy >= 0
                        && guessCursor == null && x == term.cursorX && gy == term.cursorY;
                if (cursorHere) {
                    int t = fgc; fgc = bgc; bgc = t;
                    if (bgc == DEFAULT_BG) bgc = DEFAULT_FG;
                    if (fgc == bgc) fgc = DEFAULT_BG;
                }
                if (bgc != DEFAULT_BG) {
                    fill.setColor(bgc);
                    canvas.drawRect(left + x * charW, top, left + (x + 1) * charW, top + charH, fill);
                }
                char g = gRow[x];
                if (g != ' ' && g != 0) {
                    text.setColor(fgc);
                    text.setFakeBoldText(bold);
                    canvas.drawText(String.valueOf(g), left + x * charW, top + baseline, text);
                }
            }
        }
        drawGuesses(canvas, guesses, guessCursor, left, focused && term.cursorVisible);
    }

    /** Unconfirmed keystrokes (predictive local echo), drawn over the grid;
     *  the real echo replaces them once it arrives. Underlined only when
     *  they're slow to confirm (mosh: round trip over 80 ms, or any guess
     *  pending past 250 ms). */
    private void drawGuesses(Canvas canvas, java.util.List<EchoPredictor.Cell> guesses, int[] cursor,
                             float left, boolean drawCursor) {
        if (guesses.isEmpty() && cursor == null) return;
        double ms = rtt != null ? rtt.ms() : -1;
        boolean underline = ms > 80 || term.predictor.glitching();
        text.setFakeBoldText(false);
        for (EchoPredictor.Cell c : guesses) {
            if (c.y >= rows || c.x >= term.cols) continue;
            float x0 = left + c.x * charW, top = c.y * charH;
            fill.setColor(DEFAULT_BG);
            canvas.drawRect(x0, top, x0 + charW, top + charH, fill);
            if (c.ch == ' ') continue;
            text.setColor(DEFAULT_FG);
            canvas.drawText(String.valueOf(c.ch), x0, top + baseline, text);
            if (underline && !c.weak) {
                fill.setColor(DEFAULT_FG);
                float lineY = top + baseline + charH * 0.08f;
                canvas.drawRect(x0, lineY, x0 + charW, lineY + Math.max(1f, density), fill);
            }
        }
        if (cursor != null && drawCursor && cursor[0] < rows && cursor[1] < term.cols) {
            float x0 = left + cursor[1] * charW, top = cursor[0] * charH;
            fill.setColor(DEFAULT_FG);
            canvas.drawRect(x0, top, x0 + charW, top + charH, fill);
            char under = term.glyph[cursor[0]][cursor[1]];
            for (EchoPredictor.Cell c : guesses) if (c.y == cursor[0] && c.x == cursor[1]) under = c.ch;
            if (under != ' ' && under != 0) {
                text.setColor(DEFAULT_BG);
                canvas.drawText(String.valueOf(under), x0, top + baseline, text);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        if (scaleDetector.isInProgress()) {
            touchMoved = true; // suppress tap-to-keyboard after a pinch
            return true;
        }
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = lastY = e.getY();
                scrollAccum = 0;
                touchMoved = false;
                return true;
            case MotionEvent.ACTION_MOVE: {
                if (e.getPointerCount() > 1) return true; // let the pinch own it
                float dy = e.getY() - lastY;
                lastY = e.getY();
                if (Math.abs(e.getY() - downY) > touchSlop) touchMoved = true;
                if (touchMoved && !term.onAlt()) {
                    scrollAccum += dy;
                    int steps = (int) (scrollAccum / charH);
                    if (steps != 0) {
                        scrollAccum -= steps * charH;
                        scrollLines = Math.max(0,
                                Math.min(term.scrollbackSize(), scrollLines + steps));
                        invalidate();
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (!touchMoved) showKeyboard();
                return true;
        }
        return super.onTouchEvent(e);
    }

    private int resolve(int index, boolean fg, boolean bold) {
        if (index == TerminalEmulator.DEFAULT) return fg ? DEFAULT_FG : DEFAULT_BG;
        if ((index & 0xFF000000) != 0) return index; // packed 24-bit truecolor
        int i = index;
        if (bold && i < 8) i += 8;
        if (i < 0 || i >= palette.length) return fg ? DEFAULT_FG : DEFAULT_BG;
        return palette[i];
    }

    // --- input --------------------------------------------------------

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
                | EditorInfo.IME_FLAG_NO_FULLSCREEN
                | EditorInfo.IME_ACTION_NONE;
        return new BaseInputConnection(this, false) {
            @Override
            public boolean commitText(CharSequence textIn, int newCursorPosition) {
                type(textIn.toString());
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                for (int i = 0; i < beforeLength; i++) sendBytes(new byte[]{0x7f});
                return true;
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) return onKeyDown(event.getKeyCode(), event);
                return super.sendKeyEvent(event);
            }

            @Override
            public boolean performEditorAction(int actionCode) {
                sendBytes(new byte[]{'\r'});
                return true;
            }
        };
    }

    /** Committed text (one key, or an IME commit). Predicted locally by
     *  {@link EchoPredictor} when it's a single plain char; a multi-char
     *  commit (autocorrect replacing a word) is never guessed. */
    private void type(String s) {
        byte[] body = (ctrlArmed && s.length() == 1)
                ? new byte[]{control(s.charAt(0))}
                : s.getBytes(StandardCharsets.UTF_8);
        if (altArmed) {
            byte[] out = new byte[body.length + 1];
            out[0] = 0x1b;
            System.arraycopy(body, 0, out, 1, body.length);
            body = out;
        }
        sendBytes(body);
        clearMods();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        byte[] seq = keySequence(keyCode, event);
        if (seq != null) {
            sendBytes(seq);
            if (keyCode == KeyEvent.KEYCODE_TAB && shiftArmed) clearMods();
            return true;
        }
        int uni = event.getUnicodeChar(event.getMetaState());
        if (uni != 0) {
            if (ctrlArmed || event.isCtrlPressed()) {
                sendBytes(new byte[]{control((char) uni)});
                clearMods();
            } else {
                type(new String(Character.toChars(uni)));
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private byte[] keySequence(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER: return new byte[]{'\r'};
            case KeyEvent.KEYCODE_DEL: return new byte[]{0x7f};
            case KeyEvent.KEYCODE_FORWARD_DEL: return esc("[3~");
            case KeyEvent.KEYCODE_TAB:
                // Shift+Tab (a keyboard's Shift, or the bar's armed shift) is ESC [ Z.
                return event.isShiftPressed() || shiftArmed ? esc("[Z") : new byte[]{'\t'};
            case KeyEvent.KEYCODE_ESCAPE: return new byte[]{0x1b};
            case KeyEvent.KEYCODE_DPAD_UP: return esc("[A");
            case KeyEvent.KEYCODE_DPAD_DOWN: return esc("[B");
            case KeyEvent.KEYCODE_DPAD_RIGHT: return esc("[C");
            case KeyEvent.KEYCODE_DPAD_LEFT: return esc("[D");
            case KeyEvent.KEYCODE_MOVE_HOME: return esc("[H");
            case KeyEvent.KEYCODE_MOVE_END: return esc("[F");
            case KeyEvent.KEYCODE_PAGE_UP: return esc("[5~");
            case KeyEvent.KEYCODE_PAGE_DOWN: return esc("[6~");
            default: return null;
        }
    }

    static byte[] esc(String tail) {
        byte[] t = tail.getBytes(StandardCharsets.US_ASCII);
        byte[] out = new byte[t.length + 1];
        out[0] = 0x1b;
        System.arraycopy(t, 0, out, 1, t.length);
        return out;
    }

    static byte control(char c) {
        char u = Character.toUpperCase(c);
        if (u >= '@' && u <= '_') return (byte) (u - '@');
        if (u == ' ') return 0;
        if (u == '?') return 0x7f;
        return (byte) c;
    }

    // --- xterm 256 palette ----------------------------------------

    private static int[] xterm256() {
        int[] p = new int[256];
        int[] base = {
                0x000000, 0xCD0000, 0x00CD00, 0xCDCD00, 0x0000EE, 0xCD00CD, 0x00CDCD, 0xE5E5E5,
                0x7F7F7F, 0xFF0000, 0x00FF00, 0xFFFF00, 0x5C5CFF, 0xFF00FF, 0x00FFFF, 0xFFFFFF,
        };
        for (int i = 0; i < 16; i++) p[i] = 0xFF000000 | base[i];
        int[] steps = {0, 95, 135, 175, 215, 255};
        int n = 16;
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    p[n++] = 0xFF000000 | (steps[r] << 16) | (steps[g] << 8) | steps[b];
                }
            }
        }
        for (int i = 0; i < 24; i++) {
            int v = 8 + i * 10;
            p[n++] = 0xFF000000 | (v << 16) | (v << 8) | v;
        }
        return p;
    }
}
