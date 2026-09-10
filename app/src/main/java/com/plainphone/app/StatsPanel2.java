package com.plainphone.app;

import android.content.Context;
import android.view.View;

/** A device-stats window: a {@link StatsSurface} in a panel. */
final class StatsPanel2 implements PanelContent {

    private final String hostLabel;
    private final String hostId;
    private StatsSurface surface;

    StatsPanel2(String hostLabel, String hostId) {
        this.hostLabel = hostLabel;
        this.hostId = hostId;
    }

    @Override public boolean needsConnection() { return true; }
    @Override public String hostId() { return hostId; }
    @Override public String kind() { return "stats"; }
    @Override public String title() { return hostLabel + " · stats"; }

    @Override public View onCreate(Context ctx) { surface = new StatsSurface(ctx); return surface; }
    @Override public void onConnection(DevConnection conn) { surface.attach(conn); }
    @Override public void onShow() { surface.setShown(true); }
    @Override public void onHide() { surface.setShown(false); }
    @Override public void onLeave() { surface.setShown(false); }
    @Override public void onClose() { surface.detach(); }
}
