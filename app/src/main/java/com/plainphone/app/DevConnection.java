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
 * keeps the link warm with a ping every {@link #PING_MS}. Frames that carry a
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
    private static final long PING_MS = 15000;

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

    private final LinkedBlockingQueue<Map<String, Object>> outbox = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<Long, Sink> channels = new ConcurrentHashMap<>();
    private final AtomicLong nextChannel = new AtomicLong(1);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private Socket socket;
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
        if (!closed.get()) outbox.offer(message);
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
        if (DevProtocol.T_PONG.equals(type)) return;
        Object ch = frame.get("ch");
        if (ch instanceof Number) {
            Sink sink = channels.get(((Number) ch).longValue());
            if (sink != null) {
                main.post(() -> sink.onMessage(frame));
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
            while (!closed.get()) {
                Map<String, Object> message = outbox.poll(PING_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (message == null) message = DevProtocol.ping();
                DevProtocol.writeFrame(out, message);
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
