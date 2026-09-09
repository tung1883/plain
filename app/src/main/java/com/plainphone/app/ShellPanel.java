package com.plainphone.app;

import android.content.Context;
import android.view.View;

/** A shell window: a {@link ShellSurface} (identical to the full-screen shell) in a panel. */
final class ShellPanel implements PanelContent {

    private final String hostLabel;
    private final String hostId;
    private final long initialSessionId;
    private ShellSurface surface;
    private String name;
    private Runnable onTitleChanged;

    ShellPanel(String hostLabel, String hostId, long sessionId) {
        this.hostLabel = hostLabel;
        this.hostId = hostId;
        this.initialSessionId = sessionId;
    }

    @Override public void setTitleListener(Runnable r) { this.onTitleChanged = r; }

    @Override public boolean needsConnection() { return true; }

    @Override public String hostId() { return hostId; }

    @Override public String kind() { return "shell"; }

    @Override public String saveExtra() {
        return surface != null ? String.valueOf(surface.sessionId()) : String.valueOf(initialSessionId);
    }

    @Override
    public View onCreate(Context ctx) {
        surface = new ShellSurface(ctx);
        surface.setSessionId(initialSessionId);
        surface.setCallbacks(new ShellSurface.Callbacks() {
            @Override public void onTitle(String n) {
                name = n;
                if (onTitleChanged != null) onTitleChanged.run();
            }
        });
        return surface;
    }

    @Override public String title() {
        return hostLabel + " · " + (name != null ? name : "shell");
    }

    @Override public View[] titleButtons(Context ctx) {
        return new View[]{ surface.keyboardButton(ctx) };
    }

    @Override public void onConnection(DevConnection conn) { surface.attach(conn); }

    @Override public void onLeave() { surface.detachKeepAlive(); }

    @Override public void onClose() { surface.closeKill(); }
}
