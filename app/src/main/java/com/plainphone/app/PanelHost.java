package com.plainphone.app;

import android.content.Context;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * The desktop surface of {@link WorkspaceActivity}: holds the floating
 * {@link Panel}s, cascades new ones, keeps the tapped one on top, and forwards
 * open / close / minimize / move to a {@link Listener} (taskbar + persistence).
 */
final class PanelHost extends FrameLayout implements Panel.Host {

    interface Listener {
        void onPanelsChanged();
    }

    private final List<Panel> panels = new ArrayList<>();
    private final float density;
    private Listener listener;
    private int spawnCount;

    PanelHost(Context ctx) {
        super(ctx);
        this.density = ctx.getResources().getDisplayMetrics().density;
        setBackgroundColor(0xFF060606);
    }

    void setListener(Listener l) { this.listener = l; }

    List<Panel> panels() { return panels; }

    /** Add a panel at a cascaded default position/size. */
    Panel add(PanelContent content) {
        int w = (int) (getMeasuredWidth() > 0 ? getMeasuredWidth() * 0.82f : dp(300));
        int h = (int) (getMeasuredHeight() > 0 ? getMeasuredHeight() * 0.5f : dp(320));
        int step = dp(26) * (spawnCount % 6);
        return addAt(content, dp(12) + step, dp(12) + step, w, h, false);
    }

    /** Add a panel at an explicit position/size (used when restoring a saved workspace). */
    Panel addAt(PanelContent content, int x, int y, int w, int h, boolean minimized) {
        Panel p = new Panel(getContext(), content, this);
        addView(p, new LayoutParams(Math.max(w, 1), Math.max(h, 1)));
        p.placeAt(x, y, w, h);
        panels.add(p);
        spawnCount++;
        if (minimized) {
            p.setVisibility(GONE);
        } else {
            p.bringToFront();
            p.content.onFocus();
        }
        notifyChanged();
        return p;
    }

    interface ConnResolver {
        DevConnection connFor(String hostId);
    }

    /** Push the current connection (per host) into every connection-backed panel. */
    void rebind(ConnResolver r) {
        for (Panel p : panels) {
            if (p.content.needsConnection()) p.content.onConnection(r.connFor(p.content.hostId()));
        }
    }

    /** Workspace backgrounded / destroyed — stop streams, keep sessions alive. */
    void leaveAll() {
        for (Panel p : panels) p.content.onLeave();
    }

    // --- Panel.Host ----------------------------------------------------

    @Override public void onFocusPanel(Panel p) {
        p.bringToFront();
        p.setVisibility(VISIBLE);
        p.content.onFocus();
        notifyChanged();
    }

    @Override public void onClosePanel(Panel p) {
        p.content.onClose();
        panels.remove(p);
        removeView(p);
        notifyChanged();
    }

    @Override public void onMinimizePanel(Panel p) {
        p.setVisibility(GONE);
        notifyChanged();
    }

    @Override public void onPanelMoved() {
        notifyChanged();
    }

    @Override public int hostWidth() {
        return getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
    }

    @Override public int hostHeight() {
        return getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
    }

    void restore(Panel p) {
        p.setVisibility(VISIBLE);
        onFocusPanel(p);
    }

    private void notifyChanged() {
        if (listener != null) listener.onPanelsChanged();
    }

    private int dp(float v) { return Math.round(v * density); }
}
