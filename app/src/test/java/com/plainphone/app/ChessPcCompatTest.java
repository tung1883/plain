package com.plainphone.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ChessPcCompatTest {

    private static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @Test
    public void ordinaryLineIsPlayable() {
        assertTrue(ChessPcCompat.playable(START, Arrays.asList("e2e4", "e7e5", "g1f3")));
    }

    @Test
    public void emptyOrMalformedIsNotPlayable() {
        assertFalse(ChessPcCompat.playable(START, Collections.<String>emptyList()));
        assertFalse(ChessPcCompat.playable(START, null));
        assertFalse(ChessPcCompat.playable(START, Arrays.asList("e2")));
        assertFalse(ChessPcCompat.playable("not a fen", Arrays.asList("e2e4")));
        assertFalse(ChessPcCompat.playable(null, Arrays.asList("e2e4")));
        assertFalse(ChessPcCompat.playable(START, Arrays.asList("e3e4"))); // no piece on e3
        assertFalse(ChessPcCompat.playable("8/8/8/8/8/8/8/8 w - - 0 1", Arrays.asList("a1a2")));
    }

    @Test
    public void queenPromotionIsFineWithOrWithoutTheLetter() {
        String fen = "8/P6k/8/8/8/8/8/K7 w - - 0 1";
        assertTrue(ChessPcCompat.playable(fen, Arrays.asList("a7a8q")));
        assertTrue(ChessPcCompat.playable(fen, Arrays.asList("a7a8Q")));
        assertTrue(ChessPcCompat.playable(fen, Arrays.asList("a7a8")));
    }

    @Test
    public void underpromotionIsRejectedBecauseThePhoneAutoQueens() {
        String fen = "8/P6k/8/8/8/8/8/K7 w - - 0 1";
        assertFalse(ChessPcCompat.playable(fen, Arrays.asList("a7a8n")));
        assertFalse(ChessPcCompat.playable(fen, Arrays.asList("a7a8r")));
        assertFalse(ChessPcCompat.playable(fen, Arrays.asList("a7a8b")));
    }

    @Test
    public void enPassantIsRejectedBecauseThePhoneHasNone() {
        // White pawn e5, black pawn just played d7-d5; exd6 e.p. captures onto an empty square.
        String fen = "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1";
        assertFalse(ChessPcCompat.playable(fen, Arrays.asList("e5d6")));
        // an ordinary diagonal capture onto an occupied square is fine
        assertTrue(ChessPcCompat.playable("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1", Arrays.asList("e5d6")));
    }

    @Test
    public void enPassantLaterInTheLineIsFoundToo() {
        // 1.e5?? is not it — but after a reply the pawn captures e.p.: simulate occupancy across moves.
        String fen = "4k3/3p4/8/4P3/8/8/8/4K3 b - - 0 1";
        // d7-d5 then exd6 e.p. (square d6 empty) -> rejected
        assertFalse(ChessPcCompat.playable(fen, Arrays.asList("d7d5", "e5d6")));
    }

    @Test
    public void castlingMovesTheRookSoLaterMovesSeeTheRightBoard() {
        String fen = "4k3/8/8/8/8/8/8/4K2R w K - 0 1";
        // O-O then the rook (now on f1) moves: only legal if the rook was relocated by the castle.
        assertTrue(ChessPcCompat.playable(fen, Arrays.asList("e1g1", "e8e7", "f1f7")));
        // the rook is NOT on h1 any more
        assertFalse(ChessPcCompat.playable(fen, Arrays.asList("e1g1", "e8e7", "h1h7")));
    }
}
