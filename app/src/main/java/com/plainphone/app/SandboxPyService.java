package com.plainphone.app;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

/**
 * Runs one Python script at a time for Dev → Sandbox. Declared with {@code android:process=":sandbox_py"},
 * so it lives in its own process: the app kills that process on timeout, which a thread can't do.
 * Talks to the app over a {@link Messenger}: HELLO (reports the pid), RUN (path in, output out).
 */
public class SandboxPyService extends Service {

    static final int MSG_HELLO = 1;
    static final int MSG_RUN = 2;
    static final int MSG_RESULT = 3;
    static final String KEY_PATH = "path";
    static final String KEY_FN = "fn";
    static final String KEY_OK = "ok";
    static final String KEY_OUT = "out";
    static final String KEY_PID = "pid";

    private final Messenger messenger = new Messenger(new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_HELLO) {
            reply(msg, MSG_HELLO, bundleWithPid());
        } else if (msg.what == MSG_RUN) {
            reply(msg, MSG_RESULT, runScript(msg.getData().getString(KEY_FN, "run_file"),
                    msg.getData().getString(KEY_PATH)));
        }
        return true;
    }));

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Python.isStarted()) Python.start(new AndroidPlatform(this));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    private Bundle bundleWithPid() {
        Bundle b = new Bundle();
        b.putInt(KEY_PID, Process.myPid());
        return b;
    }

    /** {@code fn} is {@code run_file} (execute) or {@code lint_file} (static check only). */
    private Bundle runScript(String fn, String path) {
        Bundle b = new Bundle();
        try {
            PyObject res = Python.getInstance().getModule("sandbox_runner").callAttr(fn, path);
            java.util.List<PyObject> parts = res.asList();
            b.putBoolean(KEY_OK, parts.get(0).toBoolean());
            b.putString(KEY_OUT, parts.get(1).toString());
        } catch (RuntimeException e) {
            b.putBoolean(KEY_OK, false);
            b.putString(KEY_OUT, e.toString());
        }
        return b;
    }

    private static void reply(Message request, int what, Bundle data) {
        if (request.replyTo == null) return;
        Message m = Message.obtain(null, what);
        m.setData(data);
        try {
            request.replyTo.send(m);
        } catch (RemoteException ignored) {
            // the app went away; nothing to report to
        }
    }
}
