package com.plainphone.app;

import android.os.SystemClock;
import android.util.Log;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Latency counters for the Dev plugin's link. Each named metric collects
 * samples (ms, or bytes for sizes); every {@link #WINDOW_MS} the non-empty
 * ones are summarised to logcat under tag {@code PlainLat}:
 * {@code n, avg, p50, p95, max}. Read with {@code adb logcat -s PlainLat}.
 */
final class LatencyStats {

    static final String TAG = "PlainLat";
    private static final long WINDOW_MS = 2000;

    private static final class Series {
        double[] v = new double[64];
        int n;

        void add(double x) {
            if (n == v.length) v = Arrays.copyOf(v, n * 2);
            v[n++] = x;
        }
    }

    private static final Map<String, Series> series = new LinkedHashMap<>();
    private static long windowStart = SystemClock.uptimeMillis();

    private LatencyStats() {}

    static synchronized void record(String name, double value) {
        Series s = series.get(name);
        if (s == null) series.put(name, s = new Series());
        s.add(value);
        maybeFlush();
    }

    private static void maybeFlush() {
        long now = SystemClock.uptimeMillis();
        if (now - windowStart < WINDOW_MS) return;
        double secs = (now - windowStart) / 1000.0;
        windowStart = now;
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Series> e : series.entrySet()) {
            Series s = e.getValue();
            if (s.n == 0) continue;
            double[] v = Arrays.copyOf(s.v, s.n);
            Arrays.sort(v);
            double sum = 0;
            for (double x : v) sum += x;
            out.append(String.format(java.util.Locale.US,
                    "%-18s n=%-4d (%.1f/s) avg=%-7.1f p50=%-7.1f p95=%-7.1f max=%.1f%n",
                    e.getKey(), s.n, s.n / secs, sum / s.n,
                    v[s.n / 2], v[Math.min(s.n - 1, (int) (s.n * 0.95))], v[s.n - 1]));
            s.n = 0;
        }
        if (out.length() > 0) Log.i(TAG, "--- last " + String.format(java.util.Locale.US, "%.1f", secs) + "s\n" + out);
    }
}
