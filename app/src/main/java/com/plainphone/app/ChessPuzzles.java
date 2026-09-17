package com.plainphone.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Puzzles the generator has found — a separate store from {@link ChessLibrary} (imported
 *  games): different shape, different growth pattern, and keeps "games we imported" and
 *  "tactics we found in them" as distinct concerns. No solving screen yet; this is generation
 *  + storage only, ready for whatever browses it next. */
final class ChessPuzzles {

    private ChessPuzzles() {}

    static final class Puzzle {
        final String id, gameSrc, white, black, event, date;
        final String startFen;
        final int startPly;
        final List<String> solutionUci;
        final boolean winnerWhite;
        final String category; // "Mate" or "Advantage"
        final int cp; // final eval in the solution's terms; a very large sentinel for mate

        Puzzle(String id, String gameSrc, String white, String black, String event, String date,
               String startFen, int startPly, List<String> solutionUci, boolean winnerWhite,
               String category, int cp) {
            this.id = id; this.gameSrc = gameSrc; this.white = white; this.black = black;
            this.event = event; this.date = date;
            this.startFen = startFen; this.startPly = startPly;
            this.solutionUci = solutionUci; this.winnerWhite = winnerWhite;
            this.category = category; this.cp = cp;
        }

        /** Convenience constructor for generation call sites that don't need to name an id
         *  themselves — one is assigned here. */
        Puzzle(String gameSrc, String white, String black, String event, String date,
               String startFen, int startPly, List<String> solutionUci, boolean winnerWhite,
               String category, int cp) {
            this(UUID.randomUUID().toString(), gameSrc, white, black, event, date,
                    startFen, startPly, solutionUci, winnerWhite, category, cp);
        }
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), "chess_puzzles.jsonl");
    }

    static void appendPuzzle(Context context, Puzzle p) {
        try (FileOutputStream out = new FileOutputStream(file(context), true)) {
            JSONObject o = new JSONObject();
            o.put("id", p.id);
            o.put("gameSrc", p.gameSrc);
            o.put("white", p.white);
            o.put("black", p.black);
            o.put("event", p.event);
            o.put("date", p.date);
            o.put("startFen", p.startFen);
            o.put("startPly", p.startPly);
            o.put("solutionUci", new JSONArray(p.solutionUci));
            o.put("winnerWhite", p.winnerWhite);
            o.put("category", p.category);
            o.put("cp", p.cp);
            out.write((o + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JSONException ignored) { }
    }

    private static Puzzle parseLine(String line) {
        try {
            JSONObject o = new JSONObject(line);
            List<String> sol = new ArrayList<>();
            JSONArray arr = o.optJSONArray("solutionUci");
            if (arr != null) for (int i = 0; i < arr.length(); i++) sol.add(arr.getString(i));
            String id = o.optString("id", "");
            if (id.isEmpty()) id = UUID.randomUUID().toString(); // pre-id puzzle, synthesize one
            return new Puzzle(
                    id,
                    o.optString("gameSrc", "Imported PGN"),
                    o.optString("white", "?"),
                    o.optString("black", "?"),
                    o.optString("event", "Game"),
                    o.optString("date", "????.??.??"),
                    o.optString("startFen", ""),
                    o.optInt("startPly", 0),
                    sol,
                    o.optBoolean("winnerWhite", true),
                    o.optString("category", "Advantage"),
                    o.optInt("cp", 0));
        } catch (JSONException e) {
            return null;
        }
    }

    // Same reasoning as ChessLibrary's own cache: loadAll gets called repeatedly (every
    // Puzzles-home refresh, every Play/Generate-from dialog open, once per source inside
    // those for countBySource) with nothing changed on disk between most of those calls.
    private static List<Puzzle> cache;
    private static long cacheMtime = -1;

    static synchronized List<Puzzle> loadAll(Context context) {
        File f = file(context);
        long mtime = f.exists() ? f.lastModified() : -1;
        if (cache != null && mtime == cacheMtime) return cache;
        List<Puzzle> out = new ArrayList<>();
        if (f.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    Puzzle p = parseLine(line);
                    if (p != null) out.add(p);
                }
            } catch (IOException ignored) { }
            Collections.reverse(out);
        }
        cache = out;
        cacheMtime = mtime;
        return out;
    }

    /** Puzzles generated from games in the given sources, newest first. {@code sourceLabels}
     *  {@code null} means "all sources" (same as {@link #loadAll}). */
    static List<Puzzle> loadBySources(Context context, Set<String> sourceLabels) {
        if (sourceLabels == null) return loadAll(context);
        List<Puzzle> out = new ArrayList<>();
        for (Puzzle p : loadAll(context)) if (sourceLabels.contains(p.gameSrc)) out.add(p);
        return out;
    }

    static int countBySource(Context context, String sourceLabel) {
        int n = 0;
        for (Puzzle p : loadAll(context)) if (p.gameSrc.equals(sourceLabel)) n++;
        return n;
    }

    static int count(Context context) {
        return loadAll(context).size();
    }

    private static boolean rewrite(Context context, List<String> keptLines) {
        File tmp = new File(context.getFilesDir(), "chess_puzzles.jsonl.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp, false)) {
            StringBuilder sb = new StringBuilder();
            for (String line : keptLines) sb.append(line).append('\n');
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) { return false; }
        return tmp.renameTo(file(context));
    }

    /** Deletes the given puzzles by id. Rewrites the whole file (read all, filter, atomic
     *  replace) — fine at personal-library scale, same trade-off as
     *  {@link ChessLibrary#deleteSource}. */
    static boolean deleteByIds(Context context, Set<String> ids) {
        File f = file(context);
        if (!f.exists() || ids.isEmpty()) return false;
        List<String> keep = new ArrayList<>();
        boolean removedAny = false;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                Puzzle p = parseLine(line);
                if (p != null && ids.contains(p.id)) { removedAny = true; continue; }
                keep.add(line);
            }
        } catch (IOException ignored) { return false; }
        return removedAny && rewrite(context, keep);
    }

    /** Cascade-delete: every puzzle generated from {@code sourceLabel}'s games — called when
     *  that PGN source is removed from the library. */
    static boolean deleteBySource(Context context, String sourceLabel) {
        File f = file(context);
        if (!f.exists()) return false;
        List<String> keep = new ArrayList<>();
        boolean removedAny = false;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                Puzzle p = parseLine(line);
                if (p != null && p.gameSrc.equals(sourceLabel)) { removedAny = true; continue; }
                keep.add(line);
            }
        } catch (IOException ignored) { return false; }
        return removedAny && rewrite(context, keep);
    }
}
