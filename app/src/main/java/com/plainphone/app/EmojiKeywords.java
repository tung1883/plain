package com.plainphone.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Emoji search by name and keyword, from assets/kb/emoji_keywords.tsv (see tools/emoji/build_keywords.js). */
final class EmojiKeywords {

    static final int CAP = 120;

    private static Map<String, String> loaded;

    private EmojiKeywords() {}

    /** Emoji matching {@code query}, best first. */
    static List<String> search(Context ctx, String query) {
        return search(load(ctx), query, CAP);
    }

    private static synchronized Map<String, String> load(Context ctx) {
        if (loaded != null) return loaded;
        Map<String, String> map = new LinkedHashMap<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                ctx.getAssets().open("kb/emoji_keywords.tsv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab > 0) map.put(line.substring(0, tab), TextMatch.fold(line.substring(tab + 1).replace('|', '\n')));
            }
        } catch (java.io.IOException e) {
            // no keyword file: search finds nothing
        }
        loaded = map;
        return map;
    }

    /**
     * Pure ranking over {@code keywords} (emoji -> folded names, one per line, the name first). Every
     * word of the query must occur; the first word decides the rank: starts the name, then starts a
     * keyword, then occurs inside one.
     */
    static List<String> search(Map<String, String> keywords, String query, int cap) {
        String folded = TextMatch.fold(query);
        if (folded.isEmpty()) return new ArrayList<>();
        String[] words = folded.split(" ");
        List<List<String>> ranked = new ArrayList<>();
        for (int i = 0; i < 3; i++) ranked.add(new ArrayList<>());
        for (Map.Entry<String, String> e : keywords.entrySet()) {
            String text = e.getValue();
            boolean all = true;
            for (String w : words) {
                if (!text.contains(w)) {
                    all = false;
                    break;
                }
            }
            if (!all) continue;
            int nameEnd = text.indexOf('\n');
            String name = nameEnd < 0 ? text : text.substring(0, nameEnd);
            int rank;
            if (name.startsWith(words[0])) rank = 0;
            else if (startsWord(text, words[0])) rank = 1;
            else rank = 2;
            ranked.get(rank).add(e.getKey());
        }
        List<String> out = new ArrayList<>();
        for (List<String> r : ranked) {
            for (String emoji : r) {
                if (out.size() >= cap) return out;
                out.add(emoji);
            }
        }
        return out;
    }

    private static boolean startsWord(String text, String word) {
        for (int i = text.indexOf(word); i >= 0; i = text.indexOf(word, i + 1)) {
            if (i == 0 || text.charAt(i - 1) == '\n' || text.charAt(i - 1) == ' ') return true;
        }
        return false;
    }
}
