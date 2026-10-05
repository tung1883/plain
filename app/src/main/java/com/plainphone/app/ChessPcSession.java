package com.plainphone.app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The phone's half of one {@code chess.*} conversation about the puzzle job on a computer:
 *  what to send when the link comes up, and what to do with each message that comes back
 *  (store results durably, then ack; track progress; wrap up when the PC says it's finished).
 *
 *  <p>No Android dependencies — everything it touches is behind {@link Io}, so the protocol
 *  logic is unit-tested on the JVM. Not thread-safe: {@link ChessPcRunner} calls it from one
 *  thread only. */
final class ChessPcSession {

    interface Io {
        /** Send on this session's channel (the session fills in {@code ch} via {@link #channel}). */
        void send(Map<String, Object> message);
        long channel();
        /** Store a puzzle; false if it could not be saved. */
        boolean appendPuzzle(ChessPuzzles.Puzzle puzzle);
        /** Remember these games as scanned (shared with the on-phone generator). */
        void appendScanned(List<String> gameIds);
        Set<String> scannedIds();
        void saveRecord(ChessPcJobs.Record record);
        /** Show the current state (progress card, lock-dialog line). */
        void publish(ChessPcJobs.Record record);
        /** Start streaming the library's unscanned games to the PC (async; ends with chess.games.end). */
        void uploadGames(ChessPcJobs.Record record);
        /** The conversation is over: {@code clearRecord} when the job ended cleanly. */
        void finished(ChessPcJobs.Record record, boolean clearRecord);
    }

    private final ChessPcJobs.Record rec;
    private final Io io;
    private Set<String> scanned;
    private boolean restarted;
    private boolean finishing;

    ChessPcSession(ChessPcJobs.Record record, Io io) {
        this.rec = record;
        this.io = io;
    }

    ChessPcJobs.Record record() { return rec; }

    /** Link is up: (re)announce the job. {@code chess.job.start} is idempotent on the job id, so
     *  this is the same call whether the job is brand new, still running or already finished. */
    void begin() {
        io.send(DevProtocol.chessJobStart(io.channel(), rec.jobId, rec.total, rec.workers, rec.hashMb, rec.params));
        io.publish(rec);
    }

    void requestCancel() {
        rec.cancelRequested = true;
        io.saveRecord(rec);
        io.send(DevProtocol.chessCancel(io.channel(), rec.jobId));
        io.publish(rec);
    }

    void onMessage(Map<String, Object> msg) {
        String t = DevProtocol.type(msg);
        if (t == null) return;
        switch (t) {
            case DevProtocol.T_CHESS_JOB_STARTED: onStarted(msg); break;
            case DevProtocol.T_CHESS_RESULTS: onResults(msg); break;
            case DevProtocol.T_CHESS_PROGRESS: onProgress(msg); break;
            case DevProtocol.T_CHESS_ERROR: onError(msg); break;
            default: break; // games.ack, job.removed, … — nothing to do
        }
    }

    // --- handlers ------------------------------------------------------------------

    private void onStarted(Map<String, Object> msg) {
        applySnapshot(msg);
        io.saveRecord(rec);
        io.send(DevProtocol.chessAttach(io.channel(), rec.jobId, rec.lastSeq));
        if (!DevProtocol.bool(msg, "games_complete") && !isTerminal(DevProtocol.str(msg, "state"))) {
            io.uploadGames(rec);
        }
        if (rec.cancelRequested) io.send(DevProtocol.chessCancel(io.channel(), rec.jobId));
        io.publish(rec);
    }

    private void onResults(Map<String, Object> msg) {
        List<Object> items = DevProtocol.list(msg, "items");
        if (items == null || items.isEmpty()) return;
        if (scanned == null) scanned = new HashSet<>(io.scannedIds());
        List<String> newlyScanned = new ArrayList<>();
        long upto = rec.lastSeq;
        for (Object o : items) {
            if (!(o instanceof Map)) continue;
            @SuppressWarnings("unchecked") Map<String, Object> item = (Map<String, Object>) o;
            long seq = DevProtocol.num(item, "seq", -1);
            if (seq <= rec.lastSeq) continue; // a replay of something already stored
            String game = DevProtocol.str(item, "game");
            Object pz = item.get("puzzle");
            if (pz instanceof Map && game != null && !scanned.contains(game)) {
                @SuppressWarnings("unchecked") Map<String, Object> pm = (Map<String, Object>) pz;
                ChessPuzzles.Puzzle p = toPuzzle(pm);
                if (p != null && ChessPcCompat.playable(p.startFen, p.solutionUci)) {
                    if (!io.appendPuzzle(p)) return; // not stored: don't ack, the PC will replay it
                } else {
                    rec.dropped++;
                }
            }
            if (game != null && scanned.add(game)) newlyScanned.add(game);
            upto = Math.max(upto, seq);
        }
        if (upto == rec.lastSeq) return;
        io.appendScanned(newlyScanned);   // durable before the ack, so the PC never forgets what we haven't kept
        rec.lastSeq = upto;
        io.saveRecord(rec);
        io.send(DevProtocol.chessAck(io.channel(), rec.jobId, upto));
        io.publish(rec);
    }

    private void onProgress(Map<String, Object> msg) {
        applySnapshot(msg);
        String state = DevProtocol.str(msg, "state");
        if (isTerminal(state) && !finishing) {
            long pcLast = DevProtocol.num(msg, "last_seq", 0);
            if (rec.lastSeq < pcLast) { // results still to come (or lost): wait for the next progress
                io.saveRecord(rec);
                io.publish(rec);
                return;
            }
            finishing = true;
            io.send(DevProtocol.chessJobRemove(io.channel(), rec.jobId));
            switch (state) {
                case "done": rec.state = ChessPcJobs.STATE_DONE; break;
                case "cancelled": rec.state = ChessPcJobs.STATE_CANCELLED; break;
                default:
                    rec.state = ChessPcJobs.STATE_FAILED;
                    String e = DevProtocol.str(msg, "error");
                    rec.error = e != null ? e : "the PC stopped the job";
                    break;
            }
            io.saveRecord(rec);
            io.publish(rec);
            io.finished(rec, !ChessPcJobs.STATE_FAILED.equals(rec.state));
            return;
        }
        io.saveRecord(rec);
        io.publish(rec);
    }

    private void onError(Map<String, Object> msg) {
        String code = DevProtocol.str(msg, "code");
        String text = DevProtocol.str(msg, "msg");
        if ("no_job".equals(code) && rec.running() && !restarted) {
            restarted = true; // the PC lost the job (data wiped?): announce it again, the games not yet scanned go up again
            begin();
            return;
        }
        if (finishing) return;
        finishing = true;
        rec.state = ChessPcJobs.STATE_FAILED;
        if ("no_engine".equals(code)) rec.error = "Stockfish isn't installed on the PC";
        else if ("busy".equals(code)) rec.error = "The PC is already running another puzzle job";
        else rec.error = text != null ? text : "the PC refused the job";
        io.saveRecord(rec);
        io.publish(rec);
        io.finished(rec, false);
    }

    // --- helpers -------------------------------------------------------------------

    private void applySnapshot(Map<String, Object> m) {
        if (m.containsKey("scanned")) rec.scanned = (int) DevProtocol.num(m, "scanned", rec.scanned);
        if (m.containsKey("found")) rec.found = (int) DevProtocol.num(m, "found", rec.found);
        String state = DevProtocol.str(m, "state");
        if ("running".equals(state) && ChessPcJobs.STATE_STARTING.equals(rec.state)) rec.state = ChessPcJobs.STATE_RUNNING;
        rec.etaSeconds = m.containsKey("eta_s") ? (int) DevProtocol.num(m, "eta_s", -1) : -1;
    }

    private static boolean isTerminal(String state) {
        return "done".equals(state) || "cancelled".equals(state) || "failed".equals(state);
    }

    /** A wire puzzle map -> the phone's puzzle, or null if a required field is missing. */
    static ChessPuzzles.Puzzle toPuzzle(Map<String, Object> m) {
        String fen = DevProtocol.str(m, "fen");
        List<Object> sol = DevProtocol.list(m, "solution");
        if (fen == null || sol == null || sol.isEmpty()) return null;
        List<String> uci = new ArrayList<>();
        for (Object o : sol) uci.add(String.valueOf(o));
        long cp = DevProtocol.num(m, "cp", 0);
        String id = DevProtocol.str(m, "id");
        return new ChessPuzzles.Puzzle(
                id != null ? id : java.util.UUID.randomUUID().toString(),
                nz(DevProtocol.str(m, "src")), nz(DevProtocol.str(m, "white")), nz(DevProtocol.str(m, "black")),
                nz(DevProtocol.str(m, "event")), nz(DevProtocol.str(m, "date")),
                fen, (int) DevProtocol.num(m, "ply", 0), uci, DevProtocol.bool(m, "winner_white"),
                nz(DevProtocol.str(m, "category")),
                (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, cp)));
    }

    private static String nz(String s) { return s == null ? "" : s; }

    /** One game as the wire's map. */
    static Map<String, Object> gameMap(ChessLibrary.Entry e) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("id", e.id);
        g.put("src", e.src);
        g.put("white", e.white);
        g.put("black", e.black);
        g.put("event", e.event);
        g.put("date", e.date);
        List<Object> sans = new ArrayList<>(e.sans());
        g.put("sans", sans);
        return g;
    }
}
