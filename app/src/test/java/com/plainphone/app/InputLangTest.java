package com.plainphone.app;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public class InputLangTest {

    @Test
    public void parseKeepsOrderAndDropsUnknownAndRepeats() {
        List<InputLang> l = InputLang.parse("de, xx,en,de");
        assertEquals(2, l.size());
        assertEquals("de", l.get(0).id);
        assertEquals("en", l.get(1).id);
        assertEquals("de,en", InputLang.toCsv(l));
    }

    @Test
    public void emptyOrBadListFallsBackToEnglish() {
        assertEquals("en", InputLang.parse("").get(0).id);
        assertEquals("en", InputLang.parse(null).get(0).id);
        assertEquals("en", InputLang.parse("zz").get(0).id);
    }

    @Test
    public void layoutChoiceFallsBackAndCycles() {
        InputLang de = InputLang.byId("de");
        assertEquals("Full", de.layoutOrDefault(null));
        assertEquals("Full", de.layoutOrDefault("AZERTY"));
        assertEquals("QWERTZ", de.layoutOrDefault("QWERTZ"));
        assertEquals("QWERTZ", de.nextLayout("Full"));
        assertEquals("Full", de.nextLayout("QWERTY"));
    }
}
