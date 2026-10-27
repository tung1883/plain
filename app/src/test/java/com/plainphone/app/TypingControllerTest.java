package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class TypingControllerTest {

    /** A text field with a composing region, standing in for the editor. */
    private static final class Field implements TypingController.Sink {
        final StringBuilder text = new StringBuilder();
        int comp = -1;      // start of the composing region, or -1

        @Override
        public void text(String s) {
            if (comp >= 0) {
                text.setLength(comp);
                comp = -1;
            }
            text.append(s);
        }

        @Override
        public void key(int keyCode, int meta) {
            if (keyCode == TypingController.KEY_DEL && text.length() > 0) text.setLength(text.length() - 1);
        }

        @Override
        public void enter() {
            text.append('\n');
        }

        @Override
        public void setComposing(String s) {
            if (comp < 0) comp = text.length();
            text.setLength(comp);
            text.append(s);
            if (s.isEmpty()) comp = -1;
        }

        @Override
        public void finishComposing() {
            comp = -1;
        }

        @Override
        public String textBefore(int n) {
            int from = Math.max(0, text.length() - n);
            return text.substring(from);
        }

        @Override
        public void deleteBefore(int n) {
            text.setLength(Math.max(0, text.length() - n));
        }

        @Override
        public int capsMode() {
            String t = text.toString();
            return t.isEmpty() || t.endsWith(". ") || t.endsWith("\n") ? 1 : 0;
        }

        @Override
        public void changed() {
        }
    }

    private static SpellChecker english() {
        SpellChecker sc = new SpellChecker(lang -> lang.equals("en")
                ? Arrays.asList("the", "word", "hello", "help", "held", "phone", "keyboard")
                : lang.equals("de") ? Arrays.asList("über", "schön", "für") : Collections.<String>emptyList());
        sc.setLanguages("en", Collections.<String>emptyList());
        return sc;
    }

    private static void type(TypingController c, String s) {
        for (char ch : s.toCharArray()) c.onText(String.valueOf(ch));
    }

    private static void backspace(TypingController c) {
        c.onKey(TypingController.KEY_DEL, 0);
    }

    @Test
    public void passesEverythingThroughWhenThereIsNothingToDo() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("en", "QWERTY", false, false, null);
        type(c, "wrod ");
        backspace(c);
        assertEquals("wrod", f.text.toString());
        assertFalse(c.wantsStrip());
    }

    @Test
    public void vietnameseComposesUnderlinedThenCommitsOnSpace() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("vi", "Telex", false, false, null);
        type(c, "vieetj");
        assertEquals("việt", f.text.toString());
        assertTrue(f.comp >= 0);
        assertTrue(c.isComposing());
        c.onText(" ");
        assertEquals("việt ", f.text.toString());
        assertFalse(c.isComposing());
    }

    @Test
    public void backspaceRemovesTheLastKeyOfTheComposition() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("vi", "Telex", false, false, null);
        type(c, "vieet");
        assertEquals("viêt", f.text.toString());
        backspace(c);
        assertEquals("viê", f.text.toString());
        backspace(c);
        backspace(c);
        backspace(c);
        backspace(c);
        assertEquals("", f.text.toString());
        assertFalse(c.isComposing());
    }

    @Test
    public void koreanComposesSyllables() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("ko", "2-beolsik", false, false, null);
        type(c, "gksrmf");
        assertEquals("한글", f.text.toString());
        c.onText(",");
        assertEquals("한글,", f.text.toString());
    }

    @Test
    public void passwordFieldsAreLeftAlone() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("vi", "Telex", true, true, english());
        c.setField(false, false);
        type(c, "vieetj ");
        assertEquals("vieetj ", f.text.toString());
        assertFalse(c.wantsStrip());
    }

    @Test
    public void autoCorrectOnSpaceAndBackspaceRestores() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("en", "QWERTY", true, true, english());
        type(c, "the wrod");
        c.onText(" ");
        assertEquals("the word ", f.text.toString());
        assertEquals("corrected wrod", c.note());
        backspace(c);
        assertEquals("the wrod", f.text.toString());
        c.onText(" ");
        assertEquals("the wrod ", f.text.toString());   // not corrected a second time
    }

    @Test
    public void noAutoCorrectWhenItIsOff() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("en", "QWERTY", true, false, english());
        type(c, "wrod ");
        assertEquals("wrod ", f.text.toString());
    }

    @Test
    public void suggestionsFollowTheWordAndPickReplacesIt() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("en", "QWERTY", true, false, english());
        type(c, "hel");
        assertEquals(Arrays.asList("hello", "help", "held"), c.candidates());
        c.pick(1);
        assertEquals("help ", f.text.toString());
    }

    @Test
    public void unknownWordGetsFixesAndMixedLanguageIsTagged() {
        Field f = new Field();
        SpellChecker sc = english();
        sc.setLanguages("en", Arrays.asList("de"));
        TypingController c = new TypingController(f);
        c.configure("en", "QWERTY", true, true, sc);
        type(c, "ueber");
        assertEquals("über", c.candidates().get(0));
        assertEquals("DE", c.tags().get(0));
        c.onText(" ");
        assertEquals("über ", f.text.toString());
    }

    @Test
    public void capitalCaseIsKept() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("en", "QWERTY", true, true, english());
        type(c, "Wrod ");
        assertEquals("Word ", f.text.toString());
    }

    @Test
    public void wantsShiftAtTheStartOfASentence() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("en", "QWERTY", true, false, english());
        assertTrue(c.wantsShift());
        type(c, "hi");
        assertFalse(c.wantsShift());
        type(c, ". ");
        assertTrue(c.wantsShift());
    }

    @Test
    public void vietnameseSpellRestoresAccentsInTheStrip() {
        Field f = new Field();
        SpellChecker sc = new SpellChecker(lang -> Arrays.asList("tôi", "người", "việt"));
        sc.setLanguages("vi", Collections.<String>emptyList());
        TypingController c = new TypingController(f);
        c.configure("vi", "Telex", true, false, sc);
        type(c, "nguoi");
        assertEquals("nguoi", f.text.toString());
        assertEquals("người", c.candidates().get(1));
        c.pick(1);
        assertEquals("người ", f.text.toString());
    }

    private static SpellChecker words(String lang, String... w) {
        SpellChecker sc = new SpellChecker(l -> Arrays.asList(w));
        sc.setLanguages(lang, Collections.<String>emptyList());
        return sc;
    }

    @Test
    public void nextWordsAreOfferedAfterASpaceAndAtTheSentenceStart() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("en", "QWERTY", true, false, english(), NGramModelTest.model());
        assertEquals(Arrays.asList("I", "The"), c.candidates());      // empty field: sentence starters
        c.pick(0);
        assertEquals("I ", f.text.toString());
        assertEquals("want", c.candidates().get(0));                  // after "I"
        c.pick(0);
        assertEquals("I want ", f.text.toString());
    }

    @Test
    public void partialWordGetsAMultiWordCompletion() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("en", "QWERTY", true, false, words("en", "xin", "xylophone"), NGramModelTest.model());
        type(c, "xi");
        assertEquals(Arrays.asList("xin", "xin chào"), c.candidates());
        c.pick(1);
        assertEquals("xin chào ", f.text.toString());
    }

    @Test
    public void vietnameseSyllableOffersItsUsualFollowers() {
        Field f = new Field();
        TypingController c = new TypingController(f);
        c.configure("vi", "Telex", true, false, words("vi", "xin", "chào"), NGramModelTest.model());
        type(c, "xin");
        assertEquals("xin", f.text.toString());
        assertEquals("xin", c.candidates().get(0));
        assertEquals("xin chào", c.candidates().get(1));
        c.pick(1);
        assertEquals("xin chào ", f.text.toString());
    }

    @Test
    public void previousWordDecidesBetweenAccentVariants() {
        Field f = new Field();
        f.text.append("cảm ");
        TypingController c = new TypingController(f);
        c.configure("vi", "Telex", true, false, words("vi", "ôn", "ơn", "ón"), NGramModelTest.model());
        type(c, "on");
        assertEquals("ơn", c.candidates().get(1));     // after "cảm" it is ơn, not the commoner ôn
    }

    @Test
    public void autoCorrectChoosesBySentenceContext() {
        Field f = new Field();
        NGramModel m = new NGramModel(Arrays.asList("2	my	30	hot:20"));
        TypingController c = new TypingController(f);
        c.configure("en", "QWERTY", true, true, words("en", "hat", "x1", "x2", "hot"), m);
        type(c, "my hit ");
        assertEquals("my hot ", f.text.toString());
    }
}
