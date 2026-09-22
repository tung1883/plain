package com.plainphone.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Wraps a Dev-plugin Workspace window (shell/screen/perf) with the same PIN gate
 * the Home screen's Dev section already enforces ({@code Lock.DEV}) — a Workspace
 * window used to skip that check entirely, so a locked Dev section still let you
 * open a live shell/screen/perf panel straight from the "+" menu or a restored
 * layout. The delegate's connection is withheld while locked, so an attach never
 * reaches the terminal/session underneath the gate.
 */
final class DevLockedPanel implements PanelContent {

    private final PanelContent inner;
    private Context ctx;
    private FrameLayout root;
    private LockGate gate;
    private DevConnection pendingConn;

    DevLockedPanel(PanelContent inner) {
        this.inner = inner;
    }

    @Override
    public View onCreate(Context ctx) {
        this.ctx = ctx;
        root = new FrameLayout(ctx);
        View innerView = inner.onCreate(ctx);
        root.addView(innerView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        gate = new LockGate(ctx, "Tap to unlock Dev", () -> {
            if (ctx instanceof android.app.Activity) ctx.startActivity(Lock.DEV.pinGate(ctx));
        });
        gate.setVisibility(View.GONE);
        root.addView(gate, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        refreshGate();
        return root;
    }

    private void refreshGate() {
        if (gate == null) return;
        if (Lock.DEV.gateActive(ctx)) {
            gate.setVisibility(View.VISIBLE);
            return;
        }
        Lock.DEV.keepUnlocked(ctx);
        gate.setVisibility(View.GONE);
        if (pendingConn != null) {
            DevConnection c = pendingConn;
            pendingConn = null;
            inner.onConnection(c);
        }
    }

    @Override public String title() { return inner.title(); }
    @Override public View[] titleButtons(Context ctx) { return inner.titleButtons(ctx); }
    @Override public void setTitleListener(Runnable onChanged) { inner.setTitleListener(onChanged); }
    @Override public boolean needsConnection() { return inner.needsConnection(); }
    @Override public String hostId() { return inner.hostId(); }
    @Override public String kind() { return inner.kind(); }
    @Override public String saveExtra() { return inner.saveExtra(); }

    @Override
    public void onConnection(DevConnection conn) {
        // Held back until the gate clears — see refreshGate().
        if (gate != null && Lock.DEV.gateActive(ctx)) {
            pendingConn = conn;
            return;
        }
        inner.onConnection(conn);
    }

    @Override public void onFocus() { inner.onFocus(); }
    @Override public void onShow() { refreshGate(); inner.onShow(); }
    @Override public void onHide() { inner.onHide(); }
    @Override public boolean confirmClose() { return inner.confirmClose(); }
    @Override public void onLeave() { inner.onLeave(); }
    @Override public void onClose() { inner.onClose(); }
}
