package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The developer keyboard: a dev row (Esc/Tab/Ctrl/Alt/arrows), a number row and QWERTY, drawn on
 * one canvas in Cascadia Mono. Long-press a key for its corner symbol. Shift, Ctrl and Alt:
 * tap = armed for the next key, double-tap = locked. All output goes through {@link Listener}.
 */
final class DevKeyboardView extends View {

    interface Listener {
        void text(String s);

        /** A key press with {@code meta} (KeyEvent.META_*) already folded in. */
        void key(int keyCode, int meta);

        void enter();

        void switchKeyboard();
    }

    private static final class Key {
        final String id;
        final float w;
        final String alt;       // long-press output / corner hint, or null
        final String shifted;   // output while Shift is on (non-letters), or null

        Key(String id, float w, String alt, String shifted) {
            this.id = id;
            this.w = w;
            this.alt = alt;
            this.shifted = shifted;
        }
    }

    private static final class KeyRect {
        final Key key;
        final RectF r = new RectF();

        KeyRect(Key key) {
            this.key = key;
        }
    }

    private static final class Press {
        final KeyRect kr;
        float lastX;
        boolean consumed;
        Runnable longPress, repeat;

        Press(KeyRect kr, float x) {
            this.kr = kr;
            this.lastX = x;
        }
    }

    // ----------------------------------------------------------------- layout data

    private static final String NUM = "1234567890";
    private static final String NUM_SHIFTED = "!@#$%^&*()";
    private static final String[] LETTER_ROWS = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    private static final String[] LETTER_ALTS = {"`~=+-_[]{}", "<>()/\\|;:", "'\"*&?!%"};

    private static final String[][] SYM_ROWS = {{"@", "#", "$", "%", "&", "*", "-", "+", "=", "/"},
            {"(", ")", "[", "]", "{", "}", "<", ">", "\\", "|"}};
    private static final String SYM_ROW4 = "`~^_'\"?";

    private static final long DOUBLE_TAP_MS = 350, LONG_PRESS_MS = 350, REPEAT_FIRST_MS = 400, REPEAT_MS = 45;
    private static final int SHIFT = 0, CTRL = 1, ALT = 2;

    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Map<Integer, Press> presses = new HashMap<>();
    private final int[] mods = new int[3];          // 0 off, 1 armed, 2 locked
    private final long[] lastTap = new long[3];

    private List<List<Key>> rows = new ArrayList<>();
    private List<List<KeyRect>> rects = new ArrayList<>();
    private boolean sym, devRow = true;
    private int keyH;                                // dp
    private int builtW = -1;

    DevKeyboardView(Context ctx, Listener listener) {
        super(ctx);
        this.listener = listener;
        setBackgroundColor(0xFF0A0A0A);
        text.setTypeface(Fonts.cascadiaMono(ctx));
        text.setTextAlign(Paint.Align.CENTER);
        hint.setTypeface(Fonts.cascadiaMono(ctx));
        hint.setTextAlign(Paint.Align.RIGHT);
        hint.setColor(0xFF5A5A5A);
        hint.setTextSize(dp(8.5f));
        refresh();
    }

    /** Re-read the settings; call when the keyboard is shown. */
    void refresh() {
        devRow = Config.isKbDevRow(getContext());
        switch (Config.getKbSize(getContext())) {
            case "small": keyH = 36; break;
            case "large": keyH = 48; break;
            default: keyH = 42;
        }
        buildRows();
        requestLayout();
        invalidate();
    }

    private static Key k(String id) {
        return new Key(id, 1f, null, null);
    }

    private static Key k(String id, float w, String alt) {
        return new Key(id, w, alt, null);
    }

