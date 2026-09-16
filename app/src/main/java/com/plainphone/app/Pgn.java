package com.plainphone.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A minimal PGN reader: splits a file into its games (a PGN file can hold many, back to
 *  back) and, per game, its tag pairs and movetext SAN tokens — everything
 *  {@link ChessBoardView#loadSanMoves} needs to actually play a game back, plus enough of
 *  the tags to show a games list ("choose a game" import picker) without touching the
 *  board yet. */
final class Pgn {
    private Pgn() {}

    private static final Pattern TAG_LINE = Pattern.compile("^\\[(\\w+)\\s+\"(.*)\"\\]$");

    static final class Game {
        final Map<String, String> tags;
        final List<String> sans;
        Game(Map<String, String> tags, String movetext) {
            this.tags = tags;
            this.sans = extractSans(movetext);
        }
        /** A tag's value, or {@code fallback} if it's missing or PGN's own "unknown" mark. */
        String tag(String key, String fallback) {
            String v = tags.get(key);
            return (v == null || v.equals("?") || v.isEmpty()) ? fallback : v;
        }
    }

    /** Splits {@code text} into every game it contains. A game is a run of {@code [Tag
     *  "value"]} lines followed by movetext; hitting a new tag line once movetext has
     *  already started closes the previous game off (PGN files don't reliably use blank
     *  lines as separators, but tags never appear inside movetext, so this boundary is
     *  the same one real PGN readers use). */
    static List<Game> parse(String text) {
        List<Game> games = new ArrayList<>();
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        Map<String, String> tags = new LinkedHashMap<>();
        StringBuilder movetext = new StringBuilder();
        boolean inMovetext = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                if (inMovetext) {
                    games.add(new Game(tags, movetext.toString()));
                    tags = new LinkedHashMap<>();
                    movetext = new StringBuilder();
                    inMovetext = false;
                }
                Matcher m = TAG_LINE.matcher(trimmed);
                if (m.matches()) tags.put(m.group(1), m.group(2));
                continue;
            }
            if (trimmed.isEmpty()) continue;
            inMovetext = true;
            movetext.append(' ').append(trimmed);
        }
        if (!tags.isEmpty() || movetext.length() > 0) games.add(new Game(tags, movetext.toString()));
        return games;
    }

    /** Movetext to a flat SAN move list: strips comments, (nested) variations, NAGs, move
     *  numbers and the game-termination marker, leaving just the moves in order. Variations
     *  are dropped rather than offered as alternates — this reads a game's mainline only. */
    private static List<String> extractSans(String movetext) {
        String s = movetext;
        s = s.replaceAll("\\{[^}]*\\}", " "); // comments
        String prev;
        do { prev = s; s = s.replaceAll("\\([^()]*\\)", " "); } while (!s.equals(prev)); // variations
        s = s.replaceAll("\\$\\d+", " "); // NAGs

        List<String> out = new ArrayList<>();
        for (String tok : s.trim().split("\\s+")) {
            if (tok.isEmpty()) continue;
            // Move numbers usually arrive glued to the move ("12.e4", "12...Nf6") since the
            // comment/variation stripping above can leave the dot right where it was.
            tok = tok.replaceFirst("^\\d+\\.(\\.\\.)?", "");
            if (tok.isEmpty()) continue;
            if (tok.equals("1-0") || tok.equals("0-1") || tok.equals("1/2-1/2") || tok.equals("*")) continue;
            out.add(tok);
        }
        return out;
    }

    /** Writes one game back out as PGN text: the Seven Tag Roster (defaulted to "?" for
     *  anything not supplied, "*" for a still-open result — real PGN readers expect all
     *  seven present even when unknown) followed by numbered movetext and the result. */
    static String write(Map<String, String> tags, List<String> sans, String result) {
        StringBuilder sb = new StringBuilder();
        String[] roster = {"Event", "Site", "Date", "Round", "White", "Black", "Result"};
        for (String key : roster) {
            String value = key.equals("Result") ? result : tags.getOrDefault(key, "?");
            sb.append('[').append(key).append(" \"").append(value).append("\"]\n");
        }
        sb.append('\n');
        int col = 0;
        for (int i = 0; i < sans.size(); i++) {
            String token = (i % 2 == 0) ? ((i / 2) + 1) + ". " + sans.get(i) : sans.get(i);
            if (col > 0 && col + token.length() + 1 > 80) { sb.append('\n'); col = 0; }
            else if (col > 0) { sb.append(' '); col++; }
            sb.append(token);
            col += token.length();
        }
        if (col > 0) sb.append(' ');
        sb.append(result).append('\n');
        return sb.toString();
    }
}
