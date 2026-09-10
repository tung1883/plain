package com.plainphone.app;

import java.util.List;

/** A recorder window: the full home Recorder section, in a panel. */
final class RecorderPanel extends PluginPanel {

    @Override public String kind() { return "recorder"; }
    @Override public String title() { return "Recorder"; }
    @Override HomeMode section() { return HomeMode.RECORDER; }

    @Override void renderNormal(List<Object> rows) { RecorderSection.render(this, rows); }
    @Override void renderSelectionRows(List<Object> rows) { RecorderSection.renderSelection(this, rows); }
    @Override String selectionIdOf(Object payload) { return RecorderSection.selectionId(payload); }
}
