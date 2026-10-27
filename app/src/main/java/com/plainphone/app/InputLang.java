package com.plainphone.app;

import java.util.ArrayList;
import java.util.List;

/** An input language of the keyboard: its name, tag and the layouts it offers (the first is the default). */
final class InputLang {

    final String id, nativeName, tag;
    final String[] layouts;

    private InputLang(String id, String nativeName, String tag, String... layouts) {
        this.id = id;
        this.nativeName = nativeName;
        this.tag = tag;
        this.layouts = layouts;
    }

    static final InputLang[] ALL = {
            new InputLang("en", "English", "EN", "QWERTY"),
            new InputLang("de", "Deutsch", "DE", "Full", "QWERTZ", "QWERTY"),
            new InputLang("fr", "Français", "FR", "AZERTY", "QWERTY"),
            new InputLang("es", "Español", "ES", "QWERTY"),
            new InputLang("ru", "Русский", "RU", "ЙЦУКЕН"),
            new InputLang("vi", "Tiếng Việt", "VI", "Telex", "VNI"),
            new InputLang("ko", "한국어", "KO", "2-beolsik"),
    };

    static InputLang byId(String id) {
        for (InputLang l : ALL) if (l.id.equals(id)) return l;
        return null;
    }

    /** The languages named in {@code csv} ("en,de"), in that order; unknown ids and repeats are dropped; English if none. */
    static List<InputLang> parse(String csv) {
        List<InputLang> out = new ArrayList<>();
        if (csv != null) {
            for (String id : csv.split(",")) {
                InputLang l = byId(id.trim());
                if (l != null && !out.contains(l)) out.add(l);
            }
        }
        if (out.isEmpty()) out.add(ALL[0]);
        return out;
    }

    static String toCsv(List<InputLang> langs) {
        StringBuilder sb = new StringBuilder();
        for (InputLang l : langs) {
            if (sb.length() > 0) sb.append(',');
            sb.append(l.id);
        }
        return sb.toString();
    }

    /** {@code chosen} if this language offers it, else its default layout. */
    String layoutOrDefault(String chosen) {
        for (String l : layouts) if (l.equals(chosen)) return l;
        return layouts[0];
    }

    /** The layout after {@code current} in this language's list, wrapping round. */
    String nextLayout(String current) {
        for (int i = 0; i < layouts.length; i++) {
            if (layouts[i].equals(current)) return layouts[(i + 1) % layouts.length];
        }
        return layouts[0];
    }
}
