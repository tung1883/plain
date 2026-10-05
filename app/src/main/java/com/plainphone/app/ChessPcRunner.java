package com.plainphone.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.LongFunction;

/** Runs the Android side of "generate puzzles on a computer": starts the job, and keeps the
 *  phone in step with it across disconnects.
 *
 *  <p>The job lives in plaind. Whenever the Dev link to that computer comes up
 *  ({@link DevService} calls {@link #onLinkUp}), a {@link ChessPcSession} re-announces the job
 *  (idempotent), re-attaches after the last stored result and, if the computer never got every
 *  game, uploads the rest. A locked phone, a dropped link or a killed app therefore only
 *  delays results; nothing is lost and the PC never stops working.
 *
 *  <p>Threading: everything protocol-related runs on {@link #IO} (one thread, so a session is
 *  never touched concurrently); {@link DevConnection} delivers on the main thread and hops over. */
final class ChessPcRunner {

    private ChessPcRunner() {}

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> new Thread(r, "chess-pc-io"));
    private static final ExecutorService UPLOAD = Executors.newSingleThreadExecutor(r -> new Thread(r, "chess-pc-upload"));
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Live links by host id (kept current by DevService's callbacks). */
    private static final Map<String, DevConnection> links = new ConcurrentHashMap<>();
    private static final Map<String, List<Object>> linkCaps = new ConcurrentHashMap<>();

    // IO-thread state
    private static ChessPcSession session;
    private static String sessionHost;
    private static DevConnection sessionConn;
    private static long sessionChannel;

    // --- hooks from DevService (main thread) ------------------------------------------

    static void onLinkUp(Context context, String hostId, DevConnection conn, List<Object> caps) {
        links.put(hostId, conn);
        linkCaps.put(hostId, caps == null ? new ArrayList<>() : caps);
        Context app = context.getApplicationContext();
        ChessPcJobs.Record r = ChessPcJobs.current(app);
        if (r == null || !hostId.equals(r.hostId)) return;
        IO.execute(() -> attach(app, hostId, conn));
    }

    static void onLinkDown(Context context, String hostId) {
        links.remove(hostId);
        linkCaps.remove(hostId);
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            if (hostId.equals(sessionHost)) endSession();
            ChessPcJobs.Record r = ChessPcJobs.current(app);
            if (r != null && hostId.equals(r.hostId)) publish(app, r);
        });
    }

    // --- state for the UI ---------------------------------------------------------------

    /** A job exists on a computer that this phone is still following (running, or finished but
     *  not yet wrapped up / failed and not dismissed). */
    static boolean hasJob(Context context) {
        return ChessPcJobs.current(context) != null;
    }

    /** Whether the computer this job runs on may be reached right now: Dev is unlocked (or has
     *  no lock). Reconnecting a locked Dev section on its own would defeat its PIN. */
    static boolean linkAllowed(Context context, String hostId) {
        return !Lock.DEV.isLocked(context) || Lock.DEV.isUnlocked(context);
    }

    /** Called whenever the Puzzles tab is shown: brings the progress card back after a process
     *  restart and, if the computer's link is down and allowed, reconnects so results can flow. */
    static void resume(Context context) {
        Context app = context.getApplicationContext();
        ChessPcJobs.Record r = ChessPcJobs.current(app);
        if (r == null) return;
        if (ChessPuzzleJobs.snapshot == null) publish(app, r); // only restores a missing card
        if (!DevService.isConnected(r.hostId) && linkAllowed(app, r.hostId)) {
            DevService.connect(app, r.hostId);
        }
    }

    /** One line for the lock dialog, notification and job card. */
    static String label(Context context, ChessPcJobs.Record r) {
        String pc = r.hostLabel != null ? r.hostLabel : "PC";
        if (ChessPcJobs.STATE_FAILED.equals(r.state)) return "puzzles on " + pc + " failed — " + r.error;
        String progress = r.total > 0
                ? String.format(java.util.Locale.US, "%,d of %,d games — %,d found", r.scanned, r.total, r.found)
                : "starting";
        String line = "generating puzzles on " + pc + " — " + progress;
        if (r.cancelRequested) return line + " — stopping";
        if (!DevService.isConnected(r.hostId)) line += " — keeps running on the PC; reconnect to sync";
        return line;
    }

    // --- starting / stopping -------------------------------------------------------------

    interface StartListener {
        void onProgress(String text);                    // "Connecting…", "Installing Stockfish…"
        void onNeedInstall(String pcLabel, Runnable go); // ask first; run `go` if the user agrees
        void onFailed(String reason);
        void onStarted();
    }

    /** Main thread. Connects if needed, makes sure Stockfish is on the PC, then creates the job. */
    static void start(Context context, String hostId, String scope, Map<String, Object> params,
                      int workers, int hashMb, StartListener ui) {
        Context app = context.getApplicationContext();
        DevHost host = DevHost.find(app, hostId);
        if (host == null) { ui.onFailed("That computer isn't paired any more"); return; }
        if (ChessPcJobs.current(app) != null) { ui.onFailed("A puzzle job on a computer is already running"); return; }
        // The Dev lock only guards *opening* a link; one that is already up can be used.
        if (links.get(hostId) == null && !linkAllowed(app, hostId)) { ui.onFailed("Unlock Dev first"); return; }

        if (links.get(hostId) == null) {
            ui.onProgress("Connecting to " + host.label + "…");
            DevService.connect(app, hostId);
            waitForLink(hostId, 12000, conn -> {
                if (conn == null) ui.onFailed("Can't reach " + host.label);
                else checkEngine(app, host, conn, scope, params, workers, hashMb, ui);
            });
        } else {
            checkEngine(app, host, links.get(hostId), scope, params, workers, hashMb, ui);
        }
    }

    private static void waitForLink(String hostId, long timeoutMs, Consumer<DevConnection> cb) {
        long end = android.os.SystemClock.uptimeMillis() + timeoutMs;
        Runnable[] tick = new Runnable[1];
        tick[0] = () -> {
            DevConnection c = links.get(hostId);
            if (c != null) cb.accept(c);
            else if (android.os.SystemClock.uptimeMillis() > end) cb.accept(null);
            else MAIN.postDelayed(tick[0], 250);
        };
        MAIN.post(tick[0]);
    }

    private static void checkEngine(Context app, DevHost host, DevConnection conn, String scope,
                                    Map<String, Object> params, int workers, int hashMb, StartListener ui) {
        List<Object> caps = linkCaps.get(host.id);
        if (caps == null || !caps.contains(DevProtocol.CAP_CHESS)) {
            ui.onFailed("plaind on " + host.label + " is too old for puzzle generation — update it");
            return;
        }
        ui.onProgress("Checking " + host.label + "…");
        query(conn, DevProtocol::chessStatus, DevProtocol.T_CHESS_STATUS, 20000, status -> {
            if (status == null) { ui.onFailed(host.label + " didn't answer"); return; }
            if (DevProtocol.bool(status, "installed")) {
                createJob(app, host, scope, params, workers, hashMb, ui);
            } else {
                ui.onNeedInstall(host.label, () -> install(app, host, conn, scope, params, workers, hashMb, ui));
            }
        });
    }

    private static void install(Context app, DevHost host, DevConnection conn, String scope,
                                Map<String, Object> params, int workers, int hashMb, StartListener ui) {
        ui.onProgress("Installing Stockfish on " + host.label + "…");
        final long[] ch = new long[1];
        boolean[] done = {false};
        ch[0] = conn.openChannel(msg -> {
            String t = DevProtocol.type(msg);
            if (DevProtocol.T_CHESS_INSTALL_PROGRESS.equals(t)) {
                ui.onProgress("Installing Stockfish on " + host.label + " — " + DevProtocol.str(msg, "stage")
                        + " " + DevProtocol.num(msg, "pct", 0) + "%");
            } else if (DevProtocol.T_CHESS_INSTALL_DONE.equals(t) && !done[0]) {
                done[0] = true;
                conn.closeChannel(ch[0]);
                if (DevProtocol.bool(msg, "ok")) createJob(app, host, scope, params, workers, hashMb, ui);
                else ui.onFailed("Couldn't install Stockfish: " + DevProtocol.str(msg, "err"));
            } else if (DevProtocol.T_CHESS_ERROR.equals(t) && !done[0]) {
                done[0] = true;
                conn.closeChannel(ch[0]);
                ui.onFailed(DevProtocol.str(msg, "msg"));
            }
        });
        conn.send(DevProtocol.chessInstall(ch[0]));
    }

    private static void createJob(Context app, DevHost host, String scope, Map<String, Object> params,
                                  int workers, int hashMb, StartListener ui) {
        ui.onProgress("Counting games…");
        IO.execute(() -> {
            int total = countUnscanned(app, scope);
            if (total == 0) { MAIN.post(() -> ui.onFailed("No games left to scan")); return; }
            ChessPcJobs.Record r = new ChessPcJobs.Record();
            r.jobId = UUID.randomUUID().toString();
            r.hostId = host.id;
            r.hostLabel = host.label;
            r.scope = scope;
            r.total = total;
            r.workers = workers;
            r.hashMb = hashMb;
            r.params.putAll(params);
            r.startedAt = System.currentTimeMillis();
            ChessPcJobs.save(app, r);
            publish(app, r);
            DevConnection conn = links.get(host.id);
            if (conn != null) attach(app, host.id, conn);
            MAIN.post(ui::onStarted);
        });
    }

    /** Main thread. Stops the job: asks the PC to cancel now, or as soon as the link is back.
     *  A second tap while a cancel is already pending gives up waiting and forgets the job here
     *  (it keeps its place on the PC until that computer is next reachable). */
    static void cancel(Context context) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            ChessPcJobs.Record r = ChessPcJobs.current(app);
            if (r == null) return;
            boolean live = session != null && r.hostId.equals(sessionHost);
            if (!r.running() || (r.cancelRequested && !live)) { // finished/failed card, or forced discard
                ChessPcJobs.clear(app);
                clearSnapshot();
                return;
            }
            if (live) {
                session.requestCancel();
            } else {
                r.cancelRequested = true;
                ChessPcJobs.save(app, r);
                publish(app, r);
            }
        });
    }

    // --- session wiring (IO thread) -------------------------------------------------------

    private static void attach(Context app, String hostId, DevConnection conn) {
        ChessPcJobs.Record r = ChessPcJobs.current(app);
        if (r == null || !hostId.equals(r.hostId)) return;
        List<Object> caps = linkCaps.get(hostId);
        if (caps == null || !caps.contains(DevProtocol.CAP_CHESS)) return;
        if (!r.running()) return; // finished/failed cards never re-announce the job
        endSession();
        ChessPcSession[] holder = new ChessPcSession[1];
        long ch = conn.openChannel(msg -> IO.execute(() -> {
            if (holder[0] != null && session == holder[0]) holder[0].onMessage(msg);
        }));
        holder[0] = new ChessPcSession(r, new SessionIo(app, conn, ch, hostId));
        session = holder[0];
        sessionHost = hostId;
        sessionConn = conn;
        sessionChannel = ch;
        session.begin();
    }

    private static void endSession() {
        if (sessionConn != null) {
            sessionConn.send(DevProtocol.chessDetach(sessionChannel));
            sessionConn.closeChannel(sessionChannel);
        }
        session = null;
        sessionHost = null;
        sessionConn = null;
    }

    private static final class SessionIo implements ChessPcSession.Io {
        private final Context app;
        private final DevConnection conn;
        private final long ch;
        private final String hostId;

        SessionIo(Context app, DevConnection conn, long ch, String hostId) {
            this.app = app; this.conn = conn; this.ch = ch; this.hostId = hostId;
        }

        @Override public void send(Map<String, Object> message) { conn.send(message); }
        @Override public long channel() { return ch; }

        @Override public boolean appendPuzzle(ChessPuzzles.Puzzle p) {
            try { ChessPuzzles.appendPuzzle(app, p); return true; }
            catch (RuntimeException e) { return false; }
        }

        @Override public void appendScanned(List<String> ids) { ChessPuzzleJobs.appendScannedIds(app, ids); }
        @Override public Set<String> scannedIds() { return ChessPuzzleJobs.loadScannedIds(app); }
        @Override public void saveRecord(ChessPcJobs.Record r) { ChessPcJobs.save(app, r); }
        @Override public void publish(ChessPcJobs.Record r) { ChessPcRunner.publish(app, r); }

        @Override public void uploadGames(ChessPcJobs.Record r) {
            UPLOAD.execute(() -> upload(app, conn, ch, r.jobId, r.scope));
        }

        @Override public void finished(ChessPcJobs.Record r, boolean clearRecord) {
            if (clearRecord) {
                // keep the finished card up briefly, same as the on-phone generator's "done" state
                ChessPuzzleJobs.Snapshot done = snapshotOf(app, r);
                done.done = true;
                ChessPuzzleJobs.publish(done);
                MAIN.postDelayed(() -> IO.execute(() -> {
                    ChessPcJobs.Record now = ChessPcJobs.current(app);
                    if (now != null && now.jobId.equals(r.jobId) && !now.running()) {
                        ChessPcJobs.clear(app);
                        clearSnapshot();
                    }
                }), 2500);
            }
            endSession();
        }
    }

    // --- games upload ------------------------------------------------------------------------

    private static void upload(Context app, DevConnection conn, long ch, String jobId, String scope) {
        Set<String> scanned = ChessPuzzleJobs.loadScannedIds(app);
        List<Object> chunk = new ArrayList<>();
        int[] bytes = {0};
        ChessLibrary.LineCallback cb = (line, entry) -> {
            if (conn.isClosed()) return false;
            if (scanned.contains(entry.id)) return true;
            Map<String, Object> g = ChessPcSession.gameMap(entry);
            chunk.add(g);
            bytes[0] += 160 + entry.sansJoined.length() + 8;
            if (chunk.size() >= 200 || bytes[0] >= DevProtocol.CHESS_GAMES_CHUNK_BYTES) {
                conn.send(DevProtocol.chessGames(ch, jobId, new ArrayList<>(chunk)));
                chunk.clear();
                bytes[0] = 0;
            }
            return true;
        };
        forEachGame(app, scope, cb);
        if (conn.isClosed()) return; // the next link-up re-uploads what the PC is missing
        if (!chunk.isEmpty()) conn.send(DevProtocol.chessGames(ch, jobId, new ArrayList<>(chunk)));
        conn.send(DevProtocol.chessGamesEnd(ch, jobId));
    }

    /** Oldest-first over the whole library or one PGN source — same order the on-phone job uses. */
    private static void forEachGame(Context app, String scope, ChessLibrary.LineCallback cb) {
        if (Config.CHESS_PUZZLEGEN_SCOPE_ALL.equals(scope)) {
            ChessLibrary.scanFrom(app, 0, cb);
        } else {
            List<ChessLibrary.Entry> entries = ChessLibrary.loadBySourceWithSans(app, scope);
            java.util.Collections.reverse(entries);
            int n = 0;
            for (ChessLibrary.Entry e : entries) if (!cb.onEntry(++n, e)) break;
        }
    }

    private static int countUnscanned(Context app, String scope) {
        Set<String> scanned = ChessPuzzleJobs.loadScannedIds(app);
        int[] n = {0};
        forEachGame(app, scope, (line, entry) -> { if (!scanned.contains(entry.id)) n[0]++; return true; });
        return n[0];
    }

    // --- one-shot request/response ---------------------------------------------------------

    /** Main thread. Sends one request and hands the first {@code expect} reply to {@code cb}
     *  (null on timeout). */
    private static void query(DevConnection conn, LongFunction<Map<String, Object>> request,
                              String expect, long timeoutMs, Consumer<Map<String, Object>> cb) {
        final long[] ch = new long[1];
        boolean[] done = {false};
        Runnable timeout = () -> {
            if (done[0]) return;
            done[0] = true;
            conn.closeChannel(ch[0]);
            cb.accept(null);
        };
        ch[0] = conn.openChannel(msg -> {
            if (done[0]) return;
            if (expect.equals(DevProtocol.type(msg)) || DevProtocol.T_CHESS_ERROR.equals(DevProtocol.type(msg))) {
                done[0] = true;
                MAIN.removeCallbacks(timeout);
                conn.closeChannel(ch[0]);
                cb.accept(msg);
            }
        });
        MAIN.postDelayed(timeout, timeoutMs);
        conn.send(request.apply(ch[0]));
    }

    // --- UI snapshot ---------------------------------------------------------------------------

    private static ChessPuzzleJobs.Snapshot snapshotOf(Context app, ChessPcJobs.Record r) {
        ChessPuzzleJobs.Snapshot s = new ChessPuzzleJobs.Snapshot();
        s.scope = r.scope;
        s.gamesScanned = r.scanned;
        s.gamesTotal = r.total;
        s.puzzlesFound = r.found - r.dropped < 0 ? 0 : r.found - r.dropped;
        s.onPc = true;
        s.pcLabel = r.hostLabel;
        s.connected = DevService.isConnected(r.hostId);
        s.etaSeconds = r.etaSeconds;
        s.error = ChessPcJobs.STATE_FAILED.equals(r.state) ? r.error : null;
        s.stopping = r.cancelRequested;
        return s;
    }

    static void publish(Context app, ChessPcJobs.Record r) {
        ChessPuzzleJobs.publish(snapshotOf(app, r));
    }

    private static void clearSnapshot() {
        ChessPuzzleJobs.clearSnapshot();
    }
}
