package com.plainphone.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The keyboard's language data, shipped in {@code assets/kb}: {@code words_<lang>.txt} (one word per
 * line, most common first) and {@code ngram_<lang>.txt} (which words follow which).
 */
final class AssetWords implements SpellChecker.Source, NGramModel.Source {

    private final Context ctx;

    AssetWords(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    @Override
    public List<String> words(String lang) {
        return read("kb/words_" + lang + ".txt");
    }

    @Override
    public List<String> lines(String lang) {
        return read("kb/ngram_" + lang + ".txt");
    }

    private List<String> read(String path) {
        List<String> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                ctx.getAssets().open(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isEmpty()) out.add(line);
            }
        } catch (IOException ignored) {
            // no data for this language: nothing is known, so nothing is flagged or suggested
        }
        return out;
    }
}
