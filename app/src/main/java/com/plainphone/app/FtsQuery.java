package com.plainphone.app;

import java.util.ArrayList;
import java.util.List;

/** Turns a typed search into a query for the content index (SQLite FTS4, standard query syntax). */
final class FtsQuery {

    private FtsQuery() {}

    /**
     * Every word must appear (the last one may be only the start of a word) and "quoted phrases" must
     * appear as written. Minus words are not part of the query: SQLite's query syntax for "not" differs
     * between builds, so {@link ContentSearch} leaves those passages out itself. Null when there is
     * nothing to look for.
     */
    static String build(ParsedQuery q) {
        List<String> free = tokens(q.text);
        List<String> phrases = new ArrayList<>();
        for (String phrase : q.phrases) {
            List<String> words = tokens(phrase);
            if (words.isEmpty()) continue;
            phrases.add(String.join(" ", words));
            for (String w : words) free.remove(w);          // the phrase covers these words
        }

        int chars = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < free.size(); i++) {
            String t = free.get(i);
            chars += t.length();
            if (sb.length() > 0) sb.append(' ');
            sb.append('"').append(t).append(i == free.size() - 1 && phrases.isEmpty() ? "*" : "").append('"');
        }
        for (String ph : phrases) {
            chars += ph.length();
            if (sb.length() > 0) sb.append(' ');
            sb.append('"').append(ph).append('"');
        }
        if (sb.length() == 0 || chars < 2) return null;

        return sb.toString();
    }

    /** The words of {@code text} as the index sees them. */
    static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        String folded = Fold.forIndex(text);
        if (folded.isEmpty()) return out;
        for (String t : folded.split(" ")) if (!t.isEmpty()) out.add(t);
        return out;
    }
}
