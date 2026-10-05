package com.plainphone.app;

import java.util.List;

/** Can the phone's own board play a PC-generated puzzle? The PC side uses a full chess
 *  library; {@link ChessRules}/{@link ChessBoardView} deliberately keep two simplifications
 *  (pawns always promote to a queen; no en passant), so a puzzle whose solution needs either
 *  would be unplayable — such puzzles are dropped on arrival (the game still counts as
 *  scanned). Pure logic, unit-tested. */
final class ChessPcCompat {

    private ChessPcCompat() {}

    static boolean playable(String fen, List<String> solution) {
        char[][] b = board(fen);
        if (b == null || solution == null || solution.isEmpty()) return false;
        for (String uci : solution) {
            if (uci == null || uci.length() < 4) return false;
            int fc = uci.charAt(0) - 'a', fr = 8 - (uci.charAt(1) - '0');
            int tc = uci.charAt(2) - 'a', tr = 8 - (uci.charAt(3) - '0');
            if (!in(fr, fc) || !in(tr, tc)) return false;
            char piece = b[fr][fc];
            if (piece == 0) return false;
            if (uci.length() > 4 && Character.toLowerCase(uci.charAt(4)) != 'q') return false; // underpromotion
            char kind = Character.toUpperCase(piece);
            if (kind == 'P' && fc != tc && b[tr][tc] == 0) return false; // en passant
            b[tr][tc] = piece;
            b[fr][fc] = 0;
            if (kind == 'K' && Math.abs(tc - fc) == 2) { // castling: bring the rook along
                int rookFrom = tc > fc ? 7 : 0, rookTo = tc > fc ? 5 : 3;
                b[tr][rookTo] = b[tr][rookFrom];
                b[tr][rookFrom] = 0;
            }
        }
        return true;
    }

    private static boolean in(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    private static char[][] board(String fen) {
        if (fen == null) return null;
        String[] rows = fen.split(" ")[0].split("/");
        if (rows.length != 8) return null;
        char[][] b = new char[8][8];
        for (int r = 0; r < 8; r++) {
            int c = 0;
            for (char ch : rows[r].toCharArray()) {
                if (Character.isDigit(ch)) c += ch - '0';
                else if (c < 8) b[r][c++] = ch;
                else return null;
            }
            if (c != 8) return null;
        }
        return b;
    }
}
