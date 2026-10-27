package com.plainphone.app;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Word lookup for suggestions and spell check, over frequency-ordered word lists (most common
 * first) of the typing language plus any mixed-in languages. Matching ignores accents and also
 * accepts the ASCII spelling of German umlauts ("ueber" finds "über"). Pure Java, so it is unit-tested.
 */
final class SpellChecker {

    /** Where the word lists come from; {@code words(lang)} is most common first. */
    interface Source {
        List<String> words(String lang);
    }

    static final class Hit {
        final String word, lang;
        final int dist;     // 0 = same word up to accents / umlaut spelling, 1.. = edits away
        final int rank;     // position in its language's list (lower = more common)

        Hit(String word, String lang, int dist, int rank) {
            this.word = word;
            this.lang = lang;
            this.dist = dist;
            this.rank = rank;
        }
    }

    private static final class Dict {
        final String lang;
        final String[] words, plain, trl;
        final Set<String> set = new HashSet<>();

        Dict(String lang, List<String> list) {
            this.lang = lang;
            words = list.toArray(new String[0]);
            plain = new String[words.length];
            trl = new String[words.length];
            for (int i = 0; i < words.length; i++) {
                set.add(words[i]);
                plain[i] = strip(words[i]);
                trl[i] = translit(words[i]);
            }
        }
    }

    private final Source source;
    private final Map<String, Dict> loaded = new HashMap<>();
    private final Set<String> ignored = new HashSet<>();
    private List<Dict> active = new ArrayList<>();

    SpellChecker(Source source) {
        this.source = source;
    }

    /** Use the word lists of {@code primary} first, then of each language in {@code mix}. */
    void setLanguages(String primary, List<String> mix) {
        List<Dict> next = new ArrayList<>();
        next.add(dict(primary));
        for (String m : mix) if (!m.equals(primary)) next.add(dict(m));
        active = next;
    }

    private Dict dict(String lang) {
        Dict d = loaded.get(lang);
        if (d == null) {
            List<String> w = source.words(lang);
            d = new Dict(lang, w == null ? Collections.<String>emptyList() : w);
            loaded.put(lang, d);
        }
        return d;
    }

    void ignore(String word) {
        ignored.add(norm(word));
    }

    static String norm(String w) {
        return w.toLowerCase(Locale.ROOT).replace('’', '\'');
    }

    /** True for a word that should not be flagged: known, ignored, a number, a lone letter. */
    boolean known(String word) {
        String w = norm(word);
        if (w.length() < 2 || ignored.contains(w)) return true;
        for (int i = 0; i < w.length(); i++) {
            if (Character.isDigit(w.charAt(i))) return true;
        }
        for (Dict d : active) if (d.set.contains(w)) return true;
        return false;
    }

    /** Words that complete what was typed so far, most common first. */
    List<Hit> prefix(String typed, int max) {
        String sw = strip(typed);
        List<Hit> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Dict d : active) {
            for (int i = 0; i < d.words.length && out.size() < max; i++) {
                if (!(d.plain[i].startsWith(sw) || d.trl[i].startsWith(sw))) continue;
                if (d.plain[i].equals(sw)) continue;
                if (seen.add(d.words[i])) out.add(new Hit(d.words[i], d.lang, 0, i));
            }
        }
        return out;
    }

    /** Likely intended words for an unknown word: accent / umlaut restores first, then near edits. */
    List<Hit> suggest(String typed, int max) {
        String lc = norm(typed), sw = strip(lc);
        int lim = lc.length() >= 7 ? 2 : 1;
        List<Hit> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Dict d : active) {
            for (int i = 0; i < d.words.length; i++) {
                String w = d.words[i];
                if (w.equals(lc)) continue;
                int dist;
                if (d.plain[i].equals(sw) || d.trl[i].equals(sw)) {
                    dist = 0;
                } else if (Math.abs(w.length() - lc.length()) > lim) {
                    continue;
                } else if (lim == 1 && lc.length() > 2 && lc.charAt(0) != w.charAt(0)
                        && lc.charAt(lc.length() - 1) != w.charAt(w.length() - 1)) {
                    continue;   // one edit cannot change both ends of a word
                } else {
                    dist = osa(lc, w, lim);
                }
                if (dist <= lim && seen.add(w)) out.add(new Hit(w, d.lang, dist, i));
            }
        }
        Collections.sort(out, (a, b) -> a.dist != b.dist ? a.dist - b.dist : a.rank - b.rank);
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    /**
     * The one word an auto-correct may swap in, or null: an accent / umlaut restore, or a single
     * edit that is clearly the likelier word.
     */
    Hit autoFix(String typed) {
        return autoFix(typed, w -> 0);
    }

    /** As {@link #autoFix(String)}; {@code context} scores how well a word follows what came before (0 = unknown). */
    Hit autoFix(String typed, java.util.function.ToDoubleFunction<String> context) {
        if (known(typed) || typed.length() < 3) return null;
        List<Hit> s = suggest(typed, 6);
        if (s.isEmpty()) return null;
        Collections.sort(s, (a, b) -> {
            if (a.dist != b.dist) return a.dist - b.dist;
            return Double.compare(context.applyAsDouble(b.word), context.applyAsDouble(a.word));
        });
        Hit best = s.get(0);
        if (best.dist == 0) return best;
        if (best.dist > 1) return null;
        if (s.size() == 1) return best;
        Hit second = s.get(1);
        if (second.dist > 1) return best;
        // two one-edit candidates: take the one the sentence points to, else the clearly commoner one
        double cb = context.applyAsDouble(best.word), cs = context.applyAsDouble(second.word);
        if (cb > 0 && cb > cs * 2) return best;
        return best.rank * 4 < second.rank ? best : null;
    }

    /** Scratch table for {@link #osa}: no allocation per word. Used from one thread only. */
    private static final int[][] BUF = new int[40][40];

    // ---------------------------------------------------------------- string helpers

    /** Lower case without accents (đ counts as d). */
    static String strip(String w) {
        String n = Normalizer.normalize(w.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) continue;
            sb.append(c == 'đ' ? 'd' : c);
        }
        return sb.toString();
    }

    /** Like {@link #strip} but German umlauts and ß become their two-letter spellings. */
    static String translit(String w) {
        String lc = w.toLowerCase(Locale.ROOT).replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        return strip(lc);
    }

    /** Edit distance with transpositions, giving up (returns limit + 1) when it is over {@code limit}. */
    static int osa(String a, String b, int limit) {
        int m = a.length(), n = b.length();
        if (Math.abs(m - n) > limit) return limit + 1;
        int[][] d = m < BUF.length && n < BUF.length ? BUF : new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) d[i][0] = i;
        for (int j = 0; j <= n; j++) d[0][j] = j;
        for (int i = 1; i <= m; i++) {
            int rowMin = Integer.MAX_VALUE;
            for (int j = 1; j <= n; j++) {
                int c = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int v = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + c);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    v = Math.min(v, d[i - 2][j - 2] + 1);
                }
                d[i][j] = v;
                rowMin = Math.min(rowMin, v);
            }
            if (rowMin > limit) return limit + 1;
        }
        return d[m][n];
    }
}
