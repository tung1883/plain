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
    public void tonePlacementFollowsTheRules() {
        assertEquals("hoà", t("hoaf"));
        assertEquals("quá", t("quas"));
        assertEquals("già", t("giaf"));
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
