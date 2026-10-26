package com.plainphone.app;

/** A short passage of a longer text around where a search matched. */
final class Snippets {

    private Snippets() {}

    /**
     * About {@code before} + {@code after} characters of {@code body} around the first place the query
     * matches (the start of the text when it does not), cut at word breaks, whitespace collapsed, with
     * "..." where text was left out.
     */
    static String make(String body, TextMatch.Query query, int before, int after) {
        if (body == null) return "";
        String flat = body;
        Highlight.Folded f = Highlight.foldWithMap(flat);
        int at = -1, matchEnd = 0;
        TextMatch.Query q = query != null && query.withoutQualifier != null ? query.withoutQualifier : query;
        if (q != null) {
            for (String token : q.tokens) {
                if (token.isEmpty()) continue;
                int i = f.text.indexOf(token);
                if (i >= 0 && (at < 0 || i < at)) {
                    at = i;
                    int last = i + token.length() - 1;
                    matchEnd = f.start[last] + f.length[last];
                }
            }
        }
        int origStart = at >= 0 ? f.start[at] : 0;
        if (at < 0) matchEnd = 0;

        int from = Math.max(0, origStart - before);
        int to = Math.min(flat.length(), Math.max(matchEnd, origStart) + after);
        if (from > 0) {
            int sp = nextSpace(flat, from, Math.min(origStart, from + 20));
            if (sp >= 0) from = sp + 1;
        }
        if (to < flat.length()) {
            int sp = prevSpace(flat, to, Math.max(matchEnd, to - 20));
            if (sp > from) to = sp;
        }
        String piece = flat.substring(from, Math.max(from, to)).replaceAll("\\s+", " ").trim();
        return (from > 0 ? "..." : "") + piece + (to < flat.length() ? "..." : "");
    }

    private static int nextSpace(String s, int from, int limit) {
        for (int i = from; i < limit && i < s.length(); i++) if (Character.isWhitespace(s.charAt(i))) return i;
        return -1;
    }

    private static int prevSpace(String s, int from, int limit) {
        for (int i = Math.min(from, s.length() - 1); i > limit && i >= 0; i--) {
            if (Character.isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }
}
