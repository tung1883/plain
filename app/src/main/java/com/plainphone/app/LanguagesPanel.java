package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings > Languages: which input languages are on, their order in the globe cycle (drag the
 * handle), and each one's layout. Applied immediately.
 */
final class LanguagesPanel extends LinearLayout {

    private final KeyboardRoot.Host host;
    private final DragList rows;
    private List<InputLang> enabled = new ArrayList<>();

    LanguagesPanel(Context ctx, KeyboardRoot.Host host, Runnable onBack) {
        super(ctx);
        this.host = host;
        setOrientation(VERTICAL);

        LinearLayout head = new LinearLayout(ctx);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(PanelKit.dp(ctx, 4), 0, 0, 0);
        TextView cap = PanelKit.caption(ctx, "LANGUAGES");
        head.addView(cap, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(PanelKit.link(ctx, "Back", v -> onBack.run()));
        addView(head, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelKit.dp(ctx, 30)));

        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        rows = new DragList(ctx, this::saveOrder);
        rows.setOrientation(VERTICAL);
        scroll.addView(rows, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    void refresh() {
        enabled = InputLang.parse(Config.getKbLangs(getContext()));
        build();
    }

    private void build() {
        Context ctx = getContext();
        rows.removeAllViews();
        for (InputLang l : enabled) rows.addView(row(ctx, l, true), rowLp(ctx));
        for (InputLang l : InputLang.ALL) {
            if (!enabled.contains(l)) rows.addView(row(ctx, l, false), rowLp(ctx));
        }
    }

    private static LayoutParams rowLp(Context ctx) {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelKit.dp(ctx, 52));
    }

    private View row(Context ctx, InputLang l, boolean on) {
        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, PanelKit.dp(ctx, 4), 0);
        row.setTag(on ? l : null);

        View grip = new Grip(ctx);
        grip.setVisibility(on ? VISIBLE : INVISIBLE);
        if (on) {
            grip.setOnTouchListener((v, e) -> {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        rows.startDrag(row);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        rows.endDrag();
                        return true;
                    default:
                        return true;
                }
            });
        }
        row.addView(grip, new LayoutParams(PanelKit.dp(ctx, 28), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(VERTICAL);
        LinearLayout line1 = new LinearLayout(ctx);
        line1.setGravity(Gravity.CENTER_VERTICAL);
        TextView tag = new TextView(ctx);
        tag.setText(l.tag);
        tag.setTextSize(9);
        tag.setTextColor(0xFF888888);
        tag.setTypeface(Fonts.cascadiaMono(ctx));
        tag.setPadding(PanelKit.dp(ctx, 4), 0, PanelKit.dp(ctx, 4), 0);
        tag.setBackground(UiKit.rounded(ctx, 0x00000000, 0xFF333333, 1f, 5));
        line1.addView(tag);
        TextView name = new TextView(ctx);
        name.setText(l.nativeName);
        name.setTextSize(14);
        name.setTextColor(on ? PanelKit.FG : 0xFF707070);
        name.setTypeface(Fonts.cascadiaMono(ctx));
        name.setPadding(PanelKit.dp(ctx, 8), 0, 0, 0);
        line1.addView(name);
        text.addView(line1);
        TextView sub = new TextView(ctx);
        sub.setText(on ? "layout: " + l.layoutOrDefault(Config.getKbLayout(ctx, l.id)) : "off");
        sub.setTextSize(10.5f);
        sub.setTextColor(PanelKit.DIM);
        sub.setTypeface(Fonts.cascadiaMono(ctx));
        text.addView(sub);
        row.addView(text, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (on && l.layouts.length > 1) {
            String cur = l.layoutOrDefault(Config.getKbLayout(ctx, l.id));
            TextView chip = PanelKit.chipSmall(ctx, cur, false, v -> {
                Config.setKbLayout(ctx, l.id, l.nextLayout(cur));
                host.settingsChanged();
                refresh();
            });
            LayoutParams clp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = PanelKit.dp(ctx, 8);
            row.addView(chip, clp);
        }

        View sw = PanelKit.toggle(ctx, on, now -> {
            if (now) {
                enabled.add(l);
            } else if (enabled.size() > 1) {
                enabled.remove(l);
            }
            save();
            refresh();
        });
        row.addView(sw);

        View line = new View(ctx);
        line.setBackgroundColor(0xFF181818);
        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setOrientation(VERTICAL);
        wrap.setTag(row.getTag());
        wrap.addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelKit.dp(ctx, 51)));
        wrap.addView(line, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return wrap;
    }

    /** The drag ended: read the new order off the rows. */
    private void saveOrder() {
        List<InputLang> order = new ArrayList<>();
        for (int i = 0; i < rows.getChildCount(); i++) {
            Object t = rows.getChildAt(i).getTag();
            if (t instanceof InputLang) order.add((InputLang) t);
        }
        if (!order.isEmpty()) enabled = order;
        save();
    }

    private void save() {
        Config.setKbLangs(getContext(), InputLang.toCsv(enabled));
        host.settingsChanged();
    }

    /** A column that lets one row be dragged up and down by its handle. */
    private static final class DragList extends LinearLayout {
        private final Runnable onDone;
        private View dragging;

        DragList(Context ctx, Runnable onDone) {
            super(ctx);
            this.onDone = onDone;
        }

        void startDrag(View row) {
            dragging = row;
            row.setBackgroundColor(0xFF161616);
        }

        void endDrag() {
            if (dragging == null) return;
            dragging.setBackgroundColor(0x00000000);
            dragging = null;
            onDone.run();
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            return dragging != null && ev.getActionMasked() == MotionEvent.ACTION_MOVE;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (dragging == null) return false;
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_MOVE: {
                    // swap with the language row under the finger (only the enabled rows can move)
                    View row = dragging;
                    while (row != null && row.getParent() != this) row = (View) row.getParent();
                    if (row == null) return true;
                    int[] loc = new int[2];
                    float y = ev.getRawY();
                    for (int i = 0; i < getChildCount(); i++) {
                        View c = getChildAt(i);
                        if (!(c.getTag() instanceof InputLang) || c == row) continue;
                        c.getLocationOnScreen(loc);
                        if (y >= loc[1] && y < loc[1] + c.getHeight()) {
                            int from = indexOfChild(row);
                            removeViewAt(from);
                            addView(row, i);
                            break;
                        }
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    endDrag();
                    return true;
                default:
                    return true;
            }
        }
    }

    /** Six dots: the drag handle. */
    private static final class Grip extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Grip(Context c) {
            super(c);
            p.setColor(0xFF666666);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, d = PanelKit.dp(getContext(), 4.5f);
            float r = PanelKit.dp(getContext(), 1.4f);
            for (int i = -1; i <= 1; i++) {
                canvas.drawCircle(cx - d / 2f, cy + i * d, r, p);
                canvas.drawCircle(cx + d / 2f, cy + i * d, r, p);
            }
        }
    }
}
