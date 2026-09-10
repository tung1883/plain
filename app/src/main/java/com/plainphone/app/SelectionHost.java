package com.plainphone.app;

import java.util.Collection;
import java.util.Set;

/**
 * A {@link SectionHost} that also drives multi-select for the section — the home
 * screen ({@link MainActivity}) or a workspace {@link PluginPanel}. The
 * {@code *Section.renderSelection} methods build the checkbox rows and bind the
 * {@link SelectionBar} through this.
 */
interface SelectionHost extends SectionHost {

    /** The currently-selected item ids (note id / recording id / {@code "todo:"+index}). */
    Set<String> selection();

    void toggle(String id);

    void setSelected(Collection<String> ids);

    void exitSelection();

    SelectionBar selectionBar();
}
