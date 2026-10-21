package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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

        /** Tap on the globe key (only when several languages are on): the next language. */
        default void nextLanguage() {
        }

        /** Long-press on the globe key: pick a language. */
        default void languagePicker() {
        }
    }

    private static final class Key {
        final String id;
        final float w;
        final String alt;       // long-press output / corner hint, or null
        final String shifted;   // output while Shift is on (non-letters), or null
        boolean block;          // part of the joined arrow block

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

    private static final int ROW_DEV = 0, ROW_NUM = 1, ROW_KEYS = 2;
    private static final int KEY_BG = 0xFF161616, FN_BG = 0xFF0F0F0F, DOWN_BG = 0xFF2B2B2B,
            KEY_FG = 0xFFD2D2D2, FN_FG = 0xFF9A9A9A, HINT_FG = 0xFF585858;

    private List<List<Key>> rows = new ArrayList<>();
    private List<Integer> rowKinds = new ArrayList<>();
    private KeyRect bubbleKey;
    private String bubbleText = "";
    private List<List<KeyRect>> rects = new ArrayList<>();
    private boolean sym, devRow = true;
    private Layouts.Spec letterSpec = Layouts.QWERTY;
    private String langTag = "", langName = "";
    private boolean numRow = true, forceNumRow;          // the number row: the setting, and a layout that needs it
    private boolean jamoLabels;                         // Korean: letter keys show their jamo
    private boolean multiLang;                        // several languages on: the Ime key is a globe
    private float heightPct = 100f;                   // 75..130
    private int builtW = -1;

    DevKeyboardView(Context ctx, Listener listener) {
        super(ctx);
        this.listener = listener;
        setBackgroundColor(0xFF0A0A0A);
        text.setTypeface(Fonts.cascadiaMono(ctx));
        text.setTextAlign(Paint.Align.CENTER);
        hint.setTypeface(Fonts.cascadiaMono(ctx));
        hint.setTextAlign(Paint.Align.RIGHT);
        hint.setColor(HINT_FG);
        hint.setTextSize(dp(8.5f));
        refresh();
    }

    /** Re-read the settings; call when the keyboard is shown. */
    void refresh() {
        devRow = Config.isKbDevRow(getContext());
        numRow = Config.isKbNumRow(getContext());
        heightPct = Config.getKbHeightPct(getContext());
        buildRows();
        requestLayout();
        invalidate();
    }

    /** Swap the letter rows (language layout); the number row, modifiers and bottom row stay. */
    void setLetterLayout(Layouts.Spec spec) {
        letterSpec = spec;
        buildRows();
        requestLayout();
        invalidate();
    }

    /** Korean keys show jamo; Vietnamese VNI needs the number row even when the setting hides it. */
    void setLabelsAndNumRow(boolean jamo, boolean needNumRow) {
        boolean rebuild = needNumRow != forceNumRow;
        jamoLabels = jamo;
        forceNumRow = needNumRow;
        if (rebuild) {
            buildRows();
            requestLayout();
        }
        invalidate();
    }

    /** Arm Shift for the next letter (auto-capital), unless the user already set a modifier state. */
    void armShift() {
        if (mods[SHIFT] == 0) {
            mods[SHIFT] = 1;
            invalidate();
        }
    }

    /** The active language: its tag shows on the globe key, its name on the space bar (when {@code multi}). */
    void setLanguage(String tag, String nativeName, boolean multi) {
        langTag = tag;
        langName = nativeName;
        multiLang = multi;
        invalidate();
    }

    /** Resize live while the user drags the height button; not saved here. */
    void setHeightPct(float pct) {
        heightPct = Math.max(75f, Math.min(130f, pct));
        builtW = -1;
        requestLayout();
        invalidate();
    }

    /** Height of one row in dp, before its 4dp gap: the dev row and number row are shorter. */
    private int rowDp(int kind) {
        return rowDp(kind, heightPct);
    }

    private int rowDp(int kind, float pct) {
        float base = 42f * pct / 100f;
        if (kind == ROW_DEV) return Math.round(base * 0.76f);
        if (kind == ROW_NUM) return Math.round(base * 0.9f);
        return Math.round(base);
    }

    private static Key k(String id) {
        return new Key(id, 1f, null, null);
    }

    private static Key k(String id, float w, String alt) {
        return new Key(id, w, alt, null);
    }

    private void buildRows() {
        List<List<Key>> r = new ArrayList<>();
        List<Integer> kinds = new ArrayList<>();
        if (devRow) {
            List<Key> d = new ArrayList<>();
            d.add(k("Esc"));
            d.add(k("Tab"));
            d.add(k("Ctrl"));
            d.add(k("Alt"));
            d.add(k("Del"));
            for (String[] a : new String[][]{{"Left", "Home"}, {"Down", "PgDn"}, {"Up", "PgUp"}, {"Right", "End"}}) {
                Key arrow = k(a[0], 1f, a[1]);
                arrow.block = true;
                d.add(arrow);
            }
            r.add(d);
            kinds.add(ROW_DEV);
        }
        if (numRow || forceNumRow) {
            List<Key> num = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                num.add(new Key(NUM.substring(i, i + 1), 1f, "F" + (i + 1), NUM_SHIFTED.substring(i, i + 1)));
            }
            r.add(num);
            kinds.add(ROW_NUM);
        }
        if (!sym) {
            for (int row = 0; row < 3; row++) {
                List<Key> l = new ArrayList<>();
                String letters = letterSpec.rows[row];
                // the middle row is centred with a spacer when shorter than the 10-key row
                float spacer = row == 1 ? (10 - letters.length()) / 2f : 0f;
                if (spacer > 0) l.add(k("", spacer, null));
                if (row == 2) l.add(k("Shift", 1.5f, null));
                for (int i = 0; i < letters.length(); i++) {
                    l.add(k(letters.substring(i, i + 1), 1f, letterSpec.alt(row, i)));
                }
                if (spacer > 0) l.add(k("", spacer, null));
                if (row == 2) l.add(k("Bksp", 1.5f, null));
                r.add(l);
                kinds.add(ROW_KEYS);
            }
        } else {
            for (String[] row : SYM_ROWS) {
                List<Key> l = new ArrayList<>();
                for (String s : row) l.add(k(s));
                r.add(l);
                kinds.add(ROW_KEYS);
            }
            List<Key> l = new ArrayList<>();
            for (int i = 0; i < SYM_ROW4.length(); i++) l.add(k(SYM_ROW4.substring(i, i + 1)));
            l.add(k("Bksp", 3f, null));
            r.add(l);
            kinds.add(ROW_KEYS);
        }
        List<Key> bottom = new ArrayList<>();
        bottom.add(k("Sym", 1.5f, null));
        bottom.add(k("Ime", 1.3f, null));
        bottom.add(k(",", 1f, null));
        bottom.add(k("Space", 3.4f, null));
        bottom.add(k(".", 1f, null));
        bottom.add(k("Enter", 1.8f, null));
        r.add(bottom);
        kinds.add(ROW_KEYS);
        rows = r;
        rowKinds = kinds;
        builtW = -1;
    }

    // ------------------------------------------------------------------- geometry

    int heightPx() {
        return heightPxAt(heightPct);
    }

    /** What {@link #heightPx()} would be at {@code pct} percent. */
    int heightPxAt(float pct) {
        int h = dp(4);
        for (int kind : rowKinds) h += dp(rowDp(kind, pct) + 4);
        return h;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), heightPx());
    }

    private void buildRects(int w) {
        rects = new ArrayList<>();
        float pad = dp(3), gap = dp(2);
        float y = dp(2);
        for (int ri = 0; ri < rows.size(); ri++) {
            List<Key> row = rows.get(ri);
            float rowH = dp(rowDp(rowKinds.get(ri)) + 4);
            float total = 0;
            for (Key key : row) total += key.w;
            float unit = (w - 2 * pad) / total;
            float x = pad;
            List<KeyRect> line = new ArrayList<>();
            for (int i = 0; i < row.size(); i++) {
                Key key = row.get(i);
                float kw = key.w * unit;
                if (!key.id.isEmpty()) {
                    KeyRect kr = new KeyRect(key);
                    // keys of the arrow block touch each other; only its outer ends keep the gap
                    float l = key.block && i > 0 && row.get(i - 1).block ? x : x + gap;
                    float r = key.block && i < row.size() - 1 && row.get(i + 1).block ? x + kw : x + kw - gap;
                    kr.r.set(l, y + gap, r, y + rowH - gap);
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

    private static boolean isFn(String id) {
        switch (id) {
            case "Esc": case "Tab": case "Ctrl": case "Alt": case "Del": case "Shift": case "Bksp":
            case "Sym": case "Ime": case "Left": case "Right": case "Up": case "Down":
                return true;
            default:
                return false;
        }
    }

    private final Path keyPath = new Path();

    @Override
    protected void onDraw(Canvas canvas) {
        if (builtW != getWidth()) buildRects(getWidth());
        float radius = dp(9);
        for (List<KeyRect> line : rects) {
            for (int i = 0; i < line.size(); i++) {
                KeyRect kr = line.get(i);
                String id = kr.key.id;
                int mod = modIndex(id);
                int state = mod >= 0 ? mods[mod] : 0;
                boolean down = isPressed(kr);
                boolean lit = state > 0;
                boolean enter = id.equals("Enter");
                boolean fn = isFn(id);
                int bg = lit || enter ? (enter && down ? 0xFFBBBBBB : Color.WHITE)
                        : down ? DOWN_BG : fn ? FN_BG : KEY_BG;
                int fg = lit || enter ? Color.BLACK : fn ? FN_FG : KEY_FG;
                fill.setStyle(Paint.Style.FILL);
                fill.setColor(bg);
                if (kr.key.block) {
                    boolean first = i == 0 || !line.get(i - 1).key.block;
                    boolean last = i == line.size() - 1 || !line.get(i + 1).key.block;
                    float lr = first ? radius : 0, rr = last ? radius : 0;
                    keyPath.reset();
                    keyPath.addRoundRect(kr.r, new float[]{lr, lr, rr, rr, rr, rr, lr, lr}, Path.Direction.CW);
                    canvas.drawPath(keyPath, fill);
                    if (!first) {
                        fill.setColor(Color.BLACK);
                        canvas.drawRect(kr.r.left - dp(0.5f), kr.r.top, kr.r.left + dp(0.5f), kr.r.bottom, fill);
                    }
                } else {
                    canvas.drawRoundRect(kr.r, radius, radius, fill);
                }
                if (state == 2) {
                    fill.setColor(Color.BLACK);
                    canvas.drawCircle(kr.r.right - dp(7), kr.r.top + dp(7), dp(2.5f), fill);
                }

                float cx = kr.r.centerX(), cy = kr.r.centerY();
                int icon = KeyIcons.forId(id);
                if (id.equals("Space") && multiLang) {
                    text.setColor(0xFF777777);
                    text.setTextSize(dp(11));
                    canvas.drawText(langName, cx, cy - (text.ascent() + text.descent()) / 2f, text);
                } else if (id.equals("Space")) {
                    fill.setColor(0xFF3A3A3A);
                    float hw = kr.r.width() * 0.15f;
                    canvas.drawRoundRect(cx - hw, cy - dp(1), cx + hw, cy + dp(1), dp(1), dp(1), fill);
                } else if (id.equals("Ime") && multiLang) {
                    KeyIcons.draw(canvas, KeyIcons.GLOBE, cx, cy - dp(5), dp(6), fg, false);
                    text.setColor(fg);
                    text.setTextSize(dp(8.5f));
                    canvas.drawText(langTag, cx, cy + dp(10) - (text.ascent() + text.descent()) / 2f, text);
                } else if (icon != 0) {
                    KeyIcons.draw(canvas, icon, cx, cy, dp(7), fg, id.equals("Shift") && mods[SHIFT] == 2);
                } else {
                    String label = label(kr.key);
                    text.setColor(fg);
                    text.setTextSize(dp(label.length() > 1 ? 12f : 18f));
                    canvas.drawText(label, cx, cy - (text.ascent() + text.descent()) / 2f, text);
                }
                if (kr.key.alt != null && !lit) {
                    hint.setTextSize(dp(8.5f));
                    canvas.drawText(kr.key.alt, kr.r.right - dp(4), kr.r.top + dp(11), hint);
                }
            }
        }
        if (bubbleKey != null) drawBubble(canvas);
    }

    /** A white bubble above the pressed key showing what it will type. */
    private void drawBubble(Canvas canvas) {
        RectF k = bubbleKey.r;
        float w = Math.max(dp(46), k.width()), h = dp(54);
        float left = Math.max(dp(2), Math.min(getWidth() - dp(2) - w, k.centerX() - w / 2f));
        float top = Math.max(dp(1), k.top - h - dp(4));
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(Color.WHITE);
        canvas.drawRoundRect(left, top, left + w, top + h, dp(12), dp(12), fill);
        text.setColor(Color.BLACK);
        text.setTextSize(dp(28));
        canvas.drawText(bubbleText, left + w / 2f, top + h / 2f - (text.ascent() + text.descent()) / 2f, text);
    }

    private String label(Key key) {
        String id = key.id;
        switch (id) {
            case "Space": return "";
            case "Sym": return sym ? "abc" : "?#+";
            default:
        }
        if (jamoLabels && id.length() == 1) {
            String j = HangulComposer.jamoFor(id.charAt(0), mods[SHIFT] != 0);
            if (j != null) return j;
        }
        if (id.length() == 1) return shiftedOut(key);
        return id;
    }

    /** The character a character key produces right now (honours Shift). */
    private String shiftedOut(Key key) {
        if (mods[SHIFT] == 0 || key.id.equals("ß")) return key.id;   // ß has no one-letter capital
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
        if (kr.key.id.length() == 1) {
            bubbleKey = kr;
            bubbleText = label(kr.key);
        }
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
        } else if (multiLang && kr.key.id.equals("Ime")) {
            p.longPress = () -> {
                p.consumed = true;
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                listener.languagePicker();
            };
            handler.postDelayed(p.longPress, LONG_PRESS_MS);
        } else if (kr.key.alt != null) {
            p.longPress = () -> {
                p.consumed = true;
                if (bubbleKey == kr) bubbleText = kr.key.alt;
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
            if (bubbleKey == p.kr) bubbleKey = null;
            invalidate();
        }
    }

    private void up(int pointer, boolean fire) {
        Press p = presses.remove(pointer);
        if (p == null) return;
        clearCallbacks(p);
        if (bubbleKey == p.kr) bubbleKey = null;
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
            case "Ime":
                if (multiLang) listener.nextLanguage();
                else listener.switchKeyboard();
                return;
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
        if (mods[SHIFT] == 0 || id.equals("ß")) return id;
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
