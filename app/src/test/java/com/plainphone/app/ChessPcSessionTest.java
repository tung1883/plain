package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChessPcSessionTest {

    private static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /** Records everything the session does; stands in for the connection and the phone's storage. */
    private static final class FakeIo implements ChessPcSession.Io {
        final List<Map<String, Object>> sent = new ArrayList<>();
        final List<ChessPuzzles.Puzzle> puzzles = new ArrayList<>();
        final List<String> scannedAppended = new ArrayList<>();
        final Set<String> alreadyScanned = new HashSet<>();
        final List<String> saves = new ArrayList<>();
        int publishes, uploads;
        Boolean finishedClear;
        boolean appendOk = true;

        @Override public void send(Map<String, Object> m) { sent.add(m); }
        @Override public long channel() { return 7; }
        @Override public boolean appendPuzzle(ChessPuzzles.Puzzle p) { if (appendOk) puzzles.add(p); return appendOk; }
        @Override public void appendScanned(List<String> ids) { scannedAppended.addAll(ids); }
        @Override public Set<String> scannedIds() { return alreadyScanned; }
        @Override public void saveRecord(ChessPcJobs.Record r) { saves.add(ChessPcJobs.toJson(r)); }
        @Override public void publish(ChessPcJobs.Record r) { publishes++; }
        @Override public void uploadGames(ChessPcJobs.Record r) { uploads++; }
        @Override public void finished(ChessPcJobs.Record r, boolean clear) { finishedClear = clear; }

        List<Map<String, Object>> sentOfType(String t) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Map<String, Object> m : sent) if (t.equals(DevProtocol.type(m))) out.add(m);
            return out;
        }
    }

    private ChessPcJobs.Record rec;
    private FakeIo io;
    private ChessPcSession s;

    @Before
    public void setUp() {
        rec = new ChessPcJobs.Record();
        rec.jobId = "job1";
        rec.hostId = "h";
        rec.hostLabel = "Desk";
        rec.total = 5;
        rec.params.put("walk_depth", 20L);
        io = new FakeIo();
        s = new ChessPcSession(rec, io);
    }

    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) out.put((String) kv[i], kv[i + 1]);
        return out;
    }

    private static Map<String, Object> puzzle(String id, String fen, String... solution) {
        List<Object> sol = new ArrayList<>(Arrays.asList((Object[]) solution));
        return m("id", id, "src", "a.pgn", "white", "W", "black", "B", "event", "E", "date", "2020.01.01",
                "fen", fen, "ply", 30L, "solution", sol, "winner_white", true, "category", "Mate",
                "cp", (long) (Integer.MAX_VALUE - 1));
    }

    private static Map<String, Object> item(long seq, String game, Map<String, Object> puzzle) {
        Map<String, Object> i = m("seq", seq, "game", game);
        if (puzzle != null) i.put("puzzle", puzzle);
        return i;
    }

    private static Map<String, Object> results(Map<String, Object>... items) {
        List<Object> list = new ArrayList<>(Arrays.asList((Object[]) items));
        return m("t", DevProtocol.T_CHESS_RESULTS, "ch", 7L, "job", "job1", "items", list);
    }

    private static Map<String, Object> progress(String state, long scanned, long found, long lastSeq) {
        return m("t", DevProtocol.T_CHESS_PROGRESS, "ch", 7L, "job", "job1", "state", state,
                "scanned", scanned, "found", found, "last_seq", lastSeq);
    }

    // --- begin ------------------------------------------------------------------------

    @Test
    public void beginAnnouncesTheJobIdempotentlyWithItsParams() {
        s.begin();
        Map<String, Object> start = io.sentOfType(DevProtocol.T_CHESS_JOB_START).get(0);
        assertEquals("job1", start.get("job"));
        assertEquals(5L, start.get("total"));
        assertEquals(7L, start.get("ch"));
        assertNotNull(start.get("params"));
    }

    @Test
    public void startedAttachesAfterTheLastStoredResultAndUploadsMissingGames() {
        rec.lastSeq = 12;
        s.onMessage(m("t", DevProtocol.T_CHESS_JOB_STARTED, "state", "running", "games_complete", false,
                "scanned", 3L, "found", 1L));
        Map<String, Object> attach = io.sentOfType(DevProtocol.T_CHESS_ATTACH).get(0);
        assertEquals(12L, attach.get("after"));
        assertEquals(1, io.uploads);
        assertEquals(ChessPcJobs.STATE_RUNNING, rec.state);
        assertEquals(3, rec.scanned);
    }

    @Test
    public void startedWithAllGamesAlreadyThereDoesNotUploadAgain() {
        s.onMessage(m("t", DevProtocol.T_CHESS_JOB_STARTED, "state", "running", "games_complete", true));
        assertEquals(0, io.uploads);
        assertEquals(1, io.sentOfType(DevProtocol.T_CHESS_ATTACH).size());
    }

    @Test
    public void startedOnAnAlreadyFinishedJobAttachesToReplayButDoesNotUpload() {
        s.onMessage(m("t", DevProtocol.T_CHESS_JOB_STARTED, "state", "done", "games_complete", false));
        assertEquals(0, io.uploads);
        assertEquals(1, io.sentOfType(DevProtocol.T_CHESS_ATTACH).size());
    }

    @Test
    public void pendingCancelIsSentOnceTheJobIsConfirmed() {
        rec.cancelRequested = true;
        s.onMessage(m("t", DevProtocol.T_CHESS_JOB_STARTED, "state", "running", "games_complete", true));
        assertEquals(1, io.sentOfType(DevProtocol.T_CHESS_CANCEL).size());
    }

    // --- results ------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void resultsAreStoredThenAckedAndGamesRecordedAsScanned() {
        s.onMessage(results(
                item(1, "g1", null),
                item(2, "g2", puzzle("p-2", START, "e2e4", "e7e5", "g1f3")),
                item(3, "g3", null)));
        assertEquals(1, io.puzzles.size());
        ChessPuzzles.Puzzle p = io.puzzles.get(0);
        assertEquals("p-2", p.id);
        assertEquals(START, p.startFen);
        assertEquals(Arrays.asList("e2e4", "e7e5", "g1f3"), p.solutionUci);
        assertTrue(p.winnerWhite);
        assertEquals("Mate", p.category);
        assertEquals(30, p.startPly);
        assertEquals(Arrays.asList("g1", "g2", "g3"), io.scannedAppended);
        assertEquals(3L, rec.lastSeq);
        Map<String, Object> ack = io.sentOfType(DevProtocol.T_CHESS_ACK).get(0);
        assertEquals(3L, ack.get("upto"));
        assertEquals("job1", ack.get("job"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void replayedResultsAreIgnored() {
        rec.lastSeq = 2;
        s.onMessage(results(item(1, "g1", puzzle("p1", START, "e2e4")), item(2, "g2", null), item(3, "g3", null)));
        assertEquals(0, io.puzzles.size());
        assertEquals(Arrays.asList("g3"), io.scannedAppended);
        assertEquals(3L, rec.lastSeq);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void aReplayOfOnlyOldResultsChangesNothingAndSendsNoAck() {
        rec.lastSeq = 5;
        s.onMessage(results(item(4, "g4", null), item(5, "g5", null)));
        assertTrue(io.sentOfType(DevProtocol.T_CHESS_ACK).isEmpty());
        assertTrue(io.scannedAppended.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void puzzleForAnAlreadyScannedGameIsNotStoredTwice() {
        io.alreadyScanned.add("g1");
        s.onMessage(results(item(1, "g1", puzzle("p1", START, "e2e4")), item(2, "g2", puzzle("p2", START, "d2d4"))));
        assertEquals(1, io.puzzles.size());
        assertEquals("p2", io.puzzles.get(0).id);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void unplayablePuzzlesAreDroppedButTheGameStillCountsAsScanned() {
        String promo = "8/P6k/8/8/8/8/8/K7 w - - 0 1";
        s.onMessage(results(item(1, "g1", puzzle("p1", promo, "a7a8n")), item(2, "g2", null)));
        assertEquals(0, io.puzzles.size());
        assertEquals(1, rec.dropped);
        assertEquals(Arrays.asList("g1", "g2"), io.scannedAppended);
        assertEquals(2L, rec.lastSeq);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedStoreMeansNoAckSoThePcReplaysIt() {
        io.appendOk = false;
        s.onMessage(results(item(1, "g1", puzzle("p1", START, "e2e4"))));
        assertTrue(io.sentOfType(DevProtocol.T_CHESS_ACK).isEmpty());
        assertEquals(0L, rec.lastSeq);
        assertTrue(io.scannedAppended.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void malformedPuzzleIsDroppedNotCrashing() {
        Map<String, Object> bad = m("id", "x", "fen", START); // no solution
        s.onMessage(results(item(1, "g1", bad)));
        assertEquals(0, io.puzzles.size());
        assertEquals(1, rec.dropped);
        assertEquals(1L, rec.lastSeq);
    }

    // --- progress / finish -----------------------------------------------------------------

    @Test
    public void runningProgressUpdatesTheRecordAndEta() {
        Map<String, Object> p = progress("running", 40, 3, 40);
        p.put("eta_s", 600L);
        s.onMessage(p);
        assertEquals(40, rec.scanned);
        assertEquals(3, rec.found);
        assertEquals(600, rec.etaSeconds);
        assertEquals(ChessPcJobs.STATE_RUNNING, rec.state);
        assertNull(io.finishedClear);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doneProgressAfterAllResultsRemovesTheJobAndClears() {
        s.onMessage(results(item(1, "g1", null), item(2, "g2", null)));
        s.onMessage(progress("done", 2, 0, 2));
        assertEquals(ChessPcJobs.STATE_DONE, rec.state);
        assertEquals(1, io.sentOfType(DevProtocol.T_CHESS_JOB_REMOVE).size());
        assertEquals(Boolean.TRUE, io.finishedClear);
    }

    @Test
    public void terminalProgressWaitsWhileResultsAreStillMissing() {
        rec.lastSeq = 1;
        s.onMessage(progress("done", 3, 0, 3)); // PC has 3 results, we have 1
        assertNull(io.finishedClear);
        assertTrue(io.sentOfType(DevProtocol.T_CHESS_JOB_REMOVE).isEmpty());
        assertTrue(rec.running());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void finishIsReportedOnlyOnce() {
        s.onMessage(progress("done", 0, 0, 0));
        s.onMessage(progress("done", 0, 0, 0));
        assertEquals(1, io.sentOfType(DevProtocol.T_CHESS_JOB_REMOVE).size());
    }

    @Test
    public void cancelledJobClearsTheRecord() {
        s.onMessage(progress("cancelled", 4, 1, 0));
        assertEquals(ChessPcJobs.STATE_CANCELLED, rec.state);
        assertEquals(Boolean.TRUE, io.finishedClear);
    }

    @Test
    public void failedJobKeepsTheRecordWithItsError() {
        Map<String, Object> p = progress("failed", 4, 1, 0);
        p.put("error", "disk: cannot save results");
        s.onMessage(p);
        assertEquals(ChessPcJobs.STATE_FAILED, rec.state);
        assertEquals("disk: cannot save results", rec.error);
        assertEquals(Boolean.FALSE, io.finishedClear);
    }

    // --- errors ----------------------------------------------------------------------------------

    @Test
    public void noEngineBusyAndOtherErrorsFailTheJobWithAReadableReason() {
        s.onMessage(m("t", DevProtocol.T_CHESS_ERROR, "code", "no_engine", "msg", "x"));
        assertEquals(ChessPcJobs.STATE_FAILED, rec.state);
        assertTrue(rec.error.contains("Stockfish"));
        assertEquals(Boolean.FALSE, io.finishedClear);

        ChessPcJobs.Record r2 = new ChessPcJobs.Record();
        r2.jobId = "j2"; r2.hostId = "h";
        FakeIo io2 = new FakeIo();
        new ChessPcSession(r2, io2).onMessage(m("t", DevProtocol.T_CHESS_ERROR, "code", "busy", "msg", "x"));
        assertTrue(r2.error.contains("already running"));
    }

    @Test
    public void unknownJobOnThePcIsAnnouncedAgainOnceThenFails() {
        s.onMessage(m("t", DevProtocol.T_CHESS_ERROR, "code", "no_job", "msg", "unknown job"));
        assertEquals(1, io.sentOfType(DevProtocol.T_CHESS_JOB_START).size());
        assertTrue(rec.running());
        s.onMessage(m("t", DevProtocol.T_CHESS_ERROR, "code", "no_job", "msg", "unknown job"));
        assertEquals(ChessPcJobs.STATE_FAILED, rec.state);
    }

    @Test
    public void unrelatedMessagesAreIgnored() {
        s.onMessage(m("t", DevProtocol.T_CHESS_GAMES_ACK, "have", 3L));
        s.onMessage(m("t", DevProtocol.T_CHESS_JOB_REMOVED, "ok", true));
        s.onMessage(m("t", "pong"));
        s.onMessage(new LinkedHashMap<String, Object>());
        assertTrue(io.sent.isEmpty());
        assertFalse(rec.state.equals(ChessPcJobs.STATE_FAILED));
    }

    @Test
    public void requestCancelSavesTheFlagAndAsksThePc() {
        s.requestCancel();
        assertTrue(rec.cancelRequested);
        assertEquals(1, io.sentOfType(DevProtocol.T_CHESS_CANCEL).size());
        assertFalse(io.saves.isEmpty());
    }

    // --- record persistence --------------------------------------------------------------------------

    @Test
    public void recordSurvivesAJsonRoundTrip() {
        rec.params.put("strict", Boolean.TRUE);
        rec.scope = "My games.pgn";
        rec.lastSeq = 99;
        rec.scanned = 12; rec.found = 3; rec.dropped = 1; rec.etaSeconds = 77;
        rec.cancelRequested = true;
        rec.error = "e";
        rec.state = ChessPcJobs.STATE_RUNNING;
        ChessPcJobs.Record back = ChessPcJobs.parse(ChessPcJobs.toJson(rec));
        assertNotNull(back);
        assertEquals("job1", back.jobId);
        assertEquals("My games.pgn", back.scope);
        assertEquals(99L, back.lastSeq);
        assertEquals(12, back.scanned);
        assertEquals(3, back.found);
        assertEquals(1, back.dropped);
        assertEquals(77, back.etaSeconds);
        assertTrue(back.cancelRequested);
        assertEquals("e", back.error);
        assertEquals(Boolean.TRUE, back.params.get("strict"));
        assertEquals(20, ((Number) back.params.get("walk_depth")).intValue());
    }

    @Test
    public void corruptRecordParsesToNull() {
        assertNull(ChessPcJobs.parse(null));
        assertNull(ChessPcJobs.parse("{not json"));
        assertNull(ChessPcJobs.parse("{}")); // no job/host
    }
}
