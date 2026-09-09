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
 */
final class TerminalView extends View {

    interface OnInput { void bytes(byte[] data); }
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
    private boolean ctrlArmed;

    /** Lines scrolled up into history; 0 = following the live bottom. */
    private int scrollLines;
    private final int touchSlop;
    private float downY, lastY, scrollAccum;
    private boolean touchMoved;

    private final float density;
    private float fontSp;
    private static final float FONT_MIN = 7f, FONT_MAX = 26f, FONT_DEFAULT = 12.5f;
    private final ScaleGestureDetector scaleDetector;
    private long badgeUntil;

    TerminalView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackgroundColor(DEFAULT_BG);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        density = context.getResources().getDisplayMetrics().scaledDensity;

        text.setTypeface(Fonts.cascadiaMono(context));
        fontSp = FONT_DEFAULT;
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
                        remeasure();
                        badgeUntil = System.currentTimeMillis() + 900;
                        invalidate();
                        return true;
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
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
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

    /** Arm Ctrl for the next character (the key-bar "ctrl" chip). */
    void armCtrl(boolean armed) {
        ctrlArmed = armed;
    }

    boolean ctrlArmed() {
        return ctrlArmed;
    }

    void sendBytes(byte[] data) {
        if (data != null && data.length > 0 && onInput != null) {
            if (scrollLines != 0) {
                scrollLines = 0; // snap to the live bottom on any input
                postInvalidate();
            }
            onInput.bytes(data);
        }
    }

    void sendString(String s) {
        sendBytes(s.getBytes(StandardCharsets.UTF_8));
    }

    private void emit(byte[] data) {
        // replies the emulator generates itself (e.g. cursor-position report)
        sendBytes(data);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        remeasure();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(DEFAULT_BG);
        boolean focused = isFocused();
        int sb = term.scrollbackSize();
        int off = Math.min(scrollLines, sb);

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
                        && x == term.cursorX && gy == term.cursorY;
                if (cursorHere) {
                    int t = fgc; fgc = bgc; bgc = t;
                    if (bgc == DEFAULT_BG) bgc = DEFAULT_FG;
                    if (fgc == bgc) fgc = DEFAULT_BG;
                }
                if (bgc != DEFAULT_BG) {
                    fill.setColor(bgc);
                    canvas.drawRect(x * charW, top, (x + 1) * charW, top + charH, fill);
                }
                char g = gRow[x];
                if (g != ' ' && g != 0) {
                    text.setColor(fgc);
                    text.setFakeBoldText(bold);
                    canvas.drawText(String.valueOf(g), x * charW, top + baseline, text);
                }
            }
        }

        if (System.currentTimeMillis() < badgeUntil) {
            String label = cols + "×" + rows;
            float tw = text.measureText(label);
            float left = getWidth() - tw - 34;
            fill.setColor(0xE6000000);
            canvas.drawRect(left, 10, getWidth() - 12, 10 + charH + 8, fill);
            text.setColor(DEFAULT_FG);
            text.setFakeBoldText(false);
            canvas.drawText(label, left + 11, 14 + baseline, text);
            postInvalidateDelayed(120);
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

    private void type(String s) {
        if (ctrlArmed && s.length() == 1) {
            sendBytes(new byte[]{control(s.charAt(0))});
            ctrlArmed = false;
            return;
        }
        sendString(s);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        byte[] seq = keySequence(keyCode, event);
        if (seq != null) {
            sendBytes(seq);
            return true;
        }
        int uni = event.getUnicodeChar(event.getMetaState());
        if (uni != 0) {
            if (ctrlArmed || event.isCtrlPressed()) {
                sendBytes(new byte[]{control((char) uni)});
                ctrlArmed = false;
            } else {
                sendString(new String(Character.toChars(uni)));
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
            case KeyEvent.KEYCODE_TAB: return new byte[]{'\t'};
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
