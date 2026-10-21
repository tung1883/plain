package com.plainphone.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What typing does for the current language: Vietnamese and Korean keys are composed into text that
 * stays underlined until the word ends, Latin languages get word suggestions, and (when on) spell
 * check with auto-correct that backspace undoes. With nothing to do it passes every key straight
 * through. Pure Java: everything it touches in the editor goes through {@link Sink}.
 */
final class TypingController {

    /** The editor and the host app's field, as seen by the controller. */
    interface Sink {
        void text(String s);

        void key(int keyCode, int meta);

        void enter();

        /** Show {@code s} underlined at the cursor as text still being composed; "" removes it. */
        void setComposing(String s);

        /** Keep the composing text as typed. */
        void finishComposing();

        String textBefore(int n);

        void deleteBefore(int n);

        /** android.text.TextUtils cap mode at the cursor: non-zero when the next letter should be a capital. */
        int capsMode();

        /** The candidates or the composition changed. */
        void changed();
    }

    static final int KEY_DEL = 67;   // KeyEvent.KEYCODE_DEL

    private static final Pattern TRAILING_WORD = Pattern.compile("[\\p{L}'’]+$");

    private static final class Undo {
        final String orig, fixed, sep;

        Undo(String orig, String fixed, String sep) {
            this.orig = orig;
            this.fixed = fixed;
            this.sep = sep;
        }
    }

    private final Sink sink;
    private final StringBuilder raw = new StringBuilder();
    private String lang = "en";
    private boolean vni, composeOk = true, suggestOk = true, spellOn, autoOn;
    private SpellChecker spell;
    private NGramModel model;
    private Undo undo;
    private List<String> cands = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private String note = "";

    TypingController(Sink sink) {
        this.sink = sink;
    }

    /** Set the language and the features that apply; {@code spell} may be null when none are needed. */
    void configure(String lang, String layout, boolean spellOn, boolean autoOn, SpellChecker spell) {
        configure(lang, layout, spellOn, autoOn, spell, null);
    }

    /** As above, plus the word-sequence {@code model} (next-word suggestions, phrases, context); may be null. */
    void configure(String lang, String layout, boolean spellOn, boolean autoOn, SpellChecker spell,
                   NGramModel model) {
        if (!lang.equals(this.lang)) commitComposing();
        this.model = model;
        this.lang = lang;
        this.vni = "VNI".equals(layout);
        this.spellOn = spellOn;
        this.autoOn = autoOn;
        this.spell = spell;
        undo = null;
        refresh();
    }

    /**
     * What the focused field allows: {@code compose} false for passwords and non-text fields (keys
     * go through as typed), {@code suggest} false as well for URL, e-mail and no-suggestion fields.
     */
    void setField(boolean compose, boolean suggest) {
        if (!compose) commitComposing();
        composeOk = compose;
        suggestOk = suggest;
        undo = null;
        refresh();
    }

    private boolean composes() {
        return composeOk && (lang.equals("vi") || lang.equals("ko"));
    }

    private boolean assist() {
        return suggestOk && spellOn && spell != null;
    }

    /** Whether the candidate strip has anything to do for this language and field. */
    boolean wantsStrip() {
        return composes() || assist();
    }

    boolean isComposing() {
        return raw.length() > 0;
    }

    List<String> candidates() {
        return cands;
    }

    /** Language tag shown next to a candidate from another language ("" for the typing language). */
    List<String> tags() {
        return tags;
    }

    /** A short remark for the strip when it has no candidates (for example the last correction). */
    String note() {
        return note;
    }

    /** True when the next letter should be a capital (and auto-capitalisation applies). */
    boolean wantsShift() {
        return assist() && !lang.equals("ko") && raw.length() == 0 && sink.capsMode() != 0;
    }

    // ------------------------------------------------------------------- input

    void onText(String s) {
        if (s.isEmpty()) return;
        if (!composes() && !assist()) {
            sink.text(s);
            return;
        }
        if (s.codePointCount(0, s.length()) != 1) {
            commitComposing();
            undo = null;
            sink.text(s);
            refresh();
            return;
        }
        char c = s.charAt(0);
        if (composes()) {
            if (isComposeKey(c)) {
                raw.append(c);
                showComposing();
                return;
            }
            commitComposing();
        }
        if (isSeparator(c)) {
            separate(s);
        } else {
            undo = null;
            sink.text(s);
        }
        refresh();
    }

    void onEnter() {
        if (!composes() && !assist()) {
            sink.enter();
            return;
        }
        commitComposing();
        if (assist() && autoOn) fixLastWord(null);
        undo = null;
        sink.enter();
        refresh();
    }

