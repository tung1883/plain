package com.plainphone.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.location.modes.RenderMode;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Nav home section, v1: a bundled Hanoi basemap (vector PMTiles, extracted from a Protomaps
 * daily build) rendered fully offline — the pmtiles archive and its MapLibre style both ship
 * as app assets, no network tile fetch at render time. Map geometry (roads, water, buildings,
 * landuse) renders offline; text labels and icon sprites still point at Protomaps' CDN
 * (glyphs/sprite in style.json), so they only show up with a connection — bundling those too
 * is follow-up work, not done here.
 *
 * <p>No routing, no radio yet; those are separate pieces of the broader nav-plugin scope. No
 * region picker either — one bundled city for now. The location dot uses GPS/network fixes same
 * as everywhere else on the phone — fully offline-capable via GPS, no connection needed.
 */
final class MapPanel {

    static final int REQUEST_LOCATION = 4401;

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

        mapView = new MapView(activity);
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
            Toast.makeText(activity, "Location is off — turn it on in system settings",
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

    /** Map-pin-drop glyph, drawn rather than loaded from a font so it never falls back to a
     *  tofu box — same teardrop-with-hole shape as the mockup's option 5. */
    private static final class PinIconView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hole = new Paint(Paint.ANTI_ALIAS_FLAG);

        PinIconView(Context context) {
            super(context);
            fill.setColor(Color.BLACK);
            fill.setStyle(Paint.Style.FILL);
            hole.setColor(Color.WHITE);
            hole.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = Math.min(getWidth(), getHeight()) / 24f;   // 24x24 viewBox, scaled to view
            Path pin = new Path();
            pin.moveTo(12 * s, 2 * s);
            pin.cubicTo(8 * s, 2 * s, 5 * s, 5 * s, 5 * s, 9 * s);
            pin.cubicTo(5 * s, 14.2f * s, 12 * s, 22 * s, 12 * s, 22 * s);
            pin.cubicTo(12 * s, 22 * s, 19 * s, 14.2f * s, 19 * s, 9 * s);
            pin.cubicTo(19 * s, 5 * s, 16 * s, 2 * s, 12 * s, 2 * s);
            pin.close();
            canvas.drawPath(pin, fill);
            canvas.drawCircle(12 * s, 9 * s, 2.4f * s, hole);
        }
    }
}
