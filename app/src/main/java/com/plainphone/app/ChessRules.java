package com.plainphone.app;

import java.util.ArrayList;
import java.util.List;

/** Headless chess rules for the puzzle generator — it needs to replay an imported game and
 *  branch into hypothetical engine lines off the UI thread, with no {@link ChessBoardView} on
 *  screen. Deliberately a standalone port of that view's own move-generation logic rather than
 *  shared code: the view's rules are entangled with its mutable on-screen fields (used live by
 *  drag/drop, double-tap search, analysis rendering), and re-deriving the same rules purely
 *  here is safer than reshaping that working, shipped code to fit a background job. */
final class ChessRules {
    private ChessRules() {}

    static final int WK = 1, WQ = 2, BK = 4, BQ = 8;

    /** One position snapshot. {@code board} is only ever mutated internally, temporarily, by
     *  the check-safety probe in {@link #legalMoves} — never left mutated across a call. */
    static final class Pos {
        final char[][] board;
        final boolean whiteTurn;
        final int castleRights;
        final int fullmoveNumber;

        Pos(char[][] board, boolean whiteTurn, int castleRights, int fullmoveNumber) {
            this.board = board;
            this.whiteTurn = whiteTurn;
            this.castleRights = castleRights;
            this.fullmoveNumber = fullmoveNumber;
        }
    }

    static Pos start() {
        char[][] b = new char[][]{
                {'r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'},
                {'p', 'p', 'p', 'p', 'p', 'p', 'p', 'p'},
                {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0},
                {'P', 'P', 'P', 'P', 'P', 'P', 'P', 'P'},
                {'R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R'}};
        return new Pos(b, true, WK | WQ | BK | BQ, 1);
    }

    // --- replay ---------------------------------------------------------

    /** Replays {@code sans} from the start position, matching each token against the legal
     *  moves from wherever it lands — same approach as {@code ChessBoardView.loadSanMoves}.
     *  Returns one {@link Pos} per successfully-applied ply (index i = position after move i);
     *  stops early (a short list) at the first token that doesn't match a legal move. */
    static List<Pos> replay(List<String> sans) {
        List<Pos> out = new ArrayList<>();
        Pos p = start();
        for (String raw : sans) {
            String san = normalizeSan(raw);
            int[] move = findMoveBySan(p, san);
            if (move == null) break;
            p = apply(p, move[0], move[1], move[2], move[3]);
            out.add(p);
        }
        return out;
    }

    static String normalizeSan(String san) {
        String s = san.replace("0-0-0", "O-O-O").replace("0-0", "O-O");
        int end = s.length();
        while (end > 0 && "+#!?".indexOf(s.charAt(end - 1)) >= 0) end--;
        return s.substring(0, end);
    }

