package com.plainphone.app;

import android.content.Context;
import android.view.View;

/** A process-list window: a {@link ProcSurface} (identical to the full-screen view) in a panel. */
final class ProcPanel implements PanelContent {

    private final String hostLabel;
    private final String hostId;
    private ProcSurface surface;

    ProcPanel(String hostLabel, String hostId) {
        this.hostLabel = hostLabel;
        this.hostId = hostId;
    }

    @Override public boolean needsConnection() { return true; }

    @Override public String hostId() { return hostId; }

    @Override public String kind() { return "proc"; }

    @Override
    public View onCreate(Context ctx) {
        surface = new ProcSurface(ctx);
        return surface;
    }

    @Override public String title() { return hostLabel + " · processes"; }

    @Override public void onConnection(DevConnection conn) { surface.attach(conn); }

    @Override public void onClose() { surface.detach(); }
}
