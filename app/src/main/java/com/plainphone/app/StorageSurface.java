package com.plainphone.app;

import android.content.Context;
import android.widget.LinearLayout;

import java.util.List;
import java.util.Map;

/** Disk mounts with usage bars, filesystem/kind, and per-disk I/O. */
final class StorageSurface extends MetricSurface {

    StorageSurface(Context ctx) { super(ctx); }

    @Override long pollMs() { return 4000; }
    @Override Map<String, Object> requestFrame(long ch) { return DevProtocol.diskGet(ch); }
    @Override String responseType() { return DevProtocol.T_DISK; }

    @Override
    @SuppressWarnings("unchecked")
    void render(Map<String, Object> f) {
        List<Object> disks = DevProtocol.list(f, "disks");
        if (disks == null) return;
        for (Object o : disks) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> d = (Map<String, Object>) o;
            long total = DevProtocol.num(d, "total", 0);
            long used = DevProtocol.num(d, "used", 0);
            double frac = total > 0 ? (double) used / total : 0;

            LinearLayout c = card();
            c.addView(line(str(d, "mount"), android.graphics.Color.WHITE));
            c.addView(Meters.bar(ctx, font, "", frac, Meters.fillFor(frac, 0xFFD0D0D0),
                    Fmt.bytes(used) + " / " + Fmt.bytes(total)));
            c.addView(line(str(d, "fs") + "  ·  " + str(d, "kind"), 0xFF8B8B8B));
            c.addView(line("↓ " + Fmt.rate(DevProtocol.num(d, "read_bps", 0))
                    + "   ↑ " + Fmt.rate(DevProtocol.num(d, "write_bps", 0)), 0xFF8B8B8B));
            body.addView(c);
        }
    }

    private static String str(Map<String, Object> m, String k) {
        String v = DevProtocol.str(m, k);
        return v == null ? "" : v;
    }
}