    private void buildRows() {
        List<List<Key>> r = new ArrayList<>();
        if (devRow) {
            List<Key> d = new ArrayList<>();
            d.add(k("Esc"));
            d.add(k("Tab"));
            d.add(k("Ctrl"));
            d.add(k("Alt"));
            d.add(k("Del"));
            d.add(k("Left", 1f, "Home"));
            d.add(k("Down", 1f, "PgDn"));
            d.add(k("Up", 1f, "PgUp"));
            d.add(k("Right", 1f, "End"));
            r.add(d);
        }
        List<Key> num = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            num.add(new Key(NUM.substring(i, i + 1), 1f, "F" + (i + 1), NUM_SHIFTED.substring(i, i + 1)));
        }
        r.add(num);
        if (!sym) {
            for (int row = 0; row < 3; row++) {
                List<Key> l = new ArrayList<>();
                String letters = LETTER_ROWS[row];
                if (row == 1) l.add(k("", 0.5f, null));
                if (row == 2) l.add(k("Shift", 1.5f, null));
                for (int i = 0; i < letters.length(); i++) {
                    l.add(k(letters.substring(i, i + 1), 1f, LETTER_ALTS[row].substring(i, i + 1)));
                }
                if (row == 1) l.add(k("", 0.5f, null));
                if (row == 2) l.add(k("Bksp", 1.5f, null));
                r.add(l);
            }
        } else {
            for (String[] row : SYM_ROWS) {
                List<Key> l = new ArrayList<>();
                for (String s : row) l.add(k(s));
                r.add(l);
            }
            List<Key> l = new ArrayList<>();
            for (int i = 0; i < SYM_ROW4.length(); i++) l.add(k(SYM_ROW4.substring(i, i + 1)));
            l.add(k("Bksp", 3f, null));
            r.add(l);
        }
        List<Key> bottom = new ArrayList<>();
        bottom.add(k("Sym", 1.5f, null));
        bottom.add(k("Ime", 1.3f, null));
        bottom.add(k(",", 1f, null));
        bottom.add(k("Space", 3.4f, null));
        bottom.add(k(".", 1f, null));
        bottom.add(k("Enter", 1.8f, null));
        r.add(bottom);
        rows = r;
        builtW = -1;
    }

    // ------------------------------------------------------------------- geometry

    int heightPx() {
        return rows.size() * dp(keyH + 4) + dp(4);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), heightPx());
    }

    private void buildRects(int w) {
        rects = new ArrayList<>();
        float pad = dp(3), gap = dp(2), rowH = dp(keyH + 4);
        float y = dp(2);
        for (List<Key> row : rows) {
            float total = 0;
            for (Key key : row) total += key.w;
            float unit = (w - 2 * pad) / total;
            float x = pad;
            List<KeyRect> line = new ArrayList<>();
            for (Key key : row) {
                float kw = key.w * unit;
                if (!key.id.isEmpty()) {
                    KeyRect kr = new KeyRect(key);
                    kr.r.set(x + gap, y + gap, x + kw - gap, y + rowH - gap);
                    line.add(kr);
                }
                x += kw;
            }
            rects.add(line);
            y += rowH;
        }
        builtW = w;
    }

    private KeyRect keyAt(float x, float y) {
        for (List<KeyRect> line : rects) {
            for (KeyRect kr : line) {
                // grow the hit box over the gaps so a tap between keys still lands on one
                if (x >= kr.r.left - dp(2) && x < kr.r.right + dp(2)
                        && y >= kr.r.top - dp(2) && y < kr.r.bottom + dp(2)) return kr;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------- drawing

    @Override
    protected void onDraw(Canvas canvas) {
        if (builtW != getWidth()) buildRects(getWidth());
        float radius = dp(7);
        for (List<KeyRect> line : rects) {
            for (KeyRect kr : line) {
                String id = kr.key.id;
                int mod = modIndex(id);
                int state = mod >= 0 ? mods[mod] : 0;
                boolean down = isPressed(kr);
                boolean lit = state > 0;
                int bg = lit ? Color.WHITE : down ? 0xFF262626 : 0xFF121212;
                int fg = lit ? Color.BLACK : 0xFFB5B5B5;
                fill.setStyle(Paint.Style.FILL);
                fill.setColor(bg);
                canvas.drawRoundRect(kr.r, radius, radius, fill);
                fill.setStyle(Paint.Style.STROKE);
                fill.setStrokeWidth(dp(1));
                fill.setColor(lit ? Color.WHITE : 0xFF242424);
                canvas.drawRoundRect(kr.r, radius, radius, fill);
                if (state == 2) {
                    fill.setStyle(Paint.Style.FILL);
                    fill.setColor(Color.BLACK);
                    canvas.drawCircle(kr.r.right - dp(7), kr.r.top + dp(7), dp(2.5f), fill);
                }

                float cx = kr.r.centerX(), cy = kr.r.centerY();
                int icon = KeyIcons.forId(id);
                if (icon != 0) {
                    KeyIcons.draw(canvas, icon, cx, cy, dp(7), fg, id.equals("Shift") && mods[SHIFT] == 2);
                } else {
                    String label = label(kr.key);
                    text.setColor(fg);
                    text.setTextSize(dp(label.length() > 1 ? 12.5f : 17f));
                    canvas.drawText(label, cx, cy - (text.ascent() + text.descent()) / 2f, text);
                }
                if (kr.key.alt != null && !lit) {
                    hint.setTextSize(dp(8.5f));
                    canvas.drawText(kr.key.alt, kr.r.right - dp(4), kr.r.top + dp(11), hint);
                }
            }
        }
    }

    private String label(Key key) {
        String id = key.id;
        switch (id) {
            case "Space": return "";
            case "Sym": return sym ? "abc" : "?#+";
            default:
        }
        if (id.length() == 1) return shiftedOut(key);
        return id;
    }

    /** The character a character key produces right now (honours Shift). */
    private String shiftedOut(Key key) {
        if (mods[SHIFT] == 0) return key.id;
        if (key.shifted != null) return key.shifted;
        return key.id.toUpperCase(java.util.Locale.ROOT);
    }

    private boolean isPressed(KeyRect kr) {
        for (Press p : presses.values()) if (p.kr == kr) return true;
        return false;
    }

    // ---------------------------------------------------------------------- touch

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = ev.getActionIndex();
                down(ev.getPointerId(i), ev.getX(i), ev.getY(i));
                return true;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < ev.getPointerCount(); i++) move(ev.getPointerId(i), ev.getX(i), ev.getY(i));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                up(ev.getPointerId(ev.getActionIndex()), true);
                return true;
            case MotionEvent.ACTION_CANCEL:
                for (Integer id : new ArrayList<>(presses.keySet())) up(id, false);
                return true;
            default:
                return true;
        }
    }

    private static boolean repeats(String id) {
        switch (id) {
            case "Bksp": case "Del": case "Left": case "Right": case "Up": case "Down": return true;
            default: return false;
        }
    }

    private void down(int pointer, float x, float y) {
        if (builtW != getWidth()) buildRects(getWidth());
        KeyRect kr = keyAt(x, y);
        if (kr == null) return;
        Press p = new Press(kr, x);
        presses.put(pointer, p);
        performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        if (repeats(kr.key.id)) {
            p.consumed = true;
            dispatch(kr.key.id);
            p.repeat = new Runnable() {
                @Override
                public void run() {
                    dispatch(kr.key.id);
                    handler.postDelayed(this, REPEAT_MS);
                }
            };
            handler.postDelayed(p.repeat, REPEAT_FIRST_MS);
        } else if (kr.key.alt != null) {
            p.longPress = () -> {
                p.consumed = true;
                dispatch(kr.key.alt);
                invalidate();
            };
            handler.postDelayed(p.longPress, LONG_PRESS_MS);
        }
        invalidate();
    }

    private void move(int pointer, float x, float y) {
        Press p = presses.get(pointer);
        if (p == null) return;
        if (p.kr.key.id.equals("Space")) {
            float step = dp(16);
            while (Math.abs(x - p.lastX) > step) {
                int dir = x > p.lastX ? 1 : -1;
                p.lastX += dir * step;
                p.consumed = true;
                clearCallbacks(p);
                listener.key(dir > 0 ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT, metaNow(false));
            }
            return;
        }
        if (!p.kr.r.contains(x, y) && keyAt(x, y) != p.kr) {   // slid off: drop the press
            clearCallbacks(p);
            p.consumed = true;
            invalidate();
        }
    }

    private void up(int pointer, boolean fire) {
        Press p = presses.remove(pointer);
        if (p == null) return;
        clearCallbacks(p);
        if (fire && !p.consumed) dispatch(p.kr.key.id);
        invalidate();
    }

    private void clearCallbacks(Press p) {
        if (p.longPress != null) handler.removeCallbacks(p.longPress);
        if (p.repeat != null) handler.removeCallbacks(p.repeat);
        p.longPress = null;
        p.repeat = null;
    }

    // ------------------------------------------------------------------- dispatch

    private static int modIndex(String id) {
        switch (id) {
            case "Shift": return SHIFT;
            case "Ctrl": return CTRL;
            case "Alt": return ALT;
            default: return -1;
        }
    }

    private void tapMod(int m) {
        long now = System.currentTimeMillis();
        if (mods[m] == 2) {
            mods[m] = 0;
        } else if (mods[m] == 1) {
            mods[m] = (now - lastTap[m] < DOUBLE_TAP_MS) ? 2 : 0;
        } else {
            mods[m] = 1;
        }
        lastTap[m] = now;
    }

    private int metaNow(boolean includeShift) {
        int meta = 0;
        if (includeShift && mods[SHIFT] != 0) meta |= KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
        if (mods[CTRL] != 0) meta |= KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
        if (mods[ALT] != 0) meta |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;
        return meta;
    }

    private void consumeArmed() {
        for (int i = 0; i < mods.length; i++) if (mods[i] == 1) mods[i] = 0;
        invalidate();
    }

    private void dispatch(String id) {
        int mod = modIndex(id);
        if (mod >= 0) {
            tapMod(mod);
            invalidate();
            return;
        }
        switch (id) {
            case "Sym": sym = !sym; buildRows(); requestLayout(); invalidate(); return;
            case "Ime": listener.switchKeyboard(); return;
            case "Enter": listener.enter(); consumeArmed(); return;
            case "Space": charOut(" "); return;
            default:
        }
        int code = keyCode(id);
        if (code != 0) {
            listener.key(code, metaNow(true));
            consumeArmed();
            return;
        }
        if (id.length() == 1) {
            charOut(shiftedOutFor(id));
        }
    }

    /** Output for a typed (or long-press) single char: Shift applies to letters and number-row keys. */
    private String shiftedOutFor(String id) {
        if (mods[SHIFT] == 0) return id;
        int n = NUM.indexOf(id);
        if (n >= 0) return NUM_SHIFTED.substring(n, n + 1);
        return id.toUpperCase(java.util.Locale.ROOT);
    }

    private void charOut(String s) {
        int meta = metaNow(true);
        boolean chord = (meta & (KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON)) != 0;
        if (chord && s.length() == 1) {
            KeyEvent[] evs = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents(s.toCharArray());
            if (evs != null) {
                for (KeyEvent e : evs) {
                    if (e.getAction() == KeyEvent.ACTION_DOWN && !KeyEvent.isModifierKey(e.getKeyCode())) {
                        listener.key(e.getKeyCode(), e.getMetaState() | meta);
                    }
                }
                consumeArmed();
                return;
            }
        }
        listener.text(s);
        if (mods[SHIFT] == 1) mods[SHIFT] = 0;
        consumeArmed();
    }

    private static int keyCode(String id) {
        switch (id) {
            case "Esc": return KeyEvent.KEYCODE_ESCAPE;
            case "Tab": return KeyEvent.KEYCODE_TAB;
            case "Bksp": return KeyEvent.KEYCODE_DEL;
            case "Del": return KeyEvent.KEYCODE_FORWARD_DEL;
            case "Left": return KeyEvent.KEYCODE_DPAD_LEFT;
            case "Right": return KeyEvent.KEYCODE_DPAD_RIGHT;
            case "Up": return KeyEvent.KEYCODE_DPAD_UP;
            case "Down": return KeyEvent.KEYCODE_DPAD_DOWN;
            case "Home": return KeyEvent.KEYCODE_MOVE_HOME;
            case "End": return KeyEvent.KEYCODE_MOVE_END;
            case "PgUp": return KeyEvent.KEYCODE_PAGE_UP;
            case "PgDn": return KeyEvent.KEYCODE_PAGE_DOWN;
            default:
        }
        if (id.length() >= 2 && id.charAt(0) == 'F') {
            try {
                int n = Integer.parseInt(id.substring(1));
                if (n >= 1 && n <= 12) return KeyEvent.KEYCODE_F1 + n - 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for (Press p : presses.values()) clearCallbacks(p);
        presses.clear();
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
