package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * The take UI: timer, waveform, Pause/Resume, Stop. Shown full-screen by {@link RecordActivity}
 * and inline in the Recorder workspace window ({@link RecorderPanel}). The capture lives in
 * {@link RecorderService}; this view only binds to it and polls it.
 */
final class RecordingSurface extends LinearLayout {

    interface Listener {
        /** The take is over: Stop was tapped, or the service ended it. */
        void onDone();
    }

    private final Context ctx;
    private final Listener listener;
    private final Typeface font;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView timeView;
    private TextView sub;
    private Button pauseBtn;
    private WaveformView wave;

    private RecorderService svc;
    private boolean bound;
    private boolean uiBuilt;
    private long boundAt;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (svc == null) return;
            if (!svc.recording()) {
                if (SystemClock.uptimeMillis() - boundAt > 2500) {
                    listener.onDone();
                    return;
                }
                handler.postDelayed(this, 80);
                return;
            }
            if (!uiBuilt) buildRecordingUi();
            timeView.setText(fmt(svc.recElapsedMs()));
            boolean paused = svc.recPaused();
            if (!paused) wave.pushLevel(svc.recLevel());
            pauseBtn.setText(paused ? "Resume" : "Pause");
            sub.setText(paused ? "Paused" : "Recording…");
            handler.postDelayed(this, 60);
        }
    };

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            svc = ((RecorderService.LocalBinder) service).service();
            boundAt = SystemClock.uptimeMillis();
            handler.post(tick);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            svc = null;
        }
    };

    RecordingSurface(Context ctx, Listener listener) {
        super(ctx);
        this.ctx = ctx;
        this.listener = listener;
        this.font = Fonts.current(ctx);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
    }

    /** Start a new take (mic permission must already be granted) and attach to it. */
    void begin() {
        RecorderService.startRecording(ctx);
        attach();
    }

    /** Attach to a take already running in the service. Safe to call twice. */
    void attach() {
        if (bound) return;
        bound = ctx.bindService(new Intent(ctx, RecorderService.class), conn, Context.BIND_AUTO_CREATE);
    }

    /** Detach and stop polling. The service keeps recording. */
    void release() {
        handler.removeCallbacks(tick);
        if (bound) {
            try {
                ctx.unbindService(conn);
            } catch (IllegalArgumentException ignored) {
            }
            bound = false;
        }
        svc = null;
    }

    private void stop() {
        if (svc != null) svc.recStop();
        listener.onDone();
    }

    private void buildRecordingUi() {
        removeAllViews();
        setGravity(Gravity.CENTER);
        uiBuilt = true;
        float density = ctx.getResources().getDisplayMetrics().density;

        TextView fmt = new TextView(ctx);
        String format = svc.recFormat() != null ? svc.recFormat() : Config.getRecorderFormat(ctx);
        int rate = svc.recSampleRate() > 0 ? svc.recSampleRate() : Config.getRecorderSampleRate(ctx);
        String label = format + " · " + rate + " Hz";
        if (Config.isRecorderNoiseReduction(ctx)) label += " · NR";
        fmt.setText(label.toUpperCase(Locale.US));
        fmt.setTextColor(0xFF666666);
        fmt.setTextSize(11);
        fmt.setTypeface(font);
        fmt.setLetterSpacing(0.1f);
        fmt.setGravity(Gravity.CENTER);
        addView(fmt);

        timeView = new TextView(ctx);
        timeView.setText("0:00");
        timeView.setTextColor(0xFFFFFFFF);
        timeView.setTextSize(44);
        timeView.setTypeface(font);
        timeView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.topMargin = 28;
        tp.bottomMargin = 28;
        addView(timeView, tp);

        wave = new WaveformView(ctx);
        addView(wave, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (density * 76)));

        sub = new TextView(ctx);
        sub.setText("Recording…");
        sub.setTextColor(0xFF8A8A8A);
        sub.setTextSize(12);
        sub.setTypeface(font);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.topMargin = 20;
        addView(sub, sp);

        LinearLayout buttons = new LinearLayout(ctx);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.topMargin = 36;
        addView(buttons, bp);

        pauseBtn = new Button(ctx);
        pauseBtn.setText("Pause");
        UiKit.style(ctx, pauseBtn);
        pauseBtn.setOnClickListener(v -> {
            if (svc != null) svc.recTogglePause();
        });
        buttons.addView(pauseBtn);

        Button stopBtn = new Button(ctx);
        stopBtn.setText("Stop");
        UiKit.style(ctx, stopBtn);
        stopBtn.setOnClickListener(v -> stop());
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stopParams.leftMargin = 20;
        buttons.addView(stopBtn, stopParams);
    }

    private static String fmt(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + String.format(Locale.US, "%02d", s % 60);
    }
}
