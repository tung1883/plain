package com.plainphone.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
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
    private static final Pattern MOVE_OR_COMMENT = Pattern.compile("\\{[^}]*\\}|\\S+");

    static final class Game {
        final Map<String, String> tags;
        final List<String> sans;
        /** Same index as {@link #sans}; {@code ""} where that move has no comment. A
         *  comment before any move (a game-opening remark) is dropped — there's no move
         *  yet to attach it to. */
        final List<String> comments;

        Game(Map<String, String> tags, String movetext) {
            this.tags = tags;
            Extracted e = extractSansAndComments(movetext);
            this.sans = e.sans;
            this.comments = e.comments;
        }
        /** A tag's value, or {@code fallback} if it's missing or PGN's own "unknown" mark. */
        String tag(String key, String fallback) {
            String v = tags.get(key);
            return (v == null || v.equals("?") || v.isEmpty()) ? fallback : v;
        }
    }

    /** Return {@code false} to stop parsing early (a cancelled import job); {@code true} to
     *  keep going. */
    interface GameCallback { boolean onGame(Game game); }

    /** How many games {@code source} contains — same boundary rule as {@link #parse}, but
     *  without building a single {@link Game} (no tag map, no SAN extraction): just enough
     *  bookkeeping to know when one game ends and the next begins. A cheap first pass so an
     *  import job can show real "N of total" progress instead of only a running count with no
     *  denominator. */
    static int countGames(Reader source) throws IOException {
        BufferedReader r = source instanceof BufferedReader ? (BufferedReader) source : new BufferedReader(source);
        int count = 0;
        boolean hasTags = false;
        boolean inMovetext = false;
        String line;
        while ((line = r.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                if (inMovetext) {
                    count++;
                    hasTags = false;
                    inMovetext = false;
                }
                hasTags = true;
                continue;
            }
            if (trimmed.isEmpty()) continue;
            inMovetext = true;
        }
        if (hasTags || inMovetext) count++;
        return count;
    }

    /** Streams {@code text} game by game, handing each one to {@code callback} as soon as it's
     *  complete instead of collecting them all into a list first — a real PGN collection (a
     *  player's whole career, an opening database) is routinely thousands of games, and every
     *  one carries its own full move list; materializing all of them at once is what blew the
     *  heap on a 7000+-game import (see {@link ChessLibrary}). A game is a run of {@code [Tag
     *  "value"]} lines followed by movetext; hitting a new tag line once movetext has already
     *  started closes the previous game off (PGN files don't reliably use blank lines as
     *  separators, but tags never appear inside movetext, so this boundary is the same one real
     *  PGN readers use). */
    static void parse(Reader source, GameCallback callback) throws IOException {
        BufferedReader r = source instanceof BufferedReader ? (BufferedReader) source : new BufferedReader(source);
        Map<String, String> tags = new LinkedHashMap<>();
        StringBuilder movetext = new StringBuilder();
        boolean inMovetext = false;
        String line;
        while ((line = r.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                if (inMovetext) {
                    if (!callback.onGame(new Game(tags, movetext.toString()))) return;
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
        if (!tags.isEmpty() || movetext.length() > 0) callback.onGame(new Game(tags, movetext.toString()));
    }

    private static final class Extracted {
        List<String> sans;
        List<String> comments;
    }

    /** Movetext to a flat SAN move list plus a parallel per-move comment list: strips
     *  (nested) variations and NAGs first — variations are dropped rather than offered as
     *  alternates, this reads a game's mainline only — then walks what's left token by
     *  token, attaching each {@code {...}} comment to the move immediately before it
     *  (standard PGN ordering: "e4 {good move} e5"). Move numbers and the game-termination
     *  marker are stripped from move tokens as they're read. */
    private static Extracted extractSansAndComments(String movetext) {
        String s = movetext;
        String prev;
        do { prev = s; s = s.replaceAll("\\([^()]*\\)", " "); } while (!s.equals(prev)); // variations
        s = s.replaceAll("\\$\\d+", " "); // NAGs

        List<String> sans = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        Matcher tokenizer = MOVE_OR_COMMENT.matcher(s);
        while (tokenizer.find()) {
            String tok = tokenizer.group();
            if (tok.startsWith("{")) {
                String text = tok.substring(1, tok.length() - 1).trim();
                if (!comments.isEmpty() && comments.get(comments.size() - 1).isEmpty()) {
                    comments.set(comments.size() - 1, text);
                }
                continue;
            }
            String move = tok.replaceFirst("^\\d+\\.(\\.\\.)?", "");
            if (move.isEmpty()) continue;
            if (move.equals("1-0") || move.equals("0-1") || move.equals("1/2-1/2") || move.equals("*")) continue;
            sans.add(move);
            comments.add("");
        }
        Extracted e = new Extracted();
        e.sans = sans;
        e.comments = comments;
        return e;
    }

    /** Writes one game back out as PGN text: the Seven Tag Roster (defaulted to "?" for
     *  anything not supplied, "*" for a still-open result — real PGN readers expect all
     *  seven present even when unknown) followed by numbered movetext and the result. */
    static String write(Map<String, String> tags, List<String> sans, String result) {
        return write(tags, sans, null, result);
    }

    /** Same as {@link #write(Map, List, String)}, plus a parallel per-move comment list
     *  (same index as {@code sans}, {@code null}/{@code ""} entries skipped) emitted as
     *  standard {@code {comment}} PGN syntax right after the move it belongs to. */
    static String write(Map<String, String> tags, List<String> sans, List<String> comments, String result) {
        StringBuilder sb = new StringBuilder();
        String[] roster = {"Event", "Site", "Date", "Round", "White", "Black", "Result"};
        for (String key : roster) {
            String value = key.equals("Result") ? result : tags.getOrDefault(key, "?");
            sb.append('[').append(key).append(" \"").append(value).append("\"]\n");
        }
        sb.append('\n');
        int col = 0;
        for (int i = 0; i < sans.size(); i++) {
            String comment = (comments != null && i < comments.size()) ? comments.get(i) : null;
            String token = (i % 2 == 0) ? ((i / 2) + 1) + ". " + sans.get(i) : sans.get(i);
            if (comment != null && !comment.isEmpty()) token = token + " {" + comment + "}";
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
