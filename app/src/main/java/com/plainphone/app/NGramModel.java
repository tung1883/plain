package com.plainphone.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which word follows which, learned from text: the words that usually come after one or two given
 * words. Drives next-word suggestions, multi-word completions ("xin" gives "xin chào") and picks
 * between candidates by what was typed before. The sentence start is the word {@link #START}.
 * Pure Java; the data comes from {@code assets/kb/ngram_<lang>.txt} (see tools/lang/build_ngrams.js).
 */
final class NGramModel {

    /** Stands for "nothing yet in this sentence". */
    static final String START = "^";

    /** A word that may come next and how likely it is (0..1, a share of what followed the context). */
    static final class Pred {
        final String word;
        final double p;

        Pred(String word, double p) {
            this.word = word;
            this.p = p;
        }
    }

    private static final class Row {
        final int total;
        final String[] next;
        final int[] count;

        Row(int total, String[] next, int[] count) {
            this.total = total;
            this.next = next;
            this.count = count;
        }
    }

    /** Where the model's lines come from: "2\\ta\\ttotal\\tb:n,c:n" (after a) and "3\\ta b\\ttotal\\tc:n" (after a b). */
    interface Source {
        List<String> lines(String lang);
    }

    private final Map<String, Row> bi = new HashMap<>();
    private final Map<String, Row> tri = new HashMap<>();

    NGramModel(List<String> lines) {
        for (String line : lines) {
            String[] f = line.split("\t");
            if (f.length < 4) continue;
            int total;
            try {
                total = Integer.parseInt(f[2]);
            } catch (NumberFormatException e) {
                continue;
            }
            String[] items = f[3].split(",");
            String[] next = new String[items.length];
            int[] count = new int[items.length];
            int n = 0;
            for (String it : items) {
                int c = it.lastIndexOf(':');
                if (c <= 0) continue;
                try {
                    count[n] = Integer.parseInt(it.substring(c + 1));
                } catch (NumberFormatException e) {
                    continue;
                }
                next[n++] = it.substring(0, c);
            }
            Row row = new Row(total, java.util.Arrays.copyOf(next, n), java.util.Arrays.copyOf(count, n));
            if (f[0].equals("2")) bi.put(f[1], row);
            else if (f[0].equals("3")) tri.put(f[1], row);
        }
    }

    boolean isEmpty() {
        return bi.isEmpty();
    }

    /**
     * The words likely after {@code p1} (the previous word) given {@code p2} before it; either may be
     * {@link #START} or null. Two-word context wins, one-word context fills in.
     */
    List<Pred> next(String p2, String p1, int max) {
        Map<String, Double> score = new HashMap<>();
        if (p1 != null) {
            Row t = p2 == null ? null : tri.get(p2 + " " + p1);
            if (t != null) {
                for (int i = 0; i < t.next.length; i++) score.put(t.next[i], (double) t.count[i] / t.total);
            }
            Row b = bi.get(p1);
            if (b != null) {
                for (int i = 0; i < b.next.length; i++) {
                    double s = 0.35 * b.count[i] / b.total;
                    Double old = score.get(b.next[i]);
                    if (old == null || s > old) score.put(b.next[i], s);
                }
            }
        }
        List<Pred> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : score.entrySet()) out.add(new Pred(e.getKey(), e.getValue()));
        Collections.sort(out, (a, b) -> Double.compare(b.p, a.p));
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    /** How likely {@code w} is right after {@code p1} (given {@code p2}); 0 when never seen. */
    double prob(String p2, String p1, String w) {
        double best = 0;
        Row t = p2 == null || p1 == null ? null : tri.get(p2 + " " + p1);
        if (t != null) best = share(t, w);
        Row b = p1 == null ? null : bi.get(p1);
        if (b != null) best = Math.max(best, 0.35 * share(b, w));
        return best;
    }

    private static double share(Row r, String w) {
        for (int i = 0; i < r.next.length; i++) if (r.next[i].equals(w)) return (double) r.count[i] / r.total;
        return 0;
    }

    /** The one word that follows with real confidence (a true share, not backed-off), or null. */
    private Pred sure(String p2, String p1) {
        Row t = p2 == null ? null : tri.get(p2 + " " + p1);
        if (t != null && t.total >= 8 && t.next.length > 0 && (double) t.count[0] / t.total >= 0.45) {
            return new Pred(t.next[0], (double) t.count[0] / t.total);
        }
        Row b = bi.get(p1);
        if (b != null && b.total >= 30 && b.next.length > 0 && (double) b.count[0] / b.total >= 0.45) {
            return new Pred(b.next[0], (double) b.count[0] / b.total);
        }
        return null;
    }

    /** Up to {@code maxExtra} words that very likely follow {@code w} (typed after p2 p1): "chào" after "xin". */
    List<String> chain(String p2, String p1, String w, int maxExtra) {
        List<String> out = new ArrayList<>();
        String a = p1, b = w;
        for (int i = 0; i < maxExtra; i++) {
            Pred s = sure(a, b);
            if (s == null || s.word.equals(START) || out.contains(s.word)) break;
            out.add(s.word);
            a = b;
            b = s.word;
        }
        return out;
    }
}
