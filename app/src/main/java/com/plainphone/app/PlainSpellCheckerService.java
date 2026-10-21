package com.plainphone.app;

import android.content.Context;
import android.service.textservice.SpellCheckerService;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The system spell checker behind the red underlines in text fields (pick it in the phone's
 * spell-checker settings). It uses the keyboard's own word lists: unknown words are marked and get
 * the same fixes the keyboard offers, accents and umlauts restored first. Names (capitalised
 * words) are left alone, as are words with digits.
 */
public class PlainSpellCheckerService extends SpellCheckerService {

    private static final Map<String, SpellChecker> CHECKERS = new HashMap<>();

    /** One checker per language, shared by all sessions; used under the lock because it keeps scratch tables. */
    private static SpellChecker checker(Context ctx, String lang) {
        SpellChecker c = CHECKERS.get(lang);
        if (c == null) {
            c = new SpellChecker(new AssetWords(ctx));
            c.setLanguages(lang, Collections.<String>emptyList());
            CHECKERS.put(lang, c);
        }
        return c;
    }

    @Override
    public Session createSession() {
        return new Session() {
            private String lang;

            @Override
            public void onCreate() {
                lang = languageOf(getLocale());
            }

            @Override
            public SuggestionsInfo onGetSuggestions(TextInfo textInfo, int suggestionsLimit) {
                String word = textInfo.getText();
                SuggestionsInfo fine = new SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, new String[0]);
                if (lang == null || skip(word)) return fine;
                List<SpellChecker.Hit> hits;
                synchronized (CHECKERS) {
                    SpellChecker sc = checker(PlainSpellCheckerService.this, lang);
                    if (sc.known(word)) return fine;
                    hits = sc.suggest(word, Math.max(1, Math.min(suggestionsLimit, 5)));
                }
                if (hits.isEmpty()) return new SuggestionsInfo(0, new String[0]);   // unknown, but nothing near it: leave it
                List<String> out = new ArrayList<>();
                for (SpellChecker.Hit h : hits) out.add(matchCase(word, h.word));
                return new SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO, out.toArray(new String[0]));
            }
        };
    }

    /** Our language id for a locale such as "en_US", or null when we have no word list for it. */
    static String languageOf(String locale) {
        if (locale == null || locale.length() < 2) return null;
        String l = locale.substring(0, 2).toLowerCase(Locale.ROOT);
        switch (l) {
            case "en": case "de": case "fr": case "es": case "ru": case "vi": return l;
            default: return null;
        }
    }

    /** Words not worth checking: short ones, names (capitalised), anything with digits or odd characters. */
    static boolean skip(String w) {
        if (w == null || w.length() < 3) return true;
        if (Character.isUpperCase(w.charAt(0))) return true;
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (!Character.isLetter(c) && c != '\'' && c != '’' && c != '-') return true;
        }
        return false;
    }

    private static String matchCase(String typed, String word) {
        if (!typed.isEmpty() && Character.isUpperCase(typed.charAt(0))) {
            return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
        }
        return word;
    }
}
