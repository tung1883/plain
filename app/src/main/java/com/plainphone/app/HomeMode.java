package com.plainphone.app;

enum HomeMode {
    APPS("Apps"),
    NOTES("Notes");

    final String label;

    HomeMode(String label) {
        this.label = label;
    }
}
