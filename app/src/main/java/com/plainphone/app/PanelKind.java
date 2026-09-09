package com.plainphone.app;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

/**
 * The catalogue of things a {@link WorkspaceActivity} window can be. The {@code +}
 * menu is built from this, and {@link WorkspaceStore} restores through it.
 */
final class PanelKind {

    interface Factory {
        PanelContent create(Context ctx, String hostId, String extra);
    }

    final String id;
    final String label;
    final boolean needsDevice;
    final Factory factory;

    private PanelKind(String id, String label, boolean needsDevice, Factory factory) {
        this.id = id;
        this.label = label;
        this.needsDevice = needsDevice;
        this.factory = factory;
    }

    static String devLabel(Context c, String hostId) {
        DevHost h = DevHost.find(c, hostId);
        return h != null ? h.label : hostId;
    }

    private static long parseId(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return -1; }
    }

    static final List<PanelKind> ALL = Arrays.asList(
            new PanelKind("shell", "Shell", true,
                    (c, h, x) -> new ShellPanel(devLabel(c, h), h, parseId(x))),
            new PanelKind("screen", "Screen", true,
                    (c, h, x) -> new ScreenPanel(devLabel(c, h), h)),
            new PanelKind("proc", "Processes", true,
                    (c, h, x) -> new ProcPanel(devLabel(c, h), h)),
            new PanelKind("notes", "Notes", false,
                    (c, h, x) -> new NotesPanel()),
            new PanelKind("todo", "To-do", false,
                    (c, h, x) -> new TodoPanel()),
            new PanelKind("recorder", "Recorder", false,
                    (c, h, x) -> new RecorderPanel()),
            new PanelKind("web", "Web", false,
                    (c, h, x) -> new WebPanel(x))
    );

    static PanelKind byId(String id) {
        for (PanelKind k : ALL) if (k.id.equals(id)) return k;
        return null;
    }
}
