package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The {@code chess.*} builders must produce exactly what plaind's PROTOCOL.md says, and survive
 *  the wire encoding (nested lists/maps, 64-bit numbers). */
public class ChessPcProtocolTest {

    private static Map<String, Object> wire(Map<String, Object> m) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DevProtocol.writeFrame(out, m);
        return DevProtocol.readFrame(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    public void simpleRequestsCarryTypeAndChannel() throws Exception {
        assertEquals("chess.status", DevProtocol.type(wire(DevProtocol.chessStatus(4))));
        assertEquals(4L, DevProtocol.num(wire(DevProtocol.chessStatus(4)), "ch", -1));
        assertEquals("chess.install", DevProtocol.type(wire(DevProtocol.chessInstall(1))));
        assertEquals("chess.job.list", DevProtocol.type(wire(DevProtocol.chessJobList(1))));
        assertEquals("chess.detach", DevProtocol.type(wire(DevProtocol.chessDetach(1))));
    }

    @Test
    public void jobStartCarriesTheDocumentedFields() throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("walk_depth", 20L);
        params.put("strict", Boolean.TRUE);
        params.put("swing", 0.4);
        Map<String, Object> m = wire(DevProtocol.chessJobStart(3, "abc-1", 1200, 6, 128, params));
        assertEquals("chess.job.start", DevProtocol.type(m));
        assertEquals("abc-1", DevProtocol.str(m, "job"));
        assertEquals(1200L, DevProtocol.num(m, "total", 0));
        assertEquals(6L, DevProtocol.num(m, "workers", 0));
        assertEquals(128L, DevProtocol.num(m, "hash_mb", 0));
        @SuppressWarnings("unchecked") Map<String, Object> p = (Map<String, Object>) m.get("params");
        assertEquals(20L, DevProtocol.num(p, "walk_depth", 0));
        assertTrue(DevProtocol.bool(p, "strict"));
        assertEquals(0.4, DevProtocol.dbl(p, "swing", 0), 1e-9);
    }

    @Test
    public void jobStartOmitsUnsetOptionalFields() throws Exception {
        Map<String, Object> m = wire(DevProtocol.chessJobStart(3, "j", 10, 0, 0, null));
        assertFalse(m.containsKey("workers"));
        assertFalse(m.containsKey("hash_mb"));
        assertFalse(m.containsKey("params"));
        assertFalse(wire(DevProtocol.chessJobStart(3, "j", 10, 0, 0, new LinkedHashMap<String, Object>())).containsKey("params"));
    }

    @Test
    public void gamesFrameRoundTripsAndStaysUnderTheFrameLimit() throws Exception {
        List<Object> games = new ArrayList<>();
        List<Object> sans = new ArrayList<>();
        for (int i = 0; i < 80; i++) sans.add(i % 2 == 0 ? "Nf3" : "Nf6");
        for (int i = 0; i < 200; i++) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("id", "game-" + i);
            g.put("src", "x.pgn");
            g.put("white", "White Player");
            g.put("black", "Black Player");
            g.put("event", "Some Open");
            g.put("date", "2020.01.01");
            g.put("sans", sans);
            games.add(g);
        }
        Map<String, Object> msg = DevProtocol.chessGames(2, "job", games);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DevProtocol.writeFrame(out, msg); // would throw if over MAX_FRAME
        assertTrue("200 typical games fit a frame: " + out.size(), out.size() < DevProtocol.MAX_FRAME);
        Map<String, Object> back = DevProtocol.readFrame(new ByteArrayInputStream(out.toByteArray()));
        List<Object> got = DevProtocol.list(back, "games");
        assertEquals(200, got.size());
        @SuppressWarnings("unchecked") Map<String, Object> first = (Map<String, Object>) got.get(0);
        assertEquals("game-0", DevProtocol.str(first, "id"));
        assertEquals(80, DevProtocol.list(first, "sans").size());
    }

    @Test
    public void attachAckCancelRemoveEndCarryTheJobAndNumbers() throws Exception {
        Map<String, Object> attach = wire(DevProtocol.chessAttach(5, "j", 5_000_000_000L));
        assertEquals("chess.attach", DevProtocol.type(attach));
        assertEquals(5_000_000_000L, DevProtocol.num(attach, "after", 0));
        Map<String, Object> ack = wire(DevProtocol.chessAck(5, "j", 42));
        assertEquals("chess.ack", DevProtocol.type(ack));
        assertEquals(42L, DevProtocol.num(ack, "upto", 0));
        assertEquals("j", DevProtocol.str(wire(DevProtocol.chessCancel(5, "j")), "job"));
        assertEquals("chess.job.remove", DevProtocol.type(wire(DevProtocol.chessJobRemove(5, "j"))));
        assertEquals("chess.games.end", DevProtocol.type(wire(DevProtocol.chessGamesEnd(5, "j"))));
    }

    @Test
    public void typeConstantsMatchTheDaemonsSpelling() {
        // plaind's wire.rs matches on these exact strings
        assertEquals("chess.job.start", DevProtocol.T_CHESS_JOB_START);
        assertEquals("chess.job.started", DevProtocol.T_CHESS_JOB_STARTED);
        assertEquals("chess.results", DevProtocol.T_CHESS_RESULTS);
        assertEquals("chess.progress", DevProtocol.T_CHESS_PROGRESS);
        assertEquals("chess.error", DevProtocol.T_CHESS_ERROR);
        assertEquals("chess", DevProtocol.CAP_CHESS);
    }

    @Test
    public void sessionTurnsAWirePuzzleIntoThePhonesPuzzle() throws Exception {
        List<Object> sol = new ArrayList<>();
        sol.add("b3b8");
        sol.add("d7b8");
        Map<String, Object> pz = new LinkedHashMap<>();
        pz.put("id", "p1");
        pz.put("fen", "x");
        pz.put("solution", sol);
        pz.put("cp", 2147483646L);
        pz.put("winner_white", Boolean.TRUE);
        ChessPuzzles.Puzzle p = ChessPcSession.toPuzzle(wire(withType(pz)));
        assertEquals("p1", p.id);
        assertEquals(2147483646, p.cp);
        assertEquals(2, p.solutionUci.size());
        assertEquals("", p.event); // missing text fields become empty, not "null"
        pz.remove("fen");
        assertEquals(null, ChessPcSession.toPuzzle(pz));
    }

    private static Map<String, Object> withType(Map<String, Object> m) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("t", "x");
        out.putAll(m);
        return out;
    }
}
