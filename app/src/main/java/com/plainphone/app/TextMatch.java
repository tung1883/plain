package com.plainphone.app;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class TextMatch {

    private TextMatch() {}

    static final int NO_MATCH = -1;

    private static final int FUZZY_BASE = 100;

    private static final double MIN_SIMILARITY = 0.5;

    private static final int TRIGRAM = 3;

    private static final String PAD = "  ";

    enum Kind { ANY, DIRECTORY, FILE }

    private static Kind qualifier(String token) {
        switch (token) {
            case "folder": case "folders":
            case "dir": case "dirs":
            case "directory": case "directories":
                return Kind.DIRECTORY;
            case "file": case "files":
                return Kind.FILE;
            default:
                return null;
        }
    }

    static class Query {

        final String raw;
        final String folded;
        final String[] tokens;
        final boolean empty;
        final String[] trigrams;

        final long signature;

        final Kind kind;

        final Query withoutQualifier;

        private Query(String raw, String folded, Kind kind, Query withoutQualifier) {
            this.raw = raw;
            this.folded = folded;
            this.empty = folded.isEmpty();
            this.tokens = folded.isEmpty() ? new String[0] : folded.split(" ");
            List<String> grams = trigramsOf(folded);
            this.trigrams = grams.toArray(new String[0]);
            this.signature = signatureOf(grams);
            this.kind = kind;
            this.withoutQualifier = withoutQualifier;
        }
    }

    static Query prepare(String raw) {
        String trimmed = raw.trim();
        String folded = fold(trimmed);
        if (folded.isEmpty()) return new Query(trimmed, folded, Kind.ANY, null);

        Kind kind = Kind.ANY;
        List<String> nameTokens = new ArrayList<>();
        for (String token : folded.split(" ")) {
            Kind asQualifier = qualifier(token);
            if (asQualifier != null && kind == Kind.ANY) {
                kind = asQualifier;
            } else {
                nameTokens.add(token);
            }
        }

        if (kind == Kind.ANY || nameTokens.isEmpty()) {
            return new Query(trimmed, folded, Kind.ANY, null);
        }
        String strippedFolded = String.join(" ", nameTokens);
        Query stripped = new Query(strippedFolded, strippedFolded, Kind.ANY, null);
        return new Query(trimmed, folded, kind, stripped);
    }

    static String fold(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);

        StringBuilder folded = new StringBuilder(decomposed.length());
        boolean pendingSpace = false;
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) continue;

            String replacement = undecomposable(c);
            if (replacement == null && !Character.isLetterOrDigit(c)) {

                pendingSpace = folded.length() > 0;
                continue;
            }
            if (pendingSpace) {
                folded.append(' ');
                pendingSpace = false;
            }
            folded.append(replacement != null ? replacement : c);
        }
        return folded.toString();
    }

    private static String undecomposable(char c) {
        switch (c) {
            case 'đ': return "d";
            case 'ø': return "o";
            case 'ß': return "ss";
            case 'æ': return "ae";
            case 'œ': return "oe";
            case 'ł': return "l";
            default: return null;
        }
    }

    static int lexicalScore(String foldedText, Query query) {
        if (query.empty) return 1;

        if (foldedText.equals(query.folded)) return 0;
        if (foldedText.startsWith(query.folded)) return 1;

        int phraseAt = foldedText.indexOf(query.folded);
        if (phraseAt > 0) return foldedText.charAt(phraseAt - 1) == ' ' ? 2 : 3;

        boolean allAtWordStart = true;
        for (String token : query.tokens) {
            int at = foldedText.indexOf(token);
            if (at < 0) return NO_MATCH;
            if (at != 0 && foldedText.charAt(at - 1) != ' ') allAtWordStart = false;
        }
        return allAtWordStart ? 4 : 5;
    }

    static int score(String foldedText, long signature, Query query) {
        int lexical = lexicalScore(foldedText, query);
        if (lexical != NO_MATCH) return lexical;
        if (query.empty || query.trigrams.length == 0) return NO_MATCH;

        if (signature != 0) {
            int possible = Long.bitCount(signature & query.signature);
            if (possible < Math.ceil(MIN_SIMILARITY * Long.bitCount(query.signature))) {
                return NO_MATCH;
            }
        }

        double similarity = similarity(foldedText, query);
        if (similarity < MIN_SIMILARITY) return NO_MATCH;
        return FUZZY_BASE + (int) Math.round((1 - similarity) * 100);
    }

    static int score(String foldedText, long signature, Query query, boolean isDirectory) {
        int best = score(foldedText, signature, query);

        Query stripped = query.withoutQualifier;
        if (stripped != null && matchesKind(query.kind, isDirectory)) {
            int strippedScore = score(foldedText, signature, stripped);
            if (strippedScore != NO_MATCH && (best == NO_MATCH || strippedScore < best)) {
                best = strippedScore;
            }
        }
        return best;
    }

    private static boolean matchesKind(Kind kind, boolean isDirectory) {
        if (kind == Kind.DIRECTORY) return isDirectory;
        if (kind == Kind.FILE) return !isDirectory;
        return true;
    }

    static int score(String rawText, Query query) {
        return score(fold(rawText), 0L, query);
    }

    static int score(String rawTitle, String[] keywords, Query query) {
        int best = score(rawTitle, query);
        if (best == 0 || best == 1) return best;

        for (String keyword : keywords) {
            int keywordScore = score(keyword, query);
            if (keywordScore == NO_MATCH) continue;
            int demoted = keywordScore + 6;
            if (best == NO_MATCH || demoted < best) best = demoted;
        }
        return best;
    }

    static double similarity(String foldedText, Query query) {
        if (query.trigrams.length == 0) return 0;

        String padded = PAD + foldedText;
        int present = 0;
        for (String trigram : query.trigrams) {
            if (padded.contains(trigram)) present++;
        }
        return (double) present / query.trigrams.length;
    }

    static long signatureOf(String folded) {
        return signatureOf(trigramsOf(folded));
    }

    private static long signatureOf(List<String> trigrams) {
        long signature = 0;
        for (String trigram : trigrams) {
            signature |= 1L << (trigram.hashCode() & 63);
        }
        return signature;
    }

    private static List<String> trigramsOf(String folded) {
        List<String> trigrams = new ArrayList<>();
        if (folded.isEmpty()) return trigrams;

        String padded = PAD + folded;
        for (int i = 0; i + TRIGRAM <= padded.length(); i++) {
            trigrams.add(padded.substring(i, i + TRIGRAM));
        }
        return trigrams;
    }
}

