package com.plainphone.app;

import android.content.Context;
import android.view.View;

/** A device-network window: a {@link NetworkSurface} in a panel. */
final class NetworkPanel implements PanelContent {

    private final String hostLabel;
    private final String hostId;
    private NetworkSurface surface;

    NetworkPanel(String hostLabel, String hostId) {
        this.hostLabel = hostLabel;
        this.hostId = hostId;
    }

    @Override public boolean needsConnection() { return true; }
    @Override public String hostId() { return hostId; }
    @Override public String kind() { return "net"; }
    @Override public String title() { return hostLabel + " · network"; }

    @Override public View onCreate(Context ctx) { surface = new NetworkSurface(ctx); return surface; }
    @Override public void onConnection(DevConnection conn) { surface.attach(conn); }
    @Override public void onShow() { surface.setShown(true); }
    @Override public void onHide() { surface.setShown(false); }
    @Override public void onLeave() { surface.setShown(false); }
    @Override public void onClose() { surface.detach(); }
}
