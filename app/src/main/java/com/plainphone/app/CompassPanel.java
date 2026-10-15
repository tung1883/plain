package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Nav home section: a standalone magnetometer compass — device heading, independent of the
 * Map section (works without GPS, without the map open, phone flat in hand). Uses the fused
 * rotation-vector sensor (accelerometer + magnetometer), smoothed, no permission needed.
 */
final class CompassPanel implements SensorEventListener {

    private final FrameLayout root;
    private final Dial dial;
    private final TextView reading;
    private final TextView strengthLabel;
    private final TextView accuracyLabel;
    private final SensorManager sensorManager;
    private final Sensor rotationSensor;
    private final Sensor magnetometer;
    private boolean active;
    private float smoothedAzimuth;
    private boolean haveReading;

    CompassPanel(Context context) {
        root = new FrameLayout(context);
        root.setBackgroundColor(Color.BLACK);

        // Dial + reading + strength grouped as one vertically-stacked block (not three
        // independently bottom/center-anchored overlays) — anchoring reading/strength to the
        // root's own bottom edge let them drift up near the dial's lower rim on tall screens
        // and cross its rotating 30°-interval degree labels (120, 150, ...) as the dial spun.
        // Wrapped in a ScrollView (fillViewport so it still centers when content is short) —
        // the dial + full accuracy message together can be taller than some screens, which was
        // clipping the accuracy text instead of letting it scroll into view.
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        root.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        scroll.addView(column, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        dial = new Dial(context);
        column.addView(dial, new LinearLayout.LayoutParams(
                UiKit.dp(context, 260), UiKit.dp(context, 260)));

        reading = new TextView(context);
        reading.setTextColor(Color.WHITE);
        reading.setTextSize(16);
        reading.setTypeface(Fonts.current(context));
        reading.setGravity(Gravity.CENTER);
        reading.setText("—");
        LinearLayout.LayoutParams readingLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        readingLp.topMargin = UiKit.dp(context, 12);
        column.addView(reading, readingLp);

        // Raw magnetic field magnitude, like most phone compass apps show — a direct read on
        // interference: Earth's field is ~25-65 µT, so a reading well outside that range means
        // metal/electronics nearby are throwing the heading off, which the accuracy flag below
        // doesn't always catch promptly.
        strengthLabel = new TextView(context);
        strengthLabel.setTextColor(0xFFAAAAAA);
        strengthLabel.setTextSize(12);
        strengthLabel.setTypeface(Fonts.current(context));
        strengthLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams strengthLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        strengthLp.topMargin = UiKit.dp(context, 4);
        column.addView(strengthLabel, strengthLp);

        // Raw magnetometer accuracy — indoors, near metal/electronics, this sensor is
        // genuinely unreliable (every phone compass has this problem, not just this one).
        // Surfacing it directly instead of leaving a silently-wrong reading on screen.
        // Last in the same centered column, below the µT reading — not a separate top/bottom
        // overlay, so it never crosses the dial or the strength label.
        accuracyLabel = new TextView(context);
        accuracyLabel.setTextColor(0xFFAAAAAA);
        accuracyLabel.setTextSize(12);
        accuracyLabel.setTypeface(Fonts.current(context));
        accuracyLabel.setGravity(Gravity.CENTER);
        accuracyLabel.setPadding(UiKit.dp(context, 24), 0, UiKit.dp(context, 24), 0);
        LinearLayout.LayoutParams accuracyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        accuracyLp.topMargin = UiKit.dp(context, 8);
        column.addView(accuracyLabel, accuracyLp);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager == null
                ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        magnetometer = sensorManager == null
                ? null : sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (sensorManager == null || rotationSensor == null) {
            reading.setText("No compass sensor on this device");
        }
    }

    View view() { return root; }

    /** Only samples the sensor while the Compass tab is actually visible — a magnetometer
     *  listener left running on every other tab would be a pointless battery drain. */
    void setActive(boolean wantActive) {
        if (wantActive == active || rotationSensor == null) return;
        active = wantActive;
        if (active) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
            if (magnetometer != null) {
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        } else {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            float[] v = event.values;
            float strength = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            strengthLabel.setText(String.format(java.util.Locale.US, "%.1f µT", strength));
            return;
        }

        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);
        float azimuthDeg = (float) Math.toDegrees(orientation[0]);
        if (azimuthDeg < 0) azimuthDeg += 360f;

        // Raw readings jitter a few degrees frame to frame — exponential smoothing across the
        // wrap-around point (359° <-> 0°) needs the shortest-path delta, not a plain lerp.
        if (!haveReading) {
            smoothedAzimuth = azimuthDeg;
            haveReading = true;
        } else {
            float delta = azimuthDeg - smoothedAzimuth;
            while (delta > 180f) delta -= 360f;
            while (delta < -180f) delta += 360f;
            smoothedAzimuth = (smoothedAzimuth + delta * 0.15f + 360f) % 360f;
        }

        dial.setAzimuth(smoothedAzimuth);
        reading.setText(Math.round(smoothedAzimuth) + "° " + cardinal(smoothedAzimuth));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                accuracyLabel.setText("Unreliable — move away from metal and wave the phone in a figure-8");
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                accuracyLabel.setText("Low accuracy — calibrate with a figure-8 motion");
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                accuracyLabel.setText("Medium accuracy");
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                accuracyLabel.setText("");
                break;
            default:
                accuracyLabel.setText("");
        }
    }

    private static String cardinal(float deg) {
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return dirs[Math.round(deg / 45f) % 8];
    }

    /** The dial itself — rotates under a fixed top pointer, same convention as every phone
     *  compass app: the pointer always shows where the phone is facing, the dial's N/E/S/W
     *  letters rotate underneath it to reflect true north. */
    private static final class Dial extends View {
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint northLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint degreeLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pointer = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float azimuth;

        Dial(Context context) {
            super(context);
            ring.setColor(Color.WHITE);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(UiKit.dp(context, 2));

            tick.setColor(0xFFAAAAAA);
            tick.setStrokeWidth(UiKit.dp(context, 1.5f));

            label.setColor(Color.WHITE);
            label.setTextAlign(Paint.Align.CENTER);
            label.setTypeface(Fonts.current(context));
            label.setTextSize(UiKit.dp(context, 14));

            northLabel.set(label);
            northLabel.setColor(0xFFE05B4B);
            northLabel.setTypeface(Typeface.create(Fonts.current(context), Typeface.BOLD));

            degreeLabel.set(label);
            degreeLabel.setColor(0xFF888888);
            degreeLabel.setTextSize(UiKit.dp(context, 10));

            pointer.setColor(0xFFE05B4B);
            pointer.setStyle(Paint.Style.FILL);
        }

        void setAzimuth(float deg) {
            azimuth = deg;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - UiKit.dp(getContext(), 20);

            canvas.save();
            canvas.rotate(-azimuth, cx, cy);
            canvas.drawCircle(cx, cy, r, ring);
            String[] dirs = {"N", "E", "S", "W"};
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(i * 90);
                float x1 = cx + (float) Math.sin(a) * r;
                float y1 = cy - (float) Math.cos(a) * r;
                float x2 = cx + (float) Math.sin(a) * (r - UiKit.dp(getContext(), 10));
                float y2 = cy - (float) Math.cos(a) * (r - UiKit.dp(getContext(), 10));
                canvas.drawLine(x1, y1, x2, y2, tick);
                float lx = cx + (float) Math.sin(a) * (r - UiKit.dp(getContext(), 26));
                float ly = cy - (float) Math.cos(a) * (r - UiKit.dp(getContext(), 26));
                drawUprightText(canvas, dirs[i], lx, ly, i == 0 ? northLabel : label);
            }
            for (int deg = 0; deg < 360; deg += 30) {
                if (deg % 90 == 0) continue;
                double a = Math.toRadians(deg);
                float x1 = cx + (float) Math.sin(a) * r;
                float y1 = cy - (float) Math.cos(a) * r;
                float x2 = cx + (float) Math.sin(a) * (r - UiKit.dp(getContext(), 6));
                float y2 = cy - (float) Math.cos(a) * (r - UiKit.dp(getContext(), 6));
                canvas.drawLine(x1, y1, x2, y2, tick);
                float lx = cx + (float) Math.sin(a) * (r - UiKit.dp(getContext(), 20));
                float ly = cy - (float) Math.cos(a) * (r - UiKit.dp(getContext(), 20));
                drawUprightText(canvas, String.valueOf(deg), lx, ly, degreeLabel);
            }
            for (int deg = 0; deg < 360; deg += 10) {
                if (deg % 30 == 0) continue;
                double a = Math.toRadians(deg);
                float x1 = cx + (float) Math.sin(a) * r;
                float y1 = cy - (float) Math.cos(a) * r;
                float x2 = cx + (float) Math.sin(a) * (r - UiKit.dp(getContext(), 3));
                float y2 = cy - (float) Math.cos(a) * (r - UiKit.dp(getContext(), 3));
                canvas.drawLine(x1, y1, x2, y2, tick);
            }
            canvas.restore();

            // Fixed pointer, not rotated — marks the phone's own forward direction. Tip sits
            // right on the ring, pointing inward, but shallow — it stops well short of the
            // rotating degree-label band (drawn at r - 20dp), so it never touches a number.
            float pw = UiKit.dp(getContext(), 7);
            float ptipY = cy - r + UiKit.dp(getContext(), 40);
            float pbaseY = cy - r + UiKit.dp(getContext(), 52);
            android.graphics.Path p = new android.graphics.Path();
            p.moveTo(cx, ptipY);
            p.lineTo(cx - pw, pbaseY);
            p.lineTo(cx + pw, pbaseY);
            p.close();
            canvas.drawPath(p, pointer);
        }

        /** Draws text at a point that orbits with the (already-rotated) dial, but counter-
         *  rotates the glyphs themselves back to upright — so N/E/S/W and the degree numbers
         *  stay readable right-side-up instead of tilting as the ring spins underneath. */
        private void drawUprightText(Canvas canvas, String text, float x, float y, Paint paint) {
            canvas.save();
            canvas.rotate(azimuth, x, y);
            canvas.drawText(text, x, y + paint.getTextSize() / 3f, paint);
            canvas.restore();
        }
    }
}
