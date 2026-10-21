package com.plainphone.app;

/**
 * Letter-row data for the keyboard, per language and layout. Pure data (no Android types) so it can
 * be unit-tested. {@code rows[i]} are the keys of letter row i; {@code alts[i]} is the long-press
 * output / corner hint for the key at the same index, or ' ' for none.
 */
final class Layouts {

    static final class Spec {
        final String[] rows;
        final String[] alts;

        Spec(String[] rows, String[] alts) {
            this.rows = rows;
            this.alts = alts;
        }

        /** Long-press output for key {@code i} of row {@code row}, or null. */
        String alt(int row, int i) {
            String a = alts[row];
            if (i >= a.length() || a.charAt(i) == ' ') return null;
            return a.substring(i, i + 1);
        }
    }

    private static final String[] NO_ALTS = {"", "", ""};

    /** The original dev layout: QWERTY with symbol corner hints. */
    static final Spec QWERTY = new Spec(
            new String[]{"qwertyuiop", "asdfghjkl", "zxcvbnm"},
            new String[]{"`~=+-_[]{}", "<>()/\\|;:", "'\"*&?!%"});

    static final Spec QWERTZ = new Spec(
            new String[]{"qwertzuiop", "asdfghjkl", "yxcvbnm"}, NO_ALTS);

    /** German with dedicated umlaut and sharp-s keys. */
    static final Spec DE_FULL = new Spec(
            new String[]{"qwertzuiopü", "asdfghjklöä", "yxcvbnmß"}, NO_ALTS);

    static final Spec AZERTY = new Spec(
            new String[]{"azertyuiop", "qsdfghjklm", "wxcvbn"}, NO_ALTS);

    static final Spec RU = new Spec(
            new String[]{"йцукенгшщзх", "фывапролджэ", "ячсмитьбю"}, NO_ALTS);

    /** Accent each language puts on a key's long-press, as two-char pairs (key, accent). */
    private static String[] accents(String lang) {
        switch (lang) {
            case "de": return new String[]{"aä", "oö", "uü", "sß"};
            case "fr": return new String[]{"eé", "aà", "uù", "cç", "oô", "iî"};
            case "es": return new String[]{"nñ", "aá", "eé", "ií", "oó", "uú"};
            case "ru": return new String[]{"её"};
            default: return new String[0];
        }
    }

    /**
     * The rows for {@code lang} in {@code layout}. The symbol hints of the dev layout stay on their
     * positions in every layout; an accent replaces the hint on the key that carries it. German Full
     * has dedicated umlaut keys, so it needs no accents.
     */
    static Spec forLang(String lang, String layout) {
        Spec base = get(layout);
        String[] acc = "Full".equals(layout) ? new String[0] : accents(lang);
        String[] alts = new String[3];
        for (int r = 0; r < 3; r++) {
            StringBuilder sb = new StringBuilder();
            String keys = base.rows[r];
            for (int i = 0; i < keys.length(); i++) {
                char a = QWERTY.alts[r].length() > i ? QWERTY.alts[r].charAt(i) : ' ';
                for (String p : acc) if (p.charAt(0) == keys.charAt(i)) a = p.charAt(1);
                sb.append(a);
            }
            alts[r] = sb.toString();
        }
        return new Spec(base.rows, alts);
    }

    /** The layout for {@code layout} (the name shown in Settings); QWERTY when unknown. */
    static Spec get(String layout) {
        if (layout == null) return QWERTY;
        switch (layout) {
            case "QWERTZ": return QWERTZ;
            case "Full": return DE_FULL;
            case "AZERTY": return AZERTY;
            case "ЙЦУКЕН": return RU;
            default: return QWERTY;
        }
    }

    private Layouts() {
    }
}
