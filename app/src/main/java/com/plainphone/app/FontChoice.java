package com.plainphone.app;

enum FontChoice {
    CASCADIA_MONO("Cascadia Mono"),
    IBM_PLEX_MONO("IBM Plex Mono"),
    GEORGIA("Georgia"),
    EXCALIFONT("Excalifont");

    final String label;

    FontChoice(String label) {
        this.label = label;
    }
}

