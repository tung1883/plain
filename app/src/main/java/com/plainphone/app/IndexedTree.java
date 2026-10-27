package com.plainphone.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** The indexed files laid out as a folder tree, with counts, ready to be shown with folders opened or closed. */
final class IndexedTree {

    private IndexedTree() {}

    /** A folder: its sub-folders, the files directly in it, and the counts for everything below it. */
    static final class Node {
        final String name;
        final String key;       // full path, the same for the same folder
        final Map<String, Node> dirs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final List<ContentDb.Entry> files = new ArrayList<>();
        int total;              // files in this folder and below
        int bad;                // of those, the ones that are not read

        Node(String name, String key) {
            this.name = name;
            this.key = key;
        }
    }

    /**
     * The entries whose path holds every word of {@code query} (case and accents ignored): those whose
     * file name holds them all first, then the ones matched through the folder. At most {@code cap}.
     */
    static List<ContentDb.Entry> filter(List<ContentDb.Entry> all, String query, int cap) {
        String folded = TextMatch.fold(query);
        if (folded.isEmpty()) return new ArrayList<>();
        String[] words = folded.split(" ");
        List<ContentDb.Entry> byName = new ArrayList<>();
        List<ContentDb.Entry> byFolder = new ArrayList<>();
        for (ContentDb.Entry e : all) {
            String path = TextMatch.fold(e.path);
            if (!holdsAll(path, words)) continue;
            int slash = e.path.lastIndexOf('/');
            (holdsAll(TextMatch.fold(e.path.substring(slash + 1)), words) ? byName : byFolder).add(e);
        }
        byName.addAll(byFolder);
        return byName.size() > cap ? new ArrayList<>(byName.subList(0, cap)) : byName;
    }

    private static boolean holdsAll(String text, String[] words) {
        for (String w : words) if (!text.contains(w)) return false;
        return true;
    }

    /** One line of the flattened tree. Exactly one of {@code folder} / {@code file} is set. */
    static final class Row {
        final int depth;
        final Node folder;
        final ContentDb.Entry file;
        final boolean open;

        Row(int depth, Node folder, ContentDb.Entry file, boolean open) {
            this.depth = depth;
            this.folder = folder;
            this.file = file;
            this.open = open;
        }
    }

    /**
     * Builds the tree. {@code roots} is a list of {root folder path, label} (the storages); a file
     * below one of them goes under that label, any other file under its own absolute path.
     * Returns the top-level nodes, sorted by name.
     */
    static List<Node> build(List<ContentDb.Entry> entries, List<String[]> roots) {
        Map<String, Node> top = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ContentDb.Entry e : entries) {
            String label = null, rest = null, rootPath = "";
            for (String[] r : roots) {
                String prefix = r[0].endsWith("/") ? r[0] : r[0] + "/";
                if (e.path.startsWith(prefix) && (rootPath.length() < prefix.length())) {
                    label = r[1];
                    rest = e.path.substring(prefix.length());
                    rootPath = prefix;
                }
            }
            if (label == null) {
                label = "Other";
                rest = e.path.startsWith("/") ? e.path.substring(1) : e.path;
                rootPath = "/";
            }
            Node node = top.get(label);
            if (node == null) top.put(label, node = new Node(label, rootPath));
            String[] parts = rest.split("/");
            node.total++;
            if (!e.status.equals("ok")) node.bad++;
            String key = node.key;
            for (int i = 0; i < parts.length - 1; i++) {
                key = key + parts[i] + "/";
                Node child = node.dirs.get(parts[i]);
                if (child == null) node.dirs.put(parts[i], child = new Node(parts[i], key));
                child.total++;
                if (!e.status.equals("ok")) child.bad++;
                node = child;
            }
            node.files.add(e);
        }
        for (Node n : top.values()) sortFiles(n);
        return new ArrayList<>(top.values());
    }

    /** Files that were not read first, then by name. */
    private static void sortFiles(Node n) {
        java.util.Collections.sort(n.files, (a, b) -> {
            boolean ba = !a.status.equals("ok"), bb = !b.status.equals("ok");
            if (ba != bb) return ba ? -1 : 1;
            return a.path.compareToIgnoreCase(b.path);
        });
        for (Node c : n.dirs.values()) sortFiles(c);
    }

    /** A storage and any folder with files that were not read start open, the rest closed. */
    static boolean defaultOpen(Node n, int depth) {
        return depth == 0 || n.bad > 0;
    }

    /** The lines to show: {@code flipped} holds the keys of folders whose open state differs from the default. */
    static List<Row> rows(List<Node> top, Set<String> flipped) {
        List<Row> out = new ArrayList<>();
        for (Node n : top) add(out, n, 0, flipped);
        return out;
    }

    private static void add(List<Row> out, Node n, int depth, Set<String> flipped) {
        boolean open = defaultOpen(n, depth) != flipped.contains(n.key);
        out.add(new Row(depth, n, null, open));
        if (!open) return;
        for (Node c : n.dirs.values()) add(out, c, depth + 1, flipped);
        for (ContentDb.Entry f : n.files) out.add(new Row(depth + 1, null, f, false));
    }
}
