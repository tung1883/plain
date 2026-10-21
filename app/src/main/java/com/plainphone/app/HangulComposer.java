package com.plainphone.app;

import java.util.HashMap;
import java.util.Map;

/** Korean 2-beolsik: the Latin keys typed (rkskek ...) become jamo and are assembled into syllables. */
final class HangulComposer {

    private static final String CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
    private static final String JUNG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ";
    private static final String JONG = "\0ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ";

    private static final Map<String, Character> VCOMB = new HashMap<>();
    private static final Map<String, Character> CCOMB = new HashMap<>();
    private static final Map<Character, Character> KEYS = new HashMap<>();

    static {
        String[][] v = {{"ㅗㅏ", "ㅘ"}, {"ㅗㅐ", "ㅙ"}, {"ㅗㅣ", "ㅚ"}, {"ㅜㅓ", "ㅝ"}, {"ㅜㅔ", "ㅞ"}, {"ㅜㅣ", "ㅟ"}, {"ㅡㅣ", "ㅢ"}};
        for (String[] e : v) VCOMB.put(e[0], e[1].charAt(0));
        String[][] c = {{"ㄱㅅ", "ㄳ"}, {"ㄴㅈ", "ㄵ"}, {"ㄴㅎ", "ㄶ"}, {"ㄹㄱ", "ㄺ"}, {"ㄹㅁ", "ㄻ"}, {"ㄹㅂ", "ㄼ"},
                {"ㄹㅅ", "ㄽ"}, {"ㄹㅌ", "ㄾ"}, {"ㄹㅍ", "ㄿ"}, {"ㄹㅎ", "ㅀ"}, {"ㅂㅅ", "ㅄ"}};
        for (String[] e : c) CCOMB.put(e[0], e[1].charAt(0));
        String lower = "rsefaqtdwczxvgkoijpuhynbml";
        String jamo = "ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎㅏㅐㅑㅓㅔㅕㅗㅛㅜㅠㅡㅣ";
        for (int i = 0; i < lower.length(); i++) KEYS.put(lower.charAt(i), jamo.charAt(i));
        // Shift gives the doubled consonants and the two vowels with a y-glide
        String upper = "REQTWOP";
        String shifted = "ㄲㄸㅃㅆㅉㅒㅖ";
        for (int i = 0; i < upper.length(); i++) KEYS.put(upper.charAt(i), shifted.charAt(i));
    }

    private HangulComposer() {
    }

    /** The jamo on a key ("r" gives ㄱ, "R" gives ㄲ), or null for a key that has none. */
    static String jamoFor(char key, boolean shift) {
        Character j = KEYS.get(shift ? Character.toUpperCase(key) : Character.toLowerCase(key));
        if (j == null) j = KEYS.get(Character.toLowerCase(key));
        return j == null ? null : String.valueOf(j);
    }

    private static boolean isV(char j) {
        return JUNG.indexOf(j) >= 0;
    }

    private static boolean isC(char j) {
        return CHO.indexOf(j) >= 0;
    }

    /** The keys {@code raw} typed, as Korean text. Characters that are not letter keys pass through. */
    static String compose(String raw) {
        StringBuilder out = new StringBuilder();
        int l = -1, v = -1, t = 0;
        for (int p = 0; p < raw.length(); p++) {
            char k = raw.charAt(p);
            Character mapped = KEYS.get(k);
            if (mapped == null) mapped = KEYS.get(Character.toLowerCase(k));   // Shift on a key without a shifted jamo
            if (mapped == null) {
                // another character: finish the syllable and keep it
                flush(out, l, v, t);
                l = v = -1;
                t = 0;
                out.append(k);
                continue;
            }
            char j = mapped;
            if (isV(j)) {
                if (l >= 0 && v < 0) {
                    v = JUNG.indexOf(j);
                } else if (l >= 0 && v >= 0 && t == 0) {
                    Character c = VCOMB.get("" + JUNG.charAt(v) + j);
                    if (c != null) {
                        v = JUNG.indexOf(c);
                    } else {
                        flush(out, l, v, t);
                        l = -1;
                        t = 0;
                        v = JUNG.indexOf(j);
                    }
                } else if (l >= 0 && v >= 0 && t > 0) {
                    // the final consonant moves to the next syllable as its first sound
                    char tj = JONG.charAt(t);
                    char keep = 0, mv = tj;
                    for (Map.Entry<String, Character> e : CCOMB.entrySet()) {
                        if (e.getValue() == tj) {
                            keep = e.getKey().charAt(0);
                            mv = e.getKey().charAt(1);
                        }
                    }
                    t = keep != 0 ? JONG.indexOf(keep) : 0;
                    flush(out, l, v, t);
                    l = CHO.indexOf(mv);
                    v = JUNG.indexOf(j);
                    t = 0;
                } else {
                    flush(out, l, v, t);
                    l = -1;
                    t = 0;
                    v = JUNG.indexOf(j);
                }
            } else if (isC(j)) {
                if (l >= 0 && v >= 0 && t == 0 && JONG.indexOf(j) > 0) {
                    t = JONG.indexOf(j);
                } else if (l >= 0 && v >= 0 && t > 0) {
                    Character c = CCOMB.get("" + JONG.charAt(t) + j);
                    if (c != null && JONG.indexOf(c) > 0) {
                        t = JONG.indexOf(c);
                    } else {
                        flush(out, l, v, t);
                        v = -1;
                        t = 0;
                        l = CHO.indexOf(j);
                    }
                } else {
                    flush(out, l, v, t);
                    v = -1;
                    t = 0;
                    l = CHO.indexOf(j);
                }
            }
        }
        flush(out, l, v, t);
        return out.toString();
    }

    private static void flush(StringBuilder out, int l, int v, int t) {
        if (l >= 0 && v >= 0) out.append((char) (0xAC00 + (l * 21 + v) * 28 + t));
        else if (l >= 0) out.append(CHO.charAt(l));
        else if (v >= 0) out.append(JUNG.charAt(v));
    }
}
