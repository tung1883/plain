package com.plainphone.app;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Unikey-style Vietnamese typing (Telex and VNI). The whole word is replayed from its raw
 * keystrokes every time, so tone and mark keys work anywhere in the word, pressing one twice undoes
 * it, and the tone lands on the right vowel (modern style: hoà, not hòa).
 */
final class VietnameseEngine {

    private static final class Ch {
        char b;           // lower-case base letter (or the literal key)
        char m;           // mark: 0, '^' (â ê ô), '(' (ă), '+' (ơ ư), '-' (đ)
        boolean up;
        boolean w;        // an ư made by a lone w, which a second w turns back into 'w'

        Ch(char b, boolean up) {
            this.b = b;
            this.up = up;
        }
    }

    private VietnameseEngine() {
    }

    private static boolean isV(Ch c) {
        return "aeiouy".indexOf(c.b) >= 0;
    }

    private static int lastV(List<Ch> ch) {
        for (int i = ch.size() - 1; i >= 0; i--) if (isV(ch.get(i))) return i;
        return -1;
    }

    /**
     * The vowels the w (Telex) and 7 (VNI) keys act on: the last run of vowels in the word, which may
     * be followed by a final consonant (c ch g gh h m n ng nh p t), so "duongw" reaches back over ng.
     * Returns {first, last, number of letters after the run}; null when there is no such run. The u of
     * "qu" and the i of "gi" belong to the consonant, not the run.
     */
    private static int[] vowelRun(List<Ch> ch) {
        int e = ch.size() - 1;
        int trail = 0;
        while (e >= 0 && !isV(ch.get(e))) {
            if (++trail > 3 || "cghmnpt".indexOf(ch.get(e).b) < 0 || ch.get(e).m != 0) return null;
            e--;
        }
        if (e < 0) return null;
        int s = e;
        while (s > 0 && isV(ch.get(s - 1))) s--;
        if (s < e && s > 0 && ch.get(s - 1).b == 'q' && ch.get(s).b == 'u') s++;
        if (s < e && s > 0 && ch.get(s - 1).b == 'g' && ch.get(s).b == 'i') s++;
        return new int[]{s, e, trail};
    }

    /**
     * Put the horn (ư ơ) or, for Telex w, the breve (ă) on the right vowel of the run: ư-ơ together in
     * "uo" (ươ), the a of "oa" or of "ua" before a final consonant (ă), a lone o (ơ), the first u (ư:
     * "ui" gives ưi, "uu" gives ưu, "ua" gives ưa) and a lone a. Pressed again it takes the mark off.
     * Returns 1 when a mark was put on, 0 when one was taken off, -1 when no vowel can take it.
     */
    private static int applyHorn(List<Ch> ch, int s, int e, int trail, boolean breve) {
        for (int i = s; i < e; i++) {
            Ch u = ch.get(i), o = ch.get(i + 1);
            if (u.b == 'u' && o.b == 'o' && u.m != '^' && o.m != '^') {
                if (u.m == '+' && o.m == '+') {
                    u.m = 0;
                    o.m = 0;
                    return 0;
                }
                u.m = '+';
                o.m = '+';
                return 1;
            }
        }
        if (breve) {
            for (int i = s; i < e; i++) {
                Ch p = ch.get(i), a = ch.get(i + 1);
                if (a.b == 'a' && a.m != '^' && (p.b == 'o' || (p.b == 'u' && trail > 0))) {
                    return mark(ch, i + 1, '(') ? 1 : 0;
                }
            }
        }
        for (int i = s; i <= e; i++) {
            Ch c = ch.get(i);
            if (c.b == 'o' && c.m != '^') return mark(ch, i, '+') ? 1 : 0;
        }
        for (int i = s; i <= e; i++) {
            Ch c = ch.get(i);
            if (c.b == 'u' && c.m != '^' && !(i < e && ch.get(i + 1).b == 'y')) return mark(ch, i, '+') ? 1 : 0;
        }
        if (breve && s == e && ch.get(s).b == 'a' && ch.get(s).m != '^') return mark(ch, s, '(') ? 1 : 0;
        return -1;
    }

    private static char toneKey(char c) {
        switch (c) {
            case '1': return 's';
            case '2': return 'f';
            case '3': return 'r';
            case '4': return 'x';
            case '5': return 'j';
            default: return 0;
        }
    }

