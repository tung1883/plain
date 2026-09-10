package com.plainphone.app;

import java.util.List;

/** A to-do window: the full home To-do section, in a panel. */
final class TodoPanel extends PluginPanel {

    @Override public String kind() { return "todo"; }
    @Override public String title() { return "To-do"; }
    @Override HomeMode section() { return HomeMode.TODOS; }

    @Override void renderNormal(List<Object> rows) { TodoSection.render(this, rows); }
    @Override void renderSelectionRows(List<Object> rows) { TodoSection.renderSelection(this, rows); }
    @Override String selectionIdOf(Object payload) { return TodoSection.selectionId(payload); }
}
