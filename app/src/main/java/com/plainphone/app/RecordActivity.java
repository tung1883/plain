package com.plainphone.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

/**
 * Records one memo. Lives only while on screen — Stop, Back, or Home all stop
 * and save. Sub-½-second takes are discarded.
 */
public class RecordActivity extends Activity {

    private static final int REQ_MIC = 7701;
    private static final long MIN_KEEP_MS = 500;

    private Typeface font;
    private LinearLayout root;

    private AudioCapture capture;
    private File file;
    private String format;
    private boolean saved;

    private TextView timeView;
    private TextView sub;
    private Button pauseBtn;
    private WaveformView wave;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (capture == null || !capture.recording()) return;
            timeView.setText(fmt(capture.elapsedMs()));
            if (!capture.paused()) wave.pushLevel(capture.level());
            handler.postDelayed(this, 60);
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
        startRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQ_MIC) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            showDenied();
        }
    }

    private void showDenied() {
        root.removeAllViews();
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

    private void startRecording() {
        format = Config.getRecorderFormat(this);
        int rate = Config.getRecorderSampleRate(this);
        file = new File(getCacheDir(), "rec-" + System.currentTimeMillis() + "." + format);
        capture = new AudioCapture(format, rate, file, Config.isRecorderNoiseReduction(this));
        try {
            capture.start();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't start recording", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        buildRecordingUi();
        handler.post(tick);
    }

    private void buildRecordingUi() {
        root.removeAllViews();
        root.setGravity(Gravity.CENTER);

        TextView fmt = new TextView(this);
        String label = format + " · " + capture.effectiveSampleRate() + " Hz";
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
        pauseBtn.setOnClickListener(v -> togglePause());
        buttons.addView(pauseBtn);

        Button stop = new Button(this);
        stop.setText("Stop");
        UiKit.style(this, stop);
        stop.setOnClickListener(v -> {
            finishRecording();
            finish();
        });
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stopParams.leftMargin = 20;
        buttons.addView(stop, stopParams);
    }

    private void togglePause() {
        if (capture == null || !capture.recording()) return;
        if (capture.paused()) {
            capture.resume();
            pauseBtn.setText("Pause");
            sub.setText("Recording…");
        } else {
            capture.pause();
            pauseBtn.setText("Resume");
            sub.setText("Paused");
        }
    }

    private void finishRecording() {
        handler.removeCallbacks(tick);
        if (saved || capture == null) return;
        saved = true;
        int[] peaks = capture.envelope();
        long durationMs = capture.stop();
        if (durationMs < MIN_KEEP_MS || !file.isFile() || file.length() == 0) {
            file.delete();
            return;
        }
        String name = Recorder.nextName(this);
        Recording r = Recording.create(name, format, capture.effectiveSampleRate(),
                durationMs, Recording.peaksToString(peaks));
        File dest = Recorder.fileFor(this, r);
        if (file.renameTo(dest) || copy(file, dest)) {
            Recorder.add(this, r);
        }
        file.delete();
    }

    private static boolean copy(File from, File to) {
        try (java.io.FileInputStream in = new java.io.FileInputStream(from);
             java.io.FileOutputStream out = new java.io.FileOutputStream(to)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (capture == null) return;          // still on the permission prompt
        finishRecording();
        if (!isFinishing()) finish();
    }

    private static String fmt(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + String.format(Locale.US, "%02d", s % 60);
    }
}
