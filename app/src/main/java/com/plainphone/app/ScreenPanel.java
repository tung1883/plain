package com.plainphone.app;

import android.content.Context;
import android.view.View;

/** A screen-mirror window: a {@link ScreenSurface} (identical to the full-screen mirror) in a panel. */
final class ScreenPanel implements PanelContent {

    private final String hostLabel;
    private final String hostId;
    private ScreenSurface surface;

    ScreenPanel(String hostLabel, String hostId) {
        this.hostLabel = hostLabel;
        this.hostId = hostId;
    }

    @Override public boolean needsConnection() { return true; }

    @Override public String hostId() { return hostId; }

    @Override public String kind() { return "screen"; }

    @Override
    public View onCreate(Context ctx) {
        surface = new ScreenSurface(ctx);
        return surface;
    }

    @Override public String title() { return hostLabel + " · screen"; }

    @Override public View[] titleButtons(Context ctx) {
        return new View[]{ surface.keyboardButton(ctx) };
    }

    @Override public void onConnection(DevConnection conn) { surface.attach(conn); }

    @Override public void onLeave() { surface.detach(); }

    @Override public void onClose() { surface.release(); }
}
