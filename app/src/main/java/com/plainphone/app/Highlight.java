package com.plainphone.app;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finds where a search matched in the text as the user sees it (with its accents and punctuation), by
 * matching on the folded text ({@link TextMatch#fold}) and mapping back.
 */
final class Highlight {

    private Highlight() {}

    /** The folded form of a text and, for each folded character, where it came from in the original. */
    static final class Folded {
        final String text;
        final int[] start;     // original index of the character each folded character came from
        final int[] length;    // how many original chars that character covers

        Folded(String text, int[] start, int[] length) {
            this.text = text;
            this.start = start;
            this.length = length;
        }
    }

    /** Same result text as {@link TextMatch#fold}, with the map back into {@code original}. */
    static Folded foldWithMap(String original) {
        StringBuilder out = new StringBuilder(original.length());
        int[] start = new int[original.length() * 4 + 4];
        int[] length = new int[start.length];
        int n = 0;
        boolean pendingSpace = false;
        int pendingAt = 0, pendingLen = 1;
        int i = 0;
        while (i < original.length()) {
            int cp = original.codePointAt(i);
            int cpLen = Character.charCount(cp);
            String lower = new String(Character.toChars(cp)).toLowerCase(Locale.ROOT);
            String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
            for (int k = 0; k < decomposed.length(); k++) {
                char c = decomposed.charAt(k);
                if (Character.getType(c) == Character.NON_SPACING_MARK) continue;
                String replacement = TextMatch.undecomposable(c);
                if (replacement == null && !Character.isLetterOrDigit(c)) {
                    if (out.length() > 0 && !pendingSpace) {
                        pendingAt = i;
                        pendingLen = cpLen;
                    }
                    pendingSpace = out.length() > 0;
                    continue;
                }
                if (pendingSpace) {
                    out.append(' ');
                    start[n] = pendingAt;
                    length[n] = pendingLen;
                    n++;
                    pendingSpace = false;
                }
                String piece = replacement != null ? replacement : String.valueOf(c);
                for (int p = 0; p < piece.length(); p++) {
                    out.append(piece.charAt(p));
                    start[n] = i;
                    length[n] = cpLen;
                    n++;
                }
            }
            i += cpLen;
        }
        return new Folded(out.toString(), java.util.Arrays.copyOf(start, n), java.util.Arrays.copyOf(length, n));
    }

    /**
     * The character ranges ({start, end}, end exclusive, sorted, not overlapping) of {@code text}
     * where a word of the query appears.
     */
    static List<int[]> ranges(String text, TextMatch.Query query) {
        List<int[]> out = new ArrayList<>();
        if (text == null || text.isEmpty() || query == null || query.empty) return out;
        TextMatch.Query q = query.withoutQualifier != null ? query.withoutQualifier : query;
        Folded f = foldWithMap(text);
        List<int[]> raw = new ArrayList<>();
        for (String token : q.tokens) {
            if (token.isEmpty()) continue;
            int from = 0, found = 0;
            while (found < 30 && (from = f.text.indexOf(token, from)) >= 0) {
                int last = from + token.length() - 1;
                raw.add(new int[]{f.start[from], f.start[last] + f.length[last]});
                from += 1;
                found++;
            }
        }
        java.util.Collections.sort(raw, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(b[1], a[1]));
        for (int[] r : raw) {
            if (!out.isEmpty() && r[0] <= out.get(out.size() - 1)[1]) {
                int[] last = out.get(out.size() - 1);
                last[1] = Math.max(last[1], r[1]);
            } else {
                out.add(new int[]{r[0], r[1]});
            }
        }
        return out;
    }

    /** {@code text} with the matched words in bold; the plain text when nothing matched. */
    static CharSequence apply(String text, TextMatch.Query query) {
        List<int[]> ranges = ranges(text, query);
        if (ranges.isEmpty()) return text;
        android.text.SpannableString s = new android.text.SpannableString(text);
        for (int[] r : ranges) {
            s.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), r[0], r[1],
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }
}
