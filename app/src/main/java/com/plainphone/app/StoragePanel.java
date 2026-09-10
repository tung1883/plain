package com.plainphone.app;

import android.content.Context;
import android.view.View;

/** A device-storage window: a {@link StorageSurface} in a panel. */
final class StoragePanel implements PanelContent {

    private final String hostLabel;
    private final String hostId;
    private StorageSurface surface;

    StoragePanel(String hostLabel, String hostId) {
        this.hostLabel = hostLabel;
        this.hostId = hostId;
    }

    @Override public boolean needsConnection() { return true; }
    @Override public String hostId() { return hostId; }
    @Override public String kind() { return "disk"; }
    @Override public String title() { return hostLabel + " · storage"; }

    @Override public View onCreate(Context ctx) { surface = new StorageSurface(ctx); return surface; }
    @Override public void onConnection(DevConnection conn) { surface.attach(conn); }
    @Override public void onShow() { surface.setShown(true); }
    @Override public void onHide() { surface.setShown(false); }
    @Override public void onLeave() { surface.setShown(false); }
    @Override public void onClose() { surface.detach(); }
}
