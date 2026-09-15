package com.plainphone.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/** Thin UCI wrapper around the Stockfish binary bundled at jniLibs/arm64-v8a/libstockfish.so
 *  (the .so name is a packaging trick — it's a real executable, not a shared library; Android
 *  extracts it to nativeLibraryDir with exec permission the same way it would a real .so).
 *  Two independent processes are kept alive for the app's lifetime, not one: {@link #get}
 *  (the fast double-tap move-picker, {@link #bestOf}) and {@link #getAnalysis} (background
 *  eval, {@link #analyzeMultiPv}, re-run after every move) each get their own process, so a
 *  slow deep analysis on one can never make a double-tap wait behind it — they used to share
 *  a single process and lock, and a double-tap could queue up for seconds behind whatever
 *  depth the analysis setting asked for. Every call here blocks on process I/O, so callers
 *  must run it off the UI thread. */
final class StockfishEngine {
    private static StockfishEngine moveInstance;
    private static StockfishEngine analysisInstance;

    private final BufferedReader out;
    private final BufferedWriter in;

    /** The move-picker process — used by {@link #bestOf} for double-tap. */
    static synchronized StockfishEngine get(Context context) throws IOException {
        if (moveInstance == null) moveInstance = new StockfishEngine(context);
        return moveInstance;
    }

    /** The background-analysis process — used by {@link #analyzeMultiPv}. */
    static synchronized StockfishEngine getAnalysis(Context context) throws IOException {
        if (analysisInstance == null) analysisInstance = new StockfishEngine(context);
        return analysisInstance;
    }

    private StockfishEngine(Context context) throws IOException {
        String path = context.getApplicationInfo().nativeLibraryDir + "/libstockfish.so";
        Process process = new ProcessBuilder(path).redirectErrorStream(true).start();
        out = new BufferedReader(new InputStreamReader(process.getInputStream()));
        in = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        send("uci");
        waitFor("uciok");
        send("isready");
        waitFor("readyok");
    }

    private void send(String command) throws IOException {
        in.write(command);
        in.write("\n");
        in.flush();
    }

    private void waitFor(String token) throws IOException {
        String line;
        while ((line = out.readLine()) != null) {
            if (line.trim().equals(token)) return;
        }
        throw new IOException("Stockfish closed before sending " + token);
    }

    /** Restricts the search to exactly {@code candidateUciMoves} (e.g. "g1f3") and returns
     *  whichever one Stockfish judges best within {@code movetimeMs} — or null if the engine
     *  failed or answered with something outside that list, so a caller always has a
     *  deterministic fallback ready rather than trusting a stray answer blindly. A time budget
     *  rather than a fixed depth keeps a double-tap's latency predictable regardless of how
     *  tactically complex the position is. */
    synchronized String bestOf(String fen, List<String> candidateUciMoves, int movetimeMs) throws IOException {
        send("position fen " + fen);
        StringBuilder go = new StringBuilder("go movetime ").append(movetimeMs).append(" searchmoves");
        for (String move : candidateUciMoves) go.append(' ').append(move);
        send(go.toString());
        String line, best = null;
        while ((line = out.readLine()) != null) {
            if (line.startsWith("bestmove")) {
                String[] parts = line.split("\\s+");
                best = parts.length > 1 ? parts[1] : null;
                break;
            }
        }
        return (best != null && candidateUciMoves.contains(best)) ? best : null;
    }

    /** An unrestricted read on the current position: the evaluation and best line Stockfish
     *  finds within {@code movetimeMs}, both still in the side-to-move's own perspective (UCI
     *  convention) — converting that to White's perspective and to SAN is the caller's job,
     *  since only the caller has the board to resolve SAN disambiguation against. */
    static final class Analysis {
        final Integer scoreCp;
        final Integer mateIn;
        final int depth;
        final List<String> pvUci;
        Analysis(Integer scoreCp, Integer mateIn, int depth, List<String> pvUci) {
            this.scoreCp = scoreCp;
            this.mateIn = mateIn;
            this.depth = depth;
            this.pvUci = pvUci;
        }
    }

    private int lastMultiPv = 1;

    // analyzeMultiPv() no longer shares a process/lock with bestOf(), so this is no longer
    // about protecting double-tap latency — it's a safety net against Android's phantom
    // process killer (Samsung/Android 12+ kills long-running high-CPU child processes), since
    // a move re-triggers analysis every time. Stockfish still stops itself early if it
    // reaches targetDepth first.
    private static final int ANALYSIS_MOVETIME_CAP_MS = 1500;

    /** The top {@code lines} candidate moves (not just the single best one), each with its own
     *  evaluation and principal variation, ranked best-first — this is what an engine panel
     *  actually shows, as opposed to {@link #bestOf} which only ever needs the single winner
     *  among a restricted set of squares. Depth is the user-facing "engine depth" setting;
     *  see {@link #ANALYSIS_MOVETIME_CAP_MS} for why it's still paired with a time cap. */
    synchronized List<Analysis> analyzeMultiPv(String fen, int targetDepth, int lines) throws IOException {
        if (lines != lastMultiPv) {
            send("setoption name MultiPV value " + lines);
            lastMultiPv = lines;
        }
        send("position fen " + fen);
        send("go depth " + targetDepth + " movetime " + ANALYSIS_MOVETIME_CAP_MS);
        // Index 0 unused; UCI's multipv numbering starts at 1, and keeping the same numbering
        // here avoids an off-by-one every time a line is read back out of this array.
        Analysis[] slots = new Analysis[lines + 1];
        String line;
        while ((line = out.readLine()) != null) {
            if (line.startsWith("bestmove")) break;
            if (!line.startsWith("info") || !line.contains(" pv ")) continue;
            String[] tokens = line.split("\\s+");
            Integer cp = null, mate = null;
            int multipv = 1, depth = 0;
            List<String> pv = null;
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equals("multipv") && i + 1 < tokens.length) {
                    multipv = Integer.parseInt(tokens[i + 1]);
                } else if (tokens[i].equals("depth") && i + 1 < tokens.length) {
                    depth = Integer.parseInt(tokens[i + 1]);
                } else if (tokens[i].equals("score") && i + 2 < tokens.length) {
                    if (tokens[i + 1].equals("cp")) cp = Integer.parseInt(tokens[i + 2]);
                    else if (tokens[i + 1].equals("mate")) mate = Integer.parseInt(tokens[i + 2]);
                } else if (tokens[i].equals("pv")) {
                    pv = new ArrayList<>();
                    for (int j = i + 1; j < tokens.length; j++) pv.add(tokens[j]);
                    break;
                }
            }
            // Same reasoning as the single-line case: a later "info" for a given multipv slot
            // always supersedes an earlier one, so whatever's standing at "bestmove" is each
            // slot's deepest completed result.
            if (pv != null && !pv.isEmpty() && multipv >= 1 && multipv < slots.length) {
                slots[multipv] = new Analysis(cp, mate, depth, pv);
            }
        }
        List<Analysis> result = new ArrayList<>();
        for (int i = 1; i < slots.length; i++) if (slots[i] != null) result.add(slots[i]);
        return result;
    }
}
