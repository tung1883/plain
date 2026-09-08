package com.plainphone.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;

/**
 * Keeps a recording or a playback running when {@link RecordActivity} /
 * {@link RecordingPlayerActivity} is no longer on screen. One job at a time —
 * starting a recording stops any playback first.
 *
 * <ul>
 *   <li><b>Recording</b> — owns the {@link AudioCapture}, a partial wake-lock while
 *       actively capturing, and a plain ongoing notification (Pause / Stop).</li>
 *   <li><b>Playback</b> — owns the {@link MediaPlayer} and a {@link MediaSession},
 *       so a MediaStyle notification and the lock-screen transport drive it.</li>
 * </ul>
 *
 * <p>A vaulted recording plays through screen-off: while it is playing the service
 * holds the vault open ({@link VaultUnlockService#holdOpen}). Only the vault
 * actually locking (Lock now / Lock all / panic) stops it — playback pauses, the
 * decrypted temp is shredded, and it resumes from the same spot once the vault is
 * unlocked again.
 */
public class RecorderService extends Service {

    static final String ACTION_RECORD        = "com.plainphone.app.rec.RECORD";
    static final String ACTION_PLAY          = "com.plainphone.app.rec.PLAY";
    static final String ACTION_PAUSE         = "com.plainphone.app.rec.PAUSE";
    static final String ACTION_RESUME        = "com.plainphone.app.rec.RESUME";
    static final String ACTION_STOP          = "com.plainphone.app.rec.STOP";
    static final String ACTION_PLAY_TOGGLE   = "com.plainphone.app.rec.PLAY_TOGGLE";
    static final String ACTION_PLAY_PREV     = "com.plainphone.app.rec.PLAY_PREV";
    static final String ACTION_PLAY_NEXT     = "com.plainphone.app.rec.PLAY_NEXT";
    static final String ACTION_UNLOCK_RESUME = "com.plainphone.app.rec.UNLOCK_RESUME";

    private static final String CHANNEL_ID = "recorder";
    private static final int NOTIF_ID = 0x5245; // 'RE'
    private static final long MIN_KEEP_MS = 500;
    private static final int FF_MS = 15_000;
    private static final String VAULT_HOLD = "rec-play";

    private static volatile boolean active;
    private static volatile boolean vaultPlay;
    private static volatile String activeDetail;

    /** True while a recording or playback is running in this process. */
    static boolean isActive(Context context) {
        return active;
    }

    /** True while a <b>vault</b> recording is the one being played. */
    static boolean vaultPlaybackActive() {
        return vaultPlay;
    }

    /** "recording" / "playing a recording" for the lock-all confirmation, or null. */
    static String activeDetail() {
        return activeDetail;
    }

    enum Mode { NONE, RECORDING, PLAYING }

    private Mode mode = Mode.NONE;

    // --- recording ---
    private AudioCapture capture;
    private File recFile;
    private String recFormat;
    private String recName;
    private volatile int lastLevel;
    private long lastRecNotif;
    private PowerManager.WakeLock wakeLock;

    // --- playback ---
    private MediaPlayer player;
    private MediaSession session;
    private String playRecId;
    private String playDocId;
    private String playName;
    private String playFormat;
    private int[] playEnvelope = new int[0];
    private File playTemp;
    private boolean vaultPausedByLock;
    private int vaultResumePos;
    private VaultSession.Listener vaultWatcher;
    private java.util.List<Recording> playlist;
    private int playIndex = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LocalBinder binder = new LocalBinder();

    class LocalBinder extends Binder {
        RecorderService service() {
            return RecorderService.this;
        }
    }

    // --- start helpers ----------------------------------------------------

    static void startRecording(Context context) {
        context.startForegroundService(
                new Intent(context, RecorderService.class).setAction(ACTION_RECORD));
    }

