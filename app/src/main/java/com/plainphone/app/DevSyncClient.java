package com.plainphone.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A synchronous, blocking wrapper around one {@link DevConnection} channel,
 * for use from a background job thread (the sync job runs on its own
 * {@code Thread}, same as every other {@code JobService} worker — never the
 * main thread). {@link DevConnection}'s callbacks always land on the main
 * looper, so replies are bridged back to the calling thread through a queue:
 * {@code onMessage} (main thread) offers, each blocking call here (job
 * thread) polls.
 *
 * <p>One instance owns one channel at a time — every request/response pair
 * on it is strictly sequential (the job never has two outstanding daemon
 * requests at once), so a plain queue is enough; nothing here needs to
 * demultiplex by message type across concurrent calls.
 *
 * <p>If the link drops mid-run, waits fail with {@link LinkLost} as soon as
 * the connection closes (not after a full timeout), and {@link #reconnect}
 * moves the client onto the fresh connection {@link DevService} opens —
 * without that, every later request of the run would sit on the dead
 * connection until it timed out.
 */
final class DevSyncClient implements DevConnection.Sink {

    private static final long DEFAULT_TIMEOUT_MS = 20_000;
    /** Plain listing: just a directory walk, but a big tree on a slow disk can take a while. */
    private static final long LIST_TIMEOUT_MS = 120_000;
    /** Per file while hashing: a single multi-GB file on a slow disk. */
    private static final long HASH_FILE_TIMEOUT_MS = 180_000;

    /** How long {@link #reconnect} waits for the service to bring the link back. */
    private static final long RECONNECT_WAIT_MS = 45_000;

    /** The link to the daemon closed under a request. */
    static final class LinkLost extends IOException {
        LinkLost() { super("lost the connection to the PC"); }
    }

    /** Supplies the host's current live connection (null while there is none). */
    interface Link { DevConnection current(); }

    private final Link link;
    private volatile DevConnection connection;
    private volatile long channel;
    private final LinkedBlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();

    DevSyncClient(DevConnection connection) {
        this(connection, null);
    }

    /** @param link where to find a replacement connection after a drop; null = no reconnecting */
    DevSyncClient(DevConnection connection, Link link) {
        this.link = link;
        attach(connection);
    }

    private void attach(DevConnection c) {
        connection = c;
        queue.clear(); // anything still queued belongs to the old channel
        channel = c.openChannel(this);
    }

    @Override
    public void onMessage(Map<String, Object> message) {
        queue.offer(message);
    }

    void close() {
        connection.closeChannel(channel);
    }

    /** After a {@link LinkLost}: wait for the service's fresh connection and move
     *  onto it. Returns false if none came back in time. */
    boolean reconnect() {
        if (link == null) return false;
        DevConnection old = connection;
        old.closeChannel(channel);
        long deadline = android.os.SystemClock.uptimeMillis() + RECONNECT_WAIT_MS;
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            DevConnection c = link.current();
            if (c != null && c != old && !c.isClosed()) {
                attach(c);
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /** Next reply, checking twice a second that the link is still up — a dead
     *  connection fails at once instead of after the whole timeout. */
    private Map<String, Object> await(long timeoutMs) throws IOException {
        long deadline = android.os.SystemClock.uptimeMillis() + timeoutMs;
        try {
            while (true) {
                long left = deadline - android.os.SystemClock.uptimeMillis();
                if (left <= 0) throw new IOException("timed out waiting for the daemon");
                Map<String, Object> m = queue.poll(Math.min(left, 500), TimeUnit.MILLISECONDS);
                if (m != null) return m;
                if (connection.isClosed()) throw new LinkLost();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        }
    }

    // --- directory browsing (wizard) --------------------------------------

    static final class DirEntry {
        final String name;
        final boolean isDir;
        DirEntry(String name, boolean isDir) { this.name = name; this.isDir = isDir; }
    }

    /** One {@code fs.list} reply: the absolute path actually listed (an empty request
     *  path resolves to the daemon's home directory — see {@code plaind}'s {@code fs_list})
     *  plus its immediate children. */
    static final class DirListing {
        final String path;
        final List<DirEntry> entries;
        DirListing(String path, List<DirEntry> entries) { this.path = path; this.entries = entries; }
    }

    DirListing fsList(String path) throws IOException {
        connection.send(DevProtocol.fsList(channel, path));
        Map<String, Object> reply = await(DEFAULT_TIMEOUT_MS);
        List<DirEntry> out = new ArrayList<>();
        List<Object> raw = DevProtocol.list(reply, "entries");
        if (raw != null) {
            for (Object o : raw) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) o;
                out.add(new DirEntry(DevProtocol.str(m, "name"), DevProtocol.bool(m, "is_dir")));
            }
        }
        return new DirListing(DevProtocol.str(reply, "path"), out);
    }

    // --- listing (job) -----------------------------------------------------

    static final class RemoteEntry {
        final String path;
        final long size;
        final long mtimeMs;
        final String sha256; // null unless checksum mode was requested
        RemoteEntry(String path, long size, long mtimeMs, String sha256) {
            this.path = path;
            this.size = size;
            this.mtimeMs = mtimeMs;
            this.sha256 = sha256;
        }
    }

    /** Sizes and modified times only; content hashes come separately, and only
     *  for the files that need them ({@link #hashRemote}). */
    List<RemoteEntry> syncList(String root) throws IOException {
        connection.send(DevProtocol.syncList(channel, root, false));
        Map<String, Object> reply = await(LIST_TIMEOUT_MS);
        List<RemoteEntry> out = new ArrayList<>();
        List<Object> raw = DevProtocol.list(reply, "entries");
        if (raw != null) {
            for (Object o : raw) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) o;
                out.add(new RemoteEntry(DevProtocol.str(m, "path"), DevProtocol.num(m, "size", 0),
                        DevProtocol.num(m, "mtime_ms", 0), DevProtocol.str(m, "sha256")));
            }
        }
        return out;
    }

    interface HashSink {
        void onHash(String relPath, String sha256);
    }

    /** SHA-256 of each of {@code relPaths} under {@code root}, reported per file as
     *  the daemon finishes it ({@code sha256} is null if it couldn't read the file). */
    void hashRemote(String root, List<String> relPaths, HashSink sink) throws IOException {
        connection.send(DevProtocol.syncHash(channel, root, relPaths));
        while (true) {
            Map<String, Object> m = await(HASH_FILE_TIMEOUT_MS);
            String t = DevProtocol.type(m);
            if (DevProtocol.T_SYNC_HASH_END.equals(t)) return;
            if (DevProtocol.T_SYNC_HASH.equals(t)) {
                sink.onHash(DevProtocol.str(m, "path"), DevProtocol.str(m, "sha256"));
            }
        }
    }

    // --- upload: phone -> daemon --------------------------------------------

    /** Bytes of the daemon's own {@code <path>.partial} already on disk — resume from here. */
    long putBegin(String path, long size, long mtimeMs) throws IOException {
        connection.send(DevProtocol.syncPutBegin(channel, path, size, mtimeMs));
        Map<String, Object> reply = await(DEFAULT_TIMEOUT_MS);
        return DevProtocol.num(reply, "resume_offset", 0);
    }

    void putChunk(long offset, byte[] data) {
        connection.send(DevProtocol.syncPutChunk(channel, offset, data));
    }

    boolean putEnd() throws IOException {
        connection.send(DevProtocol.syncPutEnd(channel));
        Map<String, Object> reply = await(DEFAULT_TIMEOUT_MS);
        return DevProtocol.bool(reply, "ok");
    }

    /** Mirror cleanup only — the caller checks {@code pair.deletesPropagate()} first. */
    boolean deleteRemote(String path) throws IOException {
        connection.send(DevProtocol.syncDelete(channel, path));
        Map<String, Object> reply = await(DEFAULT_TIMEOUT_MS);
        return DevProtocol.bool(reply, "ok");
    }

    // --- download: daemon -> phone -------------------------------------------

    interface ChunkSink {
        void onChunk(long offset, byte[] data) throws IOException;
    }

    /** Streams {@code path} from {@code resumeOffset} onward, handing each chunk to
     *  {@code sink} in order. Throws if the daemon reports the download failed. */
    void getFile(String path, long resumeOffset, ChunkSink sink) throws IOException {
        connection.send(DevProtocol.syncGetBegin(channel, path, resumeOffset));
        long asked = android.os.SystemClock.uptimeMillis();
        boolean first = true;
        while (true) {
            Map<String, Object> m = await(60_000);
            if (first) {
                first = false;
                // idle round trip before a download starts moving
                LatencyStats.record("sync.get_first_byte", android.os.SystemClock.uptimeMillis() - asked);
            }
            String t = DevProtocol.type(m);
            if (DevProtocol.T_SYNC_GET_META.equals(t)) {
                continue; // size/mtime already known from the earlier sync.list
            } else if (DevProtocol.T_SYNC_GET_CHUNK.equals(t)) {
                byte[] data = DevProtocol.bin(m, "data");
                if (data != null) sink.onChunk(DevProtocol.num(m, "offset", 0), data);
            } else if (DevProtocol.T_SYNC_GET_END.equals(t)) {
                if (!DevProtocol.bool(m, "ok")) throw new IOException("download failed: " + path);
                return;
            }
        }
    }
}
