package com.plainphone.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Takes the operators out of a typed search:
 * <pre>
 *   "exact phrase"   -excluded   app: note: todo: file: web: contact: rec: vault: content:
 *   pdf:invoice  ext:docx  in:today|week|month|year    todo buy milk   note idea
 * </pre>
 * Anything else, including an unknown {@code x:} prefix, a URL or a time like 10:30, stays plain text.
 */
final class QueryParser {

    private QueryParser() {}

    private static final Set<String> EXTS = new HashSet<>(Arrays.asList(
            "pdf", "md", "txt", "docx", "xlsx", "pptx", "odt", "ods", "odp", "epub", "rtf", "csv", "json", "html"));

    static ParsedQuery parse(String raw) {
        return parse(raw, System.currentTimeMillis());
    }

    static ParsedQuery parse(String raw, long now) {
        String input = raw == null ? "" : raw;
        // token kinds: w word, p "phrase", n -word, N -"phrase"
        List<String[]> tokens = tokenize(input);

        String command = null, commandText = "";
        if (tokens.size() >= 2 && tokens.get(0)[0].equals("w")) {
            String first = tokens.get(0)[1].toLowerCase(Locale.ROOT);
            if (first.equals("todo") || first.equals("note")) {
                command = first;
                String t = input.trim();
                commandText = t.substring(Math.min(t.length(), first.length())).trim();
            }
        }

        List<String> words = new ArrayList<>();
        List<String> phrases = new ArrayList<>();
        List<String> excluded = new ArrayList<>();
        Set<SearchResult.Kind> kinds = EnumSet.noneOf(SearchResult.Kind.class);
        Set<String> exts = new HashSet<>();
        long since = 0;

        for (String[] tok : tokens) {
            String kind = tok[0], text = tok[1];
            if (kind.equals("p")) {
                String f = TextMatch.fold(text);
                if (!f.isEmpty()) {
                    phrases.add(f);
                    words.add(text);
                }
            } else if (kind.equals("n") || kind.equals("N")) {
                String f = TextMatch.fold(text);
                if (!f.isEmpty()) excluded.add(f);
            } else {
                int colon = text.indexOf(':');
                String prefix = colon > 1 ? text.substring(0, colon).toLowerCase(Locale.ROOT) : "";
                String rest = colon > 1 ? text.substring(colon + 1) : "";
                if (!prefix.isEmpty() && prefix.matches("[a-z]+")) {
                    if (prefix.equals("in")) {
                        long s = sinceOf(rest, now);
                        if (s > 0) {
                            since = s;
                            continue;
                        }
                    } else if (applyOperator(prefix, rest, kinds, exts)) {
                        if (!rest.isEmpty() && !prefix.equals("ext")) words.add(rest);
                        continue;
                    }
                }
                words.add(text);
            }
        }
        return new ParsedQuery(input.trim(), String.join(" ", words).trim(), phrases, excluded, kinds, exts, since,
                command, commandText);
    }

    /** Apply a {@code prefix:rest} operator; false when the prefix is not one. */
    private static boolean applyOperator(String prefix, String rest, Set<SearchResult.Kind> kinds, Set<String> exts) {
        switch (prefix) {
            case "app": case "apps": kinds.add(SearchResult.Kind.APP); return true;
            case "note": case "notes": kinds.add(SearchResult.Kind.NOTE); return true;
            case "todo": case "todos": kinds.add(SearchResult.Kind.TODO); return true;
            case "rec": case "recording": case "recordings": kinds.add(SearchResult.Kind.RECORDING); return true;
            case "web": kinds.add(SearchResult.Kind.WEB); return true;
            case "contact": case "contacts": kinds.add(SearchResult.Kind.CONTACT); return true;
            case "vault": kinds.add(SearchResult.Kind.VAULT); return true;
            case "file": case "files":
                kinds.add(SearchResult.Kind.FILE);
                kinds.add(SearchResult.Kind.CONTENT);
                return true;
            case "content": kinds.add(SearchResult.Kind.CONTENT); return true;
            case "ext":
                if (rest.isEmpty()) return false;
                exts.add(rest.toLowerCase(Locale.ROOT).replace(".", ""));
                return true;
            default:
                if (EXTS.contains(prefix)) {
                    exts.add(prefix);
                    return true;
                }
                return false;
        }
    }

    /** The start of today / a week / a month / a year ago, or 0 when {@code word} is none of those. */
    private static long sinceOf(String word, long now) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        switch (word.toLowerCase(Locale.ROOT)) {
            case "today": return c.getTimeInMillis();
            case "week": c.add(Calendar.DAY_OF_YEAR, -7); return c.getTimeInMillis();
            case "month": c.add(Calendar.MONTH, -1); return c.getTimeInMillis();
            case "year": c.add(Calendar.YEAR, -1); return c.getTimeInMillis();
            default: return 0;
        }
    }

    /** Split on spaces, keeping "quoted phrases" together; a leading minus marks an exclusion. */
    private static List<String[]> tokenize(String raw) {
        List<String[]> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false, neg = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (inQuote) {
                if (c == '"') {
                    out.add(new String[]{neg ? "N" : "p", cur.toString()});
                    cur.setLength(0);
                    inQuote = false;
                    neg = false;
                } else {
                    cur.append(c);
                }
                continue;
            }
            if (c == '"') {
                if (cur.length() == 1 && cur.charAt(0) == '-') {
                    neg = true;
                    cur.setLength(0);
                } else {
                    flush(out, cur);
                }
                inQuote = true;
            } else if (Character.isWhitespace(c)) {
                flush(out, cur);
            } else {
                cur.append(c);
            }
        }
        if (inQuote) {
            out.add(new String[]{neg ? "N" : "p", cur.toString()});   // an unclosed quote still counts
        } else {
            flush(out, cur);
        }
        return out;
    }

    private static void flush(List<String[]> out, StringBuilder cur) {
        if (cur.length() == 0) return;
        String t = cur.toString();
        cur.setLength(0);
        if (t.length() > 1 && t.charAt(0) == '-') out.add(new String[]{"n", t.substring(1)});
        else out.add(new String[]{"w", t});
    }
}
