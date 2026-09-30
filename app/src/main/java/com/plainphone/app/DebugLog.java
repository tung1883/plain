package com.plainphone.app;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * TEMPORARY: a small on-phone log of the blocking-app / accessibility hand-offs, so a bug that
 * is hard to reproduce can be read afterwards (adb run-as ... cat files/plain_debug.log).
 * Remove once the PIN-to-home-screen bug is understood.
 */
final class DebugLog {

    private static final long MAX_BYTES = 300_000L;
    private static File file;
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private DebugLog() {}

    static synchronized void init(Context context) {
        file = new File(context.getApplicationContext().getFilesDir(), "plain_debug.log");
    }

    static synchronized void i(String message) {
        if (file == null) return;
        try {
            if (file.length() > MAX_BYTES) {
                File old = new File(file.getPath() + ".old");
                old.delete();
                file.renameTo(old);
            }
            try (FileWriter out = new FileWriter(file, true)) {
                out.write(TIME.format(new Date()) + " " + message + "\n");
            }
        } catch (IOException ignored) {
            // it is only a debug aid
        }
    }

    static String caller() {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        StringBuilder out = new StringBuilder();
        for (int i = 2; i < Math.min(stack.length, 7); i++) {
            out.append(stack[i].getClassName().replace("com.plainphone.app.", ""))
                    .append('.').append(stack[i].getMethodName()).append(' ');
        }
        return out.toString();
    }
}
