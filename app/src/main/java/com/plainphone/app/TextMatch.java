package com.plainphone.app;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Query matching for search: Unicode folding, token matching, and fuzzy trigram similarity.
 *
 * <p>Three separate problems, solved in layers:
 *
 * <ul>
 *   <li><b>Folding</b> — "Sài gòn" and "Sai Gon" are the same name written two ways, and
 *       "I_heart" and "I heart" differ only in a separator. Decomposing to NFD and dropping
 *       the combining marks collapses both, so neither has to be typed exactly.
 *   <li><b>Token matching</b> — once folded, a query's words are matched independently and
 *       in any order, so "pha gia" still finds "Gia phả".
 *   <li><b>Trigram similarity</b> — for names that are merely <i>close</i> ("Instgram",
 *       "recepit"), the folded strings are compared as sets of 3-character shingles. This is
 *       vector similarity over sparse lexical vectors — the same idea as Postgres's pg_trgm
 *       — and unlike a neural embedding it needs no model, runs in microseconds, and stays
 *       precise about spelling, which is what a name search is actually about.
 * </ul>
 *
 * <p>Exact and prefix matches always outrank fuzzy ones: a typo-tolerant search that buries
 * the file you named correctly is worse than no fuzzy matching at all.
 */
class TextMatch {

    private TextMatch() {}

    static final int NO_MATCH = -1;

    /**
     * Fuzzy scores start above every lexical tier, so an exact hit can never be displaced
     * by a close-but-wrong name.
     */
    private static final int FUZZY_BASE = 100;

    /**
     * Fraction of a query's trigrams that must appear in a name for it to count as similar.
     * Tuned against real cases: a dropped letter ("instgram") keeps 0.75, and a transposition
     * ("recepit" for "receipt") only 0.57, since swapping two characters destroys three
     * trigrams at once. Unrelated names score far below this — "monster" against "movies"
     * is 0.29 — so the gap is wide enough for a threshold here to be safe.
     */
    private static final double MIN_SIMILARITY = 0.5;

    private static final int TRIGRAM = 3;
    /** Front padding, so short names and word starts still yield trigrams. */
    private static final String PAD = "  ";

    /** What kind of thing a query's qualifier word asks for, if it has one. */
    enum Kind { ANY, DIRECTORY, FILE }

    /**
     * Words that describe the kind of result wanted rather than its name. Typing "ai folder"
     * means "the folder called AI", not "something with both 'ai' and 'folder' in its name" —
     * without this they'd be matched as name text and find nothing.
     */
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

    /** A query, folded and shredded once so the per-candidate work stays cheap. */
    static class Query {
        /**
         * The query as typed (trimmed). Kept because provider-side matching — MediaStore's
         * LIKE, the contacts filter Uri — runs against unfolded stored text, so it has to be
         * given the accented form the user actually typed.
         */
        final String raw;
        final String folded;
        final String[] tokens;
        final boolean empty;
        final String[] trigrams;
        /**
         * 64-bit presence mask of this query's trigrams, used to reject obviously unrelated
         * names with one AND and a popcount instead of a set intersection. Hash collisions
         * can only make the mask look more similar, never less, so nothing that should match
         * is ever discarded here.
         */
        final long signature;

        /** The kind of result a qualifier word asked for, or {@link Kind#ANY}. */
        final Kind kind;
        /**
         * The same query with its qualifier words removed, or null if there weren't any.
         * Candidates are matched against both this and the full query, rather than only the
         * stripped form — so "ai folder" finds the folder "AI" without losing a file that
         * genuinely is called "AI folder notes.txt".
         */
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

        // A qualifier is only stripped when something is left to search by: typing "folder"
        // on its own is a name search for the word, not a request to list every directory.
        if (kind == Kind.ANY || nameTokens.isEmpty()) {
            return new Query(trimmed, folded, Kind.ANY, null);
        }
        String strippedFolded = String.join(" ", nameTokens);
        Query stripped = new Query(strippedFolded, strippedFolded, Kind.ANY, null);
        return new Query(trimmed, folded, kind, stripped);
    }

