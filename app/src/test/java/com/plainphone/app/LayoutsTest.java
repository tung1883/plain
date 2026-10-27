package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class LayoutsTest {

    @Test
    public void qwertyKeepsDevHints() {
        Layouts.Spec s = Layouts.QWERTY;
        assertEquals("qwertyuiop", s.rows[0]);
        assertEquals("`", s.alt(0, 0));
        assertEquals("}", s.alt(0, 9));
        assertEquals("<", s.alt(1, 0));
        assertEquals("%", s.alt(2, 6));
    }

    @Test
    public void qwertzSwapsYAndZ() {
        assertEquals("qwertzuiop", Layouts.QWERTZ.rows[0]);
        assertEquals("yxcvbnm", Layouts.QWERTZ.rows[2]);
        assertNull(Layouts.QWERTZ.alt(0, 0));
    }

    @Test
    public void germanFullHasUmlautKeys() {
        Layouts.Spec s = Layouts.get("Full");
        assertEquals("qwertzuiopü", s.rows[0]);
        assertEquals("asdfghjklöä", s.rows[1]);
        assertEquals("yxcvbnmß", s.rows[2]);
    }

    @Test
    public void unknownNameFallsBackToQwerty() {
        assertSame(Layouts.QWERTY, Layouts.get(null));
        assertSame(Layouts.QWERTY, Layouts.get("nope"));
        assertSame(Layouts.AZERTY, Layouts.get("AZERTY"));
        assertSame(Layouts.RU, Layouts.get("ЙЦУКЕН"));
    }

    @Test
    public void accentReplacesTheHintOnItsKey() {
        Layouts.Spec de = Layouts.forLang("de", "QWERTZ");
        assertEquals("ü", de.alt(0, 6));   // u on QWERTZ
        assertEquals("ö", de.alt(0, 8));
        assertEquals("ä", de.alt(1, 0));
        assertEquals("ß", de.alt(1, 1));
        assertEquals("=", de.alt(0, 2));   // other keys keep the dev symbol on their position
    }

    @Test
    public void germanFullAndEnglishAddNoAccents() {
        Layouts.Spec full = Layouts.forLang("de", "Full");
        assertEquals("asdfghjklöä", full.rows[1]);
        assertEquals("<", full.alt(1, 0));
        assertEquals("`", Layouts.forLang("en", "QWERTY").alt(0, 0));
        assertEquals("é", Layouts.forLang("fr", "AZERTY").alt(0, 2));  // e is third on AZERTY
    }

    @Test
    public void everyRowFitsTheKeyboardWidth() {
        // letter rows are at most 11 keys; the shortest row is the one with Shift + Backspace
        for (Layouts.Spec s : new Layouts.Spec[]{Layouts.QWERTY, Layouts.QWERTZ, Layouts.DE_FULL,
                Layouts.AZERTY, Layouts.RU}) {
            assertEquals(3, s.rows.length);
            for (String r : s.rows) assertEquals(true, r.length() >= 6 && r.length() <= 11);
        }
    }
}
