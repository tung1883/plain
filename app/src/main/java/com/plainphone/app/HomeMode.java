package com.plainphone.app;

enum HomeMode {
    APPS("Apps"),
    NOTES("Notes"),
    TODOS("To-do"),
    STATS("Stats"),
    RECORDER("Rec"),
    VAULT("Vault"),
    DEV("Dev"),
    CHESS("Chess"),
    WORKSPACE("Workspace"),
    NAV("Nav");

    final String label;

    HomeMode(String label) {
        this.label = label;
    }
}
