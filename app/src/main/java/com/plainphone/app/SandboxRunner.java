package com.plainphone.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Compiles and runs one C program with TinyCC ({@code jniLibs/arm64-v8a/libtcc.so}, executed
 * from {@code nativeLibraryDir} like the bundled Stockfish). Compile and run share one process
 * with a wall-clock timeout and an output cap, so a runaway program can't hang or fill the app.
 * Only for code the user wrote: no memory cap, since the linker needs a large address range.
 */
final class SandboxRunner {

    static final class Result {
        final boolean ok;
        final String output;

        Result(boolean ok, String output) {
            this.ok = ok;
            this.output = output;
        }
    }

    /** Compile + run together, wall clock. */
    private static final long TIMEOUT_MS = 15_000;
    private static final int OUTPUT_CAP = 64 * 1024;

    /** Languages the sandbox can run. Each one is a native binary packaged as jniLibs. */
    enum Lang {
        C("main.c", "C", "text/x-csrc",
                "#include <stdio.h>\n\nint main(void) {\n    printf(\"hello from sandbox\\n\");\n    return 0;\n}\n"),
        JS("main.js", "JS", "text/javascript", "console.log(\"hello from sandbox\");\n"),
        TS("main.ts", "TS", "text/plain", "const greet = (name: string): string => `hello ${name}`;\nconsole.log(greet(\"sandbox\"));\n"),
        PY("main.py", "PY", "text/x-python", "print(\"hello from sandbox\")\n");

        final String fileName;
        final String label;
        final String mime;
        final String sample;

        Lang(String fileName, String label, String mime, String sample) {
            this.fileName = fileName;
            this.label = label;
            this.mime = mime;
            this.sample = sample;
        }
    }

    private SandboxRunner() {}

    /**
     * Static checks only; nothing runs. C: tcc -Wall (compile to a scratch object). Python: pyflakes.
     * JS/TS: syntax check. Blocking; call off the main thread.
     */
    static Result lint(Context ctx, Lang lang, String source) {
        try {
            File work = new File(ctx.getFilesDir(), "sandbox/work");
            deleteRecursively(work);
            if (!work.mkdirs()) return new Result(false, "cannot create " + work);
            File src = new File(work, lang.fileName);
            write(src, source);

            if (lang == Lang.PY) return runPython(ctx, "lint_file", src.getPath());

            String nativeDir = ctx.getApplicationInfo().nativeLibraryDir;
            List<String> cmd = new ArrayList<>();
            cmd.add("sh"); cmd.add("-c");
            cmd.add("exec \"$0\" \"$@\"");
            if (lang == Lang.C) {
                File tool = toolDir(ctx);
                if (!PackStore.installed(ctx, PackStore.C)) return new Result(false, "C pack not installed");
                File lib = new File(tool, "lib");
                File sysInc = new File(tool, "sysinc/include");
                cmd.add(nativeDir + "/libtcc.so");
                cmd.add("-Wall");
                cmd.add("-B"); cmd.add(lib.getPath());
                cmd.add("-I"); cmd.add(new File(tool, "tccinc").getPath());
                cmd.add("-I"); cmd.add(sysInc.getPath());
                cmd.add("-I"); cmd.add(new File(sysInc, "aarch64-linux-android").getPath());
                cmd.add("-c");
                cmd.add(src.getPath());
                cmd.add("-o"); cmd.add(new File(work, "lint.o").getPath());
                Result r = exec(cmd, work, TIMEOUT_MS, "lint");
                // tcc prints warnings to stderr and still exits 0, so judge by the text too.
                boolean clean = r.ok && !r.output.contains("warning") && !r.output.contains("error");
                return new Result(clean, clean ? "no issues" : r.output.trim());
            }

            // JS / TS: the acorn (JS) or sucrase (TS) parser from the js pack, run by QuickJS.
            if (!PackStore.installed(ctx, PackStore.JS)) return new Result(false, "JS pack not installed");
            File checker = new File(PackStore.dir(ctx, PackStore.JS), "lint-js.js");
            cmd.add(nativeDir + "/libqjs.so");
            cmd.add("-m");
            cmd.add(checker.getPath());
            cmd.add(src.getPath());
            cmd.add(lang == Lang.TS ? "ts" : "js");
            return exec(cmd, work, TIMEOUT_MS, "lint");
        } catch (Exception e) {
            return new Result(false, "lint error: " + e);
        }
    }