    void onKey(int keyCode, int meta) {
        if (keyCode == KEY_DEL && meta == 0) {
            backspace();
            return;
        }
        commitComposing();
        undo = null;
        sink.key(keyCode, meta);
        refresh();
    }

    private void backspace() {
        if (raw.length() > 0) {
            raw.setLength(raw.length() - 1);
            if (raw.length() == 0) {
                sink.setComposing("");
                refresh();
            } else {
                showComposing();
            }
            return;
        }
        if (undo != null && assist()) {
            String n = undo.fixed + undo.sep;
            if (n.equals(sink.textBefore(n.length()))) {
                sink.deleteBefore(n.length());
                sink.text(undo.orig);
                spell.ignore(undo.orig);
                undo = null;
                refresh();
                return;
            }
        }
        undo = null;
        sink.key(KEY_DEL, 0);
        refresh();
    }

    /**
     * Pick candidate {@code i} from the strip: it replaces the composition or the word before the
     * cursor, or (when no word is started) is added as the next word. A candidate may be several words.
     */
    void pick(int i) {
        if (i < 0 || i >= cands.size()) return;
        String pick = cands.get(i);
        undo = null;
        if (raw.length() > 0) {
            sink.setComposing(pick);
            sink.finishComposing();
            raw.setLength(0);
            sink.text(" ");
        } else {
            String word = trailingWord(sink.textBefore(48));
            if (!word.isEmpty()) sink.deleteBefore(word.length());
            sink.text(pick + " ");
        }
        refresh();
    }

    /** The cursor moved on its own while nothing was being composed: re-read the word at the cursor. */
    void cursorMoved() {
        refresh();
    }

    /** The cursor moved or the field changed under us: stop composing, keep what is typed. */
    void reset() {
        commitComposing();
        undo = null;
        refresh();
    }

    // ----------------------------------------------------------------- internals

    private boolean isComposeKey(char c) {
        boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        if (lang.equals("ko")) return letter;
        return letter || (vni && c >= '0' && c <= '9' && raw.length() > 0);
    }

    private static boolean isSeparator(char c) {
        return c == ' ' || c == ',' || c == '.' || c == '!' || c == '?' || c == ';' || c == ':' || c == '\n';
    }

    private String display() {
        return lang.equals("ko") ? HangulComposer.compose(raw.toString())
                : VietnameseEngine.compose(raw.toString(), vni);
    }

    private void showComposing() {
        sink.setComposing(display());
        refresh();
    }

    private void commitComposing() {
        if (raw.length() > 0) {
            sink.finishComposing();
            raw.setLength(0);
        }
    }

    private void separate(String sep) {
        if (assist() && autoOn && fixLastWord(sep)) return;
        undo = null;
        sink.text(sep);
    }

    /** Swap the word before the cursor for its auto-correction. Types {@code sep} after it when non-null. */
    private boolean fixLastWord(String sep) {
        String before = sink.textBefore(80);
        String word = trailingWord(before);
        if (word.isEmpty()) return false;
        Ctx ctx = context(before, word);
        SpellChecker.Hit h = spell.autoFix(word, w -> ctxScore(ctx, w));
        if (h == null) return false;
        String fixed = matchCase(word, h.word);
        sink.deleteBefore(word.length());
        sink.text(fixed);
        if (sep != null) {
            sink.text(sep);
            undo = new Undo(word, fixed, sep);
        }
        return true;
    }

    private static String trailingWord(String before) {
        if (before == null) return "";
        Matcher m = TRAILING_WORD.matcher(before);
        return m.find() ? m.group() : "";
    }

    private static String matchCase(String typed, String word) {
        if (typed.length() > 1 && typed.equals(typed.toUpperCase(Locale.ROOT))) return word.toUpperCase(Locale.ROOT);
        if (!typed.isEmpty() && Character.isUpperCase(typed.charAt(0))) {
            return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
        }
        return word;
    }

    /** The words typed before the current one: p1 the last, p2 the one before; START at a sentence start. */
    private static final class Ctx {
        String p2, p1;
        boolean sentenceStart;
    }

    private static final Pattern SENTENCE_END = Pattern.compile("[.!?\\n]\\s*$");

