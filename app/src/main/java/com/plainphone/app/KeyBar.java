package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The special-keys bar under the shell and the Screen panel: grouped soft keys that scroll
 * vertically, a few rows showing at a time (the Rows shown setting).
 * It only draws keys and tracks Ctrl/Alt/Shift; what a key does is up to the {@link Listener}.
 * Ctrl, Alt and Shift: tap = armed for the next key, double-tap = locked, tap again = off.
 */
final class KeyBar extends LinearLayout {

    interface Listener {
        /** A non-modifier key went down, by its label ("Tab", "Left", "^C", "F5", "|" ...). */
        void onKeyDown(String id);

        /** The finger came off that key (or the bar started scrolling instead). */
        default void onKeyUp(String id) {}

        /** Ctrl/Alt/Shift changed: the set of modifiers now in effect ("Ctrl", "Alt", "Shift"). */
        default void onMods(Set<String> active) {}
    }

    private static final String[] MODS = {"Ctrl", "Alt", "Shift"};
    private static final long DOUBLE_TAP_MS = 350;
    private static final int LABEL_H = 26;   // dp, a section label
    private static final int GAP = 4;        // dp, margin on every side of a key (8 between two keys)
    private static final int PEEK = 14;      // dp, a sliver of the next row so it is clear there is more

    private static final String[][] KEYS_TOP = {
            {"Esc", "Tab", "Ctrl", "Alt", "Shift", "^C"},
            {"Left", "Down", "Up", "Right", "Bksp", "Enter"},
    };

    /** label (null = none), then rows. */
    private static final Object[][] SECTIONS = {
            {"NAVIGATE", new String[][]{{"Home", "End", "PgUp", "PgDn", "Ins", "Del"}}},
            {"CONTROL", new String[][]{{"^D", "^Z", "^L", "^R", "^A", "^E"}, {"^W", "^U", "^K", "^X", "^V", "^B"}}},
            {"FUNCTION", new String[][]{{"F1", "F2", "F3", "F4", "F5", "F6"}, {"F7", "F8", "F9", "F10", "F11", "F12"}}},
            {"SYMBOLS", new String[][]{{"|", "~", "\\", "/", "-", "`"}, {"_", "=", "[", "]", "{", "}"}}},
    };
    private static final String[][] SCREEN_ONLY = {{"PrtSc", "Win", "Menu", "Caps", "", ""}};

    private final boolean screenPanel;
    private final Listener listener;
    private final LinkedHashSet<String> armed = new LinkedHashSet<>();
    private final LinkedHashSet<String> locked = new LinkedHashSet<>();
    private final Map<String, Long> lastTap = new LinkedHashMap<>();
    private final Map<String, TextView> modViews = new LinkedHashMap<>();

    private final BoundedScrollView scroller;
    private final View fade;

    private int keyH;
    private int rowsShown;           // 0 = all
    private int totalKeyRows;
    private final List<Integer> itemHeights = new ArrayList<>();   // dp, top to bottom: labels and rows
    private final List<Boolean> itemIsRow = new ArrayList<>();

