package com.plainphone.app;

import java.util.Locale;

/** Small shared formatters for the Dev-plugin panels (bytes, rates, ages). */
final class Fmt {

    private Fmt() {}

    /** Human byte size: {@code 1.6 TB}, {@code 214 GB}, {@code 88 MB}, {@code 312 KB}, {@code 40 B}. */
    static String bytes(long b) {
        if (b < 0) b = 0;
        if (b >= 1_000_000_000_000L) return String.format(Locale.US, "%.2f TB", b / 1e12);
        if (b >= 1_000_000_000L) return String.format(Locale.US, "%.1f GB", b / 1e9);
        if (b >= 1_000_000L) return String.format(Locale.US, "%.0f MB", b / 1e6);
        if (b >= 1_000L) return String.format(Locale.US, "%.0f KB", b / 1e3);
        return b + " B";
    }

    /** Bytes-per-second as a rate: {@code 1.2 MB/s}, {@code 340 KB/s}, {@code 0 B/s}. */
    static String rate(double bytesPerSec) {
        if (bytesPerSec < 0 || Double.isNaN(bytesPerSec)) bytesPerSec = 0;
        if (bytesPerSec >= 1e9) return String.format(Locale.US, "%.1f GB/s", bytesPerSec / 1e9);
        if (bytesPerSec >= 1e6) return String.format(Locale.US, "%.1f MB/s", bytesPerSec / 1e6);
        if (bytesPerSec >= 1e3) return String.format(Locale.US, "%.0f KB/s", bytesPerSec / 1e3);
        return Math.round(bytesPerSec) + " B/s";
    }

    /** Compact relative age of an epoch-millis instant: {@code just now}, {@code 12m ago}, {@code 3h ago}, {@code 6d ago}. */
    static String age(long epochMillis) {
        if (epochMillis <= 0) return "";
        long s = (System.currentTimeMillis() - epochMillis) / 1000;
        if (s < 0) s = 0;
        if (s < 45) return "just now";
        if (s < 3600) return (s / 60) + "m ago";
        if (s < 86400) return (s / 3600) + "h ago";
        if (s < 86400 * 30) return (s / 86400) + "d ago";
        return (s / (86400 * 30)) + "mo ago";
    }

    /** {@code 6d 04:12} / {@code 04:12} — uptime from seconds. */
    static String duration(long seconds) {
        long d = seconds / 86400, h = (seconds % 86400) / 3600, m = (seconds % 3600) / 60;
        return d > 0 ? String.format(Locale.US, "%dd %02d:%02d", d, h, m)
                : String.format(Locale.US, "%02d:%02d", h, m);
    }
}