    /**
     * Collapses a string to its bare comparable form: lowercase, no diacritics, and every
     * run of punctuation or whitespace reduced to a single space.
     *
     * <p>NFD splits an accented character into a base letter plus combining marks, which are
     * then dropped — that's what turns "à" into "a". A handful of letters are not accented
     * forms at all but distinct letters that NFD leaves alone, so they're mapped by hand;
     * Vietnamese "đ" is the one that matters here, since without it "Sơ đồ" would never fold
     * to "so do".
     */
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
                // Separators of every kind (_ - . , ( ) and whitespace) collapse together,
                // which is what makes "I_heart" and "I heart" the same query.
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

    /** Letters NFD won't decompose because they're their own letter, not an accented base. */
    private static String undecomposable(char c) {
        switch (c) {
            case 'đ': return "d";  // Vietnamese
            case 'ø': return "o";
            case 'ß': return "ss";
            case 'æ': return "ae";
            case 'œ': return "oe";
            case 'ł': return "l";
            default: return null;
        }
    }

    /**
     * Lexical rank of an already-folded name, or {@link #NO_MATCH}. Lower is better, and the
     * tiers run from "this is the thing" to "all the words are in there somewhere".
     */
    static int lexicalScore(String foldedText, Query query) {
        if (query.empty) return 1;

        if (foldedText.equals(query.folded)) return 0;
        if (foldedText.startsWith(query.folded)) return 1;

        int phraseAt = foldedText.indexOf(query.folded);
        if (phraseAt > 0) return foldedText.charAt(phraseAt - 1) == ' ' ? 2 : 3;

        // No contiguous phrase: fall back to every token appearing somewhere, in any order.
        boolean allAtWordStart = true;
        for (String token : query.tokens) {
            int at = foldedText.indexOf(token);
            if (at < 0) return NO_MATCH;
            if (at != 0 && foldedText.charAt(at - 1) != ' ') allAtWordStart = false;
        }
        return allAtWordStart ? 4 : 5;
    }

    /**
     * Full rank for a candidate: its lexical tier if it matches outright, otherwise a fuzzy
     * score derived from trigram similarity, or {@link #NO_MATCH} if it isn't even close.
     *
     * @param signature the name's precomputed trigram mask, or 0 to skip the fast reject
     */
    static int score(String foldedText, long signature, Query query) {
        int lexical = lexicalScore(foldedText, query);
        if (lexical != NO_MATCH) return lexical;
        if (query.empty || query.trigrams.length == 0) return NO_MATCH;

        // Cheap upper bound first: if even the collision-inflated mask can't clear the
        // threshold, the real intersection certainly can't either.
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

    /**
     * Rank for a candidate whose kind is known, honouring any type qualifier in the query.
     * Scored against both the full query and the qualifier-stripped one, keeping whichever
     * fits better.
     */
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

    /** Convenience for small candidate sets, where precomputing a signature isn't worth it. */
    static int score(String rawText, Query query) {
        return score(fold(rawText), 0L, query);
    }

    /**
     * Best score across a name and its alternate search terms. Keyword hits rank below every
     * direct hit, so a screen's real name always beats something merely listing that word.
     */
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

    /**
     * Share of the query's trigrams present in the name — containment rather than a
     * symmetric Jaccard, because a short query is routinely a small part of a long filename
     * and shouldn't be penalised for the rest of it.
     */
    static double similarity(String foldedText, Query query) {
        if (query.trigrams.length == 0) return 0;

        // The query's trigrams were built from a padded string, so the name has to be padded
        // the same way — otherwise the leading grams ("  s", " sa") could never be found and
        // every short query would be scored far worse than it deserves.
        String padded = PAD + foldedText;
        int present = 0;
        for (String trigram : query.trigrams) {
            if (padded.contains(trigram)) present++;
        }
        return (double) present / query.trigrams.length;
    }

    /** Trigram mask for a name, precomputed once at index time. */
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

    /**
     * Overlapping 3-character shingles, padded at the front so short names and word starts
     * still produce grams ("hi" alone would otherwise yield none).
     */
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
