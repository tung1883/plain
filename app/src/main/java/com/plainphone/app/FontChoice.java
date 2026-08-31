package com.plainphone.app;

enum FontChoice {
    GEORGIA("Georgia"),
    IBM_PLEX_MONO("IBM Plex Mono"),
    EXCALIFONT("Excalifont");

    final String label;

    FontChoice(String label) {
        this.label = label;
    }
}

