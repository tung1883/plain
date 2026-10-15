package com.plainphone.app;

/** One searchable place/POI, loaded from the bundled {@code places.json} index — see
 *  {@link PlaceIndex}. */
final class Place {
    final String name;
    final double lat;
    final double lon;
    final String kind;
    final String folded;
    final long signature;

    Place(String name, double lat, double lon, String kind) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.kind = kind;
        this.folded = TextMatch.fold(name);
        this.signature = TextMatch.signatureOf(this.folded);
    }
}
