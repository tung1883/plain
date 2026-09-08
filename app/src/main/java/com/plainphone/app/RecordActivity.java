package com.plainphone.app;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
 * Records one memo. The capture itself lives in {@link RecorderService}, so
 * leaving this screen (Home, Back, screen-off) keeps it running with a
 * notification. Only <b>Stop</b> — here or in the notification — ends the take.
 */
public class RecordActivity extends Activity {

    private static final int REQ_MIC = 7701;

    private Typeface font;
    private LinearLayout root;

    private TextView timeView;
    private TextView sub;
    private Button pauseBtn;
    private WaveformView wave;

    private RecorderService svc;
    private boolean bound;
    private boolean uiBuilt;
    private long boundAt;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (svc == null) return;
            if (!svc.recording()) {
                if (SystemClock.uptimeMillis() - boundAt > 2500) {
                    finish();
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
            bound = true;
            boundAt = SystemClock.uptimeMillis();
            handler.post(tick);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            svc = null;
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setGravity(Gravity.CENTER);
        root.setPadding(60, 60, 60, 60);
        UiKit.screen(this, "Recording", root);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        startAndBind();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQ_MIC) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startAndBind();
        } else {
            showDenied();
        }
    }

    private void startAndBind() {
        RecorderService.startRecording(this);
        bindService(new Intent(this, RecorderService.class), conn, Context.BIND_AUTO_CREATE);
    }

    private void showDenied() {
        root.removeAllViews();
        root.setGravity(Gravity.CENTER);
        TextView msg = new TextView(this);
        msg.setText("plainphone needs microphone access to record.");
        msg.setTextColor(0xFFB5B5B5);
        msg.setTextSize(15);
        msg.setTypeface(font);
        msg.setGravity(Gravity.CENTER);
        root.addView(msg);

        Button close = new Button(this);
        close.setText("Close");
        UiKit.style(this, close);
        close.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 40;
        root.addView(close, lp);
    }

    private void buildRecordingUi() {
        root.removeAllViews();
        root.setGravity(Gravity.CENTER);
        uiBuilt = true;

        TextView fmt = new TextView(this);
        String format = svc.recFormat() != null ? svc.recFormat() : Config.getRecorderFormat(this);
        int rate = svc.recSampleRate() > 0 ? svc.recSampleRate() : Config.getRecorderSampleRate(this);
        String label = format + " · " + rate + " Hz";
        if (Config.isRecorderNoiseReduction(this)) label += " · NR";
        fmt.setText(label.toUpperCase(Locale.US));
        fmt.setTextColor(0xFF666666);
        fmt.setTextSize(11);
        fmt.setTypeface(font);
        fmt.setLetterSpacing(0.1f);
        fmt.setGravity(Gravity.CENTER);
        root.addView(fmt);

        timeView = new TextView(this);
        timeView.setText("0:00");
        timeView.setTextColor(Color.WHITE);
        timeView.setTextSize(44);
        timeView.setTypeface(font);
        timeView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.topMargin = 28;
        tp.bottomMargin = 28;
        root.addView(timeView, tp);

        wave = new WaveformView(this);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (getResources()
                .getDisplayMetrics().density * 76));
        root.addView(wave, wp);

        sub = new TextView(this);
        sub.setText("Recording…");
        sub.setTextColor(0xFF8A8A8A);
        sub.setTextSize(12);
        sub.setTypeface(font);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.topMargin = 20;
        root.addView(sub, sp);

        TextView hint = new TextView(this);
        hint.setText("Keeps recording if you leave — control it from the shade");
        hint.setTextColor(0xFF5C5C5C);
        hint.setTextSize(11);
        hint.setTypeface(font);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hp.topMargin = 8;
        root.addView(hint, hp);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.topMargin = 36;
        root.addView(buttons, bp);

        pauseBtn = new Button(this);
        pauseBtn.setText("Pause");
        UiKit.style(this, pauseBtn);
        pauseBtn.setOnClickListener(v -> {
            if (svc != null) svc.recTogglePause();
        });
        buttons.addView(pauseBtn);

        Button stop = new Button(this);
        stop.setText("Stop");
        UiKit.style(this, stop);
        stop.setOnClickListener(v -> {
            if (svc != null) svc.recStop();
            finish();
        });
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stopParams.leftMargin = 20;
        buttons.addView(stop, stopParams);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tick);
        if (bound) {
            try {
                unbindService(conn);
            } catch (IllegalArgumentException ignored) {
            }
            bound = false;
        }
    }

    private static String fmt(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + String.format(Locale.US, "%02d", s % 60);
    }
}
