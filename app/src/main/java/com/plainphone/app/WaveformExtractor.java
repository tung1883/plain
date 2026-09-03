package com.plainphone.app;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a ~200-point amplitude envelope from an existing audio file by decoding
 * it to PCM. Used for imported and vaulted recordings, which have no capture-time
 * envelope. The 0–100 log scale matches {@link AudioCapture}.
 */
final class WaveformExtractor {

    private WaveformExtractor() {}

    private static final int TARGET = 200;
    private static final int WINDOW = 1024;          // mono samples per coarse peak
    private static final long TIMEOUT_US = 10_000;

    /** @return up to {@value #TARGET} peaks (0–100), or an empty array on failure. */
    static int[] peaks(File file) {
        MediaExtractor ex = new MediaExtractor();
        MediaCodec codec = null;
        try {
            ex.setDataSource(file.getAbsolutePath());
            int track = -1;
            MediaFormat fmt = null;
            for (int i = 0; i < ex.getTrackCount(); i++) {
                MediaFormat f = ex.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    track = i;
                    fmt = f;
                    break;
                }
            }
            if (track < 0) return new int[0];
            ex.selectTrack(track);

            String mime = fmt.getString(MediaFormat.KEY_MIME);
            int channels = fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                    ? fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
            if (channels < 1) channels = 1;

            List<Integer> raw = new ArrayList<>();
            if (MediaFormat.MIMETYPE_AUDIO_RAW.equals(mime)) {
                readRaw(ex, channels, raw);
            } else {
                codec = MediaCodec.createDecoderByType(mime);
                codec.configure(fmt, null, null, 0);
                codec.start();
                decode(ex, codec, channels, raw);
            }
            return bucketize(raw);
        } catch (Exception e) {
            return new int[0];
        } finally {
            if (codec != null) {
                try { codec.stop(); } catch (Exception ignored) {}
                try { codec.release(); } catch (Exception ignored) {}
            }
            try { ex.release(); } catch (Exception ignored) {}
        }
    }

    private static void readRaw(MediaExtractor ex, int channels, List<Integer> raw) {
        ByteBuffer buf = ByteBuffer.allocate(1 << 16);
        int count = 0, max = 0;
        for (;;) {
            buf.clear();
            int sz = ex.readSampleData(buf, 0);
            if (sz < 0) break;
            for (int p = 0; p + 1 < sz; p += 2) {
                int s = Math.abs((short) ((buf.get(p + 1) << 8) | (buf.get(p) & 0xff)));
                if (s > max) max = s;
                if (++count >= WINDOW * channels) {
                    raw.add(max);
                    count = 0;
                    max = 0;
                }
            }
            ex.advance();
        }
        if (count > 0) raw.add(max);
    }

    private static void decode(MediaExtractor ex, MediaCodec codec, int channels, List<Integer> raw) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean inputDone = false, outputDone = false;
        int count = 0, max = 0;
        while (!outputDone) {
            if (!inputDone) {
                int inIx = codec.dequeueInputBuffer(TIMEOUT_US);
                if (inIx >= 0) {
                    ByteBuffer ib = codec.getInputBuffer(inIx);
                    int sz = ib == null ? -1 : ex.readSampleData(ib, 0);
                    if (sz < 0) {
                        codec.queueInputBuffer(inIx, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        codec.queueInputBuffer(inIx, 0, sz, ex.getSampleTime(), 0);
                        ex.advance();
                    }
                }
            }
            int outIx = codec.dequeueOutputBuffer(info, TIMEOUT_US);
            if (outIx >= 0) {
                ByteBuffer ob = codec.getOutputBuffer(outIx);
                if (ob != null) {
                    int end = info.offset + info.size;
                    for (int p = info.offset; p + 1 < end; p += 2) {
                        int s = Math.abs((short) ((ob.get(p + 1) << 8) | (ob.get(p) & 0xff)));
                        if (s > max) max = s;
                        if (++count >= WINDOW * channels) {
                            raw.add(max);
                            count = 0;
                            max = 0;
                        }
                    }
                }
                codec.releaseOutputBuffer(outIx, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true;
            } else if (outIx == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                // Some decoders stall at EOS without flagging the last buffer — bail.
                break;
            }
        }
        if (count > 0) raw.add(max);
    }

    private static int[] bucketize(List<Integer> raw) {
        if (raw.isEmpty()) return new int[0];
        int n = Math.min(TARGET, raw.size());
        int[] out = new int[n];
        double bucket = raw.size() / (double) n;
        for (int i = 0; i < n; i++) {
            int from = (int) (i * bucket);
            int to = (int) ((i + 1) * bucket);
            int max = 0;
            for (int j = from; j < to && j < raw.size(); j++) max = Math.max(max, raw.get(j));
            out[i] = toLevel(max);
        }
        return out;
    }

    private static int toLevel(int amp) {
        if (amp <= 0) return 0;
        double db = 20 * Math.log10(amp / 32767.0);
        double norm = (db + 50) / 50.0;
        return (int) Math.max(0, Math.min(100, norm * 100));
    }
}
