package com.plainphone.app;

/** One labelled action in a {@link SelectionBar}. */
final class BarAction {
    final String label;
    final Runnable run;

    BarAction(String label, Runnable run) {
        this.label = label;
        this.run = run;
    }
}
