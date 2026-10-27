package com.plainphone.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The short texts of the Search settings screen, kept apart from the views so they can be tested. */
final class SearchSettingsText {

    private SearchSettingsText() {}

    /** "All" when every kind of file is read, "None", or the kinds in order: "Text, PDF". */
    static String typeSummary(Set<String> types) {
        String[][] names = {{Extractors.TEXT, "Text"}, {Extractors.OFFICE, "Office"}, {Extractors.EPUB, "E-books"}, {Extractors.PDF, "PDF"}};
        List<String> on = new ArrayList<>();
        for (String[] n : names) if (types.contains(n[0])) on.add(n[1]);
        if (on.isEmpty()) return "None";
        if (on.size() == names.length) return "All";
        return String.join(", ", on);
    }

    /** Why some files are not in the index: "3 scanned", "1 too big" ... (only the reasons that apply). */
    static List<String> skippedPills(Map<String, Integer> byStatus) {
        String[][] reasons = {{"scanned", "scanned"}, {"toobig", "too big"}, {"locked", "locked"}, {"failed", "unreadable"}, {"crashed", "crashed"}};
        List<String> out = new ArrayList<>();
        for (String[] r : reasons) {
            Integer n = byStatus.get(r[0]);
            if (n != null && n > 0) out.add(n + " " + r[1]);
        }
        return out;
    }

    /** Why a file is not in the index, in words. {@code limitMb} is the largest size read for its kind. */
    static String reason(String status, String error, long limitMb) {
        switch (status) {
            case "scanned": return "Scanned pages, there is no text to read";
            case "locked": return "Password protected";
            case "toobig": return limitMb > 0 ? "Larger than " + limitMb + " MB, too big to read" : "Too big to read";
            case "empty": return "No text in it";
            case "crashed": return "It crashed the PDF reader, so it is skipped";
            case "failed": return error == null || error.trim().isEmpty() ? "Could not be read" : "Could not be read: " + error.trim();
            default: return status;
        }
    }

    /** "24 MB of 150 MB", or just "24 MB" with no limit (sizes under a megabyte read "under 1 MB"). */
    static String sizeLine(long bytes, int maxMb) {
        long mb = bytes / (1024 * 1024);
        String used = mb < 1 ? "under 1 MB" : mb + " MB";
        return maxMb <= 0 ? used : used + " of " + limitLabel(maxMb);
    }

    /** "50 MB", "1 GB", "Unlimited". */
    static String limitLabel(int maxMb) {
        if (maxMb <= 0) return "Unlimited";
        if (maxMb % 1000 == 0) return (maxMb / 1000) + " GB";
        return maxMb + " MB";
    }

    /** How full the index is, 0..1. */
    static float usedFraction(long bytes, int maxMb) {
        if (maxMb <= 0) return 0f;
        return Math.max(0f, Math.min(1f, bytes / (maxMb * 1024f * 1024f)));
    }

    /** "last run 3 min ago", "last run 2 h ago", "last run 4 days ago", or "not run yet". */
    static String lastRun(long lastMillis, long nowMillis) {
        if (lastMillis <= 0) return "not run yet";
        long min = Math.max(0, (nowMillis - lastMillis) / 60000);
        if (min < 1) return "last run just now";
        if (min < 60) return "last run " + min + " min ago";
        long h = min / 60;
        if (h < 24) return "last run " + h + " h ago";
        long d = h / 24;
        return "last run " + d + (d == 1 ? " day ago" : " days ago");
    }

    /** "~9 min left", "~2 h left" or "under 1 min left"; empty until enough files are done to tell. */
    static String eta(int done, int total, long elapsedMs) {
        if (done < 5 || total <= done || elapsedMs <= 0) return "";
        long leftMin = Math.round(elapsedMs / (double) done * (total - done) / 60000.0);
        if (leftMin < 1) return "under 1 min left";
        if (leftMin < 90) return "~" + leftMin + " min left";
        return "~" + Math.round(leftMin / 60.0) + " h left";
    }

    /** "Paused at 212 of 480". */
    static String pausedLine(int done, int total) {
        return "Paused at " + done + " of " + total;
    }

    /** "Reading 12 of 34…" while a run is going. */
    static String progressLine(int done, int total) {
        return "Reading " + done + " of " + total + "…";
    }
}