    private static String toneMark(char t) {
        switch (t) {
            case 's': return "́";
            case 'f': return "̀";
            case 'r': return "̉";
            case 'x': return "̃";
            case 'j': return "̣";
            default: return "";
        }
    }

    private static String markChar(char m) {
        switch (m) {
            case '^': return "̂";
            case '(': return "̆";
            case '+': return "̛";
            default: return "";
        }
    }

    /** Toggle mark {@code m} on vowel {@code vi}; false when it was already there and got removed. */
    private static boolean mark(List<Ch> ch, int vi, char m) {
        Ch c = ch.get(vi);
        if (c.m == m) {
            c.m = 0;
            return false;
        }
        c.m = m;
        return true;
    }

    /**
     * The keystrokes that make {@code word}, so a word already in the field can be typed on: "việt" is
     * "vieetj" in Telex. Null when the word is not one this engine would write that way (old-style
     * tones such as "hòa", or letters it does not know), in which case it is better left alone.
     */
    static String raw(String word, boolean vni) {
        StringBuilder out = new StringBuilder();
        char tone = 0;
        String nfd = Normalizer.normalize(word, Normalizer.Form.NFD);
        for (int i = 0; i < nfd.length(); i++) {
            char c = nfd.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) return null;   // a mark with no letter
            if (c == '\u0111' || c == '\u0110') {          // đ Đ
                out.append(c == '\u0111' ? 'd' : 'D').append(vni ? "9" : (c == '\u0111' ? "d" : "D"));
                continue;
            }
            if (c > 'z' || !Character.isLetter(c)) return null;
            out.append(c);
            while (i + 1 < nfd.length() && Character.getType(nfd.charAt(i + 1)) == Character.NON_SPACING_MARK) {
                char mk = nfd.charAt(++i);
                char lc = Character.toLowerCase(c);
                if (mk == '\u0302' && "aeo".indexOf(lc) >= 0) out.append(vni ? "6" : String.valueOf(c));
                else if (mk == '\u0306' && lc == 'a') out.append(vni ? "8" : "w");
                else if (mk == '\u031B' && "ou".indexOf(lc) >= 0) out.append(vni ? "7" : "w");
                else if (tone == 0 && toneOf(mk, vni) != 0) tone = toneOf(mk, vni);
                else return null;
            }
        }
        if (tone != 0) out.append(tone);
        String r = out.toString();
        return compose(r, vni).equals(Normalizer.normalize(word, Normalizer.Form.NFC)) ? r : null;
    }

    private static char toneOf(char mark, boolean vni) {
        switch (mark) {
            case '\u0301': return vni ? '1' : 's';
            case '\u0300': return vni ? '2' : 'f';
            case '\u0309': return vni ? '3' : 'r';
            case '\u0303': return vni ? '4' : 'x';
            case '\u0323': return vni ? '5' : 'j';
            default: return 0;
        }
    }

    /** The word typed as {@code raw}, in Telex or (when {@code vni}) VNI. */
    static String compose(String raw, boolean vni) {
        List<Ch> ch = new ArrayList<>();
        char tone = 0;
        for (int p = 0; p < raw.length(); p++) {
            char c = raw.charAt(p);
            char lc = Character.toLowerCase(c);
            boolean up = c != lc;
            boolean hasV = lastV(ch) >= 0;
            if (vni) {
                char tk = toneKey(c);
                if (tk != 0) {
                    if (hasV) {
                        if (tone == tk) {
                            tone = 0;
                            ch.add(new Ch(c, false));
                        } else {
                            tone = tk;
                        }
                    } else {
                        ch.add(new Ch(c, false));
                    }
                    continue;
                }
                if (c == '0') {
                    if (tone != 0) tone = 0;
                    else ch.add(new Ch(c, false));
                    continue;
                }
                if (c == '6') {
                    int v = lastV(ch);
                    if (v >= 0 && "aeo".indexOf(ch.get(v).b) >= 0 && ch.get(v).m != '+' && ch.get(v).m != '(') {
                        if (!mark(ch, v, '^')) ch.add(new Ch(c, false));
                    } else {
                        ch.add(new Ch(c, false));
                    }
                    continue;
                }
                if (c == '7') {
                    int[] run = vowelRun(ch);
                    if (run == null || applyHorn(ch, run[0], run[1], run[2], false) != 1) ch.add(new Ch(c, false));
                    continue;
                }
                if (c == '8') {
                    int v = lastV(ch);
                    if (v >= 0 && ch.get(v).b == 'a' && ch.get(v).m != '^') {
                        if (!mark(ch, v, '(')) ch.add(new Ch(c, false));
                    } else {
                        ch.add(new Ch(c, false));
                    }
                    continue;
                }
                if (c == '9') {
                    Ch l = ch.isEmpty() ? null : ch.get(ch.size() - 1);
                    if (l != null && l.b == 'd') {
                        if (l.m == '-') {
                            l.m = 0;
                            ch.add(new Ch(c, false));
                        } else {
                            l.m = '-';
                        }
                    } else {
                        ch.add(new Ch(c, false));
                    }
                    continue;
                }
                ch.add(new Ch(lc, up));
                continue;
            }

            // ---- Telex
            if ("sfrxj".indexOf(lc) >= 0 && hasV) {
                if (tone == lc) {
                    tone = 0;
                    ch.add(new Ch(lc, up));
                } else {
                    tone = lc;
                }
                continue;
            }
            if (lc == 'z' && tone != 0) {
                tone = 0;
                continue;
            }
            if (lc == 'd') {
                Ch l = ch.isEmpty() ? null : ch.get(ch.size() - 1);
                if (l != null && l.b == 'd' && l.m == 0) {
                    l.m = '-';
                } else if (l != null && l.b == 'd' && l.m == '-') {
                    l.m = 0;
                    ch.add(new Ch('d', up));
                } else {
                    ch.add(new Ch('d', up));
                }
                continue;
            }
            if (lc == 'a' || lc == 'e' || lc == 'o') {
                int v = lastV(ch);
                if (v >= 0 && ch.get(v).b == lc && (ch.get(v).m == 0 || ch.get(v).m == '^')) {
                    if (!mark(ch, v, '^')) ch.add(new Ch(lc, up));
                } else {
                    ch.add(new Ch(lc, up));
                }
                continue;
            }
            if (lc == 'w') {
                int n = ch.size();
                Ch l = n > 0 ? ch.get(n - 1) : null;
                if (l != null && l.w) {
                    ch.set(n - 1, new Ch('w', l.up));
                    continue;
                }
                int[] run = vowelRun(ch);
                int done = run == null ? -1 : applyHorn(ch, run[0], run[1], run[2], true);
                if (done == 1) continue;
                if (done == 0) {
                    ch.add(new Ch('w', up));
                    continue;
                }
                Ch u = new Ch('u', up);
                u.m = '+';
                u.w = true;
                ch.add(u);
                continue;
            }
            ch.add(new Ch(lc, up));
        }

        // where the tone goes
        List<Integer> vs = new ArrayList<>();
        for (int i = 0; i < ch.size(); i++) if (isV(ch.get(i))) vs.add(i);
        if (!ch.isEmpty() && ch.get(0).b == 'q' && !vs.isEmpty() && vs.get(0) == 1 && ch.get(1).b == 'u') vs.remove(0);
        if (!ch.isEmpty() && ch.get(0).b == 'g' && !vs.isEmpty() && vs.get(0) == 1 && ch.get(1).b == 'i'
                && vs.size() > 1) vs.remove(0);
        int tp = -1;
        if (tone != 0 && !vs.isEmpty()) {
            int lastMarked = -1;
            for (int i : vs) if (ch.get(i).m != 0 && ch.get(i).m != '-') lastMarked = i;
            if (lastMarked >= 0) {
                tp = lastMarked;
            } else if (vs.size() == 1) {
                tp = vs.get(0);
            } else {
                boolean fin = ch.size() - 1 > vs.get(vs.size() - 1);
                if (vs.size() == 2) {
                    String pr = "" + ch.get(vs.get(0)).b + ch.get(vs.get(1)).b;
                    boolean modern = pr.equals("oa") || pr.equals("oe") || pr.equals("uy");
                    tp = (!fin && !modern) ? vs.get(0) : vs.get(1);
                } else {
                    tp = vs.get(1);
                }
            }
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ch.size(); i++) {
            Ch c = ch.get(i);
            if (c.m == '-') {
                out.append(c.up ? 'Đ' : 'đ');
                continue;
            }
            String base = String.valueOf(c.b);
            if (c.up) base = base.toUpperCase(Locale.ROOT);
            StringBuilder s = new StringBuilder(base);
            if (c.m != 0) s.append(markChar(c.m));
            if (i == tp) s.append(toneMark(tone));
            out.append(Normalizer.normalize(s, Normalizer.Form.NFC));
        }
        return Normalizer.normalize(out, Normalizer.Form.NFC);
    }
}