    /** Blocking; call off the main thread. */
    static Result run(Context ctx, Lang lang, String source) {
        try {
            File work = new File(ctx.getFilesDir(), "sandbox/work");
            deleteRecursively(work);
            if (!work.mkdirs()) return new Result(false, "cannot create " + work);
            File src = new File(work, lang.fileName);
            write(src, source);

            if (lang == Lang.PY) return runPython(ctx, "run_file", src.getPath());

            // `exec` keeps one pid so a timeout kills everything. No `ulimit -v`: the linker's CFI
            // shadow needs a huge address reservation, and a virtual-memory cap makes it abort.
            List<String> cmd = new ArrayList<>();
            cmd.add("sh"); cmd.add("-c");
            cmd.add("exec \"$0\" \"$@\"");

            // Android 10+ forbids apps from exec'ing files in their data dir (targetSdk 29+), so
            // nothing is written out as a binary. Each runtime is packaged in nativeLibraryDir,
            // which is allowed to exec, and runs the script itself.
            String nativeDir = ctx.getApplicationInfo().nativeLibraryDir;
            if (lang == Lang.C) {
                File tool = toolDir(ctx);
                if (!PackStore.installed(ctx, PackStore.C)) return new Result(false, "C pack not installed");
                File lib = new File(tool, "lib");
                File sysInc = new File(tool, "sysinc/include");
                cmd.add(nativeDir + "/libtcc.so");
                cmd.add("-B"); cmd.add(lib.getPath());
                cmd.add("-I"); cmd.add(new File(tool, "tccinc").getPath());
                cmd.add("-I"); cmd.add(sysInc.getPath());
                cmd.add("-I"); cmd.add(new File(sysInc, "aarch64-linux-android").getPath());
                cmd.add("-run");
                cmd.add(src.getPath());
            } else if (lang == Lang.JS) {
                cmd.add(nativeDir + "/libqjs.so");
                cmd.add(src.getPath());
            } else {
                // TypeScript: QuickJS runs a bundled type-stripper (sucrase) that evals the result.
                if (!PackStore.installed(ctx, PackStore.JS)) return new Result(false, "JS pack not installed");
                File runner = new File(PackStore.dir(ctx, PackStore.JS), "ts-run.js");
                cmd.add(nativeDir + "/libqjs.so");
                cmd.add("-m");
                cmd.add(runner.getPath());
                cmd.add(src.getPath());
            }

            return exec(cmd, work, TIMEOUT_MS, "run");
        } catch (Exception e) {
            return new Result(false, "sandbox error: " + e);
        }
    }

    /**
     * Python runs in {@link SandboxPyService}'s own process. The app waits for the service's pid
     * (HELLO) and the result (RUN). On timeout it kills that process, which stops a loop that no
     * thread could stop.
     */
    private static Result runPython(Context ctx, String fn, String path) {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch hello = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        Messenger[] service = new Messenger[1];
        int[] pid = {-1};
        Bundle[] result = new Bundle[1];

        Handler replies = new Handler(Looper.getMainLooper(), msg -> {
            if (msg.what == SandboxPyService.MSG_HELLO) {
                pid[0] = msg.getData().getInt(SandboxPyService.KEY_PID);
                hello.countDown();
            } else if (msg.what == SandboxPyService.MSG_RESULT) {
                result[0] = msg.getData();
                finished.countDown();
            }
            return true;
        });
        Messenger replyTo = new Messenger(replies);

        ServiceConnection conn = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                service[0] = new Messenger(binder);
                connected.countDown();
            }

            @Override public void onServiceDisconnected(ComponentName name) {
            }
        };

        if (!ctx.bindService(new Intent(ctx, SandboxPyService.class), conn, Context.BIND_AUTO_CREATE)) {
            return new Result(false, "cannot start the Python service");
        }
        try {
            if (!connected.await(10, TimeUnit.SECONDS)) return new Result(false, "Python did not start");
            Message hi = Message.obtain(null, SandboxPyService.MSG_HELLO);
            hi.replyTo = replyTo;
            service[0].send(hi);
            if (!hello.await(10, TimeUnit.SECONDS)) return new Result(false, "Python did not start");

            Bundle args = new Bundle();
            args.putString(SandboxPyService.KEY_FN, fn);
            args.putString(SandboxPyService.KEY_PATH, path);
            Message run = Message.obtain(null, SandboxPyService.MSG_RUN);
            run.setData(args);
            run.replyTo = replyTo;
            service[0].send(run);

            if (!finished.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                android.os.Process.killProcess(pid[0]);
                return new Result(false, "[run timed out after " + TIMEOUT_MS / 1000 + " s; process stopped]");
            }
            Bundle r = result[0];
            String out = r.getString(SandboxPyService.KEY_OUT, "");
            boolean ok = r.getBoolean(SandboxPyService.KEY_OK);
            if (out.length() > OUTPUT_CAP) out = out.substring(0, OUTPUT_CAP) + "\n[output truncated]";
            String tail = "[exit " + (ok ? 0 : 1) + "]";
            return new Result(ok, out.isEmpty() || out.endsWith("\n") ? out + tail : out + "\n" + tail);
        } catch (RemoteException | InterruptedException e) {
            return new Result(false, "sandbox error: " + e);
        } finally {
            try {
                ctx.unbindService(conn);
            } catch (IllegalArgumentException ignored) {
                // already unbound
            }
        }
    }

    private static Result exec(List<String> cmd, File dir, long timeoutMs, String what) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).directory(dir).redirectErrorStream(true).start();
        try {
            p.getOutputStream().close(); // no stdin
        } catch (IOException ignored) {
        }
        StringBuilder out = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (InputStream in = p.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (out.length() < OUTPUT_CAP) {
                        out.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                        if (out.length() >= OUTPUT_CAP) out.append("\n[output truncated]");
                    }
                    // past the cap: keep reading and drop, so the child never blocks on a full pipe
                }
            } catch (IOException ignored) {
            }
        });
        reader.start();

        boolean done = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (!done) p.destroyForcibly();
        p.waitFor();
        reader.join(1000);

        if (!done) return new Result(false, out + "\n[" + what + " timed out after " + timeoutMs / 1000 + " s]");
        int code = p.exitValue();
        String text = out.toString();
        if (what.equals("run")) text += (text.endsWith("\n") || text.isEmpty() ? "" : "\n") + "[exit " + code + "]";
        return new Result(code == 0, text);
    }

    /** Compiler headers and runtime from the C pack. */
    private static File toolDir(Context ctx) {
        return new File(PackStore.dir(ctx, PackStore.C), "tcc");
    }

    static void write(File f, String text) throws IOException {
        File parent = f.getParentFile();
        if (parent != null) parent.mkdirs();
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursively(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
