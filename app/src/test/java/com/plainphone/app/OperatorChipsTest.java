package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OperatorChipsTest {

    @Test
    public void addsTheOperatorAtTheFront() {
        assertEquals("note:", OperatorChips.toggle("", "note:"));
        assertEquals("pdf:invoice", OperatorChips.toggle("invoice", "pdf:"));
        assertEquals("note:two words", OperatorChips.toggle("two words", "note:"));
    }

    @Test
    public void removesItWhenPresent() {
        assertEquals("", OperatorChips.toggle("note:", "note:"));
        assertEquals("invoice", OperatorChips.toggle("pdf:invoice", "pdf:"));
        assertEquals("invoice", OperatorChips.toggle("PDF:invoice", "pdf:"));
        assertEquals("a b", OperatorChips.toggle("a pdf: b", "pdf:").replaceAll("\\s+", " "));
    }

    @Test
    public void onlyWholeOperatorsCount() {
        assertTrue(OperatorChips.isOn("note:meeting", "note:"));
        assertTrue(OperatorChips.isOn("meeting note:", "note:"));
        assertTrue(OperatorChips.isOn("NOTE:x", "note:"));
        assertFalse(OperatorChips.isOn("footnote:x", "note:"));     // inside a word
        assertFalse(OperatorChips.isOn("meeting", "note:"));
        assertFalse(OperatorChips.isOn("", "note:"));
    }

    @Test
    public void toggleTwiceRestoresTheQuery() {
        for (String[] chip : OperatorChips.ALL) {
            String q = "budget report";
            assertEquals(q, OperatorChips.toggle(OperatorChips.toggle(q, chip[1]), chip[1]));
        }
    }

    @Test
    public void everyChipOperatorIsOneTheParserKnows() {
        for (String[] chip : OperatorChips.ALL) {
            ParsedQuery p = QueryParser.parse(chip[1] + "x", 0);
            assertTrue(chip[1], p.hasOperators());
            assertEquals("x", p.text);
        }
    }
}
