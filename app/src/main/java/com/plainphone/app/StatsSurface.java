package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** CPU / MEM / SWP meters, per-core row, a CPU-history sparkline, load / uptime. */
final class StatsSurface extends MetricSurface {

    StatsSurface(Context ctx) { super(ctx); }

    @Override long pollMs() { return 2000; }
    @Override Map<String, Object> requestFrame(long ch) { return DevProtocol.statsGet(ch); }
    @Override String responseType() { return DevProtocol.T_STATS; }

    @Override
    void render(Map<String, Object> f) {
        double cpu = DevProtocol.dbl(f, "cpu", 0) / 100.0;
        long memU = DevProtocol.num(f, "mem_used_kb", 0);
        long memT = DevProtocol.num(f, "mem_total_kb", 1);
        long swU = DevProtocol.num(f, "swap_used_kb", 0);
        long swT = DevProtocol.num(f, "swap_total_kb", 0);
        long cores = DevProtocol.num(f, "cpu_count", 1);

        LinearLayout meters = card();
        meters.addView(Meters.bar(ctx, font, "CPU", cpu, Meters.fillFor(cpu, 0xFFD0D0D0)));
        double memFrac = memT > 0 ? (double) memU / memT : 0;
        meters.addView(Meters.bar(ctx, font, "MEM", memFrac, Meters.fillFor(memFrac, 0xFF9A9A9A),
                Fmt.bytes(memU * 1024) + " / " + Fmt.bytes(memT * 1024)));
        double swFrac = swT > 0 ? (double) swU / swT : 0;
        meters.addView(Meters.bar(ctx, font, "SWP", swFrac, Meters.fillFor(swFrac, 0xFF6A6A6A),
                swT > 0 ? Fmt.bytes(swU * 1024) + " / " + Fmt.bytes(swT * 1024) : "none"));
        body.addView(meters);

        List<Object> perCpu = DevProtocol.list(f, "per_cpu");
        if (perCpu != null && !perCpu.isEmpty()) {
            body.addView(sparkRow(perCpu, dp(26)));
        }

        List<Object> hist = DevProtocol.list(f, "cpu_hist");
        if (hist != null && hist.size() > 1) {
            body.addView(header("CPU history"));
            body.addView(sparkRow(hist, dp(96)));
        }

        StringBuilder meta = new StringBuilder();
        List<Object> load = DevProtocol.list(f, "load");
        if (load != null && load.size() == 3
                && (n(load.get(0)) > 0 || n(load.get(1)) > 0 || n(load.get(2)) > 0)) {
            meta.append(String.format(Locale.US, "load %.2f %.2f %.2f",
                    n(load.get(0)), n(load.get(1)), n(load.get(2))));
        }
        long up = DevProtocol.num(f, "uptime_s", 0);
        if (up > 0) {
            if (meta.length() > 0) meta.append("   ·   ");
            meta.append("up ").append(Fmt.duration(up));
        }
        if (meta.length() > 0) meta.append("   ·   ");
        meta.append(cores).append(cores == 1 ? " core" : " cores");
        body.addView(line(meta.toString(), 0xFF8B8B8B));
    }

    /** A row of thin bottom-aligned bars, heights scaled to the max value in {@code vals}. */
    private View sparkRow(List<Object> vals, int heightPx) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
        rp.topMargin = dp(6);
        rp.bottomMargin = dp(6);
        row.setLayoutParams(rp);

        double max = 1;
        for (Object o : vals) max = Math.max(max, n(o));
        for (Object o : vals) {
            double v = Math.max(0, n(o));
            int h = (int) Math.round(heightPx * Math.min(1.0, v / max));
            View bar = new View(ctx);
            bar.setBackgroundColor(v / max >= 0.85 ? 0xFFC88F87 : 0xFFEDEDED);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, Math.max(1, h), 1f);
            bp.leftMargin = 1;
            bp.rightMargin = 1;
            bp.gravity = Gravity.BOTTOM;
            row.addView(bar, bp);
        }
        return row;
    }

    private static double n(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }
}
