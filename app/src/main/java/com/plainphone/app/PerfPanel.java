package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A device-perf window: {@link ProcSurface}, {@link NetworkSurface} and
 * {@link StorageSurface} sharing one panel behind a tab strip, instead of
 * three separate {@link PanelKind}s. The tab strip is a straight copy of the
 * home screen's own mode toggle — {@code MainActivity.buildModeToggle} /
 * {@code headerChevron} / {@code updateHeaderChevrons} — same font
 * ({@link Fonts#current}, whatever the user picked in Settings), sizes,
 * spacing, colors and chevron-dim behavior, minus the long-press reordering
 * (nothing to persist here).
 */
final class PerfPanel implements PanelContent {

    private static final String[] LABELS = {"Processes", "Network", "Storage"};

    private final String hostLabel;
    private final String hostId;

    private View[] surfaces;   // ProcSurface, NetworkSurface, StorageSurface, in LABELS order
    private TextView[] tabs;
    private TextView chevLeft, chevRight;
    private HorizontalScrollView scroller;
    private int active = 0;
    private boolean shownGate;   // whether the panel itself is currently visible

    PerfPanel(String hostLabel, String hostId) {
        this.hostLabel = hostLabel;
        this.hostId = hostId;
    }

    @Override public boolean needsConnection() { return true; }
    @Override public String hostId() { return hostId; }
    @Override public String kind() { return "perf"; }
    @Override public String title() { return hostLabel + " · perf"; }

    @Override
    public View onCreate(Context ctx) {
        surfaces = new View[]{new ProcSurface(ctx), new NetworkSurface(ctx), new StorageSurface(ctx)};

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.addView(buildTabStrip(ctx), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout body = new FrameLayout(ctx);
        for (View v : surfaces) {
            body.addView(v, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        root.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        select(0);
        return root;
    }

    // --- tab strip: exact copy of MainActivity.buildModeToggle ---------

    private View buildTabStrip(Context ctx) {
        Typeface font = Fonts.current(ctx);   // whatever font the user picked, same as the home toggle

        LinearLayout tabRow = new LinearLayout(ctx);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(20, 28, 20, 20);

        tabs = new TextView[LABELS.length];
        for (int i = 0; i < LABELS.length; i++) {
            final int idx = i;
            TextView t = new TextView(ctx);
            t.setText(LABELS[i].toUpperCase());
            t.setTypeface(font);
            t.setTextSize(13);
            t.setLetterSpacing(0.15f);
            t.setPadding(0, 8, i == LABELS.length - 1 ? 0 : 56, 8);
            t.setOnClickListener(v -> select(idx));
            tabs[i] = t;
            tabRow.addView(t, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        scroller = new HorizontalScrollView(ctx);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.addView(tabRow, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroller.setOnScrollChangeListener((v, x, y, ox, oy) -> updateChevrons());

        chevLeft = headerChevron(ctx, "‹");
        chevRight = headerChevron(ctx, "›");

        // Centered, unlike the home toggle (which is left-anchored for many
        // tabs) — three short labels read better centered in a narrow panel.
        LinearLayout strip = new LinearLayout(ctx);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER);
        strip.setBackgroundColor(Color.BLACK);
        strip.addView(chevLeft, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        strip.addView(scroller, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        strip.addView(chevRight, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        scroller.post(this::updateChevrons);
        return strip;
    }

    private TextView headerChevron(Context ctx, String glyph) {
        TextView t = new TextView(ctx);
        t.setText(glyph);
        t.setTextColor(0xFF5C5C5C);
        t.setTextSize(15);
        t.setTypeface(Fonts.current(ctx));
        t.setGravity(Gravity.CENTER);
        t.setPadding(14, 0, 14, 0);
        t.setVisibility(View.INVISIBLE);   // space reserved; the strip never re-flows
        return t;
    }

    /** Show a chevron only while the strip can still scroll that way, and dim any
     *  tab the viewport edge is cutting through so the clip looks intentional. */
    private void updateChevrons() {
        if (scroller == null) return;
        chevLeft.setVisibility(scroller.canScrollHorizontally(-1) ? View.VISIBLE : View.INVISIBLE);
        chevRight.setVisibility(scroller.canScrollHorizontally(1) ? View.VISIBLE : View.INVISIBLE);
        int start = scroller.getScrollX();
        int end = start + scroller.getWidth();
        for (TextView tab : tabs) {
            boolean clipped = tab.getLeft() < start || tab.getRight() > end;
            tab.setAlpha(clipped ? 0.35f : 1f);
        }
    }

    private void select(int idx) {
        active = idx;
        for (int i = 0; i < surfaces.length; i++) {
            surfaces[i].setVisibility(i == idx ? View.VISIBLE : View.GONE);
            setShown(i, shownGate && i == idx);
        }
        for (int i = 0; i < tabs.length; i++) {
            boolean on = i == idx;
            tabs[i].setTextColor(on ? Color.WHITE : Color.DKGRAY);
            tabs[i].setTypeface(Typeface.create(tabs[i].getTypeface(), on ? Typeface.BOLD : Typeface.NORMAL));
        }
    }

    // --- surfaces --------------------------------------------------------

    private void setShown(int i, boolean shown) {
        switch (i) {
            case 0: ((ProcSurface) surfaces[0]).setShown(shown); break;
            case 1: ((NetworkSurface) surfaces[1]).setShown(shown); break;
            case 2: ((StorageSurface) surfaces[2]).setShown(shown); break;
        }
    }

    @Override
    public void onConnection(DevConnection conn) {
        ((ProcSurface) surfaces[0]).attach(conn);
        ((NetworkSurface) surfaces[1]).attach(conn);
        ((StorageSurface) surfaces[2]).attach(conn);
    }

    @Override public void onShow() { shownGate = true; setShown(active, true); }
    @Override public void onHide() { shownGate = false; for (int i = 0; i < surfaces.length; i++) setShown(i, false); }
    @Override public void onLeave() { onHide(); }

    @Override
    public void onClose() {
        ((ProcSurface) surfaces[0]).detach();
        ((NetworkSurface) surfaces[1]).detach();
        ((StorageSurface) surfaces[2]).detach();
    }
}
