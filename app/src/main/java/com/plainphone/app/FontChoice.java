package com.plainphone.app;

/** A selectable app-wide typeface, picked in Settings > Font. */
enum FontChoice {
    GEORGIA("Georgia"),
    IBM_PLEX_MONO("IBM Plex Mono");

    final String label;

    FontChoice(String label) {
        this.label = label;
    }
}
