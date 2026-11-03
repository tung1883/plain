package com.plainphone.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VietnameseEngineTest {

    private static String t(String raw) {
        return VietnameseEngine.compose(raw, false);
    }

    private static String v(String raw) {
        return VietnameseEngine.compose(raw, true);
    }

    @Test
    public void telexMarksAndTones() {
        assertEquals("việt", t("vieetj"));
        assertEquals("người", t("nguowif"));
        assertEquals("được", t("dduwowcj"));
        assertEquals("toán", t("toans"));
        assertEquals("đi", t("ddi"));
        assertEquals("tiếng", t("tieengs"));
        assertEquals("chào", t("chaof"));
        assertEquals("Nguyễn", t("Nguyeenx"));
    }

    @Test
    public void wReachesBackOverTheVowelsAtTheEnd() {
        assertEquals("ưi", t("uiw"));       // w marks the u, not a new ư after the i
        assertEquals("ưu", t("uuw"));
        assertEquals("lưu", t("luuw"));
        assertEquals("hơi", t("hoiw"));
        assertEquals("ưa", t("uaw"));
        assertEquals("mưa", t("muaw"));
        assertEquals("ưi", v("ui7"));
        assertEquals("ưu", v("uu7"));
        assertEquals("uiw", t("uiww"));     // a second w undoes it
        assertEquals("uuw", t("uuww"));
    }

    @Test
    public void wReachesBackOverAFinalConsonant() {
        assertEquals("dương", t("duongw"));
        assertEquals("dương", v("duong7"));
        assertEquals("nước", t("nuocws"));
        assertEquals("hương", t("huongw"));
        assertEquals("người", t("nguoiwf"));
        assertEquals("rượu", t("ruouwj"));
        assertEquals("lưng", t("lungw"));
        assertEquals("trưng", t("trungw"));
        assertEquals("nhăn", t("nhanw"));
        assertEquals("xoăn", t("xoanw"));
        assertEquals("hoặc", t("hoawcj"));
        assertEquals("quăn", t("quanw"));
        assertEquals("duongw", t("duongww"));      // a second w undoes it
        assertEquals("trư", t("trw"));             // no vowel to mark: a new ư
    }

    @Test
    public void rawRebuildsTheKeystrokesOfAWord() {
        assertEquals("vieetj", VietnameseEngine.raw("việt", false));
        assertEquals("dduwowcj", VietnameseEngine.raw("được", false));
        assertEquals("Nguyeenx", VietnameseEngine.raw("Nguyễn", false));
        assertEquals("hello", VietnameseEngine.raw("hello", false));
        assertEquals("vie6t5", VietnameseEngine.raw("việt", true));
        assertEquals("hoaf", VietnameseEngine.raw("hòa", false));
        assertEquals(null, VietnameseEngine.raw("hoà", false));      // reform-style tone: not what this engine writes
        assertEquals(null, VietnameseEngine.raw("café", false));     // é is not a Telex letter here
    }

    @Test
    public void tonePlacementFollowsTheRules() {
        assertEquals("quá", t("quas"));
        assertEquals("già", t("giaf"));
    }

    @Test
    public void oaOeUyTakeTheToneOnTheFirstVowelLikeProductionKeyboards() {
        // old style (hòa, khỏe, thủy): the spelling everyone actually types and that Samsung
        // Keyboard, Gboard and Laban Key all default to -- not the reform spelling (hoà, khoẻ, thuỷ).
        assertEquals("hòa", t("hoaf"));
        assertEquals("khỏe", t("khoer"));
        assertEquals("thủy", t("thuyr"));
        assertEquals("xòe", t("xoef"));
        assertEquals("Thúy", t("Thuys"));
        // with a final consonant the tone still goes on the last vowel of the pair either way
        assertEquals("hoàn", t("hoanf"));
        assertEquals("ngoại", t("ngoaij"));
    }

    @Test
    public void doubleKeyEscapeBreaksTheDoubleLetterMark() {
        // "ff" escapes to a literal "f" (same double-tap convention as "ww" -> "w" below), and that
        // literal "f" sits between the two "e"s: the second one must not reach back across it and
        // turn the first, now-distant "e" into "ê".
        assertEquals("refe", t("reffe"));
        assertEquals("ref", t("reff"));
    }

    @Test
    public void foreignWordsStopPickingUpDiacriticsOnceTheyDontParseAsVietnamese() {
        // Not just "reff": any word whose consonants no longer fit a Vietnamese onset/coda shape is
        // left alone from that point on, the way production Vietnamese keyboards fall back to the
        // raw keys for a word that fails their own spelling check.
        assertEquals("chill", t("chill"));         // "ll" is not a Vietnamese final
        assertEquals("stress", t("stress"));       // "str" is not a Vietnamese onset
        assertEquals("reflect", t("reflect"));     // "fl" breaks it right after the absorbed tone key
        assertEquals("facebook", t("facebook"));
        assertEquals("selfie", t("selfie"));
    }

    @Test
    public void pressingAKeyTwiceUndoesIt() {
        assertEquals("as", t("ass"));
        assertEquals("aa", t("aaa"));
        assertEquals("w", t("ww"));
    }

    @Test
    public void loneWMakesU() {
        assertEquals("ư", t("w"));
    }

    @Test
    public void vniDigits() {
        assertEquals("việt", v("vie6t5"));
        assertEquals("người", v("ngu7o7i2"));
        assertEquals("đi", v("d9i"));
        assertEquals("toán", v("toa1n"));
        assertEquals("tiếng", v("tie6ng1"));
    }

    @Test
    public void digitWithNothingToMarkStaysADigit() {
        assertEquals("5", v("5"));
    }
}
