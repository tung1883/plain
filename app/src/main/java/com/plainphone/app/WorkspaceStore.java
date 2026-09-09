package com.plainphone.app;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists one workspace's layout — one line per panel in
 * {@code filesDir/workspaces/<id>.txt} — so leaving and reopening brings back the
 * same windows (and, for shells, reattaches the same daemon session).
 *
 * <p>Line: {@code kind \t hostId \t x \t y \t w \t h \t min \t extra}.
 */
final class WorkspaceStore {

    private WorkspaceStore() {}

    static final class Rec {
        String kind, hostId, extra;
        int x, y, w, h;
        boolean minimized;

        Rec(String kind, String hostId, int x, int y, int w, int h, boolean min, String extra) {
            this.kind = kind;
            this.hostId = hostId == null ? "" : hostId;
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.minimized = min;
            this.extra = extra == null ? "" : extra;
        }
    }

    static File dir(Context c) {
        File d = new File(c.getFilesDir(), "workspaces");
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
        return d;
    }

    static File file(Context c, String id) {
        return new File(dir(c), id + ".txt");
    }

    static List<Rec> load(Context c, String id) {
        List<Rec> out = new ArrayList<>();
        File f = file(c, id);
        if (!f.exists()) return out;
        try (java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t", 8);
                if (p.length < 7) continue;
                try {
                    out.add(new Rec(p[0], p[1],
                            Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                            Integer.parseInt(p[4]), Integer.parseInt(p[5]),
                            "1".equals(p[6]), p.length >= 8 ? p[7] : ""));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    static void save(Context c, String id, List<Rec> recs) {
        StringBuilder sb = new StringBuilder();
        for (Rec r : recs) {
            sb.append(r.kind).append('\t').append(r.hostId).append('\t')
                    .append(r.x).append('\t').append(r.y).append('\t')
                    .append(r.w).append('\t').append(r.h).append('\t')
                    .append(r.minimized ? '1' : '0').append('\t')
                    .append(r.extra.replace('\t', ' ').replace('\n', ' ')).append('\n');
        }
        try (java.io.FileOutputStream o = new java.io.FileOutputStream(file(c, id))) {
            o.write(sb.toString().getBytes("UTF-8"));
        } catch (Exception ignored) {
        }
    }

    static int count(Context c, String id) {
        return load(c, id).size();
    }

    static void deleteFile(Context c, String id) {
        //noinspection ResultOfMethodCallIgnored
        file(c, id).delete();
    }
}
