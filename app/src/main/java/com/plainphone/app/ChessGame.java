package com.plainphone.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A deliberately small, offline PGN main-line reader. Positions are kept after each ply. */
final class ChessGame {
    final Map<String, String> headers = new LinkedHashMap<>();
    final List<String> moves = new ArrayList<>();
    final List<char[]> positions = new ArrayList<>();
    final List<String> castleRights = new ArrayList<>();
    private boolean whiteToMove = true;

    static ChessGame fromPgn(String pgn) {
        ChessGame game = new ChessGame();
        game.positions.add(start());
        game.castleRights.add("KQkq");
        if (pgn == null) return game;
        Matcher tags = Pattern.compile("(?m)^\\s*\\[([A-Za-z0-9_]+)\\s+\\\"(.*?)\\\"\\s*]\\s*$").matcher(pgn);
        while (tags.find()) game.headers.put(tags.group(1), tags.group(2));
        String text = tags.replaceAll(" ");
        text = text.replaceAll("(?s)\\{.*?}", " ").replaceAll("(?m);.*$", " ");
        StringBuilder main = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') { depth++; continue; }
            if (c == ')') { if (depth > 0) depth--; continue; }
            if (depth == 0) main.append(c);
        }
        for (String raw : main.toString().trim().split("\\s+")) {
            String san = raw.replaceFirst("^\\d+\\.(\\.\\.)?", "");
            if (san.isEmpty() || san.startsWith("$") || san.matches("1-0|0-1|1/2-1/2|\\*")) continue;
            if (!game.play(san)) break; // preserve the valid prefix of a malformed game
        }
        return game;
    }

    static ChessGame newGame() {
        ChessGame game = new ChessGame();
        game.positions.add(start());
        game.castleRights.add("KQkq");
        return game;
    }

    String title() {
        String white = headers.get("White"), black = headers.get("Black");
        if (white != null || black != null) return (white == null ? "White" : white) + " — " +
                (black == null ? "Black" : black);
        return moves.isEmpty() ? "Untitled game" : "Chess game";
    }

    String subtitle() {
        String event = headers.get("Event");
        String result = headers.get("Result");
        return event == null ? (result == null ? moves.size() + " moves" : result) :
                event + (result == null ? "" : " · " + result);
    }

    char[] positionAt(int ply) { return positions.get(Math.max(0, Math.min(ply, positions.size() - 1))).clone(); }
    int plies() { return moves.size(); }
    boolean whiteAt(int ply) { return (ply & 1) == 0; }

    String fenAt(int ply) {
        char[] b = positionAt(ply);
        StringBuilder out = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                char p = b[idx(file, rank)];
                if (p == '.') empty++; else {
                    if (empty > 0) { out.append(empty); empty = 0; }
                    out.append(p);
                }
            }
            if (empty > 0) out.append(empty);
            if (rank > 0) out.append('/');
        }
        return out.append(whiteAt(ply) ? " w " : " b ").append(castleRights.get(Math.max(0, Math.min(ply, castleRights.size() - 1)))).append(" - 0 ")
                .append(1 + ply / 2).toString();
    }

    /** Play a board gesture at the end of the game. Returns false for an illegal basic move. */
    boolean move(int from, int to) {
        if (positions.isEmpty() || from < 0 || from >= 64 || to < 0 || to >= 64) return false;
        char[] b = positions.get(positions.size() - 1).clone();
        char moving = b[from];
        if (moving == '.' || Character.isUpperCase(moving) != whiteToMove) return false;
        int f = from % 8, r = from / 8, tf = to % 8, tr = to / 8;
        char kind = Character.toUpperCase(moving);
        if (kind == 'K' && Math.abs(tf - f) == 2 && r == tr) {
            castle(b, tf > f); moves.add(tf > f ? "O-O" : "O-O-O"); positions.add(b);
            castleRights.add(withoutKing(castleRights.get(castleRights.size()-1), whiteToMove)); whiteToMove=!whiteToMove; return true;
        }
        if (!canMove(b, kind, f, r, tf, tr, whiteToMove)) return false;
        char captured = b[to];
        if (kind == 'P' && tf != f && captured == '.') return false; // no hidden en-passant state in board mode
        b[to] = (kind == 'P' && (tr == 0 || tr == 7)) ? (whiteToMove ? 'Q' : 'q') : moving;
        b[from] = '.';
        String san = kind == 'P' ? "" : String.valueOf(kind);
        if (captured != '.') san += "x";
        san += (char)('a' + tf) + String.valueOf(tr + 1);
        if (kind == 'P' && (tr == 0 || tr == 7)) san += "=Q";
        moves.add(san); positions.add(b); castleRights.add(updateRights(castleRights.get(castleRights.size()-1), moving, from, captured, to)); whiteToMove=!whiteToMove;
        return true;
    }

    String toPgn() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String,String> h : headers.entrySet()) out.append('[').append(h.getKey()).append(" \"").append(h.getValue().replace("\"", "'")).append("\"]\n");
        if (!headers.isEmpty()) out.append('\n');
        for (int i = 0; i < moves.size(); i++) { if ((i & 1) == 0) out.append(i / 2 + 1).append(". "); out.append(moves.get(i)).append(' '); }
        String result = headers.get("Result"); out.append(result == null ? "*" : result); return out.toString().trim() + "\n";
    }

    private boolean play(String raw) {
        String san = raw.replaceAll("[+#?!]+$", "").replaceAll("e\\.p\\.?", "");
        char[] b = positions.get(positions.size() - 1).clone();
        if (san.equals("O-O") || san.equals("0-0")) {
            castle(b, true); moves.add(raw); positions.add(b); castleRights.add(withoutKing(castleRights.get(castleRights.size()-1), whiteToMove)); whiteToMove = !whiteToMove; return true;
        }
        if (san.equals("O-O-O") || san.equals("0-0-0")) {
            castle(b, false); moves.add(raw); positions.add(b); castleRights.add(withoutKing(castleRights.get(castleRights.size()-1), whiteToMove)); whiteToMove = !whiteToMove; return true;
        }
        Matcher dest = Pattern.compile("([a-h])([1-8])(?:=([QRBN]))?$").matcher(san);
        if (!dest.find()) return false;
        int toFile = dest.group(1).charAt(0) - 'a', toRank = dest.group(2).charAt(0) - '1';
        char promotion = dest.group(3) == null ? 0 : dest.group(3).charAt(0);
        String pre = san.substring(0, dest.start()).replace("x", "");
        char wanted = 'P';
        if (!pre.isEmpty() && "KQRBN".indexOf(pre.charAt(0)) >= 0) { wanted = pre.charAt(0); pre = pre.substring(1); }
        char piece = whiteToMove ? wanted : Character.toLowerCase(wanted);
        int from = -1;
        for (int r = 0; r < 8 && from < 0; r++) for (int f = 0; f < 8; f++) {
            int candidate = idx(f, r);
            if (b[candidate] != piece || !matchesHint(pre, f, r) || !canMove(b, wanted, f, r, toFile, toRank, whiteToMove)) continue;
            from = candidate;
        }
        if (from < 0) return false;
        // En-passant's only special board effect; FEN's transient target is intentionally omitted.
        char moving = b[from];
        char captured = b[idx(toFile, toRank)];
        if (wanted == 'P' && toFile != from % 8 && captured == '.') {
            b[idx(toFile, from / 8)] = '.';
        }
        b[idx(toFile, toRank)] = promotion == 0 ? b[from] : (whiteToMove ? promotion : Character.toLowerCase(promotion));
        b[from] = '.';
        moves.add(raw); positions.add(b); castleRights.add(updateRights(castleRights.get(castleRights.size()-1), moving, from, captured, idx(toFile,toRank))); whiteToMove = !whiteToMove;
        return true;
    }

    private boolean matchesHint(String hint, int file, int rank) {
        if (hint.isEmpty()) return true;
        if (hint.length() == 1) return hint.charAt(0) >= 'a' && hint.charAt(0) <= 'h'
                ? file == hint.charAt(0) - 'a' : rank == hint.charAt(0) - '1';
        return file == hint.charAt(0) - 'a' && rank == hint.charAt(1) - '1';
    }

    private static boolean canMove(char[] b, char kind, int f, int r, int tf, int tr, boolean white) {
        char target = b[idx(tf, tr)];
        if (target != '.' && Character.isUpperCase(target) == white) return false;
        int dx = tf - f, dy = tr - r, adx = Math.abs(dx), ady = Math.abs(dy);
        if (kind == 'P') {
            int dir = white ? 1 : -1, home = white ? 1 : 6;
            if (dx == 0 && target == '.' && dy == dir) return true;
            if (dx == 0 && target == '.' && dy == 2 * dir && r == home && b[idx(f, r + dir)] == '.') return true;
            return adx == 1 && dy == dir; // capture or en-passant
        }
        if (kind == 'N') return adx * ady == 2;
        if (kind == 'K') return Math.max(adx, ady) == 1;
        boolean diagonal = adx == ady && adx > 0, straight = (dx == 0) != (dy == 0);
        if (kind == 'B' && !diagonal || kind == 'R' && !straight || kind == 'Q' && !diagonal && !straight) return false;
        int sx = Integer.compare(dx, 0), sy = Integer.compare(dy, 0);
        for (int x = f + sx, y = r + sy; x != tf || y != tr; x += sx, y += sy) if (b[idx(x, y)] != '.') return false;
        return true;
    }

    private void castle(char[] b, boolean kingSide) {
        int rank = whiteToMove ? 0 : 7; char king = whiteToMove ? 'K' : 'k', rook = whiteToMove ? 'R' : 'r';
        b[idx(4, rank)] = '.'; b[idx(kingSide ? 6 : 2, rank)] = king;
        b[idx(kingSide ? 7 : 0, rank)] = '.'; b[idx(kingSide ? 5 : 3, rank)] = rook;
    }

    private static String withoutKing(String rights, boolean white) {
        return rights.replace(white ? "K" : "k", "").replace(white ? "Q" : "q", "");
    }

    private static String updateRights(String rights, char moved, int from, char captured, int to) {
        if (moved == 'K') rights = withoutKing(rights, true);
        if (moved == 'k') rights = withoutKing(rights, false);
        if (from == idx(0, 0) || to == idx(0, 0) && captured == 'R') rights = rights.replace("Q", "");
        if (from == idx(7, 0) || to == idx(7, 0) && captured == 'R') rights = rights.replace("K", "");
        if (from == idx(0, 7) || to == idx(0, 7) && captured == 'r') rights = rights.replace("q", "");
        if (from == idx(7, 7) || to == idx(7, 7) && captured == 'r') rights = rights.replace("k", "");
        return rights.isEmpty() ? "-" : rights;
    }

    private static int idx(int file, int rank) { return rank * 8 + file; }
    private static char[] start() {
        char[] b = new char[64]; java.util.Arrays.fill(b, '.');
        String back = "RNBQKBNR";
        for (int f = 0; f < 8; f++) { b[idx(f, 0)] = back.charAt(f); b[idx(f, 1)] = 'P'; b[idx(f, 6)] = 'p'; b[idx(f, 7)] = Character.toLowerCase(back.charAt(f)); }
        return b;
    }
}
