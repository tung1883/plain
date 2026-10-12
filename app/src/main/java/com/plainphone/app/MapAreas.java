package com.plainphone.app;

import java.util.Arrays;
import java.util.List;

/** The bundled map areas, all shown together as one merged map — no picker, no switching.
 *  {@link MapPanel} layers every area in {@link #ALL} into one basemap, {@link PlaceIndex}
 *  merges all their place lists into one search, and {@link RoadGraph} merges all their road
 *  graphs into one routing graph (offsetting node ids so they don't collide — see
 *  {@code RoadGraph.load}). Adding a new {@link MapArea} to {@link #ALL} is the entire act of
 *  "adding an area": it folds into the existing map automatically, nothing else to wire up.
 *
 * <p>Areas with no shared road data between them (e.g. two bboxes that don't touch) still won't
 * route across each other — that's a real limit of how the data was built, not a bug: there's
 * no connecting road data to route over. */
final class MapAreas {

    private MapAreas() {}

    static final MapArea HANOI = new MapArea("hanoi", "Hanoi", "map", 21.0285, 105.8542, 12);
    /** Built over the arterial backbone (motorway through tertiary — see
     *  {@code tools/nav/build_roads.py}) rather than every street, and at basemap zoom <= 11
     *  rather than Hanoi's 14 — both deliberate size trade-offs for covering the whole country
     *  instead of one city. Routes land on the nearest backbone road, not the exact address. */
    static final MapArea VIETNAM = new MapArea("vietnam", "Vietnam", "map/vietnam", 21.0285, 105.8542, 12);

    /** Vietnam first (drawn as the base layer), Hanoi second (full-street detail over Vietnam's
     *  backbone-only tiles in the same spot). */
    static final List<MapArea> ALL = Arrays.asList(VIETNAM, HANOI);
}
