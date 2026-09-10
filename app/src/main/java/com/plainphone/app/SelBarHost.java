package com.plainphone.app;

import java.util.Collection;
import java.util.Set;

/** The minimum a {@link SelectionBar} needs from whatever owns the selection. */
interface SelBarHost {
    Set<String> selection();
    void setSelected(Collection<String> ids);
    void exitSelection();
}
