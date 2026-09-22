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
 * <p>One instance owns one channel for the lifetime of a sync run — every
 * request/response pair on it is strictly sequential (the job never has two
 * outstanding daemon requests at once), so a plain queue is enough; nothing
 * here needs to demultiplex by message type across concurrent calls.
 */
final class DevSyncClient implements DevConnection.Sink {

    private static final long DEFAULT_TIMEOUT_MS = 20_000;
    private static final long HASH_LIST_TIMEOUT_MS = 180_000; // checksum listing can hash a lot

    private final DevConnection connection;
    private final long channel;
    private final LinkedBlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();

    DevSyncClient(DevConnection connection) {
        this.connection = connection;
        this.channel = connection.openChannel(this);
    }

    @Override
    public void onMessage(Map<String, Object> message) {
        queue.offer(message);
    }

    void close() {
        connection.closeChannel(channel);
    }

    private Map<String, Object> await(long timeoutMs) throws IOException {
        try {
            Map<String, Object> m = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
            if (m == null) throw new IOException("timed out waiting for the daemon");
            return m;
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

    List<DirEntry> fsList(String path) throws IOException {
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
        return out;
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

    List<RemoteEntry> syncList(String root, boolean hash) throws IOException {
        connection.send(DevProtocol.syncList(channel, root, hash));
        Map<String, Object> reply = await(hash ? HASH_LIST_TIMEOUT_MS : DEFAULT_TIMEOUT_MS);
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
        while (true) {
            Map<String, Object> m = await(60_000);
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
