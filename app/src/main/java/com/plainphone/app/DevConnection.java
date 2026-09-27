package com.plainphone.app;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One live TCP link to a {@code plaind} daemon: a reader thread that parses
 * frames off the wire and a writer thread that drains an outbound queue and
 * keeps the link warm (and measures RTT) with a ping every {@link #PROBE_MS}. Frames that carry a
 * {@code ch} are routed to that channel's {@link Sink}; everything else
 * (welcome / error / pong) goes to the {@link Listener}. All callbacks land on
 * the main thread.
 *
 * <p>MVP auth: the pairing token is sent in the {@code hello} in clear — the
 * transport is trusted (Tailscale / LAN). See the plan for the Noise upgrade.
 */
final class DevConnection {

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 20000;
    /** Latency probe: a ping at most this often, one outstanding at a time. */
    private static final long PROBE_MS = 1000;

    interface Listener {
        void onConnected(String host, String os, List<Object> caps);
        void onDisconnected(String reason);
    }

    interface Sink {
        void onMessage(Map<String, Object> message);
    }

    private final String host;
    private final int port;
    private final String token;
    private final String device;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());

    /** An outbound message plus when it was queued (for the queue-wait metric). */
    private static final class Queued {
        final Map<String, Object> message;
        final long at;

        Queued(Map<String, Object> message) {
            this.message = message;
            this.at = android.os.SystemClock.uptimeMillis();
        }
    }

    private final LinkedBlockingQueue<Queued> outbox = new LinkedBlockingQueue<>();
    /** uptimeMillis the outstanding probe ping was written, 0 = none. */
    private final AtomicLong pingSentAt = new AtomicLong();
    private final ConcurrentHashMap<Long, Sink> channels = new ConcurrentHashMap<>();
    private final AtomicLong nextChannel = new AtomicLong(1);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private Socket socket;
    private volatile List<Object> caps = java.util.Collections.emptyList();
    /** Smoothed probe-ping round trip (ms), -1 until the first pong. */
    private volatile double srttMs = -1;

    boolean hasCap(String cap) {
        return caps.contains(cap);
    }

    double srttMs() {
        return srttMs;
    }
    private Thread reader;
    private Thread writer;

    DevConnection(String host, int port, String token, String device, Listener listener) {
        this.host = host;
        this.port = port;
        this.token = token;
        this.device = device;
        this.listener = listener;
    }

    void start() {
        reader = new Thread(this::runReader, "dev-conn-reader");
        reader.start();
    }

    void close() {
        close("closed");
    }

    boolean isClosed() {
        return closed.get();
    }

    // --- channels --------------------------------------------------------

    long openChannel(Sink sink) {
        long ch = nextChannel.getAndIncrement();
        channels.put(ch, sink);
        return ch;
    }

    void closeChannel(long ch) {
        channels.remove(ch);
    }

    void send(Map<String, Object> message) {
        if (!closed.get()) outbox.offer(new Queued(message));
    }

    // --- reader ---------------------------------------------------------

    private void runReader() {
        InputStream in;
        OutputStream rawOut;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            in = new BufferedInputStream(socket.getInputStream());
            rawOut = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            fail("can't reach " + host + ": " + e.getMessage());
            return;
        }

        try {
            DevProtocol.writeFrame(rawOut, DevProtocol.hello(token, device));
            Map<String, Object> welcome = DevProtocol.readFrame(in);
            if (welcome == null) {
                fail("closed during handshake");
                return;
            }
            String type = DevProtocol.type(welcome);
            if (DevProtocol.T_ERROR.equals(type)) {
                fail(DevProtocol.str(welcome, "msg") != null
                        ? DevProtocol.str(welcome, "msg") : "rejected");
                return;
            }
            if (!DevProtocol.T_WELCOME.equals(type)) {
                fail("unexpected handshake reply: " + type);
                return;
            }
            long proto = DevProtocol.num(welcome, "proto", DevProtocol.PROTO);
            if (proto != DevProtocol.PROTO) {
                fail("daemon speaks protocol " + proto + ", app speaks " + DevProtocol.PROTO);
                return;
            }
            String daemonHost = DevProtocol.str(welcome, "host");
            String os = DevProtocol.str(welcome, "os");
            List<Object> caps = DevProtocol.list(welcome, "caps");
            this.caps = caps != null ? caps : java.util.Collections.emptyList();
            main.post(() -> listener.onConnected(daemonHost, os, caps));

            writer = new Thread(() -> runWriter(rawOut), "dev-conn-writer");
            writer.start();

            while (!closed.get()) {
                Map<String, Object> frame;
                try {
                    frame = DevProtocol.readFrame(in);
                } catch (SocketTimeoutException timeout) {
                    fail("connection timed out");
                    return;
                }
                if (frame == null) {
                    fail("connection closed");
                    return;
                }
                dispatch(frame);
            }
        } catch (IOException e) {
            fail(e.getMessage() == null ? "connection error" : e.getMessage());
        }
    }

    private void dispatch(Map<String, Object> frame) {
        String type = DevProtocol.type(frame);
        if (DevProtocol.T_PONG.equals(type)) {
            long sent = pingSentAt.getAndSet(0);
            // round trip: phone socket -> daemon frame loop -> daemon send queue -> phone
            if (sent > 0) {
                long rtt = android.os.SystemClock.uptimeMillis() - sent;
                LatencyStats.record("net.rtt", rtt);
                srttMs = srttMs < 0 ? rtt : srttMs * 0.875 + rtt * 0.125; // TCP-style EWMA
            }
            return;
        }
        Object ch = frame.get("ch");
        if (ch instanceof Number) {
            Sink sink = channels.get(((Number) ch).longValue());
            if (sink != null) {
                long readAt = android.os.SystemClock.uptimeMillis();
                main.post(() -> {
                    // time the frame waited for the main thread
                    LatencyStats.record("phone.main_lag", android.os.SystemClock.uptimeMillis() - readAt);
                    sink.onMessage(frame);
                });
                return;
            }
        }
        if (DevProtocol.T_ERROR.equals(type)) {
            fail(DevProtocol.str(frame, "msg") != null ? DevProtocol.str(frame, "msg") : "error");
        }
        // otherwise an unrouted control frame — ignored in MVP.
    }

    // --- writer ---------------------------------------------------------

    private void runWriter(OutputStream out) {
        try {
            long lastPing = 0;
            while (!closed.get()) {
                Queued q = outbox.poll(PROBE_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                long now = android.os.SystemClock.uptimeMillis();
                if (q != null) {
                    LatencyStats.record("phone.send_queue", now - q.at);
                    DevProtocol.writeFrame(out, q.message);
                    LatencyStats.record("phone.write", android.os.SystemClock.uptimeMillis() - now);
                }
                // Probe ping (doubles as keepalive): one outstanding at a time;
                // a pong lost for 5 s frees the slot.
                long sent = pingSentAt.get();
                if (now - lastPing >= PROBE_MS && (sent == 0 || now - sent > 5000)) {
                    lastPing = now;
                    pingSentAt.set(android.os.SystemClock.uptimeMillis());
                    DevProtocol.writeFrame(out, DevProtocol.ping());
                }
            }
        } catch (IOException | InterruptedException e) {
            fail("write failed");
        }
    }

    // --- teardown ------------------------------------------------------

    private void fail(String reason) {
        close(reason);
    }

    private void close(String reason) {
        if (!closed.compareAndSet(false, true)) return;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        if (writer != null) writer.interrupt();
        channels.clear();
        main.post(() -> listener.onDisconnected(reason));
    }
}
