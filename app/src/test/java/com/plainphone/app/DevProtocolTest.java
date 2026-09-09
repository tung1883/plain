package com.plainphone.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DevProtocolTest {

    @Test
    public void roundTripsEveryValueKindThroughAFrame() throws Exception {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("t", "sample");
        in.put("pos", 7L);                 // positive fixint
        in.put("neg", -5L);                // negative fixint
        in.put("big", 5_000_000_000L);     // needs int64
        in.put("flag", true);
        in.put("nothing", null);
        in.put("cpu", 12.5);               // float64
        in.put("data", new byte[]{1, 2, 3, 0, -1});
        in.put("longStr", repeat("x", 300)); // forces str16
        in.put("nested", Arrays.asList(
                mapOf("pid", 1L, "name", "init"),
                mapOf("pid", 4321L, "name", "sshd")));

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        DevProtocol.writeFrame(sink, in);

        Map<String, Object> out = DevProtocol.readFrame(new ByteArrayInputStream(sink.toByteArray()));
        assertEquals("sample", DevProtocol.type(out));
        assertEquals(7L, DevProtocol.num(out, "pos", -1));
        assertEquals(-5L, DevProtocol.num(out, "neg", 0));
        assertEquals(5_000_000_000L, DevProtocol.num(out, "big", 0));
        assertTrue(DevProtocol.bool(out, "flag"));
        assertNull(out.get("nothing"));
        assertTrue(out.containsKey("nothing"));
        assertEquals(12.5, DevProtocol.dbl(out, "cpu", 0), 0.0001);
        assertArrayEquals(new byte[]{1, 2, 3, 0, -1}, DevProtocol.bin(out, "data"));
        assertEquals(300, DevProtocol.str(out, "longStr").length());

        List<Object> procs = DevProtocol.list(out, "nested");
        assertEquals(2, procs.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> second = (Map<String, Object>) procs.get(1);
        assertEquals(4321L, DevProtocol.num(second, "pid", 0));
        assertEquals("sshd", second.get("name"));
    }

    @Test
    public void readFrameReturnsNullAtCleanEndOfStream() throws Exception {
        assertNull(DevProtocol.readFrame(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    public void decodesUnsignedIntegerWidthsFromTheRustSide() {
        // {"ch": 200 (uint8), "cols": 4000 (uint16)} hand-encoded the way rmp emits it.
        byte[] bytes = new byte[]{
                (byte) 0x82,
                (byte) 0xa2, 'c', 'h', (byte) 0xcc, (byte) 200,
                (byte) 0xa4, 'c', 'o', 'l', 's', (byte) 0xcd, (byte) 0x0f, (byte) 0xa0,
        };
        Map<String, Object> m = DevProtocol.decode(bytes);
        assertEquals(200L, DevProtocol.num(m, "ch", 0));
        assertEquals(4000L, DevProtocol.num(m, "cols", 0));
    }

    @Test
    public void helloCarriesProtocolVersionAndToken() {
        Map<String, Object> hello = DevProtocol.hello("tok-123", "Pixel");
        assertEquals(DevProtocol.T_HELLO, DevProtocol.type(hello));
        assertEquals(DevProtocol.PROTO, DevProtocol.num(hello, "proto", -1));
        assertEquals("tok-123", DevProtocol.str(hello, "token"));
        assertEquals("Pixel", DevProtocol.str(hello, "device"));

        Map<String, Object> back = DevProtocol.decode(DevProtocol.encode(hello));
        assertEquals("tok-123", DevProtocol.str(back, "token"));
    }

    @Test
    public void ptyOpenKeepsNullCommandForALoginShell() {
        Map<String, Object> open = DevProtocol.ptyOpen(3, 80, 24, null);
        Map<String, Object> back = DevProtocol.decode(DevProtocol.encode(open));
        assertTrue(back.containsKey("cmd"));
        assertNull(back.get("cmd"));
        assertEquals(80L, DevProtocol.num(back, "cols", 0));
        assertEquals(3L, DevProtocol.num(back, "ch", 0));
    }

    @Test
    public void inputPointCarriesNormalisedCoords() {
        Map<String, Object> back = DevProtocol.decode(
                DevProtocol.encode(DevProtocol.inputPoint(0.25, 0.5)));
        assertEquals(DevProtocol.T_INPUT_POINT, DevProtocol.type(back));
        assertEquals(0.25, DevProtocol.dbl(back, "x", -1), 0.0001);
        assertEquals(0.5, DevProtocol.dbl(back, "y", -1), 0.0001);
    }

    @Test
    public void ptyDataPreservesRawBytes() throws Exception {
        byte[] keystrokes = "ls -la\n[A".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        DevProtocol.writeFrame(sink, DevProtocol.ptyData(1, keystrokes));
        Map<String, Object> back = DevProtocol.readFrame(new ByteArrayInputStream(sink.toByteArray()));
        assertEquals(DevProtocol.T_PTY_DATA, DevProtocol.type(back));
        assertArrayEquals(keystrokes, DevProtocol.bin(back, "data"));
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
