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
                    int n = ch.size();
                    if (n >= 2 && ch.get(n - 2).b == 'u' && ch.get(n - 1).b == 'o'
                            && ch.get(n - 2).m == 0 && ch.get(n - 1).m == 0) {
                        ch.get(n - 2).m = '+';
                        ch.get(n - 1).m = '+';
                    } else {
                        int v = lastV(ch);
                        if (v >= 0 && "ou".indexOf(ch.get(v).b) >= 0 && ch.get(v).m != '^') {
                            if (!mark(ch, v, '+')) ch.add(new Ch(c, false));
                        } else {
                            ch.add(new Ch(c, false));
                        }
                    }
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
                if (n >= 2 && ch.get(n - 2).b == 'u' && ch.get(n - 1).b == 'o'
                        && ch.get(n - 2).m == 0 && ch.get(n - 1).m == 0) {
                    ch.get(n - 2).m = '+';
                    ch.get(n - 1).m = '+';
                    continue;
                }
                int v = lastV(ch);
                if (v >= 0 && v == n - 1 && "aou".indexOf(ch.get(v).b) >= 0 && ch.get(v).m != '^') {
                    char m = ch.get(v).b == 'a' ? '(' : '+';
                    if (!mark(ch, v, m)) ch.add(new Ch('w', up));
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
