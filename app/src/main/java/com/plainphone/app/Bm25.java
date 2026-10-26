package com.plainphone.app;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Okapi BM25 from SQLite FTS4's {@code matchinfo(table, 'pcnalx')} for a table with one indexed column. */
final class Bm25 {

    private Bm25() {}

    private static final double K1 = 1.2, B = 0.75;

    /** The 32-bit integers of a matchinfo blob (host byte order). */
    static int[] parse(byte[] blob) {
        int[] out = new int[blob.length / 4];
        ByteBuffer buf = ByteBuffer.wrap(blob).order(ByteOrder.nativeOrder());
        for (int i = 0; i < out.length; i++) out[i] = buf.getInt();
        return out;
    }

    /**
     * Layout for one column: {@code [p, c, n, a, l, then 3 numbers per phrase: hits in this row, hits
     * in all rows, rows with a hit]}. Higher is a better match.
     */
    static double score(int[] mi) {
        if (mi.length < 5) return 0;
        int phrases = mi[0], rows = mi[2];
        double avg = Math.max(1, mi[3]), len = mi[4];
        double score = 0;
        for (int i = 0; i < phrases; i++) {
            int base = 5 + 3 * i;
            if (base + 2 >= mi.length) break;
            double tf = mi[base], docsWithHit = mi[base + 2];
            double idf = Math.log((rows - docsWithHit + 0.5) / (docsWithHit + 0.5) + 1);
            score += idf * (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * len / avg));
        }
        return score;
    }
}
