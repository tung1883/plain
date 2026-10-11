package com.plainphone.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.location.modes.RenderMode;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.Point;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Nav home section, v1: a bundled Hanoi basemap (vector PMTiles, extracted from a Protomaps
 * daily build) rendered fully offline — the pmtiles archive and its MapLibre style both ship
 * as app assets, no network tile fetch at render time. Map geometry (roads, water, buildings,
 * landuse) renders offline; text labels and icon sprites still point at Protomaps' CDN
 * (glyphs/sprite in style.json), so they only show up with a connection — bundling those too
 * is follow-up work, not done here.
 *
 * <p>No radio yet; that's a separate piece of the broader nav-plugin scope. No region picker
 * either — one bundled city for now. The location dot uses GPS/network fixes same as everywhere
 * else on the phone — fully offline-capable via GPS, no connection needed.
 *
 * <p>Search + routing ({@link MapSearchOverlay}) is a child of this panel, not a separate
 * {@code NavPanel} tab — it talks to the map only through the narrow {@link Host} methods below,
 * never touching {@link #map}/{@link #style} directly. It overlays the map as a closeable
 * pill/card (collapsed by default, so the map stays fully visible until summoned).
 */
final class MapPanel implements MapSearchOverlay.Host {

    static final int REQUEST_LOCATION = 4401;

    private static final String ROUTE_SOURCE_ID = "nav-route";
    private static final String ROUTE_LAYER_ID = "nav-route-layer";
    private static final int PIN_FROM_COLOR = 0xFF2D7FF9;
    private static final int PIN_TO_COLOR = Color.BLACK;
    /** Matches {@code MapSearchOverlay.PIN_FROM} — the only two pin ids this panel ever draws. */
    private static final String PIN_FROM = "from";

    private final Activity activity;
    private final FrameLayout root;
    private final FrameLayout locateButton;
    private MapView mapView;
    private MapLibreMap map;
    private Style style;
    private boolean locationEnabled;

    MapPanel(Activity activity) {
        this.activity = activity;
        root = new FrameLayout(activity);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(0, 0, 0, UiKit.dp(activity, 16));

        mapView = new MapView(activity);
        float mapRadiusPx = UiKit.dp(activity, 16);
        mapView.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), mapRadiusPx);
            }
        });
        mapView.setClipToOutline(true);
        root.addView(mapView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // SwipeSwitcher (the home-section left/right swipe) steals a one-finger horizontal
        // drag before MapLibre's own pan gesture sees it, same conflict ChessBoardView already
        // works around for board drags — claim the gesture for the map as soon as a finger
        // lands on it, same side-effect-only listener pattern, release on lift.
        mapView.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
        // The MainActivity-driven onStart()/onResume() below will also fire on the next real
        // lifecycle transition — harmless here since the view was just created and has
        // nothing registered yet to double-register.
        mapView.onCreate(null);
        mapView.onStart();
        mapView.onResume();

        // Drawn as a pin-drop shape rather than a text glyph — glyphs like ⌖ render as a tofu
        // box on fonts that don't carry that codepoint, which is what made the first version
        // "look bad".
        locateButton = new FrameLayout(activity);
        locateButton.setBackground(UiKit.rounded(activity, Color.WHITE, 0xFFBBBBBB, 2f, 22f));
        locateButton.addView(new PinIconView(activity), new FrameLayout.LayoutParams(
                UiKit.dp(activity, 20), UiKit.dp(activity, 20), Gravity.CENTER));
        locateButton.setOnClickListener(v -> onLocateTapped());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                UiKit.dp(activity, 44), UiKit.dp(activity, 44));
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.bottomMargin = UiKit.dp(activity, 16);
        lp.rightMargin = UiKit.dp(activity, 16);
        root.addView(locateButton, lp);

        MapSearchOverlay searchOverlay = new MapSearchOverlay(activity, this);
        FrameLayout.LayoutParams searchLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.gravity = Gravity.TOP;
        searchLp.topMargin = UiKit.dp(activity, 12);
        searchLp.leftMargin = UiKit.dp(activity, 12);
        searchLp.rightMargin = UiKit.dp(activity, 12);
        root.addView(searchOverlay.view(), searchLp);

        // MapLibre's asset:// byte-range reader for pmtiles (recently added) decompresses
        // garbage on this archive ("Error decompressing PMTiles metadata: incorrect header
        // check") even though the archive itself verifies clean with the pmtiles CLI — looks
        // like a bug in that still-new code path. Copy the bundled archive out to real app
        // storage once and read it through plain file:// instead, which is the long-proven path.
        File pmtiles = copiedPmtiles(activity);
        String styleJson = loadStyleJson(activity, pmtiles.getAbsolutePath());
        mapView.getMapAsync(m -> {
            map = m;
            // Default camera sits at (0,0) zoom 0 — the open ocean off West Africa — which
            // renders as nothing but the style's grey background layer. Point it at the
            // bundled extract's city instead of leaving the map looking blank.
            map.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(21.0285, 105.8542))
                    .zoom(12)
                    .build());
            map.setStyle(new Style.Builder().fromJson(styleJson), loadedStyle -> {
                style = loadedStyle;
                // Attribution/logo off, built-in rotation-reset compass on in their place
                // (bottom start corner). Applied after the style loads — MapLibre re-syncs
                // these UiSettings off the style's own attribution/logo config when a style
                // finishes loading, so setting them earlier (right after getMapAsync) was
                // getting silently clobbered. Note: the basemap data is OSM/Protomaps under
                // ODbL, which normally expects visible attribution — fine for local dev/
                // testing, revisit before any public release.
                map.getUiSettings().setLogoEnabled(false);
                map.getUiSettings().setAttributionEnabled(false);
                map.getUiSettings().setCompassEnabled(true);
                map.getUiSettings().setCompassGravity(Gravity.BOTTOM | Gravity.START);
                map.getUiSettings().setCompassMargins(
                        UiKit.dp(activity, 12), 0, 0, UiKit.dp(activity, 12));
                // Registered once here rather than lazily on first showPin() — route/search
                // pins are drawn as native SymbolLayer icons (not overlaid Views) specifically
                // so they render in the same GL frame as the map instead of visibly lagging a
                // frame behind during a pan.
                int pinPx = UiKit.dp(activity, 24);
                style.addImage("pin-from", PinIconView.toBitmap(pinPx, PIN_FROM_COLOR));
                style.addImage("pin-to", PinIconView.toBitmap(pinPx, PIN_TO_COLOR));
                if (hasLocationPermission()) enableLocation(true);
            });
        });
    }

    private void onLocateTapped() {
        if (!hasLocationPermission()) {
            activity.requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
            return;
        }
        LocationManager lm = (LocationManager) activity.getSystemService(Activity.LOCATION_SERVICE);
        if (lm == null || !lm.isLocationEnabled()) {
            Toast.makeText(activity, "Location is off - turn it on in system settings",
                    Toast.LENGTH_LONG).show();
            activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }
        if (!locationEnabled) enableLocation(true);
        else recenterOnLocation();
    }

    /** Called from MainActivity's onRequestPermissionsResult. */
    void onPermissionResult(boolean granted) {
        if (granted) enableLocation(true);
    }

    private boolean hasLocationPermission() {
        return activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void enableLocation(boolean thenRecenter) {
        if (map == null || style == null || !hasLocationPermission()) return;
        LocationComponent component = map.getLocationComponent();
        if (!locationEnabled) {
            component.activateLocationComponent(
                    LocationComponentActivationOptions.builder(activity, style).build());
            component.setLocationComponentEnabled(true);
            component.setRenderMode(RenderMode.NORMAL);
            component.setCameraMode(CameraMode.NONE);
            locationEnabled = true;
        }
        if (thenRecenter) recenterOnLocation();
    }

    private void recenterOnLocation() {
        if (map == null || !locationEnabled) return;
        android.location.Location last = map.getLocationComponent().getLastKnownLocation();
        if (last == null) return;
        map.animateCamera(CameraUpdateFactory.newLatLng(
                new LatLng(last.getLatitude(), last.getLongitude())));
    }

    // --- MapSearchOverlay.Host ------------------------------------------------------------

    @Override
    public void showRoute(List<LatLng> points) {
        if (style == null || points.isEmpty()) return;
        List<Point> geoPoints = new ArrayList<>();
        for (LatLng p : points) geoPoints.add(Point.fromLngLat(p.getLongitude(), p.getLatitude()));
        Feature route = Feature.fromGeometry(LineString.fromLngLats(geoPoints));

        GeoJsonSource source = style.getSourceAs(ROUTE_SOURCE_ID);
        if (source == null) {
            style.addSource(new GeoJsonSource(ROUTE_SOURCE_ID, FeatureCollection.fromFeature(route)));
            LineLayer layer = new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID);
            layer.setProperties(
                    PropertyFactory.lineColor(0xFF2D7FF9),
                    PropertyFactory.lineWidth(4f));
            style.addLayer(layer);
        } else {
            source.setGeoJson(FeatureCollection.fromFeature(route));
        }
    }

    @Override
    public void clearRoute() {
        if (style == null) return;
        GeoJsonSource source = style.getSourceAs(ROUTE_SOURCE_ID);
        if (source != null) source.setGeoJson(FeatureCollection.fromFeatures(new Feature[0]));
    }

    @Override
    public void showPin(String id, LatLng point) {
        if (style == null) return;
        String sourceId = "nav-pin-" + id;
        Feature feature = Feature.fromGeometry(Point.fromLngLat(point.getLongitude(), point.getLatitude()));

        GeoJsonSource source = style.getSourceAs(sourceId);
        if (source == null) {
            style.addSource(new GeoJsonSource(sourceId, FeatureCollection.fromFeature(feature)));
            SymbolLayer layer = new SymbolLayer("nav-pin-layer-" + id, sourceId);
            layer.setProperties(
                    PropertyFactory.iconImage(PIN_FROM.equals(id) ? "pin-from" : "pin-to"),
                    // The drawn pin's tip is at the bottom-center of the bitmap (see
                    // PinIconView) — anchor there so the tip, not the icon's center, marks
                    // the actual coordinate.
                    PropertyFactory.iconAnchor("bottom"),
                    PropertyFactory.iconAllowOverlap(true));
            style.addLayer(layer);
        } else {
            source.setGeoJson(FeatureCollection.fromFeature(feature));
        }
    }

    @Override
    public void clearPin(String id) {
        if (style == null) return;
        GeoJsonSource source = style.getSourceAs("nav-pin-" + id);
        if (source != null) source.setGeoJson(FeatureCollection.fromFeatures(new Feature[0]));
    }

    @Override
    public void moveCameraToBounds(List<LatLng> points) {
        if (map == null || points.isEmpty()) return;
        if (points.size() == 1) {
            map.animateCamera(CameraUpdateFactory.newLatLng(points.get(0)));
            return;
        }
        LatLngBounds bounds = new LatLngBounds.Builder().includes(points).build();
        map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, UiKit.dp(activity, 64)));
    }

    @Override
    public LatLng lastKnownLocation() {
        if (map == null || !locationEnabled) return null;
        android.location.Location last = map.getLocationComponent().getLastKnownLocation();
        return last == null ? null : new LatLng(last.getLatitude(), last.getLongitude());
    }

    @Override
    public Activity activity() {
        return activity;
    }

    private static File copiedPmtiles(Activity activity) {
        File dest = new File(activity.getFilesDir(), "map/hanoi.pmtiles");
        if (!dest.isFile()) {
            try {
                dest.getParentFile().mkdirs();
                try (InputStream in = activity.getAssets().open("map/hanoi.pmtiles");
                        FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return dest;
    }

    private static String loadStyleJson(Activity activity, String pmtilesPath) {
        try (InputStream in = activity.getAssets().open("map/style.json")) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8 * 1024];
            int n;
            while ((n = in.read(buf)) >= 0) bos.write(buf, 0, n);
            String json = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            return json.replace("pmtiles://asset://map/hanoi.pmtiles", "pmtiles://file://" + pmtilesPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    View view() { return root; }

    void onStart() { if (mapView != null) mapView.onStart(); }
    void onResume() { if (mapView != null) mapView.onResume(); }
    void onPause() { if (mapView != null) mapView.onPause(); }
    void onStop() { if (mapView != null) mapView.onStop(); }
    void onLowMemory() { if (mapView != null) mapView.onLowMemory(); }
    void onDestroy() { if (mapView != null) mapView.onDestroy(); }
}
