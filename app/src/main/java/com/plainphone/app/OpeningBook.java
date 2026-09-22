package com.plainphone.app;

import android.content.Context;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Classifies a game's SAN move list against the public-domain ECO opening table
 *  (assets/chess_eco.json, lichess-org/chess-openings) — walks the mainline move
 *  by move down a trie of known lines and reports the deepest one matched, the same
 *  "longest known prefix wins" rule every ECO-tagged database uses. Loaded once into
 *  memory and reused for both the live board (as moves are played) and the Save Game /
 *  Edit metadata ECO default. */
final class OpeningBook {
    private static final class Node {
        final Map<String, Node> children = new HashMap<>();
        String eco, name;
    }

    private static volatile Node root;

    /** {eco, name} for the longest opening-book line matching a prefix of {@code sans},
     *  or null if the game is past known theory (or empty) at this point. */
    static synchronized String[] classify(Context ctx, List<String> sans) {
        ensureLoaded(ctx);
        Node n = root, best = null;
        for (String san : sans) {
            Node next = n.children.get(san);
            if (next == null) break;
            n = next;
            if (n.eco != null) best = n;
        }
        return best == null ? null : new String[]{best.eco, best.name};
    }

    private static void ensureLoaded(Context ctx) {
        if (root != null) return;
        Node r = new Node();
        try (InputStream is = ctx.getAssets().open("chess_eco.json")) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = is.read(chunk)) >= 0) buf.write(chunk, 0, read);
            JSONArray rows = new JSONArray(buf.toString("UTF-8"));
            for (int i = 0; i < rows.length(); i++) {
                JSONArray row = rows.getJSONArray(i);
                String eco = row.getString(0);
                String name = row.getString(1);
                JSONArray sans = row.getJSONArray(2);
                Node n = r;
                for (int j = 0; j < sans.length(); j++) {
                    n = n.children.computeIfAbsent(sans.getString(j), k -> new Node());
                }
                n.eco = eco;
                n.name = name;
            }
        } catch (Exception ignored) { }
        root = r;
    }
}
