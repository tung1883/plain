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

/** Puzzles the generator has found — a separate store from {@link ChessLibrary} (imported
 *  games): different shape, different growth pattern, and keeps "games we imported" and
 *  "tactics we found in them" as distinct concerns. No solving screen yet; this is generation
 *  + storage only, ready for whatever browses it next. */
final class ChessPuzzles {

    private ChessPuzzles() {}

    static final class Puzzle {
        final String gameSrc, white, black, event, date;
        final String startFen;
        final int startPly;
        final List<String> solutionUci;
        final boolean winnerWhite;
        final String category; // "Mate" or "Advantage"
        final int cp; // final eval in the solution's terms; a very large sentinel for mate

        Puzzle(String gameSrc, String white, String black, String event, String date,
               String startFen, int startPly, List<String> solutionUci, boolean winnerWhite,
               String category, int cp) {
            this.gameSrc = gameSrc; this.white = white; this.black = black;
            this.event = event; this.date = date;
            this.startFen = startFen; this.startPly = startPly;
            this.solutionUci = solutionUci; this.winnerWhite = winnerWhite;
            this.category = category; this.cp = cp;
        }
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), "chess_puzzles.jsonl");
    }

    static void appendPuzzle(Context context, Puzzle p) {
        try (FileOutputStream out = new FileOutputStream(file(context), true)) {
            JSONObject o = new JSONObject();
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

    static List<Puzzle> loadAll(Context context) {
        List<Puzzle> out = new ArrayList<>();
        File f = file(context);
        if (!f.exists()) return out;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                try {
                    JSONObject o = new JSONObject(line);
                    List<String> sol = new ArrayList<>();
                    JSONArray arr = o.optJSONArray("solutionUci");
                    if (arr != null) for (int i = 0; i < arr.length(); i++) sol.add(arr.getString(i));
                    out.add(new Puzzle(
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
                            o.optInt("cp", 0)));
                } catch (JSONException ignored) { }
            }
        } catch (IOException ignored) { }
        Collections.reverse(out);
        return out;
    }

    static int count(Context context) {
        File f = file(context);
        if (!f.exists()) return 0;
        int n = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            while (r.readLine() != null) n++;
        } catch (IOException ignored) { }
        return n;
    }
}
