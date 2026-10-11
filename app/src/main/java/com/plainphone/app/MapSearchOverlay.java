package com.plainphone.app;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.maplibre.android.geometry.LatLng;

import java.util.List;
import java.util.Locale;

/** A closeable overlay docked to the top of the map: collapsed to a small pill by default (map
 *  stays fully visible), expands into a From/To card on tap, with an X to collapse it back.
 *  Draws its result through {@link Host} rather than touching MapLibre directly. Built the same
 *  programmatic-Java way as {@link CompassPanel}.
 *
 * <p>v1 scope: one route, driving-profile distance graph, no turn-by-turn, no live rerouting —
 * see the nav-plugin routing plan for the full list of what's deliberately left out. */
final class MapSearchOverlay {

    /** The narrow set of things this overlay is allowed to do to the map — keeps the coupling
     *  explicit instead of handing the overlay a raw {@code MapPanel} reference. */
    interface Host {
        void showRoute(List<LatLng> points);
        void clearRoute();
        void showPin(String id, LatLng point);
        void clearPin(String id);
        void moveCameraToBounds(List<LatLng> points);
        LatLng lastKnownLocation();
        Activity activity();
    }

    private static final String PIN_FROM = "from";
    private static final String PIN_TO = "to";
    private static final int FROM_COLOR = 0xFF2D7FF9;
    private static final int TO_COLOR = Color.BLACK;
    private static final double ASSUMED_SPEED_KMH = 25.0; // rough urban-driving placeholder

    private final Activity activity;
    private final Host host;
    private final LinearLayout root;
    private final View pill;
    private final LinearLayout card;
    private EditText fromField;
    private EditText toField;
    private LinearLayout suggestions;
    private TextView routeInfo;
    private final Handler main = new Handler(Looper.getMainLooper());

    private Endpoint from;
    private Endpoint to;
    /** Which field the currently-shown suggestions belong to. */
    private EditText activeField;

