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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Every game ever imported through "Import PGN", kept around so it can be browsed later —
 *  unlike {@link Pgn}'s one-shot "choose a game" picker, which forgets everything the moment
 *  a game is loaded or the dialog is cancelled. One JSON-Lines file, appended to at import
 *  time; a game's SAN move list is stored alongside its tags so re-opening a game never needs
 *  to re-read or re-parse the original PGN file. */
final class ChessLibrary {

    private ChessLibrary() {}

    static final class Entry {
        final String id, src, white, black, event, round, eco, date, result, sansJoined;
        final long importedAt;

        Entry(String id, String src, String white, String black, String event, String round,
              String eco, String date, String result, String sansJoined, long importedAt) {
            this.id = id; this.src = src; this.white = white; this.black = black;
            this.event = event; this.round = round; this.eco = eco; this.date = date;
            this.result = result; this.sansJoined = sansJoined; this.importedAt = importedAt;
        }

        List<String> sans() {
            if (sansJoined.isEmpty()) return new ArrayList<>();
            List<String> out = new ArrayList<>();
            for (String s : sansJoined.split(" ")) if (!s.isEmpty()) out.add(s);
            return out;
        }
    }

    /** One imported PGN file's summary, for the "PGN files" management screen and pickers. */
    static final class SourceSummary {
        final String label;
        final int gameCount;
        final long importedAt;

        SourceSummary(String label, int gameCount, long importedAt) {
            this.label = label; this.gameCount = gameCount; this.importedAt = importedAt;
        }
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), "chess_library.jsonl");
    }

    /** Appends one line per game. Safe to call off the UI thread (does its own file I/O). */
    static void appendGames(Context context, String sourceLabel, List<Pgn.Game> games) {
        try (FileOutputStream out = new FileOutputStream(file(context), true)) {
            long now = System.currentTimeMillis();
            StringBuilder sb = new StringBuilder();
            for (Pgn.Game game : games) {
                JSONObject o = new JSONObject();
                try {
                    o.put("id", UUID.randomUUID().toString());
                    o.put("src", sourceLabel);
                    o.put("white", game.tag("White", "?"));
                    o.put("black", game.tag("Black", "?"));
                    o.put("event", game.tag("Event", "Game"));
                    o.put("round", game.tag("Round", ""));
                    o.put("eco", game.tag("ECO", ""));
                    o.put("date", game.tag("Date", "????.??.??"));
                    o.put("result", game.tag("Result", "*"));
                    o.put("sans", String.join(" ", game.sans));
                    o.put("importedAt", now);
                } catch (JSONException ignored) { continue; }
                sb.append(o).append('\n');
            }
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) { }
    }

    private static Entry parseLine(String line) {
        try {
            JSONObject o = new JSONObject(line);
            return new Entry(
                    o.optString("id", ""),
                    o.optString("src", "Imported PGN"),
                    o.optString("white", "?"),
                    o.optString("black", "?"),
                    o.optString("event", "Game"),
                    o.optString("round", ""),
                    o.optString("eco", ""),
                    o.optString("date", "????.??.??"),
                    o.optString("result", "*"),
                    o.optString("sans", ""),
                    o.optLong("importedAt", 0L));
        } catch (JSONException e) {
            return null;
        }
    }

    /** Reads every game back, newest import first. Runs entirely on the calling thread —
     *  callers do this off the UI thread, same as {@link Pgn#parse}. */
    // In-memory cache of the last full parse, keyed by the file's own mtime — a big PGN
    // library (tens of thousands of games) is the same JSONL re-read and re-parsed line by
    // line on every call otherwise, and loadAll gets called a lot (every tab switch, every
    // Filters open, every picker) with nothing having changed on disk in between. mtime only
    // moves forward on a real write (appendGames/deleteSource's rename), so this can never
    // serve stale data after either.
    private static List<Entry> cache;
    private static long cacheMtime = -1;

    static synchronized List<Entry> loadAll(Context context) {
        File f = file(context);
        long mtime = f.exists() ? f.lastModified() : -1;
        if (cache != null && mtime == cacheMtime) return cache;
        List<Entry> out = new ArrayList<>();
        if (f.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    Entry e = parseLine(line);
                    if (e != null) out.add(e);
                }
            } catch (IOException ignored) { }
            Collections.reverse(out);
        }
        cache = out;
        cacheMtime = mtime;
        return out;
    }

    /** All games imported from one PGN source, newest first. */
    static List<Entry> loadBySource(Context context, String sourceLabel) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : loadAll(context)) if (e.src.equals(sourceLabel)) out.add(e);
        return out;
    }

    /** A single entry by its stable id, or {@code null} if it's gone (e.g. its source was
     *  deleted since the caller last looked it up). */
    static Entry findById(Context context, String id) {
        if (id == null) return null;
        for (Entry e : loadAll(context)) if (id.equals(e.id)) return e;
        return null;
    }

    /** One row per distinct imported PGN, for the "PGN files" screen and the
     *  generate/play-from pickers. {@code importedAt} is the earliest timestamp seen for that
     *  source (imports only ever append, so that's stable even if the same source label is
     *  imported into more than once). */
    static List<SourceSummary> listSources(Context context) {
        Map<String, int[]> counts = new LinkedHashMap<>(); // label -> {count}
        Map<String, Long> earliest = new LinkedHashMap<>();
        for (Entry e : loadAll(context)) {
            counts.computeIfAbsent(e.src, k -> new int[1])[0]++;
            Long cur = earliest.get(e.src);
            if (cur == null || (e.importedAt > 0 && e.importedAt < cur)) earliest.put(e.src, e.importedAt);
        }
        List<SourceSummary> out = new ArrayList<>();
        for (Map.Entry<String, int[]> en : counts.entrySet()) {
            out.add(new SourceSummary(en.getKey(), en.getValue()[0], earliest.getOrDefault(en.getKey(), 0L)));
        }
        return out;
    }

    /** Removes every game imported from {@code sourceLabel}. Rewrites the whole file (read all,
     *  filter, atomic replace) — fine at personal-library scale; there's no per-entry index to
     *  update incrementally. Callers are responsible for the puzzle-generation cursor (see
     *  {@link Config#clearChessPuzzlegenCursor}) and for cascade-deleting that source's puzzles
     *  (see {@link ChessPuzzles#deleteBySource}) — this method only touches the library file. */
    static boolean deleteSource(Context context, String sourceLabel) {
        File f = file(context);
        if (!f.exists()) return false;
        List<String> keep = new ArrayList<>();
        boolean removedAny = false;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                Entry e = parseLine(line);
                if (e != null && e.src.equals(sourceLabel)) { removedAny = true; continue; }
                keep.add(line);
            }
        } catch (IOException ignored) { return false; }
        if (!removedAny) return false;
        File tmp = new File(context.getFilesDir(), "chess_library.jsonl.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp, false)) {
            StringBuilder sb = new StringBuilder();
            for (String line : keep) sb.append(line).append('\n');
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) { return false; }
        return tmp.renameTo(f);
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
                Entry entry = parseLine(line);
                if (entry != null && !callback.onEntry(n, entry)) return;
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
