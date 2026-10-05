package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The phone's real {@link ChessPcSession} talking to the real {@code plaind} daemon (and a real
 * Stockfish) over a real socket — proves the two sides agree on every field name and encoding.
 *
 * <p>Opt-in: skipped unless {@code PLAIND_EXE} points at a built plaind and a Stockfish binary
 * is available ({@code PLAIND_STOCKFISH}, default: plaind's own test copy next to it):
 * <pre>PLAIND_EXE=C:/.../plaind/target/debug/plaind.exe gradle :app:testDebugUnitTest --tests "*ChessPcLive*"</pre>
 */
public class ChessPcLiveDaemonTest {

    private static final String OPERA = "e4 e5 Nf3 d6 d4 Bg4 dxe5 Bxf3 Qxf3 dxe5 Bc4 Nf6 Qb3 Qe7 Nc3 c6 Bg5 b5 Nxb5 cxb5 "
            + "Bxb5+ Nbd7 O-O-O Rd8 Rxd7 Rxd7 Rd1 Qe6 Bxd7+ Nxd7 Qb8+ Nxb8 Rd8#";
    private static final String QUIET = "Nf3 Nf6 g3 g6 Bg2 Bg7 O-O O-O d3 d6";

    private Process daemon;
    private File data;
    private int port;
    private final String token = "live-" + UUID.randomUUID();
    private final List<Socket> sockets = new CopyOnWriteArrayList<>();

    @Before
    public void startDaemon() throws Exception {
        String exe = System.getenv("PLAIND_EXE");
        assumeTrue("set PLAIND_EXE to run the live-daemon test", exe != null && new File(exe).isFile());
        String sf = System.getenv("PLAIND_STOCKFISH");
        File engine = sf != null ? new File(sf)
                : new File(new File(exe).getParentFile().getParentFile(), "chess-test-data/engines/stockfish.exe");
        assumeTrue("no Stockfish at " + engine, engine.isFile());

        data = Files.createTempDirectory("plaind-live").toFile();
        File engines = new File(data, "engines");
        assertTrue(engines.mkdirs());
        Files.copy(engine.toPath(), new File(engines, "stockfish.exe").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.write(new File(data, "paired.txt").toPath(), (token + "\n").getBytes("UTF-8"));

        try (ServerSocket probe = new ServerSocket(0)) { port = probe.getLocalPort(); }
        ProcessBuilder pb = new ProcessBuilder(exe).redirectErrorStream(true);
        pb.environment().put("PLAIND_DATA", data.getAbsolutePath());
        pb.environment().put("PLAIND_PORT", String.valueOf(port));
        pb.environment().put("PLAIND_NO_AUTOSTART", "1");
        pb.redirectOutput(new File(data, "stdout.txt"));
        daemon = pb.start();
        long end = System.currentTimeMillis() + 20000;
        while (true) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", port), 500);
                break;
            } catch (Exception e) {
                assertTrue("daemon did not start", System.currentTimeMillis() < end && daemon.isAlive());
                Thread.sleep(200);
            }
        }
    }

    @After
    public void stopDaemon() {
        for (Socket s : sockets) try { s.close(); } catch (Exception ignored) { }
        if (daemon != null) daemon.destroyForcibly();
    }

    // --- a connection driven by a real ChessPcSession -------------------------------------------

    private final class Phone implements ChessPcSession.Io {
        final Socket socket;
        final OutputStream out;
        final Thread reader;
        final List<ChessPuzzles.Puzzle> puzzles = new CopyOnWriteArrayList<>();
        final List<String> scanned = new CopyOnWriteArrayList<>();
        final List<Map<String, Object>> games;
        final ChessPcJobs.Record rec;
        volatile Boolean finishedClear;
        final AtomicBoolean closed = new AtomicBoolean();
        final ChessPcSession session;
        final Set<String> known;

        Phone(ChessPcJobs.Record rec, List<Map<String, Object>> games, Set<String> known) throws Exception {
            this.rec = rec;
            this.games = games;
            this.known = known;
            socket = new Socket("127.0.0.1", port);
            sockets.add(socket);
            out = socket.getOutputStream();
            DevProtocol.writeFrame(out, DevProtocol.hello(token, "junit"));
            InputStream in = socket.getInputStream();
            Map<String, Object> welcome = DevProtocol.readFrame(in);
            assertEquals("welcome", DevProtocol.type(welcome));
            assertTrue("daemon advertises chess: " + welcome, DevProtocol.list(welcome, "caps").contains(DevProtocol.CAP_CHESS));
            session = new ChessPcSession(rec, this);
            reader = new Thread(() -> {
                try {
                    Map<String, Object> f;
                    while (!closed.get() && (f = DevProtocol.readFrame(in)) != null) session.onMessage(f);
                } catch (Exception ignored) { }
            }, "live-reader");
            reader.setDaemon(true);
            reader.start();
        }

        void close() { closed.set(true); try { socket.close(); } catch (Exception ignored) { } }

        @Override public synchronized void send(Map<String, Object> m) {
            try { DevProtocol.writeFrame(out, m); } catch (Exception ignored) { }
        }
        @Override public long channel() { return 5; }
        @Override public boolean appendPuzzle(ChessPuzzles.Puzzle p) { puzzles.add(p); return true; }
        @Override public void appendScanned(List<String> ids) { scanned.addAll(ids); known.addAll(ids); }
        @Override public Set<String> scannedIds() { return known; }
        @Override public void saveRecord(ChessPcJobs.Record r) { }
        @Override public void publish(ChessPcJobs.Record r) { }
        @Override public void uploadGames(ChessPcJobs.Record r) {
            new Thread(() -> {
                for (int i = 0; i < games.size(); i += 2) {
                    List<Object> chunk = new ArrayList<>(games.subList(i, Math.min(games.size(), i + 2)));
                    send(DevProtocol.chessGames(5, r.jobId, chunk));
                }
                send(DevProtocol.chessGamesEnd(5, r.jobId));
            }).start();
        }
        @Override public void finished(ChessPcJobs.Record r, boolean clear) { finishedClear = clear; }
    }

    private static Map<String, Object> game(String id, String sans) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("id", id);
        g.put("src", "live.pgn");
        g.put("white", "W");
        g.put("black", "B");
        g.put("event", "E");
        g.put("date", "2020.01.01");
        g.put("sans", new ArrayList<Object>(Arrays.asList((Object[]) sans.split(" "))));
        return g;
    }

    private ChessPcJobs.Record record(int total) {
        ChessPcJobs.Record r = new ChessPcJobs.Record();
        r.jobId = "live-" + UUID.randomUUID();
        r.hostId = "h";
        r.hostLabel = "Desk";
        r.total = total;
        r.workers = 2;
        r.hashMb = 32;
        r.params.put("walk_depth", 12L);
        r.params.put("pair_depth", 14L);
        r.params.put("defense_depth", 10L);
        r.params.put("walk_cap_ms", 2000L);
        r.params.put("deep_cap_ms", 5000L);
        return r;
    }

    private static void await(String what, long ms, java.util.function.BooleanSupplier cond) throws Exception {
        long end = System.currentTimeMillis() + ms;
        while (!cond.getAsBoolean()) {
            assertTrue("timed out: " + what, System.currentTimeMillis() < end);
            Thread.sleep(100);
        }
    }

    @Test
    public void phoneSessionRunsAJobOnTheRealDaemonAndStoresTheOperaMate() throws Exception {
        List<Map<String, Object>> games = Arrays.asList(game("opera", OPERA), game("quiet", QUIET), game("short", "e4 e5 Nf3 Nc6"));
        ChessPcJobs.Record rec = record(games.size());
        Phone phone = new Phone(rec, games, new java.util.HashSet<String>());
        phone.session.begin();
        await("job to finish", 180_000, () -> phone.finishedClear != null);

        assertEquals(Boolean.TRUE, phone.finishedClear);
        assertEquals(ChessPcJobs.STATE_DONE, rec.state);
        assertEquals("one puzzle (the Opera Game mate)", 1, phone.puzzles.size());
        ChessPuzzles.Puzzle p = phone.puzzles.get(0);
        assertEquals("Mate", p.category);
        assertTrue(p.winnerWhite);
        assertEquals(Arrays.asList("b3b8", "d7b8", "d1d8"), p.solutionUci);
        assertEquals(30, p.startPly);
        assertEquals("live.pgn", p.gameSrc);
        List<String> ids = new ArrayList<>(phone.scanned);
        java.util.Collections.sort(ids);
        assertEquals(Arrays.asList("opera", "quiet", "short"), ids);
        assertEquals(3, rec.scanned);
        assertEquals(1, rec.found);
        assertEquals(3L, rec.lastSeq);

        // the phone removed the finished job: the daemon no longer lists it
        Thread.sleep(500);
        Phone probe = new Phone(record(0), games, new java.util.HashSet<String>());
        final boolean[] listed = {true};
        Socket s = probe.socket;
        probe.close();
        assertNotNull(s);
        try (Socket again = new Socket("127.0.0.1", port)) {
            sockets.add(again);
            OutputStream o = again.getOutputStream();
            InputStream in = again.getInputStream();
            DevProtocol.writeFrame(o, DevProtocol.hello(token, "junit2"));
            DevProtocol.readFrame(in);
            DevProtocol.writeFrame(o, DevProtocol.chessJobList(9));
            Map<String, Object> reply;
            do { reply = DevProtocol.readFrame(in); } while (!"chess.job.list".equals(DevProtocol.type(reply)));
            listed[0] = false;
            for (Object j : DevProtocol.list(reply, "jobs")) {
                @SuppressWarnings("unchecked") Map<String, Object> jm = (Map<String, Object>) j;
                if (rec.jobId.equals(DevProtocol.str(jm, "job"))) listed[0] = true;
            }
        }
        assertTrue("finished job was removed on the daemon", !listed[0]);
    }

    @Test
    public void reconnectAfterTheLinkDropsLosesAndDuplicatesNothing() throws Exception {
        List<Map<String, Object>> games = new ArrayList<>();
        for (int i = 0; i < 8; i++) games.add(game("q" + i, QUIET));
        games.add(game("opera", OPERA));
        ChessPcJobs.Record rec = record(games.size());
        Set<String> known = new java.util.HashSet<>();

        Phone first = new Phone(rec, games, known);
        first.session.begin();
        await("first results", 120_000, () -> first.scanned.size() >= 2);
        first.close(); // phone vanishes mid-job (locked, out of range, app killed ...)
        long seenBefore = rec.lastSeq;
        assertTrue(seenBefore >= 2 && seenBefore < games.size());

        // the daemon keeps working while nobody is attached: wait until it has done everything
        await("daemon finishes alone", 180_000, () -> {
            try (Socket s = new Socket("127.0.0.1", port)) {
                OutputStream o = s.getOutputStream();
                InputStream in = s.getInputStream();
                DevProtocol.writeFrame(o, DevProtocol.hello(token, "poll"));
                DevProtocol.readFrame(in);
                DevProtocol.writeFrame(o, DevProtocol.chessJobList(9));
                Map<String, Object> r;
                do { r = DevProtocol.readFrame(in); } while (!"chess.job.list".equals(DevProtocol.type(r)));
                for (Object j : DevProtocol.list(r, "jobs")) {
                    @SuppressWarnings("unchecked") Map<String, Object> jm = (Map<String, Object>) j;
                    if (rec.jobId.equals(DevProtocol.str(jm, "job"))) return "done".equals(DevProtocol.str(jm, "state"));
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        });

        // reconnect with the same record (lastSeq kept): only the missing results arrive
        Phone second = new Phone(rec, games, known);
        second.session.begin();
        await("second session finishes", 60_000, () -> second.finishedClear != null);

        List<String> all = new ArrayList<>(first.scanned);
        all.addAll(second.scanned);
        java.util.Collections.sort(all);
        List<String> expect = new ArrayList<>();
        for (Map<String, Object> g : games) expect.add((String) g.get("id"));
        java.util.Collections.sort(expect);
        assertEquals("every game scanned exactly once across both sessions", expect, all);
        List<ChessPuzzles.Puzzle> puzzles = new ArrayList<>(first.puzzles);
        puzzles.addAll(second.puzzles);
        assertEquals("the one puzzle, stored once", 1, puzzles.size());
        assertEquals(ChessPcJobs.STATE_DONE, rec.state);
        assertEquals(games.size(), rec.scanned);
    }
}
