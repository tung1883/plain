package com.plainphone.app;

/** A bundled grayscale GIF scene for the home screen, loaded from assets/pixel_art/. */
enum GifScene {
    CITY("City", "city.gif"),
    PARK("Park", "park.gif"),
    HIGHWAY("Highway", "highway.gif"),
    CASTLE("Castle", "castle.gif"),
    VILLAGE("Village", "village.gif"),
    CLIFFS("Cliffs", "cliffs.gif"),
    NIGHT_HOUSE("Night house", "nighthouse.gif"),
    GLITCH("Glitch", "glitch.gif");

    final String label;
    final String assetFileName;

    GifScene(String label, String assetFileName) {
        this.label = label;
        this.assetFileName = assetFileName;
    }
}
