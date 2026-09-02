package com.plainphone.app;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.SystemClock;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Records one take to a file. {@code m4a}/{@code 3gp} go through
 * {@link MediaRecorder}; {@code wav} is raw PCM-16 mono via {@link AudioRecord}
 * with a hand-written header. Poll {@link #level()} for the live meter; the
 * accumulated {@link #envelope()} is the whole take's amplitude history.
 */
class AudioCapture {

    private static final int ENVELOPE_TARGET = 200;

    private final String format;
    private final int sampleRate;
    private final File out;
    private final boolean denoise;

    private MediaRecorder recorder;         // m4a / 3gp
    private WavRecorder wav;                // wav
    private final List<Object> effects = new ArrayList<>();   // NS / AGC / AEC handles
    private boolean recording;
    private boolean paused;
    private long segmentStart;      // elapsedRealtime of the current unpaused run
    private long activeAccumMs;     // recorded time before the current run

    private final List<Integer> peaks = new ArrayList<>();
    private long lastPeakAt;

    AudioCapture(String format, int sampleRate, File out, boolean denoise) {
        this.format = format == null ? "m4a" : format.toLowerCase();
        this.sampleRate = "3gp".equals(this.format) ? 8000 : sampleRate;
        this.out = out;
        this.denoise = denoise;
    }

    int effectiveSampleRate() {
        return sampleRate;
    }

    void start() throws IOException {
        if ("wav".equals(format)) {
            wav = new WavRecorder(sampleRate, out, denoise);
            wav.start();
            if (denoise) attachEffects(wav.sessionId());
        } else {
            recorder = new MediaRecorder();
            // VOICE_COMMUNICATION runs the platform voice DSP (noise suppression,
            // echo cancel, auto gain) — MIC is the raw path.
            recorder.setAudioSource(denoise
                    ? MediaRecorder.AudioSource.VOICE_COMMUNICATION
                    : MediaRecorder.AudioSource.MIC);
            if ("3gp".equals(format)) {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            } else {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(sampleRate);
                recorder.setAudioEncodingBitRate(96_000);
            }
            recorder.setOutputFile(out.getAbsolutePath());
            recorder.prepare();
            recorder.start();
        }
        recording = true;
        paused = false;
        activeAccumMs = 0;
        segmentStart = SystemClock.elapsedRealtime();
        lastPeakAt = 0;
    }

    /** Enable the platform noise-suppression / AGC / echo-cancel effects on a session. */
    private void attachEffects(int sessionId) {
        try {
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor ns = NoiseSuppressor.create(sessionId);
                if (ns != null) {
                    ns.setEnabled(true);
                    effects.add(ns);
                }
            }
            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl agc = AutomaticGainControl.create(sessionId);
                if (agc != null) {
                    agc.setEnabled(true);
                    effects.add(agc);
                }
            }
            if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler aec = AcousticEchoCanceler.create(sessionId);
                if (aec != null) {
                    aec.setEnabled(true);
                    effects.add(aec);
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void releaseEffects() {
        for (Object fx : effects) {
            try {
                if (fx instanceof NoiseSuppressor) ((NoiseSuppressor) fx).release();
                else if (fx instanceof AutomaticGainControl) ((AutomaticGainControl) fx).release();
                else if (fx instanceof AcousticEchoCanceler) ((AcousticEchoCanceler) fx).release();
            } catch (RuntimeException ignored) {
            }
        }
        effects.clear();
    }

    void pause() {
        if (!recording || paused) return;
        paused = true;
        activeAccumMs += SystemClock.elapsedRealtime() - segmentStart;
        try {
            if (wav != null) wav.setPaused(true);
            else if (recorder != null) recorder.pause();
        } catch (RuntimeException ignored) {
        }
    }

    void resume() {
        if (!recording || !paused) return;
        paused = false;
        segmentStart = SystemClock.elapsedRealtime();
        try {
            if (wav != null) wav.setPaused(false);
            else if (recorder != null) recorder.resume();
        } catch (RuntimeException ignored) {
        }
    }

    boolean paused() {
        return paused;
    }

    /** Recorded time so far, frozen while paused. */
    long elapsedMs() {
        long base = activeAccumMs;
        if (recording && !paused) base += SystemClock.elapsedRealtime() - segmentStart;
        return base;
    }

    /** Duration in ms; 0 if nothing was captured. */
    long stop() {
        if (!recording) return 0;
        recording = false;
        if (!paused) activeAccumMs += SystemClock.elapsedRealtime() - segmentStart;
        long durationMs = activeAccumMs;
        try {
            if (wav != null) {
                wav.stop();
            } else if (recorder != null) {
                recorder.stop();
            }
        } catch (RuntimeException ignored) {
            // stop() throws when the take is too short to have any frames
        } finally {
            releaseEffects();
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }
            wav = null;
        }
        return durationMs;
    }

    boolean recording() {
        return recording;
    }

    /** 0–100, log-scaled. Also feeds the envelope on a ~60 ms tick. */
    int level() {
        if (paused) return 0;
        int amp = currentAmplitude();
        int lvl = toLevel(amp);
        long now = SystemClock.elapsedRealtime();
        if (recording && now - lastPeakAt >= 55) {
            lastPeakAt = now;
            synchronized (peaks) {
                peaks.add(lvl);
            }
        }
        return lvl;
    }

    private int currentAmplitude() {
        try {
            if (wav != null) return wav.lastPeak();
            if (recorder != null) return recorder.getMaxAmplitude();
        } catch (RuntimeException ignored) {
        }
        return 0;
    }

    private static int toLevel(int amp) {
        if (amp <= 0) return 0;
        double db = 20 * Math.log10(amp / 32767.0);   // -inf..0
        double norm = (db + 50) / 50.0;               // -50 dB floor
        return (int) Math.max(0, Math.min(100, norm * 100));
    }

    /** Down-sampled amplitude history for the whole take (≤ {@value #ENVELOPE_TARGET} points). */
    int[] envelope() {
        List<Integer> copy;
        synchronized (peaks) {
            copy = new ArrayList<>(peaks);
        }
        if (copy.isEmpty()) return new int[0];
        if (copy.size() <= ENVELOPE_TARGET) {
            int[] out = new int[copy.size()];
            for (int i = 0; i < out.length; i++) out[i] = copy.get(i);
            return out;
        }
        int[] out = new int[ENVELOPE_TARGET];
        double bucket = copy.size() / (double) ENVELOPE_TARGET;
        for (int i = 0; i < ENVELOPE_TARGET; i++) {
            int from = (int) (i * bucket);
            int to = (int) ((i + 1) * bucket);
            int max = 0;
            for (int j = from; j < to && j < copy.size(); j++) max = Math.max(max, copy.get(j));
            out[i] = max;
        }
        return out;
    }

    // --- raw PCM-16 mono WAV -------------------------------------------------

    private static final class WavRecorder {
        private final int sampleRate;
        private final File out;
        private final boolean denoise;
        private AudioRecord record;
        private Thread thread;
        private volatile boolean running;
        private volatile boolean paused;
        private volatile int lastPeak;
        private int bytesWritten;

        void setPaused(boolean p) {
            paused = p;
        }

        WavRecorder(int sampleRate, File out, boolean denoise) {
            this.sampleRate = sampleRate;
            this.out = out;
            this.denoise = denoise;
        }

        int lastPeak() {
            return lastPeak;
        }

        int sessionId() {
            return record != null ? record.getAudioSessionId() : 0;
        }

        void start() throws IOException {
            int minBuf = AudioRecord.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minBuf <= 0) throw new IOException("bad sample rate");
            int bufSize = Math.max(minBuf, sampleRate / 2);
            int source = denoise ? MediaRecorder.AudioSource.VOICE_COMMUNICATION
                    : MediaRecorder.AudioSource.MIC;
            record = new AudioRecord(source, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize);
            if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                record.release();
                throw new IOException("AudioRecord init failed");
            }
            RandomAccessFile raf = new RandomAccessFile(out, "rw");
            raf.setLength(0);
            raf.write(new byte[44]);              // header placeholder
            raf.close();

            record.startRecording();
            running = true;
            thread = new Thread(() -> pump(bufSize));
            thread.start();
        }

        private void pump(int bufSize) {
            byte[] buf = new byte[bufSize];
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out, true)) {
                while (running) {
                    int n = record.read(buf, 0, buf.length);
                    if (n <= 0) continue;
                    if (paused) {                 // keep draining the mic, drop the audio
                        lastPeak = 0;
                        continue;
                    }
                    fos.write(buf, 0, n);
                    bytesWritten += n;
                    int peak = 0;
                    for (int i = 0; i + 1 < n; i += 2) {
                        int s = Math.abs((short) ((buf[i + 1] << 8) | (buf[i] & 0xff)));
                        if (s > peak) peak = s;
                    }
                    lastPeak = peak;
                }
            } catch (IOException ignored) {
            }
        }

        void stop() {
            running = false;
            try {
                if (thread != null) thread.join(1500);
            } catch (InterruptedException ignored) {
            }
            if (record != null) {
                try {
                    record.stop();
                } catch (RuntimeException ignored) {
                }
                record.release();
                record = null;
            }
            patchHeader();
        }

        private void patchHeader() {
            try (RandomAccessFile raf = new RandomAccessFile(out, "rw")) {
                int dataLen = bytesWritten;
                int byteRate = sampleRate * 2;
                raf.seek(0);
                raf.write(new byte[]{'R', 'I', 'F', 'F'});
                writeLE(raf, 36 + dataLen);
                raf.write(new byte[]{'W', 'A', 'V', 'E', 'f', 'm', 't', ' '});
                writeLE(raf, 16);
                writeLE16(raf, 1);                 // PCM
                writeLE16(raf, 1);                 // mono
                writeLE(raf, sampleRate);
                writeLE(raf, byteRate);
                writeLE16(raf, 2);                 // block align
                writeLE16(raf, 16);                // bits per sample
                raf.write(new byte[]{'d', 'a', 't', 'a'});
                writeLE(raf, dataLen);
            } catch (IOException ignored) {
            }
        }

        private static void writeLE(RandomAccessFile raf, int v) throws IOException {
            raf.write(new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)});
        }

        private static void writeLE16(RandomAccessFile raf, int v) throws IOException {
            raf.write(new byte[]{(byte) v, (byte) (v >> 8)});
        }
    }
}
