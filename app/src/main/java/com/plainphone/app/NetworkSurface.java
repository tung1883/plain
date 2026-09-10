package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.List;
import java.util.Map;

/** Interfaces with live throughput, listening ports, and since-boot totals. */
final class NetworkSurface extends MetricSurface {

    NetworkSurface(Context ctx) { super(ctx); }

    @Override long pollMs() { return 2000; }
    @Override Map<String, Object> requestFrame(long ch) { return DevProtocol.netGet(ch); }
    @Override String responseType() { return DevProtocol.T_NET; }

    @Override
    @SuppressWarnings("unchecked")
    void render(Map<String, Object> f) {
        long rx = DevProtocol.num(f, "rx_total", 0);
        long tx = DevProtocol.num(f, "tx_total", 0);
        body.addView(line("↓ " + Fmt.bytes(rx) + "      ↑ " + Fmt.bytes(tx), Color.WHITE));

        List<Object> ifaces = DevProtocol.list(f, "ifaces");
        if (ifaces != null) {
            for (Object o : ifaces) {
                if (!(o instanceof Map)) continue;
                Map<String, Object> d = (Map<String, Object>) o;
                LinearLayout c = card();
                c.addView(line(str(d, "name"), Color.WHITE));
                c.addView(line("↓ " + Fmt.rate(DevProtocol.dbl(d, "rx_bps", 0))
                        + "   ↑ " + Fmt.rate(DevProtocol.dbl(d, "tx_bps", 0)), 0xFF8B8B8B));
                List<Object> addrs = DevProtocol.list(d, "addrs");
                if (addrs != null && !addrs.isEmpty()) {
                    StringBuilder a = new StringBuilder();
                    for (Object x : addrs) {
                        if (a.length() > 0) a.append("  ");
                        a.append(x);
                    }
                    c.addView(line(a.toString(), 0xFF8B8B8B));
                }
                long mtu = DevProtocol.num(d, "mtu", 0);
                if (mtu > 0) c.addView(line("mtu " + mtu + "  ·  " + str(d, "mac"), 0xFF5A5A5A));
                body.addView(c);
            }
        }

        List<Object> ports = DevProtocol.list(f, "ports");
        if (ports != null && !ports.isEmpty()) {
            body.addView(header("Listening"));
            for (Object o : ports) {
                if (!(o instanceof Map)) continue;
                Map<String, Object> p = (Map<String, Object>) o;
                List<Object> pids = DevProtocol.list(p, "pids");
                String pid = pids != null && !pids.isEmpty() ? String.valueOf(pids.get(0)) : "—";
                body.addView(keyval(
                        str(p, "proto") + "  " + str(p, "addr") + ":" + DevProtocol.num(p, "port", 0),
                        "pid " + pid, 0xFF8B8B8B));
            }
        }
    }

    private static String str(Map<String, Object> m, String k) {
        String v = DevProtocol.str(m, k);
        return v == null ? "" : v;
    }
}
