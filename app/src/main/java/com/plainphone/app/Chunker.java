package com.plainphone.app;

import java.util.ArrayList;
import java.util.List;

/** Splits long text into passages of about a thousand characters, at paragraph and sentence breaks. */
final class Chunker {

    static final int TARGET = 1000;
    static final int MAX = 1600;

    private Chunker() {}

    /** Passages of {@code text}; nothing for blank text. Short text comes back as one passage. */
    static List<String> split(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        String t = text.trim();
        if (t.isEmpty()) return out;
        if (t.length() <= MAX) {
            out.add(t);
            return out;
        }

        StringBuilder cur = new StringBuilder();
        for (String para : t.split("\\n\\s*\\n")) {
            String p = para.trim();
            if (p.isEmpty()) continue;
            if (p.length() > MAX) {
                flush(out, cur);
                splitLong(p, out);
                continue;
            }
            if (cur.length() > 0 && cur.length() + p.length() + 2 > TARGET) flush(out, cur);
            if (cur.length() > 0) cur.append("\n\n");
            cur.append(p);
        }
        flush(out, cur);
        return out;
    }

    private static void flush(List<String> out, StringBuilder cur) {
        if (cur.length() > 0) {
            out.add(cur.toString());
            cur.setLength(0);
        }
    }

    /** A paragraph longer than {@link #MAX}: cut after a sentence end, else at a space, else hard. */
    private static void splitLong(String p, List<String> out) {
        int start = 0;
        while (start < p.length()) {
            int end = Math.min(p.length(), start + TARGET);
            if (end < p.length()) {
                int cut = lastBreak(p, start + TARGET / 2, Math.min(p.length(), start + MAX));
                end = cut > start ? cut : Math.min(p.length(), start + MAX);
            }
            String piece = p.substring(start, end).trim();
            if (!piece.isEmpty()) out.add(piece);
            start = end;
        }
    }

    /** The index just after the last sentence end (or else space) in [from, to), or -1. */
    private static int lastBreak(String p, int from, int to) {
        int space = -1;
        for (int i = to - 1; i >= from; i--) {
            char c = p.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n' || c == '。' || c == '！' || c == '？') {
                return i + 1;
            }
            if (space < 0 && c == ' ') space = i + 1;
        }
        return space;
    }
}