    KeyBar(Context ctx, boolean screenPanel, Listener listener) {
        super(ctx);
        this.screenPanel = screenPanel;
        this.listener = listener;
        setOrientation(VERTICAL);
        setBackgroundColor(0xFF0A0A0A);

        scroller = new BoundedScrollView(ctx);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setOverScrollMode(OVER_SCROLL_NEVER);
        fade = new View(ctx);

        FrameLayout stack = new FrameLayout(ctx);
        stack.addView(scroller, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        GradientDrawable shade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x000A0A0A, 0xFF0A0A0A});
        fade.setBackground(shade);
        stack.addView(fade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(22), Gravity.BOTTOM));

        scroller.setOnScrollChangeListener((v, x, y, ox, oy) -> updateMore());
        scroller.getViewTreeObserver().addOnGlobalLayoutListener(this::updateMore);

        addView(stack, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        reload();
    }

    @Override
    public void setVisibility(int visibility) {
        if (visibility == VISIBLE) reload();   // pick up changes made in the Key bar settings
        super.setVisibility(visibility);
    }

    // ---------------------------------------------------------------- modifiers

    Set<String> active() {
        Set<String> out = new LinkedHashSet<>(locked);
        out.addAll(armed);
        return out;
    }

    /**
     * The modifiers in effect for the key being sent now, lower-case, or null if none. Armed ones
     * clear afterwards; locked ones stay on.
     */
    List<String> consumeMods() {
        Set<String> now = active();
        List<String> out = null;
        if (!now.isEmpty()) {
            out = new ArrayList<>();
            for (String m : now) out.add(m.toLowerCase(java.util.Locale.ROOT));
        }
        if (!armed.isEmpty()) {
            armed.clear();
            paintMods();
        }
        listener.onMods(active());   // a locked modifier is re-armed on the other side
        return out;
    }

    private void onModTap(String mod) {
        long now = System.currentTimeMillis();
        Long before = lastTap.get(mod);
        if (locked.contains(mod)) {
            locked.remove(mod);
        } else if (armed.contains(mod)) {
            armed.remove(mod);
            if (before != null && now - before < DOUBLE_TAP_MS) locked.add(mod);
        } else {
            armed.add(mod);
        }
        lastTap.put(mod, now);
        paintMods();
        listener.onMods(active());
    }

    private void paintMods() {
        for (Map.Entry<String, TextView> e : modViews.entrySet()) {
            String m = e.getKey();
            paintKey(e.getValue(), locked.contains(m) ? 2 : armed.contains(m) ? 1 : 0, false);
        }
    }

    // ------------------------------------------------------------------ building

    private void reload() {
        keyH = keyHeightDp();
        rowsShown = Config.getKeyBarRows(getContext());
        boolean labels = Config.isKeyBarLabels(getContext());

        itemHeights.clear();
        itemIsRow.clear();
        modViews.clear();
        totalKeyRows = 0;

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(VERTICAL);
        content.setPadding(dp(8), dp(8), dp(8), dp(8));

        for (String[] row : KEYS_TOP) addRow(content, row, false);
        for (Object[] section : SECTIONS) {
            if (labels) addLabel(content, (String) section[0]);
            for (String[] row : (String[][]) section[1]) addRow(content, row, false);
        }
        if (screenPanel) {
            if (labels) addLabel(content, "SCREEN ONLY");
            for (String[] row : SCREEN_ONLY) addRow(content, row, true);
        }

        scroller.removeAllViews();
        scroller.addView(content);
        scroller.setCapPx(initialCapPx());
        paintMods();
        scroller.post(this::updateMore);
    }

    private void addLabel(LinearLayout into, String text) {
        TextView t = new TextView(getContext());
        t.setText(text);
        t.setTextSize(9.5f);
        t.setLetterSpacing(0.22f);
        t.setTextColor(0xFF4F4F4F);
        t.setTypeface(Fonts.cascadiaMono(getContext()));
        t.setGravity(Gravity.BOTTOM);
        t.setPadding(dp(6), 0, 0, dp(2));
        into.addView(t, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(LABEL_H)));
        itemHeights.add(LABEL_H);
        itemIsRow.add(false);
    }

    private void addRow(LinearLayout into, String[] ids, boolean screenOnly) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        for (String id : ids) row.addView(makeKey(id, screenOnly));
        into.addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        itemHeights.add(keyH + 2 * GAP);
        itemIsRow.add(true);
        totalKeyRows++;
    }

    private View makeKey(String id, boolean screenOnly) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(keyH), 1f);
        lp.setMargins(dp(GAP), dp(GAP), dp(GAP), dp(GAP));
        if (id.isEmpty()) {
            View spacer = new View(getContext());
            spacer.setLayoutParams(lp);
            return spacer;
        }
        TextView k = new TextView(getContext());
        k.setText(label(id));
        k.setTextSize(14);
        k.setTypeface(Fonts.cascadiaMono(getContext()));
        k.setGravity(Gravity.CENTER);
        k.setPadding(dp(2), 0, dp(2), 0);
        k.setLayoutParams(lp);
        k.setSingleLine(true);
        boolean isMod = false;
        for (String m : MODS) if (m.equals(id)) isMod = true;
        if (isMod) {
            modViews.put(id, k);
            k.setOnClickListener(v -> onModTap(id));
        } else {
            // Down and up are told apart so a key can really be held: down while the finger is on it.
            k.setOnTouchListener((v, ev) -> {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        listener.onKeyDown(id);
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        listener.onKeyUp(id);
                        return true;
                    case MotionEvent.ACTION_CANCEL:   // e.g. the bar took the drag to scroll
                        v.setPressed(false);
                        listener.onKeyUp(id);
                        return true;
                    default:
                        return true;
                }
            });
        }
        paintKey(k, 0, screenOnly);
        return k;
    }

    /** What a key says; the arrow keys use arrows, everything else its own name. */
    private static String label(String id) {
        switch (id) {
            case "Left": return "←";
            case "Down": return "↓";
            case "Up": return "↑";
            case "Right": return "→";
            default: return id;
        }
    }

    /** state: 0 normal, 1 armed, 2 locked. */
    private void paintKey(TextView k, int state, boolean screenOnly) {
        float r = dp(9);
        if (state == 0) {
            StateListDrawable s = new StateListDrawable();
            s.addState(new int[]{android.R.attr.state_pressed}, box(0xFF262626, 0xFF242424, r));
            s.addState(new int[]{}, box(0xFF121212, 0xFF242424, r));
            s.setEnterFadeDuration(120);
            s.setExitFadeDuration(120);
            k.setBackground(s);
            k.setTextColor(screenOnly ? 0xFF7D8AA0 : 0xFFB5B5B5);
            return;
        }
        GradientDrawable white = box(Color.WHITE, Color.WHITE, r);
        if (state == 2) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(Color.BLACK);
            LayerDrawable layers = new LayerDrawable(new android.graphics.drawable.Drawable[]{white, dot});
            layers.setLayerSize(1, dp(5), dp(5));
            layers.setLayerGravity(1, Gravity.TOP | Gravity.END);
            layers.setLayerInset(1, 0, dp(5), dp(6), 0);
            k.setBackground(layers);
        } else {
            k.setBackground(white);
        }
        k.setTextColor(Color.BLACK);
    }

    private GradientDrawable box(int fill, int stroke, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setStroke(dp(1), stroke);
        g.setCornerRadius(radius);
        return g;
    }

    // -------------------------------------------------------------------- height

    private int keyHeightDp() {
        switch (Config.getKeyBarSize(getContext())) {
            case "small": return 34;
            case "large": return 50;
            default: return 42;
        }
    }

    private int initialCapPx() {
        return rowsShown == 0 ? Integer.MAX_VALUE : heightForRowsPx(rowsShown);
    }

    /** Pixel height that shows {@code n} key rows (and any labels between them) plus a peek. */
    private int heightForRowsPx(int n) {
        int dpSum = 0;
        int rows = 0;
        for (int i = 0; i < itemHeights.size() && rows < n; i++) {
            dpSum += itemHeights.get(i);
            if (itemIsRow.get(i)) rows++;
        }
        return dp(dpSum + PEEK);
    }

    private void updateMore() {
        View child = scroller.getChildAt(0);
        boolean below = child != null
                && child.getBottom() - (scroller.getScrollY() + scroller.getHeight()) > dp(6);
        fade.setVisibility(below ? VISIBLE : GONE);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    /** A vertical scroller whose height stops at a cap, however tall its content is. */
    private static final class BoundedScrollView extends ScrollView {
        private int capPx = Integer.MAX_VALUE;

        BoundedScrollView(Context c) {
            super(c);
        }

        void setCapPx(int px) {
            if (px != capPx) {
                capPx = px;
                requestLayout();
            }
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int size = MeasureSpec.getSize(heightSpec);
            int most = size == 0 ? capPx : Math.min(capPx, size);
            super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(most, MeasureSpec.AT_MOST));
        }
    }
}