    MapSearchOverlay(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;

        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        // Same guard MapPanel's own mapView uses — without it, dragging inside the card/
        // suggestions list gets stolen by the home screen's left/right SwipeSwitcher.
        root.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        pill = buildPill();
        root.addView(pill, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        card = buildCard();
        card.setVisibility(View.GONE);
        root.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    View view() {
        return root;
    }

    private View buildPill() {
        LinearLayout p = new LinearLayout(activity);
        p.setOrientation(LinearLayout.HORIZONTAL);
        p.setGravity(Gravity.CENTER_VERTICAL);
        p.setBackground(UiKit.rounded(activity, Color.WHITE, 0xFFBBBBBB, 2f, UiKit.R_SM));
        p.setPadding(UiKit.dp(activity, 12), UiKit.dp(activity, 10),
                UiKit.dp(activity, 12), UiKit.dp(activity, 10));
        p.addView(new PinIconView(activity, TO_COLOR), new LinearLayout.LayoutParams(
                UiKit.dp(activity, 16), UiKit.dp(activity, 16)));
        TextView label = new TextView(activity);
        label.setText("Search or get directions");
        label.setTextColor(0xFF555555);
        label.setTypeface(Fonts.current(activity));
        label.setTextSize(13.5f);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLp.leftMargin = UiKit.dp(activity, 8);
        p.addView(label, labelLp);
        p.setOnClickListener(v -> setExpanded(true));
        return p;
    }

    private LinearLayout buildCard() {
        LinearLayout c = new LinearLayout(activity);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(UiKit.rounded(activity, Color.WHITE, 0xFFBBBBBB, 2f, UiKit.R_SM));
        c.setPadding(UiKit.dp(activity, 10), UiKit.dp(activity, 8),
                UiKit.dp(activity, 10), UiKit.dp(activity, 8));

        LinearLayout head = new LinearLayout(activity);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(activity);
        title.setText("Directions");
        title.setTextColor(0xFF888888);
        title.setTypeface(Fonts.current(activity));
        title.setTextSize(12);
        head.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(closeButton(), new LinearLayout.LayoutParams(
                UiKit.dp(activity, 24), UiKit.dp(activity, 24)));
        c.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        fromField = fieldRow(c, "From", FROM_COLOR);
        c.addView(divider());
        toField = fieldRow(c, "To", TO_COLOR);

        suggestions = new LinearLayout(activity);
        suggestions.setOrientation(LinearLayout.VERTICAL);
        suggestions.setVisibility(View.GONE);
        c.addView(suggestions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        routeInfo = new TextView(activity);
        routeInfo.setTextColor(Color.BLACK);
        routeInfo.setTextSize(13);
        routeInfo.setTypeface(Fonts.current(activity));
        routeInfo.setGravity(Gravity.CENTER);
        routeInfo.setPadding(0, UiKit.dp(activity, 6), 0, 0);
        routeInfo.setVisibility(View.GONE);
        c.addView(routeInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return c;
    }

    private View closeButton() {
        View x = new View(activity) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                drawCloseIcon(canvas, getWidth(), getHeight());
            }
        };
        x.setBackground(UiKit.rounded(activity, 0xFFF2F2F2, 0, 0f, 12f));
        x.setOnClickListener(v -> setExpanded(false));
        return x;
    }

    /** Two crossing lines, drawn rather than a unicode "X" glyph — same hand-drawn-Path
     *  convention as {@code PinIconView}. */
    private static void drawCloseIcon(Canvas canvas, int w, int h) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFF777777);
        paint.setStrokeWidth(Math.min(w, h) / 10f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        float pad = Math.min(w, h) * 0.3f;
        canvas.drawLine(pad, pad, w - pad, h - pad, paint);
        canvas.drawLine(w - pad, pad, pad, h - pad, paint);
    }

    private void setExpanded(boolean expanded) {
        pill.setVisibility(expanded ? View.GONE : View.VISIBLE);
        card.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (!expanded) hideSuggestions();
    }

    private EditText fieldRow(LinearLayout parent, String placeholder, int iconColor) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, UiKit.dp(activity, 4), 0, UiKit.dp(activity, 4));

        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
                UiKit.dp(activity, 14), UiKit.dp(activity, 14));
        iconLp.rightMargin = UiKit.dp(activity, 8);
        row.addView(new PinIconView(activity, iconColor), iconLp);

        EditText field = new EditText(activity);
        field.setHint(placeholder);
        field.setTextColor(Color.BLACK);
        field.setHintTextColor(0xFF999999);
        field.setTypeface(Fonts.current(activity));
        field.setTextSize(14);
        field.setBackground(null);
        field.setSingleLine(true);
        field.setOnFocusChangeListener((v, focused) -> {
            if (focused) activeField = field;
            else if (activeField == field) hideSuggestions();
        });
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onQueryChanged(field, s.toString());
            }

            @Override public void afterTextChanged(Editable s) {}
        });
        row.addView(field, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView useCurrent = new TextView(activity);
        useCurrent.setText("Current location");
        useCurrent.setTextColor(0xFF2D7FF9);
        useCurrent.setTextSize(12);
        useCurrent.setTypeface(Fonts.current(activity));
        useCurrent.setPadding(UiKit.dp(activity, 6), 0, 0, 0);
        useCurrent.setOnClickListener(v -> onUseCurrentLocation(field));
        row.addView(useCurrent);

        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return field;
    }

    private View divider() {
        View d = new View(activity);
        d.setBackgroundColor(0xFFE0E0E0);
        d.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(activity, 1)));
        return d;
    }

    private void onQueryChanged(EditText field, String query) {
        if (query.isEmpty()) {
            hideSuggestions();
            return;
        }
        if (!PlaceIndex.isLoaded()) {
            PlaceIndex.ensureLoaded(activity, () -> {
                if (field.getText().toString().equals(query)) onQueryChanged(field, query);
            });
            return;
        }
        List<Place> hits = PlaceIndex.search(TextMatch.prepare(query), 8);
        showSuggestions(field, hits);
    }

    private void showSuggestions(EditText field, List<Place> hits) {
        activeField = field;
        suggestions.removeAllViews();
        if (hits.isEmpty()) {
            suggestions.setVisibility(View.GONE);
            return;
        }
        for (Place place : hits) {
            TextView row = new TextView(activity);
            row.setText(place.name);
            row.setTextColor(Color.BLACK);
            row.setTypeface(Fonts.current(activity));
            row.setTextSize(14);
            row.setPadding(UiKit.dp(activity, 4), UiKit.dp(activity, 8),
                    UiKit.dp(activity, 4), UiKit.dp(activity, 8));
            row.setOnClickListener(v -> onSuggestionTapped(field, place));
            suggestions.addView(row);
        }
        suggestions.setVisibility(View.VISIBLE);
    }

    private void hideSuggestions() {
        suggestions.setVisibility(View.GONE);
    }

    private void onSuggestionTapped(EditText field, Place place) {
        Endpoint endpoint = new Endpoint(place.name, new LatLng(place.lat, place.lon));
        setEndpoint(field, endpoint);
    }

    private void onUseCurrentLocation(EditText field) {
        LatLng here = host.lastKnownLocation();
        if (here == null) {
            android.widget.Toast.makeText(activity,
                    "No location fix yet - tap the locate button on the map first",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        setEndpoint(field, new Endpoint("Current location", here));
    }

    private void setEndpoint(EditText field, Endpoint endpoint) {
        field.setText(endpoint.label);
        hideSuggestions();
        boolean isFrom = field == fromField;
        if (isFrom) from = endpoint;
        else to = endpoint;
        host.showPin(isFrom ? PIN_FROM : PIN_TO, endpoint.point);
        maybeRoute();
    }

    private void maybeRoute() {
        if (from == null || to == null) {
            host.clearRoute();
            routeInfo.setVisibility(View.GONE);
            return;
        }
        routeInfo.setText("Finding route...");
        routeInfo.setVisibility(View.VISIBLE);

        RoadGraph.ensureLoaded(activity, ok -> {
            if (!ok) {
                routeInfo.setText("Road data unavailable");
                return;
            }
            Endpoint fromSnapshot = from;
            Endpoint toSnapshot = to;
            new Thread(() -> {
                RouteFinder.Result result = RouteFinder.route(RoadGraph.get(),
                        fromSnapshot.point.getLatitude(), fromSnapshot.point.getLongitude(),
                        toSnapshot.point.getLatitude(), toSnapshot.point.getLongitude());
                main.post(() -> onRouteComputed(fromSnapshot, toSnapshot, result));
            }, "RouteFinder-route").start();
        });
    }

    private void onRouteComputed(Endpoint fromSnapshot, Endpoint toSnapshot, RouteFinder.Result result) {
        // The From/To could have changed again while this route was computing — drop a stale
        // result rather than drawing an answer to a question that's no longer being asked.
        if (fromSnapshot != from || toSnapshot != to) return;
        if (result == null) {
            routeInfo.setText("No route found");
            return;
        }
        host.showRoute(result.points);
        host.moveCameraToBounds(result.points);
        double km = result.distanceMeters / 1000.0;
        double etaMin = (km / ASSUMED_SPEED_KMH) * 60.0;
        routeInfo.setText(String.format(Locale.US, "%.1f km - ~%d min", km, Math.round(etaMin)));
    }

    private static final class Endpoint {
        final String label;
        final LatLng point;

        Endpoint(String label, LatLng point) {
            this.label = label;
            this.point = point;
        }
    }
}
