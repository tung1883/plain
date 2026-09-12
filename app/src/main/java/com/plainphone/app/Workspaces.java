package com.plainphone.app;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The set of named workspaces (like virtual desktops). The index
 * ({@code filesDir/workspaces/index.txt}) holds the current id on line 1, then
 * one {@code id \t name} per workspace; each workspace's panels live in its own
 * {@link WorkspaceStore} file.
 */
final class Workspaces {

    private Workspaces() {}

    static final class Meta {
        final String id;
        String name;
        Meta(String id, String name) { this.id = id; this.name = name; }
    }

    private static File index(Context c) {
        return new File(WorkspaceStore.dir(c), "index.txt");
    }

    /** Ordered list; never empty (creates a default, migrating any legacy layout). */
    static List<Meta> list(Context c) {
        List<Meta> out = read(c);
        if (out.isEmpty()) {
            Meta m = new Meta(newId(), "Workspace 1");
            out.add(m);
            // migrate a pre-multi-workspace layout
            File legacy = new File(c.getFilesDir(), "workspace.txt");
            if (legacy.exists()) {
                //noinspection ResultOfMethodCallIgnored
                legacy.renameTo(WorkspaceStore.file(c, m.id));
            }
            writeAll(c, out, m.id);
        }
        return out;
    }

    static String currentId(Context c) {
        List<Meta> all = list(c);
        String cur = firstLine(c);
        for (Meta m : all) if (m.id.equals(cur)) return cur;
        return all.get(0).id;
    }

    static Meta current(Context c) {
        String id = currentId(c);
        for (Meta m : list(c)) if (m.id.equals(id)) return m;
        return list(c).get(0);
    }

    static void setCurrent(Context c, String id) {
        writeAll(c, list(c), id);
    }

    static Meta create(Context c, String name) {
        List<Meta> all = list(c);
        Meta m = new Meta(newId(), name == null || name.trim().isEmpty()
                ? "Workspace " + (all.size() + 1) : name.trim());
        all.add(m);
        writeAll(c, all, m.id);
        return m;
    }

    static void rename(Context c, String id, String name) {
        if (name == null || name.trim().isEmpty()) return;
        List<Meta> all = list(c);
        for (Meta m : all) if (m.id.equals(id)) m.name = name.trim();
        writeAll(c, all, currentId(c));
    }

    /** Deleting the last workspace is allowed — {@link #list} recreates a fresh
     *  default one next time it's read. */
    static void delete(Context c, String id) {
        List<Meta> all = list(c);
        String cur = currentId(c);
        all.removeIf(m -> m.id.equals(id));
        WorkspaceStore.deleteFile(c, id);
        writeAll(c, all, all.isEmpty() ? "" : cur.equals(id) ? all.get(0).id : cur);
    }

    // --- io ---------------------------------------------------------

    private static String newId() {
        return Long.toString(System.currentTimeMillis(), 36);
    }

    private static String firstLine(Context c) {
        List<String> lines = readLines(c);
        return lines.isEmpty() ? "" : lines.get(0).trim();
    }

    private static List<Meta> read(Context c) {
        List<Meta> out = new ArrayList<>();
        List<String> lines = readLines(c);
        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split("\t", 2);
            if (p.length == 2 && !p[0].isEmpty()) out.add(new Meta(p[0], p[1]));
        }
        return out;
    }

    private static List<String> readLines(Context c) {
        List<String> out = new ArrayList<>();
        File f = index(c);
        if (!f.exists()) return out;
        try (java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) out.add(line);
        } catch (Exception ignored) {
        }
        return out;
    }

    private static void writeAll(Context c, List<Meta> all, String currentId) {
        StringBuilder sb = new StringBuilder();
        sb.append(currentId).append('\n');
        for (Meta m : all) {
            sb.append(m.id).append('\t').append(m.name.replace('\t', ' ').replace('\n', ' ')).append('\n');
        }
        try (java.io.FileOutputStream o = new java.io.FileOutputStream(index(c))) {
            o.write(sb.toString().getBytes("UTF-8"));
        } catch (Exception ignored) {
        }
    }
}
