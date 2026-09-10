package com.plainphone.app;

/**
 * A {@link SectionHost} that also drives multi-select for the section — the home
 * screen ({@link MainActivity}) or a workspace {@link PluginPanel}. The
 * {@code *Section.renderSelection} methods build the checkbox rows and bind the
 * {@link SelectionBar} through this.
 */
interface SelectionHost extends SectionHost, SelBarHost {

    void toggle(String id);

    SelectionBar selectionBar();
}