    static void startPlayback(Context context, String recId, String docId,
                              String name, String format) {
        context.startForegroundService(new Intent(context, RecorderService.class)
                .setAction(ACTION_PLAY)
                .putExtra("recId", recId)
                .putExtra("docId", docId)
                .putExtra("name", name)
                .putExtra("format", format));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) {
            if (mode == Mode.NONE) stopSelf();
            return START_NOT_STICKY;
        }
        boolean control = !ACTION_RECORD.equals(action) && !ACTION_PLAY.equals(action);
        if (control && mode == Mode.NONE) {           // stale notification / media-button after a kill
            stopSelf();
            return START_NOT_STICKY;
        }
        switch (action) {
            case ACTION_RECORD:        beginRecording(); break;
            case ACTION_PLAY:          beginPlayback(intent); break;
            case ACTION_PAUSE:         setRecordingPaused(true); break;
            case ACTION_RESUME:        setRecordingPaused(false); break;
            case ACTION_STOP:          stopRecordingAndSave(); break;
            case ACTION_PLAY_TOGGLE:   togglePlay(); break;
            case ACTION_PLAY_PREV:     skipTrack(-1); break;
            case ACTION_PLAY_NEXT:     skipTrack(+1); break;
            case Intent.ACTION_MEDIA_BUTTON:
                if (session != null) {
                    android.view.KeyEvent ke = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (ke != null) session.getController().dispatchMediaButtonEvent(ke);
                }
                break;
            case ACTION_UNLOCK_RESUME:
                startActivity(new Intent(this, VaultActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
        }
        return START_NOT_STICKY;
    }

    // --- recording ------------------------------------------------------

    private void beginRecording() {
        if (mode == Mode.RECORDING) return;
        stopPlaybackInternal();
        recFormat = Config.getRecorderFormat(this);
        int rate = Config.getRecorderSampleRate(this);
        recFile = new File(getCacheDir(), "rec-" + System.currentTimeMillis() + "." + recFormat);
        recName = Recorder.peekName(this);
        capture = new AudioCapture(this, recFormat, rate, recFile, Config.isRecorderNoiseReduction(this));

        mode = Mode.RECORDING;
        active = true;
        vaultPlay = false;
        activeDetail = "recording";
        goForeground(recForegroundType());                  // claim foreground before mic init

        try {
            capture.start();
        } catch (Exception e) {
            capture = null;
            finishAndStop();
            return;
        }
        acquireWakeLock();
        updateWakeLock();
        updateNotification();
        lastRecNotif = SystemClock.uptimeMillis();
        handler.post(recTick);
    }

    private final Runnable recTick = new Runnable() {
        @Override
        public void run() {
            if (mode != Mode.RECORDING || capture == null) return;
            lastLevel = capture.level();
            long now = SystemClock.uptimeMillis();
            if (now - lastRecNotif >= 1000) {
                lastRecNotif = now;
                updateNotification();
                keepSectionUnlocked();
            }
            handler.postDelayed(this, 100);
        }
    };

    private void setRecordingPaused(boolean paused) {
        if (capture == null) return;
        if (paused) capture.pause();
        else capture.resume();
        updateWakeLock();
        updateNotification();
    }

    private void stopRecordingAndSave() {
        handler.removeCallbacks(recTick);
        persistRecording();
        mode = Mode.NONE;
        finishAndStop();
    }

    /** Finalise the take and add it to the store; a sub-½-second take is dropped. */
    private void persistRecording() {
        if (capture == null) return;
        int[] peaks = capture.envelope();
        int sampleRate = capture.effectiveSampleRate();
        long durationMs = capture.stop();
        try {
            if (durationMs >= MIN_KEEP_MS && recFile != null
                    && recFile.isFile() && recFile.length() > 0) {
                String name = Recorder.nextName(this);
                Recording r = Recording.create(name, recFormat, sampleRate, durationMs,
                        Recording.peaksToString(peaks));
                File dest = Recorder.fileFor(this, r);
                if (recFile.renameTo(dest) || copyFile(recFile, dest)) {
                    Recorder.add(this, r);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (recFile != null) recFile.delete();
            capture = null;
        }
    }

    private static boolean copyFile(File from, File to) {
        try (FileInputStream in = new FileInputStream(from);
             FileOutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // --- playback -----------------------------------------------------

    private void beginPlayback(Intent intent) {
        if (mode == Mode.RECORDING) return;                 // can't play while recording
        stopPlaybackInternal();
        playRecId = intent.getStringExtra("recId");
        playDocId = intent.getStringExtra("docId");
        playName = intent.getStringExtra("name");
        playFormat = intent.getStringExtra("format");
        if (playFormat == null) playFormat = "m4a";

        mode = Mode.PLAYING;
        active = true;
        activeDetail = "playing a recording";
        setUpSession();
        ensureVaultWatcher();
        goForeground(playForegroundType());                 // claim foreground before any decrypt
        loadPlaylist();

        if (!openPlayer(0)) {
            finishAndStop();
            return;
        }
        player.start();
        refreshSessionMetadata();
        updateSessionState();
        updateNotification();
        handler.post(playTick);
    }

    /**
     * The home recorder list, in the same order, as the play queue — local
     * recordings plus vault recordings <b>only while the vault is unlocked</b>.
     */
    private void loadPlaylist() {
        playlist = Recorder.orderedAll(this);
        String current = playDocId != null ? "vault:" + playDocId : playRecId;
        playIndex = -1;
        for (int i = 0; i < playlist.size(); i++) {
            if (playlist.get(i).id.equals(current)) {
                playIndex = i;
                break;
            }
        }
    }

    /** Prev / next track. "Prev" past 3 s into a track restarts it instead. */
    private void skipTrack(int dir) {
        if (mode != Mode.PLAYING) return;
        if (playlist == null) loadPlaylist();
        if (dir < 0 && position() > 3000) {
            seekTo(0);
            return;
        }
        int next = playIndex + dir;
        boolean vaultOpen = VaultSession.get().isUnlocked();
        while (next >= 0 && next < playlist.size()
                && Recorder.isVaulted(playlist.get(next).id) && !vaultOpen) {
            next += dir;                                    // skip vault tracks while locked
        }
        if (next < 0 || next >= playlist.size()) {
            if (dir < 0) seekTo(0);                         // already first -> restart
            return;
        }
        switchToIndex(next, isPlayingNow() || vaultPausedByLock);
    }

    private void switchToIndex(int idx, boolean autoStart) {
        handler.removeCallbacks(playTick);
        releasePlayerOnly();
        shredTemp();
        VaultUnlockService.releaseHold(this, VAULT_HOLD);
        vaultPlay = false;
        vaultPausedByLock = false;
        vaultResumePos = 0;

        Recording r = playlist.get(idx);
        playIndex = idx;
        if (Recorder.isVaulted(r.id)) {
            playDocId = Recorder.docIdOf(r.id);
            playRecId = null;
        } else {
            playRecId = r.id;
            playDocId = null;
        }
        playName = r.displayName();
        playFormat = r.format;

        if (!openPlayer(0)) {
            finishAndStop();
            return;
        }
        if (autoStart) player.start();
        refreshSessionMetadata();
        updateSessionState();
        updateNotification();
        if (autoStart) handler.post(playTick);
    }

    /** (Re)create the MediaPlayer, decrypting a vault blob to a temp if needed. */
    private boolean openPlayer(int seekTo) {
        File source;
        if (playDocId != null) {
            if (!VaultSession.get().isUnlocked()) return false;
            VaultUnlockService.holdOpen(this, VAULT_HOLD);
            playTemp = new File(getCacheDir(), "svc-play." + playFormat);
            try {
                VaultStore.decryptToFile(this, playDocId, playTemp);
            } catch (Exception e) {
                return false;
            }
            source = playTemp;
            playEnvelope = Recording.peaksFrom(Config.getVaultRecEnvelope(this, playDocId));
        } else {
            Recording r = Recording.findById(Config.getRecordings(this), playRecId);
            if (r == null) return false;
            source = Recorder.fileFor(this, r);
            playName = r.displayName();
            playFormat = r.format;
            playEnvelope = r.envelopePeaks();
        }
        player = new MediaPlayer();
        try {
            player.setDataSource(source.getAbsolutePath());
            player.prepare();
        } catch (Exception e) {
            releasePlayerOnly();
            return false;
        }
        if (seekTo > 0) player.seekTo(Math.min(seekTo, player.getDuration()));
        int dur = player.getDuration();
        vaultPlay = playDocId != null;
        if (playDocId != null) Config.setVaultRecDuration(this, playDocId, dur);
        else Recorder.healDuration(this, playRecId, dur);
        player.setOnCompletionListener(mp -> {
            if (playlist != null && playIndex >= 0 && playIndex + 1 < playlist.size()) {
                switchToIndex(playIndex + 1, true);         // auto-advance the queue
            } else {
                mp.seekTo(0);
                handler.removeCallbacks(playTick);
                updateSessionState();
                updateNotification();
            }
        });
        return true;
    }

    private final Runnable playTick = new Runnable() {
        @Override
        public void run() {
            if (mode != Mode.PLAYING || player == null) return;
            if (playDocId != null && !vaultPausedByLock && !VaultSession.get().isUnlocked()) {
                onVaultStateChanged();        // safety net — the listener didn't reach us
                return;
            }
            updateSessionState();
            keepSectionUnlocked();
            if (player.isPlaying()) handler.postDelayed(this, 500);
        }
    };

    /** Bump the recorder unlock window — unless the user has hard-locked the section. */
    private void keepSectionUnlocked() {
        if (!Lock.RECORDER.hardLocked(this)) Lock.RECORDER.keepUnlocked(this);
    }

    private void togglePlay() {
        if (mode != Mode.PLAYING || vaultPausedByLock || player == null) return;
        if (player.isPlaying()) {
            player.pause();
            handler.removeCallbacks(playTick);
        } else {
            player.start();
            keepSectionUnlocked();
            handler.post(playTick);
        }
        updateSessionState();
        updateNotification();
    }

    private void seekTo(int ms) {
        if (player == null) return;
        int dur = Math.max(0, player.getDuration());
        player.seekTo(Math.max(0, Math.min(ms, dur)));
        updateSessionState();
    }

    private void ensureVaultWatcher() {
        if (vaultWatcher != null) return;
        vaultWatcher = () -> handler.post(this::onVaultStateChanged);
        VaultSession.get().addListener(vaultWatcher);
    }

    private void onVaultStateChanged() {
        if (mode != Mode.PLAYING || playDocId == null) return;
        boolean unlocked = VaultSession.get().isUnlocked();
        if (!unlocked && !vaultPausedByLock) {
            vaultPausedByLock = true;
            vaultPlay = false;
            vaultResumePos = position();
            releasePlayerOnly();
            shredTemp();
            VaultUnlockService.releaseHold(this, VAULT_HOLD);
            handler.removeCallbacks(playTick);
            updateSessionState();
            updateNotification();
        } else if (unlocked && vaultPausedByLock) {
            vaultPausedByLock = false;
            if (openPlayer(vaultResumePos)) {
                player.start();
                goForeground(playForegroundType());
                refreshSessionMetadata();
                updateSessionState();
                updateNotification();
                handler.post(playTick);
            } else {
                finishAndStop();
            }
        }
    }

    private void stopPlaybackInternal() {
        handler.removeCallbacks(playTick);
        releasePlayerOnly();
        if (session != null) {
            session.setActive(false);
            session.release();
            session = null;
        }
        if (vaultWatcher != null) {
            VaultSession.get().removeListener(vaultWatcher);
            vaultWatcher = null;
        }
        shredTemp();
        VaultUnlockService.releaseHold(this, VAULT_HOLD);
        vaultPlay = false;
        vaultPausedByLock = false;
        vaultResumePos = 0;
        playRecId = null;
        playDocId = null;
    }

    private void releasePlayerOnly() {
        if (player != null) {
            try {
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
    }

    private void shredTemp() {
        if (playTemp != null) {
            playTemp.delete();
            playTemp = null;
        }
    }

    // --- MediaSession -------------------------------------------------

    private void setUpSession() {
        if (session != null) return;
        session = new MediaSession(this, "plain-recorder");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() {
                if (player != null && !player.isPlaying()) togglePlay();
            }
            @Override public void onPause() {
                if (player != null && player.isPlaying()) togglePlay();
            }
            @Override public void onSeekTo(long pos) {
                seekTo((int) pos);
            }
            @Override public void onSkipToPrevious() {
                skipTrack(-1);
            }
            @Override public void onSkipToNext() {
                skipTrack(+1);
            }
            @Override public void onFastForward() {
                seekTo(position() + FF_MS);
            }
            @Override public void onRewind() {
                seekTo(position() - FF_MS);
            }
            @Override public void onStop() {
                finishAndStop();
            }
        });
        Intent button = new Intent(Intent.ACTION_MEDIA_BUTTON)
                .setClass(this, RecorderMediaButtonReceiver.class);
        session.setMediaButtonReceiver(PendingIntent.getBroadcast(this, 0, button,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        session.setActive(true);
    }

    private void refreshSessionMetadata() {
        if (session == null) return;
        session.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, safeName())
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Plain · Voice recorder")
                .putString(MediaMetadata.METADATA_KEY_ALBUM, queueLabel())
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration())
                .build());
    }

    private void updateSessionState() {
        if (session == null) return;
        boolean playing = player != null && player.isPlaying();
        long actions = PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PLAY
                | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SEEK_TO
                | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT
                | PlaybackState.ACTION_REWIND | PlaybackState.ACTION_FAST_FORWARD
                | PlaybackState.ACTION_STOP;
        int state = playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, position(), playing ? 1f : 0f)
                .build());
    }

    // --- notification ----------------------------------------------

    private void ensureChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Recorder",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
    }

    private void goForeground(int type) {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 30 && type != 0) {
            startForeground(NOTIF_ID, n, type);
        } else {
            startForeground(NOTIF_ID, n);
        }
    }