    static int[] findMoveBySan(Pos p, String san) {
        char[][] board = p.board;
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            char moving = board[r][c];
            if (moving == 0 || Character.isUpperCase(moving) != p.whiteTurn) continue;
            for (int[] mv : legalMoves(p, r, c)) {
                char captured = board[mv[0]][mv[1]];
                boolean castle = Character.toUpperCase(moving) == 'K' && Math.abs(mv[1] - c) == 2;
                boolean promotes = Character.toUpperCase(moving) == 'P' && (mv[0] == 0 || mv[0] == 7);
                String candidate = castle ? (mv[1] > c ? "O-O" : "O-O-O")
                        : sanFor(p, moving, r, c, mv[0], mv[1], captured != 0, promotes);
                if (candidate.equals(san)) return new int[]{r, c, mv[0], mv[1]};
            }
        }
        return null;
    }

    // --- apply / undo -----------------------------------------------------

    /** Applies one move to a fresh copy of {@code p} — castling moves the rook too, pawns
     *  auto-queen (same simplification {@code ChessBoardView} already makes). */
    static Pos apply(Pos p, int fromRow, int fromCol, int toRow, int toCol) {
        char[][] b = copy(p.board);
        char moving = b[fromRow][fromCol];
        char captured = b[toRow][toCol];
        boolean castle = Character.toUpperCase(moving) == 'K' && Math.abs(toCol - fromCol) == 2;
        boolean promotes = Character.toUpperCase(moving) == 'P' && (toRow == 0 || toRow == 7);

        int rights = updateCastleRights(p.castleRights, moving, fromRow, fromCol, captured, toRow, toCol);
        b[toRow][toCol] = moving;
        b[fromRow][fromCol] = 0;
        if (castle) {
            int rookFrom = toCol > fromCol ? 7 : 0;
            int rookTo = toCol > fromCol ? 5 : 3;
            b[toRow][rookTo] = b[toRow][rookFrom];
            b[toRow][rookFrom] = 0;
        }
        if (promotes) b[toRow][toCol] = Character.isUpperCase(moving) ? 'Q' : 'q';

        boolean nextWhite = !p.whiteTurn;
        int nextFullmove = p.whiteTurn ? p.fullmoveNumber : p.fullmoveNumber + 1;
        return new Pos(b, nextWhite, rights, nextFullmove);
    }

    private static int updateCastleRights(int rights, char moving, int fromRow, int fromCol,
                                          char captured, int toRow, int toCol) {
        if (moving == 'K') rights &= ~(WK | WQ);
        if (moving == 'k') rights &= ~(BK | BQ);
        if (moving == 'R' && fromRow == 7 && fromCol == 0) rights &= ~WQ;
        if (moving == 'R' && fromRow == 7 && fromCol == 7) rights &= ~WK;
        if (moving == 'r' && fromRow == 0 && fromCol == 0) rights &= ~BQ;
        if (moving == 'r' && fromRow == 0 && fromCol == 7) rights &= ~BK;
        if (captured == 'R' && toRow == 7 && toCol == 0) rights &= ~WQ;
        if (captured == 'R' && toRow == 7 && toCol == 7) rights &= ~WK;
        if (captured == 'r' && toRow == 0 && toCol == 0) rights &= ~BQ;
        if (captured == 'r' && toRow == 0 && toCol == 7) rights &= ~BK;
        return rights;
    }

    // --- FEN / SAN ----------------------------------------------------

    /** En passant is always "-" (this rule set never tracks it, same simplification the UI
     *  board already makes) and the half-move clock is always "0" — rough, but per the UI
     *  board's own toFen, neither affects which move Stockfish prefers. */
    static String toFen(Pos p) {
        char[][] board = p.board;
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            int empty = 0;
            for (int c = 0; c < 8; c++) {
                char v = board[r][c];
                if (v == 0) { empty++; continue; }
                if (empty > 0) { sb.append(empty); empty = 0; }
                sb.append(v);
            }
            if (empty > 0) sb.append(empty);
            if (r < 7) sb.append('/');
        }
        sb.append(p.whiteTurn ? " w " : " b ");
        String castling = "" + ((p.castleRights & WK) != 0 ? "K" : "") + ((p.castleRights & WQ) != 0 ? "Q" : "")
                + ((p.castleRights & BK) != 0 ? "k" : "") + ((p.castleRights & BQ) != 0 ? "q" : "");
        sb.append(castling.isEmpty() ? "-" : castling);
        sb.append(" - 0 ").append(p.fullmoveNumber);
        return sb.toString();
    }

    private static String coordinate(int row, int col) { return "" + (char) ('a' + col) + (char) ('8' - row); }

    static String sanFor(Pos p, char moving, int fromRow, int fromCol, int toRow, int toCol,
                         boolean capture, boolean promotes) {
        char type = Character.toUpperCase(moving);
        if (type == 'P') {
            String dest = coordinate(toRow, toCol);
            String san = capture ? (char) ('a' + fromCol) + "x" + dest : dest;
            return promotes ? san + "=Q" : san;
        }
        String letter = type == 'N' ? "N" : type == 'B' ? "B" : type == 'R' ? "R" : type == 'Q' ? "Q" : "K";
        String disambig = "";
        if (type != 'K') {
            char[][] board = p.board;
            boolean sameFile = false, sameRank = false, ambiguous = false;
            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
                if (r == fromRow && c == fromCol) continue;
                if (board[r][c] != moving) continue;
                for (int[] mv : legalMoves(p, r, c)) {
                    if (mv[0] == toRow && mv[1] == toCol) {
                        ambiguous = true;
                        if (c == fromCol) sameFile = true;
                        if (r == fromRow) sameRank = true;
                    }
                }
            }
            if (ambiguous) {
                if (!sameFile) disambig = String.valueOf((char) ('a' + fromCol));
                else if (!sameRank) disambig = String.valueOf(8 - fromRow);
                else disambig = coordinate(fromRow, fromCol);
            }
        }
        return letter + disambig + (capture ? "x" : "") + coordinate(toRow, toCol);
    }

    // --- legality -------------------------------------------------------

    /** Every legal (fromRow,fromCol,toRow,toCol) for the side to move. */
    static List<int[]> allLegalMoves(Pos p) {
        List<int[]> out = new ArrayList<>();
        char[][] board = p.board;
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            char piece = board[r][c];
            if (piece == 0 || Character.isUpperCase(piece) != p.whiteTurn) continue;
            for (int[] mv : legalMoves(p, r, c)) out.add(new int[]{r, c, mv[0], mv[1]});
        }
        return out;
    }

    /** Geometrically legal moves for the piece at (row,col), filtered to drop any that would
     *  leave (or put) the mover's own king in check. */
    static List<int[]> legalMoves(Pos p, int row, int col) {
        char[][] board = p.board;
        char moving = board[row][col];
        if (moving == 0) return new ArrayList<>();
        boolean white = Character.isUpperCase(moving);
        List<int[]> out = new ArrayList<>();
        for (int[] mv : pseudoLegalMoves(p, row, col)) {
            char captured = board[mv[0]][mv[1]];
            board[mv[0]][mv[1]] = moving;
            board[row][col] = 0;
            boolean safe = !isKingInCheck(board, white);
            board[row][col] = moving;
            board[mv[0]][mv[1]] = captured;
            if (safe) out.add(mv);
        }
        return out;
    }

    static boolean isKingInCheck(char[][] board, boolean white) {
        char king = white ? 'K' : 'k';
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            if (board[r][c] == king) return isSquareAttacked(board, r, c, !white);
        }
        return false;
    }

    static boolean hasAnyLegalMove(Pos p, boolean white) {
        char[][] board = p.board;
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            char v = board[r][c];
            if (v != 0 && Character.isUpperCase(v) == white && !legalMoves(p, r, c).isEmpty()) return true;
        }
        return false;
    }

    static boolean isGameOver(Pos p) { return !hasAnyLegalMove(p, p.whiteTurn); }

    static boolean isCheckmate(Pos p) { return isGameOver(p) && isKingInCheck(p.board, p.whiteTurn); }

    /** How many of the side-to-move's legal moves deliver checkmate right now. */
    static int countImmediateMates(Pos p) {
        int mates = 0;
        for (int[] mv : allLegalMoves(p)) {
            if (isCheckmate(apply(p, mv[0], mv[1], mv[2], mv[3]))) mates++;
        }
        return mates;
    }

    private static final int[] VALUES = {0, 3, 3, 5, 9}; // N B R Q — pawn handled separately below

    static int materialCount(Pos p, boolean white) {
        char[][] board = p.board;
        int total = 0;
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            char v = board[r][c];
            if (v == 0 || Character.isUpperCase(v) != white) continue;
            total += pieceValue(v);
        }
        return total;
    }

    static int materialDiff(Pos p, boolean white) { return materialCount(p, white) - materialCount(p, !white); }

    private static int pieceValue(char v) {
        switch (Character.toUpperCase(v)) {
            case 'P': return 1;
            case 'N': case 'B': return 3;
            case 'R': return 5;
            case 'Q': return 9;
            default: return 0;
        }
    }

    /** A cheap, non-exhaustive "same position" key (board + side to move + castling rights,
     *  no move counters) — enough for the generator's repetition/duplicate-position guards,
     *  same role {@code board.epd()} plays in the reference implementation. */
    static String positionKey(Pos p) {
        StringBuilder sb = new StringBuilder(70);
        for (char[] row : p.board) for (char v : row) sb.append(v == 0 ? '.' : v);
        sb.append(p.whiteTurn ? 'w' : 'b').append(p.castleRights);
        return sb.toString();
    }

    /** Capture, pawn move, or a castling-rights change — a cheap stand-in for python-chess's
     *  {@code board.is_irreversible}, used only to decide when it's safe to stop worrying
     *  about a repeated position (see the reference generator's skip-until-irreversible). */
    static boolean isIrreversible(Pos before, int fromRow, int fromCol, int toRow, int toCol) {
        char moving = before.board[fromRow][fromCol];
        char captured = before.board[toRow][toCol];
        if (captured != 0) return true;
        if (Character.toUpperCase(moving) == 'P') return true;
        int afterRights = updateCastleRights(before.castleRights, moving, fromRow, fromCol, captured, toRow, toCol);
        return afterRights != before.castleRights;
    }

    // --- move generation internals (ported from ChessBoardView) ---------

    private static List<int[]> pseudoLegalMoves(Pos p, int row, int col) {
        char[][] board = p.board;
        List<int[]> out = new ArrayList<>();
        char piece = board[row][col];
        if (piece == 0) return out;
        boolean white = Character.isUpperCase(piece);
        int dir = white ? -1 : 1;
        switch (Character.toUpperCase(piece)) {
            case 'P':
                addIfEmpty(board, out, row + dir, col);
                if (row == (white ? 6 : 1) && board[row + dir][col] == 0) addIfEmpty(board, out, row + 2 * dir, col);
                addIfEnemy(board, out, row + dir, col - 1, white);
                addIfEnemy(board, out, row + dir, col + 1, white);
                break;
            case 'N':
                for (int[] d : new int[][]{{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}})
                    addIfReachable(board, out, row + d[0], col + d[1], white);
                break;
            case 'K':
                for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++)
                    if (dr != 0 || dc != 0) addIfReachable(board, out, row + dr, col + dc, white);
                if (canCastle(p, white, true)) out.add(new int[]{row, col + 2});
                if (canCastle(p, white, false)) out.add(new int[]{row, col - 2});
                break;
            case 'B': slide(board, out, row, col, white, new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}); break;
            case 'R': slide(board, out, row, col, white, new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}); break;
            case 'Q': slide(board, out, row, col, white, new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}}); break;
        }
        return out;
    }

    private static void addIfEmpty(char[][] b, List<int[]> out, int r, int c) { if (in(r, c) && b[r][c] == 0) out.add(new int[]{r, c}); }
    private static void addIfEnemy(char[][] b, List<int[]> out, int r, int c, boolean w) { if (in(r, c) && b[r][c] != 0 && Character.isUpperCase(b[r][c]) != w) out.add(new int[]{r, c}); }
    private static void addIfReachable(char[][] b, List<int[]> out, int r, int c, boolean w) { if (in(r, c) && (b[r][c] == 0 || Character.isUpperCase(b[r][c]) != w)) out.add(new int[]{r, c}); }

    private static boolean canCastle(Pos p, boolean white, boolean kingSide) {
        char[][] board = p.board;
        int row = white ? 7 : 0;
        int right = white ? (kingSide ? WK : WQ) : (kingSide ? BK : BQ);
        char king = white ? 'K' : 'k', rook = white ? 'R' : 'r';
        if ((p.castleRights & right) == 0 || board[row][4] != king) return false;
        int rookCol = kingSide ? 7 : 0;
        if (board[row][rookCol] != rook) return false;
        int from = kingSide ? 5 : 1, to = kingSide ? 6 : 3;
        for (int col = from; col <= to; col++) if (board[row][col] != 0) return false;
        int through = kingSide ? 5 : 3;
        int landing = kingSide ? 6 : 2;
        return !isSquareAttacked(board, row, 4, !white)
                && !isSquareAttacked(board, row, through, !white)
                && !isSquareAttacked(board, row, landing, !white);
    }

    private static boolean isSquareAttacked(char[][] board, int row, int col, boolean byWhite) {
        char pawn = byWhite ? 'P' : 'p';
        int pawnRow = row + (byWhite ? 1 : -1);
        for (int dc : new int[]{-1, 1}) if (in(pawnRow, col + dc) && board[pawnRow][col + dc] == pawn) return true;
        char knight = byWhite ? 'N' : 'n';
        for (int[] d : new int[][]{{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}})
            if (in(row + d[0], col + d[1]) && board[row + d[0]][col + d[1]] == knight) return true;
        char king = byWhite ? 'K' : 'k';
        for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++)
            if ((dr != 0 || dc != 0) && in(row + dr, col + dc) && board[row + dr][col + dc] == king) return true;
        if (attackedOnRay(board, row, col, byWhite, new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}, 'B')) return true;
        return attackedOnRay(board, row, col, byWhite, new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}, 'R');
    }

    private static boolean attackedOnRay(char[][] board, int row, int col, boolean byWhite, int[][] dirs, char specialist) {
        char a = byWhite ? specialist : Character.toLowerCase(specialist);
        char q = byWhite ? 'Q' : 'q';
        for (int[] d : dirs) {
            int r = row + d[0], c = col + d[1];
            while (in(r, c)) {
                char piece = board[r][c];
                if (piece != 0) {
                    if (piece == a || piece == q) return true;
                    break;
                }
                r += d[0]; c += d[1];
            }
        }
        return false;
    }

    private static void slide(char[][] b, List<int[]> out, int r, int c, boolean w, int[][] ds) {
        for (int[] d : ds) {
            int rr = r + d[0], cc = c + d[1];
            while (in(rr, cc)) {
                if (b[rr][cc] == 0) { out.add(new int[]{rr, cc}); }
                else { if (Character.isUpperCase(b[rr][cc]) != w) out.add(new int[]{rr, cc}); break; }
                rr += d[0]; cc += d[1];
            }
        }
    }

    private static boolean in(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    private static char[][] copy(char[][] source) {
        char[][] out = new char[8][8];
        for (int i = 0; i < 8; i++) System.arraycopy(source[i], 0, out[i], 0, 8);
        return out;
    }
}
