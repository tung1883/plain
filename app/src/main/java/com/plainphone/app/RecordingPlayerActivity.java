package com.plainphone.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Plays one recording. The {@link android.media.MediaPlayer} lives in
 * {@link RecorderService}, so playback keeps going — with lock-screen and shade
 * controls — after this screen is gone. A vaulted recording plays through
 * screen-off; it only pauses when the vault actually locks, and resumes on unlock.
 */
public class RecordingPlayerActivity extends Activity {

    private Typeface font;
    private String recId;
    private String docId;
    private String name;
    private String format;

    private RecorderService svc;
    private boolean bound;
    private String shownKey;

    private WaveformView wave;
    private TextView playBtn;
    private TextView elapsed;
    private TextView total;
    private TextView meta;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progress = new Runnable() {
        @Override
        public void run() {
            if (svc == null) return;
            if (!svc.playbackActive()) {
                finish();
                return;
            }
            String key = svc.currentKey();
            if (!key.isEmpty() && !key.equals(shownKey)) {
                shownKey = key;
                buildUi();                     // first frame, or the queue advanced
            }
            if (shownKey == null) {
                handler.postDelayed(this, 60);
                return;
            }
            int dur = Math.max(1, svc.duration());
            int pos = svc.position();
            wave.setProgress(pos / (float) dur);
            elapsed.setText(fmt(pos));
            total.setText(fmt(svc.duration()));
            playBtn.setText(svc.isPlayingNow() ? "❚❚" : "▶");
            meta.setText(svc.vaultPaused()
                    ? "Vault locked — unlock to resume"
                    : metaLine());
            handler.postDelayed(this, 60);
        }
    };

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName n, IBinder service) {
            svc = ((RecorderService.LocalBinder) service).service();
            bound = true;
            if (svc.recording()) {
                Toast.makeText(RecordingPlayerActivity.this,
                        "Stop the recording first", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            handler.post(progress);
        }

        @Override
        public void onServiceDisconnected(ComponentName n) {
            svc = null;
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        font = Fonts.current(this);

        recId = getIntent().getStringExtra("recId");
        docId = getIntent().getStringExtra("docId");
        name = getIntent().getStringExtra("name");
        format = getIntent().getStringExtra("format");
        if (format == null) format = "m4a";

        if (docId == null && recId == null) {
            finish();
            return;
        }
        if (docId != null && !VaultSession.get().isUnlocked()) {
            finish();
            return;
        }

        RecorderService.startPlayback(this, recId, docId, name, format);
        bindService(new Intent(this, RecorderService.class), conn, Context.BIND_AUTO_CREATE);
    }

    private String metaLine() {
        String m = svc != null ? svc.playMeta() : "";
        return m == null ? "" : m;
    }

    private void buildUi() {
        String title = svc != null && svc.playName() != null ? svc.playName()
                : (name == null || name.isEmpty() ? "Recording" : name);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(Color.BLACK);
        outer.addView(UiKit.header(this, title), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        View hair = new View(this);
        hair.setBackgroundColor(0xFF1C1C1C);
        outer.addView(hair, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(Color.BLACK);
        content.setPadding(48, 24, 48, 44);
        outer.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        meta = new TextView(this);
        meta.setText(metaLine());
        meta.setTextColor(0xFF7C7C7C);
        meta.setTextSize(11);
        meta.setTypeface(font);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mp.topMargin = 4;
        content.addView(meta, mp);

        playBtn = new TextView(this);
        playBtn.setText("❚❚");
        playBtn.setTextColor(Color.WHITE);
        playBtn.setTextSize(30);
        playBtn.setTypeface(font);
        playBtn.setGravity(Gravity.CENTER);
        playBtn.setOnClickListener(v -> {
            if (svc != null) svc.playToggle();
        });
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        pp.gravity = Gravity.CENTER;
        content.addView(playBtn, pp);

        wave = new WaveformView(this);
        int[] envelope = svc != null ? svc.playEnvelope() : new int[0];
        wave.setEnvelope(envelope != null && envelope.length > 0 ? envelope : placeholderEnvelope());
        wave.setOnSeek(f -> {
            if (svc != null) svc.playSeekFraction(f);
        });
        content.addView(wave, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (getResources().getDisplayMetrics().density * 84)));

        LinearLayout times = new LinearLayout(this);
        times.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.topMargin = 8;
        elapsed = smallTime("0:00");
        total = smallTime("0:00");
        total.setGravity(Gravity.END);
        times.addView(elapsed, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        times.addView(total, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(times, tp);

        setContentView(outer);
    }

    private TextView smallTime(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(0xFF8A8A8A);
        t.setTextSize(11);
        t.setTypeface(font);
        return t;
    }

    private static int[] placeholderEnvelope() {
        int[] out = new int[60];
        for (int i = 0; i < out.length; i++) {
            out[i] = (int) (30 + 25 * Math.abs(Math.sin(i * 0.5)));
        }
        return out;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progress);
        if (bound) {
            try {
                unbindService(conn);
            } catch (IllegalArgumentException ignored) {
            }
            bound = false;
        }
    }

    private static String fmt(int ms) {
        int s = ms / 1000;
        return (s / 60) + ":" + String.format(Locale.US, "%02d", s % 60);
    }
}
