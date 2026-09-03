package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

/**
 * Plays one recording. A vaulted recording is decrypted to a cache temp that is
 * deleted on close and if the vault locks.
 */
public class RecordingPlayerActivity extends Activity {

    private Typeface font;
    private MediaPlayer player;
    private File temp;                 // non-null only for vaulted playback
    private boolean vaulted;

    private WaveformView wave;
    private TextView playBtn;
    private TextView elapsed;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progress = new Runnable() {
        @Override
        public void run() {
            if (player == null) return;
            int pos = player.getCurrentPosition();
            int dur = Math.max(1, player.getDuration());
            wave.setProgress(pos / (float) dur);
            elapsed.setText(fmt(pos));
            if (player.isPlaying()) handler.postDelayed(this, 60);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        font = Fonts.current(this);

        String recId = getIntent().getStringExtra("recId");
        String docId = getIntent().getStringExtra("docId");
        String name;
        String meta;
        int[] envelope;
        File source;

        if (docId != null) {
            vaulted = true;
            if (!VaultSession.get().isUnlocked()) {
                finish();
                return;
            }
            name = getIntent().getStringExtra("name");
            String ext = getIntent().getStringExtra("format");
            if (ext == null) ext = "m4a";
            temp = new File(getCacheDir(), "play." + ext);
            try {
                VaultStore.decryptToFile(this, docId, temp);
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't open", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            source = temp;
            meta = ext.toUpperCase(Locale.US);
            envelope = new int[0];
        } else {
            Recording r = Recording.findById(Config.getRecordings(this), recId);
            if (r == null) {
                finish();
                return;
            }
            name = r.displayName();
            source = Recorder.fileFor(this, r);
            meta = r.format.toUpperCase(Locale.US) + " · " + r.sampleRate + " Hz";
            envelope = r.envelopePeaks();
        }

        buildUi(name, meta, envelope.length > 0 ? envelope : placeholderEnvelope());

        player = new MediaPlayer();
        try {
            player.setDataSource(source.getAbsolutePath());
            player.prepare();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't play", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        total.setText(fmt(player.getDuration()));
        player.setOnCompletionListener(mp -> {
            mp.seekTo(0);
            wave.setProgress(0f);
            elapsed.setText(fmt(0));
            playBtn.setText("▶");
        });
        togglePlay();   // autostart
    }

    private TextView total;

    private void buildUi(String name, String meta, int[] envelope) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(Color.BLACK);
        outer.addView(UiKit.header(this, name), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        View hair = new View(this);
        hair.setBackgroundColor(0xFF1C1C1C);
        outer.addView(hair, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 24, 48, 44);
        outer.addView(root, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView metaView = new TextView(this);
        metaView.setText(meta);
        metaView.setTextColor(0xFF7C7C7C);
        metaView.setTextSize(11);
        metaView.setTypeface(font);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mp.topMargin = 4;
        root.addView(metaView, mp);

        playBtn = new TextView(this);
        playBtn.setText("▶");
        playBtn.setTextColor(Color.WHITE);
        playBtn.setTextSize(30);
        playBtn.setTypeface(font);
        playBtn.setGravity(Gravity.CENTER);
        playBtn.setOnClickListener(v -> togglePlay());
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        pp.gravity = Gravity.CENTER;
        root.addView(playBtn, pp);

        wave = new WaveformView(this);
        wave.setEnvelope(envelope);
        wave.setOnSeek(f -> {
            if (player != null) player.seekTo((int) (f * player.getDuration()));
        });
        root.addView(wave, new LinearLayout.LayoutParams(
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
        root.addView(times, tp);

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

    private void togglePlay() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
            playBtn.setText("▶");
            handler.removeCallbacks(progress);
        } else {
            player.start();
            playBtn.setText("❚❚");
            handler.post(progress);
        }
        if (vaulted) VaultUnlockService.touch(this);
    }

    private static int[] placeholderEnvelope() {
        int[] out = new int[60];
        for (int i = 0; i < out.length; i++) {
            out[i] = (int) (30 + 25 * Math.abs(Math.sin(i * 0.5)));
        }
        return out;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {
            player.pause();
            playBtn.setText("▶");
            handler.removeCallbacks(progress);
        }
        if (vaulted && !VaultSession.get().isUnlocked()) finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progress);
        if (player != null) {
            player.release();
            player = null;
        }
        if (temp != null) temp.delete();
    }

    private static String fmt(int ms) {
        int s = ms / 1000;
        return (s / 60) + ":" + String.format(Locale.US, "%02d", s % 60);
    }
}
