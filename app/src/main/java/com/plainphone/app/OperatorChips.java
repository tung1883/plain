package com.plainphone.app;

import java.util.Locale;

/** The filter chips under the search box: each one adds or removes a search operator such as {@code note:} or {@code pdf:}. */
final class OperatorChips {

    private OperatorChips() {}

    /** The chips shown: label and the operator each one types. */
    static final String[][] ALL = {
            {"Apps", "app:"}, {"Notes", "note:"}, {"To-do", "todo:"}, {"Files", "file:"},
            {"File content", "content:"}, {"PDF", "pdf:"}, {"Contacts", "contact:"},
    };

    /** True when the query already has the operator as a word of its own (at the start of a word). */
    static boolean isOn(String query, String operator) {
        return indexOf(query.toLowerCase(Locale.ROOT), operator) >= 0;
    }

    /** The query with the operator removed when it is there, or else added at the front. */
    static String toggle(String query, String operator) {
        String lower = query.toLowerCase(Locale.ROOT);
        int at = indexOf(lower, operator);
        if (at < 0) return operator + query;
        String before = query.substring(0, at);
        String after = query.substring(at + operator.length());
        return (before + after).replaceAll("^\\s+", "");
    }

    /** Position of {@code operator} at the start of the text or after a space, or -1. */
    private static int indexOf(String lower, String operator) {
        int from = 0;
        while (true) {
            int at = lower.indexOf(operator, from);
            if (at < 0) return -1;
            if (at == 0 || Character.isWhitespace(lower.charAt(at - 1))) return at;
            from = at + 1;
        }
    }
}
