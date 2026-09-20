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
import java.util.HashMap;
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
        try (AppendSession session = beginAppend(context, sourceLabel)) {
            for (Pgn.Game game : games) session.append(game);
        } catch (IOException ignored) { }
    }

    /** Streaming counterpart to {@link #appendGames} — writes one game at a time as it's
     *  handed in, instead of building the whole batch's JSON in memory first (the other half
     *  of the 7000+-game-import OOM fix: {@link Pgn#parse} now streams games one at a time too,
     *  so the import path never holds more than one game plus this session's open file handle).
     *  Callers must close the session (try-with-resources) when done importing. */
    static AppendSession beginAppend(Context context, String sourceLabel) throws IOException {
        return new AppendSession(context, sourceLabel);
    }

    static final class AppendSession implements java.io.Closeable {
        private final FileOutputStream out;
        private final String sourceLabel;
        private final long now;

        private AppendSession(Context context, String sourceLabel) throws IOException {
            out = new FileOutputStream(file(context), true);
            this.sourceLabel = sourceLabel;
            now = System.currentTimeMillis();
        }

        /** Appends one game and returns the id it was stamped with. */
        String append(Pgn.Game game) throws IOException {
            String id = UUID.randomUUID().toString();
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
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
            } catch (JSONException e) {
                throw new IOException(e);
            }
            out.write((o.toString() + "\n").getBytes(StandardCharsets.UTF_8));
            return id;
        }

        @Override public void close() throws IOException { out.close(); }
    }

    /** {@code includeSans} false skips the "sans" field — every game's full move list is the
     *  one big string in each line, and {@link #loadAll} would otherwise hold all of them
     *  (the whole library, at once, cached indefinitely) just to show white/black/event rows
     *  that never touch a single move. Only a lookup for one specific game ({@link #findById},
     *  {@link #scanFrom}'s callers) actually needs it. */
    private static Entry parseLine(String line, boolean includeSans) {
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
                    includeSans ? o.optString("sans", "") : "",
                    o.optLong("importedAt", 0L));
        } catch (JSONException e) {
            return null;
        }
    }

    /** Reads every game back, newest import first — {@code sansJoined} empty on every entry
     *  (see {@link #parseLine}); this is the browsing/filtering list (rows only ever show
     *  white/black/event/date), never the source of a game's actual moves. Runs entirely on
     *  the calling thread — callers do this off the UI thread, same as {@link Pgn#parse}. */
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
                    Entry e = parseLine(line, false);
                    if (e != null) out.add(e);
                }
            } catch (IOException ignored) { }
            Collections.reverse(out);
        }
        cache = out;
        cacheMtime = mtime;
        return out;
    }

    /** All games imported from one PGN source, newest first — moves NOT included (see
     *  {@link #loadAll}); every browsing-list caller only ever shows white/black/event/date.
     *  {@link #loadBySourceWithSans} is the one other caller actually needs, and it's the
     *  only one that should ask for it. */
    static List<Entry> loadBySource(Context context, String sourceLabel) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : loadAll(context)) if (e.src.equals(sourceLabel)) out.add(e);
        return out;
    }

    /** Same as {@link #loadBySource}, but with each entry's full move list included — the
     *  puzzle generator's scoped-run path actually has to replay every game, unlike every
     *  other {@code loadBySource} caller. A fresh streamed read every time (not
     *  {@link #loadAll}'s cache, which deliberately never carries moves) — this used to just
     *  be {@code loadBySource}, until that stopped including sans (see {@link #loadAll}) and
     *  a scoped generation run silently analyzed every game as move-less, "finishing" almost
     *  instantly without ever calling the engine. */
    static List<Entry> loadBySourceWithSans(Context context, String sourceLabel) {
        List<Entry> out = new ArrayList<>();
        File f = file(context);
        if (f.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    Entry e = parseLine(line, true);
                    if (e != null && e.src.equals(sourceLabel)) out.add(e);
                }
            } catch (IOException ignored) { }
            Collections.reverse(out);
        }
        return out;
    }

    /** A single entry by its stable id, moves included, or {@code null} if it's gone (e.g. its
     *  source was deleted since the caller last looked it up). Always a fresh streamed read —
     *  never {@link #loadAll}'s cache, which deliberately drops every entry's moves. */
    // id -> 1-based line number, keyed by the file's own mtime — same cache-until-changed
    // pattern as loadAll's own cache. Without this, findById had to scanFrom(0) and stream
    // the WHOLE file oldest-first on every single tap; the Library list shows newest-first,
    // so tapping a just-imported game (the common case — it's at the top) meant parsing
    // nearly every line, moves included, in a 12,000+-game library before reaching a match.
    // The index build itself is still one full pass, but it only re-runs when the file
    // actually changes (a new import, a delete, a rename) — every tap in between is one
    // HashMap lookup plus a single-line read via scanFrom's skip-fast-path.
    private static Map<String, Integer> idIndex;
    private static long idIndexMtime = -1;

    private static synchronized void ensureIdIndex(Context context) {
        File f = file(context);
        long mtime = f.exists() ? f.lastModified() : -1;
        if (idIndex != null && mtime == idIndexMtime) return;
        Map<String, Integer> out = new HashMap<>();
        scanFrom(context, 0, (lineNumber, entry) -> {
            out.put(entry.id, lineNumber);
            return true;
        });
        idIndex = out;
        idIndexMtime = mtime;
    }

    /** A single entry by its stable id, moves included, or {@code null} if it's gone (e.g. its
     *  source was deleted since the caller last looked it up). Always a fresh streamed
     *  single-line read for the entry itself — never {@link #loadAll}'s cache, which
     *  deliberately drops every entry's moves — but the id-to-line lookup that gets it there
     *  is indexed (see {@link #ensureIdIndex}), not a full-file scan. */
    static Entry findById(Context context, String id) {
        if (id == null) return null;
        ensureIdIndex(context);
        Integer line = idIndex.get(id);
        if (line == null) return null;
        Entry[] found = new Entry[1];
        scanFrom(context, line - 1, (lineNumber, entry) -> {
            found[0] = entry;
            return false;
        });
        return found[0];
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
                Entry e = parseLine(line, false);
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

    /** Renames a PGN source across every game that carries it — rewrites the whole file (read
     *  all, replace matching "src" values, atomic replace), same trade-off as
     *  {@link #deleteSource}. Callers are responsible for the puzzle side
     *  ({@code ChessPuzzles#renameSource}) and the puzzle-generation cursor
     *  ({@code Config#renameChessPuzzlegenCursor}) — this method only touches the library
     *  file. */
    static boolean renameSource(Context context, String oldLabel, String newLabel) {
        File f = file(context);
        if (!f.exists()) return false;
        List<String> lines = new ArrayList<>();
        boolean changed = false;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                try {
                    JSONObject o = new JSONObject(line);
                    if (oldLabel.equals(o.optString("src", ""))) {
                        o.put("src", newLabel);
                        line = o.toString();
                        changed = true;
                    }
                } catch (JSONException ignored) { }
                lines.add(line);
            }
        } catch (IOException ignored) { return false; }
        if (!changed) return false;
        File tmp = new File(context.getFilesDir(), "chess_library.jsonl.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp, false)) {
            StringBuilder sb = new StringBuilder();
            for (String l : lines) sb.append(l).append('\n');
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
                Entry entry = parseLine(line, true);
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
