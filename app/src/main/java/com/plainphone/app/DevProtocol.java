package com.plainphone.app;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Dev-plugin wire protocol: a self-contained MessagePack subset plus the
 * length-prefixed framing and typed message builders the {@code plaind} daemon
 * and this app exchange over one TCP socket.
 *
 * <p>No third-party dependency — the app ships zero. Only the value kinds the
 * protocol actually uses are encoded (nil, bool, int, float, str, bin, array,
 * map); the decoder additionally accepts every integer/str/bin/collection width
 * so it can read whatever {@code rmp-serde} emits on the Rust side.
 *
 * <p>Frame = {@code uint32} big-endian length + that many bytes of a MessagePack
 * map. Frames larger than {@link #MAX_FRAME} are refused on read and write.
 *
 * <p>Canonical spec: {@code PROTOCOL.md} in the plaind repo. Bump {@link #PROTO}
 * on any breaking change; the handshake rejects a mismatched major.
 */
final class DevProtocol {

    private DevProtocol() {}

    static final int PROTO = 1;
    static final int DEFAULT_PORT = 8471;
    static final int MAX_FRAME = 1 << 20; // 1 MiB

    // Message types (the "t" field).
    static final String T_HELLO = "hello";
    static final String T_WELCOME = "welcome";
    static final String T_ERROR = "error";
    static final String T_PING = "ping";
    static final String T_PONG = "pong";
    static final String T_PTY_OPEN = "pty.open";
    static final String T_PTY_DATA = "pty.data";
    static final String T_PTY_RESIZE = "pty.resize";
    static final String T_PTY_EXIT = "pty.exit";
    static final String T_PTY_CLOSE = "pty.close";
    static final String T_SCREEN_START = "screen.start";
    static final String T_SCREEN_FRAME = "screen.frame";
    static final String T_SCREEN_STOP = "screen.stop";
    static final String T_INPUT_MOVE = "input.move";
    static final String T_INPUT_POINT = "input.point";
    static final String T_INPUT_CLICK = "input.click";
    static final String T_INPUT_DOWN = "input.down";
    static final String T_INPUT_UP = "input.up";
    static final String T_INPUT_KEY = "input.key";
    static final String T_PROC_LIST = "proc.list";
    static final String T_PROC_KILL = "proc.kill";
    static final String T_PROC_KILLED = "proc.killed";

    static final String CAP_PTY = "pty";
    static final String CAP_SCREEN = "screen";
    static final String CAP_INPUT = "input";
    static final String CAP_PROC = "proc";

    // ---- framing -------------------------------------------------------------

    static void writeFrame(OutputStream out, Map<String, Object> message) throws IOException {
        byte[] body = encode(message);
        if (body.length > MAX_FRAME) {
            throw new IOException("frame too large: " + body.length);
        }
        byte[] header = new byte[4];
        header[0] = (byte) (body.length >>> 24);
        header[1] = (byte) (body.length >>> 16);
        header[2] = (byte) (body.length >>> 8);
        header[3] = (byte) body.length;
        out.write(header);
        out.write(body);
        out.flush();
    }

    /** Next frame as a map, or {@code null} at a clean end of stream. */
    static Map<String, Object> readFrame(InputStream in) throws IOException {
        int b0 = in.read();
        if (b0 < 0) return null;
        int b1 = readByte(in);
        int b2 = readByte(in);
        int b3 = readByte(in);
        long len = ((long) b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        if (len < 0 || len > MAX_FRAME) {
            throw new IOException("bad frame length: " + len);
        }
        byte[] body = new byte[(int) len];
        int read = 0;
        while (read < body.length) {
            int n = in.read(body, read, body.length - read);
            if (n < 0) throw new EOFException("truncated frame");
            read += n;
        }
        Object value = new Reader(body).read();
        if (!(value instanceof Map)) {
            throw new IOException("frame is not a map");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        return map;
    }

    private static int readByte(InputStream in) throws IOException {
        int b = in.read();
        if (b < 0) throw new EOFException();
        return b;
    }

    // ---- message builders ---------------------------------------------------

    static Map<String, Object> msg(String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("t", type);
        return m;
    }

    static Map<String, Object> hello(String token, String device) {
        Map<String, Object> m = msg(T_HELLO);
        m.put("proto", (long) PROTO);
        m.put("token", token);
        m.put("device", device);
        return m;
    }

    static Map<String, Object> ping() {
        return msg(T_PING);
    }

    static Map<String, Object> ptyOpen(long ch, int cols, int rows, String cmd) {
        Map<String, Object> m = msg(T_PTY_OPEN);
        m.put("ch", ch);
        m.put("cols", (long) cols);
        m.put("rows", (long) rows);
        m.put("cmd", cmd); // null = login shell
        return m;
    }

    static Map<String, Object> ptyData(long ch, byte[] data) {
        Map<String, Object> m = msg(T_PTY_DATA);
        m.put("ch", ch);
        m.put("data", data);
        return m;
    }

    static Map<String, Object> ptyResize(long ch, int cols, int rows) {
        Map<String, Object> m = msg(T_PTY_RESIZE);
        m.put("ch", ch);
        m.put("cols", (long) cols);
        m.put("rows", (long) rows);
        return m;
    }

    static Map<String, Object> ptyClose(long ch) {
        Map<String, Object> m = msg(T_PTY_CLOSE);
        m.put("ch", ch);
        return m;
    }

    static Map<String, Object> screenStart(long ch, int maxWidth, int fps, boolean drawCursor) {
        Map<String, Object> m = msg(T_SCREEN_START);
        m.put("ch", ch);
        m.put("max_w", (long) maxWidth);
        m.put("fps", (long) fps);
        m.put("cursor", drawCursor);
        return m;
    }

    static Map<String, Object> screenStop(long ch) {
        Map<String, Object> m = msg(T_SCREEN_STOP);
        m.put("ch", ch);
        return m;
    }

    static Map<String, Object> inputMove(double dx, double dy, double scroll) {
        Map<String, Object> m = msg(T_INPUT_MOVE);
        m.put("dx", dx);
        m.put("dy", dy);
        if (scroll != 0) m.put("scroll", scroll);
        return m;
    }

    /** Absolute point: {@code x},{@code y} are 0..1 within the captured frame. */
    static Map<String, Object> inputPoint(double x, double y) {
        Map<String, Object> m = msg(T_INPUT_POINT);
        m.put("x", x);
        m.put("y", y);
        return m;
    }

    static Map<String, Object> inputClick(String button, boolean doubleClick) {
        Map<String, Object> m = msg(T_INPUT_CLICK);
        m.put("button", button);
        if (doubleClick) m.put("double", Boolean.TRUE);
        return m;
    }

    static Map<String, Object> inputKey(String text, String key) {
        return inputKey(text, key, null);
    }

    /** {@code mods} (any of "ctrl","alt","shift") are held around the key/text. */
    static Map<String, Object> inputKey(String text, String key, List<String> mods) {
        Map<String, Object> m = msg(T_INPUT_KEY);
        if (text != null) m.put("text", text);
        if (key != null) m.put("key", key);
        if (mods != null && !mods.isEmpty()) m.put("mods", new ArrayList<Object>(mods));
        return m;
    }

    static Map<String, Object> procList(long ch) {
        Map<String, Object> m = msg(T_PROC_LIST);
        m.put("ch", ch);
        return m;
    }

    static Map<String, Object> procKill(long ch, long pid, String signal) {
        Map<String, Object> m = msg(T_PROC_KILL);
        m.put("ch", ch);
        m.put("pid", pid);
        m.put("sig", signal);
        return m;
    }

    // ---- typed field access ----------------------------------------------

    static String type(Map<String, Object> m) {
        return str(m, "t");
    }

    static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }

    static long num(Map<String, Object> m, String key, long fallback) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        return fallback;
    }

    static double dbl(Map<String, Object> m, String key, double fallback) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return fallback;
    }

    static boolean bool(Map<String, Object> m, String key) {
        return Boolean.TRUE.equals(m.get(key));
    }

    static byte[] bin(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof byte[] ? (byte[]) v : null;
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof List ? (List<Object>) v : null;
    }

    // ---- MessagePack encode (only the kinds we emit) --------------------

    static byte[] encode(Object value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(64);
        writeValue(out, value);
        return out.toByteArray();
    }

    private static void writeValue(ByteArrayOutputStream out, Object value) {
        if (value == null) {
            out.write(0xc0);
        } else if (value instanceof Boolean) {
            out.write(((Boolean) value) ? 0xc3 : 0xc2);
        } else if (value instanceof byte[]) {
            writeBinary(out, (byte[]) value);
        } else if (value instanceof String) {
            writeString(out, (String) value);
        } else if (value instanceof Double || value instanceof Float) {
            writeDouble(out, ((Number) value).doubleValue());
        } else if (value instanceof Number) {
            writeLong(out, ((Number) value).longValue());
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) value;
            writeMapHeader(out, map.size());
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                writeString(out, String.valueOf(e.getKey()));
                writeValue(out, e.getValue());
            }
        } else if (value instanceof List) {
            List<?> listv = (List<?>) value;
            writeArrayHeader(out, listv.size());
            for (Object item : listv) writeValue(out, item);
        } else {
            throw new IllegalArgumentException("cannot encode " + value.getClass());
        }
    }

    private static void writeLong(ByteArrayOutputStream out, long v) {
        if (v >= 0 && v < 128) {
            out.write((int) v);
        } else if (v < 0 && v >= -32) {
            out.write((int) (v & 0xff));
        } else {
            out.write(0xd3);
            for (int shift = 56; shift >= 0; shift -= 8) out.write((int) (v >>> shift) & 0xff);
        }
    }

    private static void writeDouble(ByteArrayOutputStream out, double v) {
        long bits = Double.doubleToLongBits(v);
        out.write(0xcb);
        for (int shift = 56; shift >= 0; shift -= 8) out.write((int) (bits >>> shift) & 0xff);
    }

    private static void writeString(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int len = bytes.length;
        if (len < 32) {
            out.write(0xa0 | len);
        } else if (len < 256) {
            out.write(0xd9);
            out.write(len);
        } else if (len < 65536) {
            out.write(0xda);
            out.write((len >>> 8) & 0xff);
            out.write(len & 0xff);
        } else {
            out.write(0xdb);
            for (int shift = 24; shift >= 0; shift -= 8) out.write((len >>> shift) & 0xff);
        }
        out.write(bytes, 0, len);
    }

    private static void writeBinary(ByteArrayOutputStream out, byte[] bytes) {
        int len = bytes.length;
        if (len < 256) {
            out.write(0xc4);
            out.write(len);
        } else if (len < 65536) {
            out.write(0xc5);
            out.write((len >>> 8) & 0xff);
            out.write(len & 0xff);
        } else {
            out.write(0xc6);
            for (int shift = 24; shift >= 0; shift -= 8) out.write((len >>> shift) & 0xff);
        }
        out.write(bytes, 0, len);
    }

    private static void writeMapHeader(ByteArrayOutputStream out, int n) {
        if (n < 16) {
            out.write(0x80 | n);
        } else if (n < 65536) {
            out.write(0xde);
            out.write((n >>> 8) & 0xff);
            out.write(n & 0xff);
        } else {
            out.write(0xdf);
            for (int shift = 24; shift >= 0; shift -= 8) out.write((n >>> shift) & 0xff);
        }
    }

    private static void writeArrayHeader(ByteArrayOutputStream out, int n) {
        if (n < 16) {
            out.write(0x90 | n);
        } else if (n < 65536) {
            out.write(0xdc);
            out.write((n >>> 8) & 0xff);
            out.write(n & 0xff);
        } else {
            out.write(0xdd);
            for (int shift = 24; shift >= 0; shift -= 8) out.write((n >>> shift) & 0xff);
        }
    }

    // ---- MessagePack decode (accepts every width) ----------------------

    static Map<String, Object> decode(byte[] bytes) {
        Object value = new Reader(bytes).read();
        if (!(value instanceof Map)) throw new IllegalArgumentException("not a map");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        return map;
    }

    private static final class Reader {
        private final byte[] buf;
        private int pos;

        Reader(byte[] buf) {
            this.buf = buf;
        }

        Object read() {
            int b = u8();
            if (b <= 0x7f) return (long) b;                 // positive fixint
            if (b >= 0xe0) return (long) (byte) b;           // negative fixint
            if ((b & 0xf0) == 0x80) return readMap(b & 0x0f);
            if ((b & 0xf0) == 0x90) return readArray(b & 0x0f);
            if ((b & 0xe0) == 0xa0) return readStr(b & 0x1f);
            switch (b) {
                case 0xc0: return null;
                case 0xc2: return Boolean.FALSE;
                case 0xc3: return Boolean.TRUE;
                case 0xc4: return readBin((int) u(1));
                case 0xc5: return readBin((int) u(2));
                case 0xc6: return readBin((int) u(4));
                case 0xca: return (double) Float.intBitsToFloat((int) u(4));
                case 0xcb: return Double.longBitsToDouble(s(8));
                case 0xcc: return u(1);
                case 0xcd: return u(2);
                case 0xce: return u(4);
                case 0xcf: return u(8);
                case 0xd0: return (long) (byte) u(1);
                case 0xd1: return (long) (short) u(2);
                case 0xd2: return (long) (int) u(4);
                case 0xd3: return s(8);
                case 0xd9: return readStr((int) u(1));
                case 0xda: return readStr((int) u(2));
                case 0xdb: return readStr((int) u(4));
                case 0xdc: return readArray((int) u(2));
                case 0xdd: return readArray((int) u(4));
                case 0xde: return readMap((int) u(2));
                case 0xdf: return readMap((int) u(4));
                default:
                    throw new IllegalArgumentException(
                            String.format("unsupported msgpack byte 0x%02x at %d", b, pos - 1));
            }
        }

        private Map<String, Object> readMap(int n) {
            Map<String, Object> map = new LinkedHashMap<>(Math.max(4, n * 2));
            for (int i = 0; i < n; i++) {
                String key = String.valueOf(read());
                map.put(key, read());
            }
            return map;
        }

        private List<Object> readArray(int n) {
            List<Object> listv = new ArrayList<>(n);
            for (int i = 0; i < n; i++) listv.add(read());
            return listv;
        }

        private String readStr(int len) {
            String s = new String(buf, pos, len, StandardCharsets.UTF_8);
            pos += len;
            return s;
        }

        private byte[] readBin(int len) {
            byte[] out = new byte[len];
            System.arraycopy(buf, pos, out, 0, len);
            pos += len;
            return out;
        }

        private int u8() {
            return buf[pos++] & 0xff;
        }

        /** Unsigned big-endian integer of {@code n} bytes (n <= 8). */
        private long u(int n) {
            long v = 0;
            for (int i = 0; i < n; i++) v = (v << 8) | (buf[pos++] & 0xffL);
            return v;
        }

        /** Signed big-endian integer of {@code n} bytes. */
        private long s(int n) {
            long v = buf[pos++]; // sign-extends
            for (int i = 1; i < n; i++) v = (v << 8) | (buf[pos++] & 0xffL);
            return v;
        }
    }
}
