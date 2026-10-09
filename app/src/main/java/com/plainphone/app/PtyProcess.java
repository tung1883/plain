package com.plainphone.app;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A program running on a pseudo terminal. Java cannot create a pty, so the program is {@code pman}
 * itself, started as {@code libpman.so --pty-host COLS ROWS args...}: it forks the real program on a
 * pty and relays it over stdin/stdout. Input goes in as frames (type byte, big-endian u16 length,
 * payload; type 0 = keyboard bytes, 1 = resize with cols and rows as u16s); output comes back raw.
 * {@code libpman.so} is an executable shipped in {@code jniLibs} (Android runs those from the
 * native library dir, but not files downloaded into app data).
 */
final class PtyProcess {

    interface Listener {
        /** Called on a background thread; {@code data} is not reused. */
        void onOutput(byte[] data);

        void onExit(int code);
    }

    private final Process process;
    private final OutputStream stdin;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();

    static File binary(Context ctx) {
        return new File(ctx.getApplicationInfo().nativeLibraryDir, "libpman.so");
    }

    static boolean available(Context ctx) {
        return binary(ctx).canExecute();
    }

    PtyProcess(Context ctx, int cols, int rows, String[] args, Listener l) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(binary(ctx).getPath());
        cmd.add("--pty-host");
        cmd.add(String.valueOf(cols));
        cmd.add(String.valueOf(rows));
        for (String a : args) cmd.add(a);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("PMAN_HOME", new File(ctx.getFilesDir(), "pman").getPath());
        pb.environment().put("HOME", ctx.getFilesDir().getPath());
        pb.environment().put("PMAN_HOST", "android");
        File registry = new File(ctx.getFilesDir(), "pman/registry.json");
        if (registry.isFile()) pb.environment().put("PMAN_REGISTRY", registry.getPath());
        process = pb.start();
        stdin = process.getOutputStream();
        new Thread(() -> {
            byte[] buf = new byte[8192];
            try (InputStream in = process.getInputStream()) {
                int n;
                while ((n = in.read(buf)) != -1) {
                    byte[] copy = new byte[n];
                    System.arraycopy(buf, 0, copy, 0, n);
                    l.onOutput(copy);
                }
            } catch (IOException ignored) {
                // the process went away
            }
            int code;
            try {
                code = process.waitFor();
            } catch (InterruptedException e) {
                code = 1;
            }
            l.onExit(code);
        }, "pty-out").start();
        new Thread(() -> { // keep stderr drained so the host can never block on it
            byte[] buf = new byte[1024];
            try (InputStream in = process.getErrorStream()) {
                while (in.read(buf) != -1) { /* discard */ }
            } catch (IOException ignored) {
                // done
            }
        }, "pty-err").start();
    }

    void write(byte[] data) {
        frame(0, data);
    }

    void resize(int cols, int rows) {
        frame(1, new byte[]{(byte) (cols >> 8), (byte) cols, (byte) (rows >> 8), (byte) rows});
    }

    private void frame(int type, byte[] payload) {
        final byte[] f = new byte[3 + payload.length];
        f[0] = (byte) type;
        f[1] = (byte) (payload.length >> 8);
        f[2] = (byte) payload.length;
        System.arraycopy(payload, 0, f, 3, payload.length);
        writer.execute(() -> {
            try {
                stdin.write(f);
                stdin.flush();
            } catch (IOException ignored) {
                // process is gone
            }
        });
    }

    /** Closing stdin makes the host hang up the program. */
    void close() {
        writer.execute(() -> {
            try {
                stdin.close();
            } catch (IOException ignored) {
                // already closed
            }
        });
        writer.shutdown();
        process.destroy();
    }
}