    /** Read what precedes {@code current} (the word being typed, possibly empty) off {@code before}. */
    private static Ctx context(String before, String current) {
        Ctx c = new Ctx();
        String rest = before == null ? "" : before;
        if (!current.isEmpty() && rest.endsWith(current)) rest = rest.substring(0, rest.length() - current.length());
        String t = rest.replaceAll("[ \\t,;:]+$", "");
        if (t.isEmpty() || SENTENCE_END.matcher(t).find()) {
            c.p1 = NGramModel.START;
            c.sentenceStart = true;
            return c;
        }
        String w1 = trailingWord(t);
        if (w1.isEmpty()) return c;                       // punctuation or digits: no context
        c.p1 = w1.toLowerCase(Locale.ROOT);
        String t2 = t.substring(0, t.length() - w1.length()).replaceAll("[ \\t,;:]+$", "");
        if (t2.isEmpty() || SENTENCE_END.matcher(t2).find()) {
            c.p2 = NGramModel.START;
        } else {
            String w2 = trailingWord(t2);
            if (!w2.isEmpty()) c.p2 = w2.toLowerCase(Locale.ROOT);
        }
        return c;
    }

    private double ctxScore(Ctx ctx, String w) {
        return model == null || ctx == null ? 0 : model.prob(ctx.p2, ctx.p1, w.toLowerCase(Locale.ROOT));
    }

    private static String capitalize(String w) {
        return w.isEmpty() ? w : w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1);
    }

    /** Recompute the strip and tell the host. */
    private void refresh() {
        cands = new ArrayList<>();
        tags = new ArrayList<>();
        note = "";
        if (raw.length() > 0) {
            String d = display();
            add(d, "");
            if (assist() && lang.equals("vi")) {
                Ctx ctx = context(sink.textBefore(80), d);
                if (!spell.known(d)) {
                    for (SpellChecker.Hit h : rank(spell.suggest(d, 6), ctx, true, 3)) add(h.word, "");
                } else if (model != null) {
                    // the syllable is complete: offer it with the words that most often follow ("xin chào")
                    for (NGramModel.Pred p : model.next(ctx.p1, d.toLowerCase(Locale.ROOT), 4)) {
                        if (!p.word.equals(NGramModel.START)) add(d + " " + p.word, "");
                    }
                }
            }
        } else if (assist() && !lang.equals("ko")) {
            String before = sink.textBefore(80);
            String word = trailingWord(before);
            Ctx ctx = context(before, word);
            if (!word.isEmpty()) {
                List<SpellChecker.Hit> hits = rank(spell.prefix(word, 12), ctx, false, 5);
                boolean fixes = false;
                if (hits.isEmpty() && !spell.known(word)) {
                    hits = rank(spell.suggest(word, 8), ctx, true, 4);
                    fixes = true;
                }
                int phrases = 0;
                for (SpellChecker.Hit h : hits) {
                    String w = matchCase(word, h.word);
                    add(w, h.lang.equals(lang) ? "" : tagOf(h.lang));
                    if (!fixes && model != null && phrases < 2) {
                        List<String> more = model.chain(ctx.p2, ctx.p1, h.word.toLowerCase(Locale.ROOT), 2);
                        if (!more.isEmpty()) {
                            add(w + " " + String.join(" ", more), h.lang.equals(lang) ? "" : tagOf(h.lang));
                            phrases++;
                        }
                    }
                }
                if (cands.isEmpty()) add(word, "");
            } else if (model != null && (before.isEmpty() || Character.isWhitespace(before.charAt(before.length() - 1)))) {
                for (NGramModel.Pred p : model.next(ctx.p2, ctx.p1, 5)) {
                    if (p.word.equals(NGramModel.START)) continue;
                    add(ctx.sentenceStart ? capitalize(p.word) : p.word, "");
                }
            }
            if (cands.isEmpty() && undo != null && n(undo).equals(sink.textBefore(n(undo).length()))) {
                note = "corrected " + undo.orig;
            }
        }
        sink.changed();
    }

    /**
     * Order hits by how well they follow what was typed before. Fixes keep their edit distance first
     * ({@code byDist}); completions keep their frequency order among equals. Returns at most {@code max}.
     */
    private List<SpellChecker.Hit> rank(List<SpellChecker.Hit> hits, Ctx ctx, boolean byDist, int max) {
        List<SpellChecker.Hit> out = new ArrayList<>(hits);
        if (model != null && ctx != null) {
            java.util.Collections.sort(out, (a, b) -> {
                if (byDist && a.dist != b.dist) return a.dist - b.dist;
                return Double.compare(ctxScore(ctx, b.word), ctxScore(ctx, a.word));
            });
        }
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    private static String n(Undo u) {
        return u.fixed + u.sep;
    }

    private void add(String word, String tag) {
        cands.add(word);
        tags.add(tag);
    }

    private static String tagOf(String lang) {
        InputLang l = InputLang.byId(lang);
        return l == null ? lang.toUpperCase(Locale.ROOT) : l.tag;
    }
}
