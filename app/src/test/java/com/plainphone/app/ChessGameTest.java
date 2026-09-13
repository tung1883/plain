package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChessGameTest {
    @Test public void readsHeadersMainLineAndPositions() {
        ChessGame g = ChessGame.fromPgn("[White \"Ada\"]\n[Black \"Turing\"]\n\n1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 (3... Nf6) 1-0");
        assertEquals("Ada — Turing", g.title());
        assertEquals(6, g.plies());
        assertEquals("e4", g.moves.get(0));
        assertTrue(g.fenAt(1).startsWith("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b"));
    }

    @Test public void handlesCastlingAndFens() {
        ChessGame g = ChessGame.fromPgn("1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. O-O");
        assertEquals(7, g.plies());
        assertTrue(g.fenAt(7).contains("RNBQ1RK1"));
    }
}
