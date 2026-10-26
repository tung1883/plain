package com.plainphone.app;

/** Text folding for the content index: {@link TextMatch#fold} plus word breaks the index tokenizer needs. */
final class Fold {

    private Fold() {}

    /**
     * Folded text with a space around every Chinese / Japanese character, so each one is its own
     * token (those scripts have no spaces between words; a substring then matches as a phrase).
     * The same function is applied to the text indexed and to the words searched for.
     */
    static String forIndex(String text) {
        String folded = TextMatch.fold(text);
        StringBuilder sb = null;
        for (int i = 0; i < folded.length(); ) {
            int cp = folded.codePointAt(i);
            int len = Character.charCount(cp);
            if (isCjk(cp)) {
                if (sb == null) sb = new StringBuilder(folded.substring(0, i));
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') sb.append(' ');
                sb.appendCodePoint(cp).append(' ');
            } else if (sb != null) {
                sb.appendCodePoint(cp);
            }
            i += len;
        }
        return sb == null ? folded : sb.toString().replaceAll(" +", " ").trim();
    }

    static boolean isCjk(int cp) {
        Character.UnicodeScript s = Character.UnicodeScript.of(cp);
        return s == Character.UnicodeScript.HAN || s == Character.UnicodeScript.HIRAGANA
                || s == Character.UnicodeScript.KATAKANA;
    }
}
