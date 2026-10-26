package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CalcTest {

    private static String v(String input) {
        Calc.Answer a = Calc.evaluate(input);
        assertNotNull("expected an answer for: " + input, a);
        return a.value;
    }

    @Test
    public void arithmeticFollowsPrecedence() {
        assertEquals("8", v("2+2*3"));
        assertEquals("7", v("1+2*3"));
        assertEquals("12", v("(2+2)*3"));
        assertEquals("27", v("(1+2)^3"));
        assertEquals("512", v("2^3^2"));          // right-associative: 2^(3^2)
        assertEquals("2.5", v("10/4"));
        assertEquals("-3", v("-1-2"));
        assertEquals("1", v("5 - 4"));
    }

    @Test
    public void percentIsAPostfix() {
        assertEquals("30", v("200*15%"));
        assertEquals("0.5", v("50%+0"));
    }

    @Test
    public void functionsAndConstants() {
        assertEquals("4", v("sqrt(16)"));
        assertEquals("3", v("abs(-3)"));
        assertEquals("1", v("sin(90)"));
        assertEquals("2", v("log(100)"));
        assertEquals("3.141592654", v("pi+0"));
        assertEquals("6.283185307", v("2*pi"));
    }

    @Test
    public void unicodeOperators() {
        assertEquals("6", v("2×3"));
        assertEquals("2", v("6÷3"));
    }

    @Test
    public void formattingTrimsDigits() {
        assertEquals("0.3333333333", v("1/3"));
        assertEquals("1000000", v("1000*1000"));
        assertEquals("0.3", v("0.1+0.2"));
    }

    @Test
    public void notMathIsLeftAlone() {
        assertNull(Calc.evaluate(""));
        assertNull(Calc.evaluate("hello"));
        assertNull(Calc.evaluate("12345"));
        assertNull(Calc.evaluate("wi-fi"));
        assertNull(Calc.evaluate("2+"));
        assertNull(Calc.evaluate("(1+2"));
        assertNull(Calc.evaluate("1/0"));
        assertNull(Calc.evaluate("sqrt(-1)"));
        assertNull(Calc.evaluate("foo(2)"));
        assertNull(Calc.evaluate(null));
    }

    @Test
    public void unitConversions() {
        assertEquals("6.213711922 mi", v("10 km in mi"));
        assertEquals("11.02311311 lb", v("5 kg to lb"));
        assertEquals("1000 m", v("1 km to m"));
        assertEquals("30.48 cm", v("1 ft in cm"));
        assertEquals("60 min", v("1 hour to min"));
        assertEquals("62.13711922 mph", v("100 km/h to mph"));
    }

    @Test
    public void temperatureConversions() {
        assertEquals("37.77777778 C", v("100 f to C"));
        assertEquals("212 F", v("100 c to F"));
        assertEquals("273.15 k", v("0 c to k"));
    }

    @Test
    public void mismatchedOrUnknownUnitsAreNotAnswered() {
        assertNull(Calc.evaluate("10 km in kg"));
        assertNull(Calc.evaluate("10 foo in bar"));
    }
}