    private void updateNotification() {
        getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
    }

    private int recForegroundType() {
        return Build.VERSION.SDK_INT >= 30 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE : 0;
    }

    private int playForegroundType() {
        return Build.VERSION.SDK_INT >= 30 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK : 0;
    }

    private Notification buildNotification() {
        ensureChannel();
        return mode == Mode.RECORDING ? recNotification() : playNotification();
    }

    private Notification recNotification() {
        boolean paused = capture != null && capture.paused();
        String fmt = recFormat == null ? "" : recFormat.toUpperCase(Locale.US);
        Notification.Builder b = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(safeName(recName))
                .setContentText(fmtTime(capture == null ? 0 : capture.elapsedMs())
                        + (fmt.isEmpty() ? "" : " · " + fmt) + (paused ? " · paused" : ""))
                .setSmallIcon(R.drawable.ic_stat_recorder)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(activityPI(new Intent(this, RecordActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP), 100));
        b.addAction(plainAction(paused ? "Resume" : "Pause", paused ? ACTION_RESUME : ACTION_PAUSE));
        b.addAction(plainAction("Stop", ACTION_STOP));
        return b.build();
    }

    private Notification playNotification() {
        if (vaultPausedByLock) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Paused — vault locked")
                    .setContentText(safeName() + " · " + fmtTime(vaultResumePos)
                            + " / " + fmtTime(duration()))
                    .setSmallIcon(R.drawable.ic_stat_recorder)
                    .setOngoing(false)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(servicePI(ACTION_UNLOCK_RESUME))
                    .addAction(plainAction("Unlock & resume", ACTION_UNLOCK_RESUME))
                    .build();
        }
        boolean playing = player != null && player.isPlaying();
        Notification.MediaStyle style = new Notification.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2);
        if (session != null) style.setMediaSession(session.getSessionToken());
        String sub = queueLabel();
        Notification.Builder b = new Notification.Builder(this, CHANNEL_ID)
                .setStyle(style)
                .setContentTitle(safeName())
                .setContentText(fmtTime(position()) + " / " + fmtTime(duration()))
                .setSmallIcon(R.drawable.ic_stat_recorder)
                .setOngoing(playing)
                .setOnlyAlertOnce(true)
                .setContentIntent(activityPI(playerIntent(), 101))
                .addAction(mediaAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PLAY_PREV))
                .addAction(mediaAction(playing ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play, playing ? "Pause" : "Play", ACTION_PLAY_TOGGLE))
                .addAction(mediaAction(android.R.drawable.ic_media_next, "Next", ACTION_PLAY_NEXT));
        if (!sub.isEmpty()) b.setSubText(sub);
        return b.build();
    }

    /** "3 of 12" when the play queue is known, else "". */
    private String queueLabel() {
        if (playlist == null || playIndex < 0) return "";
        return (playIndex + 1) + " of " + playlist.size();
    }

    private Intent playerIntent() {
        Intent i = new Intent(this, RecordingPlayerActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (playDocId != null) {
            i.putExtra("docId", playDocId).putExtra("name", playName).putExtra("format", playFormat);
        } else {
            i.putExtra("recId", playRecId);
        }
        return i;
    }

    private Notification.Action plainAction(String title, String action) {
        return new Notification.Action.Builder((Icon) null, title, servicePI(action)).build();
    }

    private Notification.Action mediaAction(int icon, String title, String action) {
        return new Notification.Action.Builder(
                Icon.createWithResource(this, icon), title, servicePI(action)).build();
    }

    private PendingIntent servicePI(String action) {
        return PendingIntent.getForegroundService(this, action.hashCode(),
                new Intent(this, RecorderService.class).setAction(action),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent activityPI(Intent intent, int req) {
        return PendingIntent.getActivity(this, req, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // --- wake lock ------------------------------------------------

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = getSystemService(PowerManager.class);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "plainphone:recorder");
            wakeLock.setReferenceCounted(false);
        }
    }

    private void updateWakeLock() {
        if (wakeLock == null) return;
        boolean want = mode == Mode.RECORDING && capture != null && !capture.paused();
        if (want && !wakeLock.isHeld()) wakeLock.acquire();
        else if (!want && wakeLock.isHeld()) wakeLock.release();
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

    // --- lifecycle -----------------------------------------------

    private void finishAndStop() {
        handler.removeCallbacks(recTick);
        handler.removeCallbacks(playTick);
        stopPlaybackInternal();
        releaseWakeLock();
        mode = Mode.NONE;
        active = false;
        vaultPlay = false;
        activeDetail = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(recTick);
        handler.removeCallbacks(playTick);
        if (capture != null) persistRecording();     // don't lose a take on an OOM kill / swipe
        stopPlaybackInternal();
        releaseWakeLock();
        active = false;
        vaultPlay = false;
        activeDetail = null;
    }

    // --- binder surface (activities poll this) -------------------

    boolean recording() {
        return mode == Mode.RECORDING && capture != null;
    }

    boolean recPaused() {
        return capture != null && capture.paused();
    }

    long recElapsedMs() {
        return capture == null ? 0 : capture.elapsedMs();
    }

    int recLevel() {
        return lastLevel;
    }

    String recFormat() {
        return recFormat;
    }

    int recSampleRate() {
        return capture == null ? 0 : capture.effectiveSampleRate();
    }

    void recTogglePause() {
        setRecordingPaused(!recPaused());
    }

    void recStop() {
        stopRecordingAndSave();
    }

    boolean playbackActive() {
        return mode == Mode.PLAYING;
    }

    boolean isPlayingNow() {
        return player != null && player.isPlaying();
    }

    boolean vaultPaused() {
        return vaultPausedByLock;
    }

    int position() {
        if (vaultPausedByLock) return vaultResumePos;
        if (player == null) return 0;
        try {
            return player.getCurrentPosition();
        } catch (Exception e) {
            return 0;
        }
    }

    int duration() {
        if (player == null) return 0;
        try {
            return Math.max(0, player.getDuration());
        } catch (Exception e) {
            return 0;
        }
    }

    int[] playEnvelope() {
        return playEnvelope;
    }

    String playName() {
        return safeName();
    }

    /** Identity of the track playing right now — changes when the queue advances. */
    String currentKey() {
        if (playDocId != null) return "vault:" + playDocId;
        return playRecId == null ? "" : playRecId;
    }

    String playMeta() {
        if (playDocId != null) {
            return playFormat == null ? "" : playFormat.toUpperCase(Locale.US);
        }
        Recording r = Recording.findById(Config.getRecordings(this), playRecId);
        return r == null ? "" : r.format.toUpperCase(Locale.US) + " · " + r.sampleRate + " Hz";
    }

    void playToggle() {
        togglePlay();
    }

    void playSeekFraction(float fraction) {
        if (player == null) return;
        seekTo(Math.round(fraction * player.getDuration()));
    }

    // --- misc ---------------------------------------------------

    private String safeName() {
        return safeName(playName);
    }

    private static String safeName(String name) {
        return name == null || name.trim().isEmpty() ? "Recording" : name.trim();
    }

    private static String fmtTime(long ms) {
        long s = Math.max(0, ms) / 1000;
        return (s / 60) + ":" + String.format(Locale.US, "%02d", s % 60);
    }
}
