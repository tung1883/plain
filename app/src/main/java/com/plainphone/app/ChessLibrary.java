package com.plainphone.app;

import android.content.Context;

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

/** Every game ever imported through "Import PGN", kept around so it can be browsed later —
 *  unlike {@link Pgn}'s one-shot "choose a game" picker, which forgets everything the moment
 *  a game is loaded or the dialog is cancelled. One JSON-Lines file, appended to at import
 *  time; a game's SAN move list is stored alongside its tags so re-opening a game never needs
 *  to re-read or re-parse the original PGN file. */
final class ChessLibrary {

    private ChessLibrary() {}

    static final class Entry {
        final String src, white, black, event, date, result, sansJoined;
        Entry(String src, String white, String black, String event, String date, String result, String sansJoined) {
            this.src = src; this.white = white; this.black = black;
            this.event = event; this.date = date; this.result = result;
            this.sansJoined = sansJoined;
        }
        List<String> sans() {
            if (sansJoined.isEmpty()) return new ArrayList<>();
            List<String> out = new ArrayList<>();
            for (String s : sansJoined.split(" ")) if (!s.isEmpty()) out.add(s);
            return out;
        }
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), "chess_library.jsonl");
    }

    /** Appends one line per game. Safe to call off the UI thread (does its own file I/O). */
    static void appendGames(Context context, String sourceLabel, List<Pgn.Game> games) {
        try (FileOutputStream out = new FileOutputStream(file(context), true)) {
            StringBuilder sb = new StringBuilder();
            for (Pgn.Game game : games) {
                JSONObject o = new JSONObject();
                try {
                    o.put("src", sourceLabel);
                    o.put("white", game.tag("White", "?"));
                    o.put("black", game.tag("Black", "?"));
                    o.put("event", game.tag("Event", "Game"));
                    o.put("date", game.tag("Date", "????.??.??"));
                    o.put("result", game.tag("Result", "*"));
                    o.put("sans", String.join(" ", game.sans));
                } catch (JSONException ignored) { continue; }
                sb.append(o).append('\n');
            }
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) { }
    }

    /** Reads every game back, newest import first. Runs entirely on the calling thread —
     *  callers do this off the UI thread, same as {@link Pgn#parse}. */
    static List<Entry> loadAll(Context context) {
        List<Entry> out = new ArrayList<>();
        File f = file(context);
        if (!f.exists()) return out;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                try {
                    JSONObject o = new JSONObject(line);
                    out.add(new Entry(
                            o.optString("src", "Imported PGN"),
                            o.optString("white", "?"),
                            o.optString("black", "?"),
                            o.optString("event", "Game"),
                            o.optString("date", "????.??.??"),
                            o.optString("result", "*"),
                            o.optString("sans", "")));
                } catch (JSONException ignored) { }
            }
        } catch (IOException ignored) { }
        Collections.reverse(out);
        return out;
    }

    interface LineCallback {
        /** Called once per game, in the file's own append (oldest-first) order.
         *  {@code lineNumber} is 1-based — how many lines (including this one) have now been
         *  read, i.e. the cursor value to resume from next time. Return {@code false} to stop
         *  scanning early (e.g. the job was cancelled). */
        boolean onEntry(int lineNumber, Entry entry);
    }

    /** Streams the library forward from a given line offset, without materializing the whole
     *  file — {@link #loadAll} is fine for the "Imported games" browsing screen at tens of
     *  thousands of rows, but the puzzle generator may run for hours over the same file and
     *  shouldn't hold it all resident. Oldest-first (append order), the opposite of
     *  {@link #loadAll}'s newest-first — a stable order for a resumable line-count cursor,
     *  since new imports only ever append past the end. */
    static void scanFrom(Context context, int skipLines, LineCallback callback) {
        File f = file(context);
        if (!f.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            int n = 0;
            while ((line = r.readLine()) != null) {
                n++;
                if (n <= skipLines || line.isEmpty()) continue;
                try {
                    JSONObject o = new JSONObject(line);
                    Entry entry = new Entry(
                            o.optString("src", "Imported PGN"),
                            o.optString("white", "?"),
                            o.optString("black", "?"),
                            o.optString("event", "Game"),
                            o.optString("date", "????.??.??"),
                            o.optString("result", "*"),
                            o.optString("sans", ""));
                    if (!callback.onEntry(n, entry)) return;
                } catch (JSONException ignored) { }
            }
        } catch (IOException ignored) { }
    }

    /** Total games recorded, for progress display ("scanned N of &lt;lineCount&gt;"). */
    static int lineCount(Context context) {
        File f = file(context);
        if (!f.exists()) return 0;
        int n = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            while (r.readLine() != null) n++;
        } catch (IOException ignored) { }
        return n;
    }
}
