package com.plainphone.app;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Answers typed arithmetic and unit conversions: {@code 2+2*3}, {@code (1+2)^3}, {@code 15% of 80}
 * is not supported but {@code 200*15%} is, {@code sqrt(16)}, {@code 10 km in mi}, {@code 5 kg to lb},
 * {@code 100 f to c}. Pure Java.
 */
final class Calc {

    private Calc() {}

    /** The result: what to show big, and the expression it answers. */
    static final class Answer {
        final String value;
        final String expression;

        Answer(String value, String expression) {
            this.value = value;
            this.expression = expression;
        }
    }

    /** The answer to {@code input}, or null when it is not a calculation. */
    static Answer evaluate(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty() || s.length() > 120) return null;

        Answer converted = convert(s);
        if (converted != null) return converted;

        String normalized = s.replace('×', '*').replace('÷', '/').replace('−', '-');
        if (!looksLikeMath(normalized)) return null;
        try {
            Parser p = new Parser(normalized.toLowerCase(Locale.ROOT));
            double v = p.parse();
            if (Double.isNaN(v) || Double.isInfinite(v)) return null;
            return new Answer(format(v), s);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** At least a digit and an operator (or a function / constant) so plain words and numbers are left alone. */
    private static boolean looksLikeMath(String s) {
        boolean digit = false, operator = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) digit = true;
            if (i > 0 && "+-*/^%".indexOf(c) >= 0) operator = true;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        boolean named = lower.contains("sqrt") || lower.contains("sin(") || lower.contains("cos(")
                || lower.contains("tan(") || lower.contains("ln(") || lower.contains("log(")
                || lower.contains("abs(") || lower.matches(".*\\bpi\\b.*");
        return (digit || named) && (operator || named || s.contains("("));
    }

    static String format(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return Long.toString((long) v);
        BigDecimal d = new BigDecimal(v).round(new MathContext(10));
        String plain = d.stripTrailingZeros().toPlainString();
        if (Math.abs(v) >= 1e15 || (Math.abs(v) < 1e-6 && v != 0)) {
            return d.stripTrailingZeros().toString().toLowerCase(Locale.ROOT);
        }
        return plain;
    }

    // ------------------------------------------------------------------- parser

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        double parse() {
            double v = expr();
            skip();
            if (pos != s.length()) throw new IllegalArgumentException("trailing input");
            return v;
        }

        private void skip() {
            while (pos < s.length() && s.charAt(pos) == ' ') pos++;
        }

        private boolean eat(char c) {
            skip();
            if (pos < s.length() && s.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        private double expr() {
            double v = term();
            while (true) {
                if (eat('+')) v += term();
                else if (eat('-')) v -= term();
                else return v;
            }
        }

        private double term() {
            double v = power();
            while (true) {
                if (eat('*')) {
                    v *= power();
                } else if (eat('/')) {
                    double d = power();
                    if (d == 0) throw new ArithmeticException("divide by zero");
                    v /= d;
                } else {
                    return v;
                }
            }
        }

        private double power() {
            double base = unary();
            if (eat('^')) return Math.pow(base, power());     // right-associative
            return base;
        }

        private double unary() {
            if (eat('-')) return -unary();
            if (eat('+')) return unary();
            double v = primary();
            while (eat('%')) v /= 100.0;
            return v;
        }

        private double primary() {
            skip();
            if (pos >= s.length()) throw new IllegalArgumentException("unexpected end");
            char c = s.charAt(pos);
            if (c == '(') {
                pos++;
                double v = expr();
                if (!eat(')')) throw new IllegalArgumentException("missing )");
                return v;
            }
            if (Character.isDigit(c) || c == '.') {
                int start = pos;
                while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
                return Double.parseDouble(s.substring(start, pos));
            }
            if (Character.isLetter(c)) {
                int start = pos;
                while (pos < s.length() && Character.isLetter(s.charAt(pos))) pos++;
                String name = s.substring(start, pos);
                if (name.equals("pi")) return Math.PI;
                if (name.equals("e")) return Math.E;
                if (!eat('(')) throw new IllegalArgumentException("unknown " + name);
                double arg = expr();
                if (!eat(')')) throw new IllegalArgumentException("missing )");
                switch (name) {
                    case "sqrt": if (arg < 0) throw new ArithmeticException(); return Math.sqrt(arg);
                    case "sin": return Math.sin(Math.toRadians(arg));
                    case "cos": return Math.cos(Math.toRadians(arg));
                    case "tan": return Math.tan(Math.toRadians(arg));
                    case "ln": if (arg <= 0) throw new ArithmeticException(); return Math.log(arg);
                    case "log": if (arg <= 0) throw new ArithmeticException(); return Math.log10(arg);
                    case "abs": return Math.abs(arg);
                    default: throw new IllegalArgumentException("unknown " + name);
                }
            }
            throw new IllegalArgumentException("unexpected " + c);
        }
    }

    // --------------------------------------------------------------------- units

    private static final Pattern CONVERT = Pattern.compile(
            "^([-+]?\\d+(?:\\.\\d+)?)\\s*([a-zA-Z°/\"']+(?:/[a-zA-Z]+)?)\\s+(?:in|to|as)\\s+([a-zA-Z°]+(?:/[a-zA-Z]+)?)$");

    /** unit -> {category, factor to the category's base unit} */
    private static final Map<String, Object[]> UNITS = new HashMap<>();

    private static void unit(String category, double factor, String... names) {
        for (String n : names) UNITS.put(n, new Object[]{category, factor});
    }

    static {
        unit("length", 1, "m", "meter", "meters", "metre", "metres");
        unit("length", 1000, "km", "kilometer", "kilometers", "kilometre", "kilometres");
        unit("length", 0.01, "cm", "centimeter", "centimeters");
        unit("length", 0.001, "mm", "millimeter", "millimeters");
        unit("length", 1609.344, "mi", "mile", "miles");
        unit("length", 0.3048, "ft", "foot", "feet");
        unit("length", 0.0254, "inch", "inches");
        unit("length", 0.9144, "yd", "yard", "yards");
        unit("mass", 1, "kg", "kilogram", "kilograms");
        unit("mass", 0.001, "g", "gram", "grams");
        unit("mass", 0.000001, "mg", "milligram", "milligrams");
        unit("mass", 0.45359237, "lb", "lbs", "pound", "pounds");
        unit("mass", 0.028349523125, "oz", "ounce", "ounces");
        unit("mass", 1000, "t", "tonne", "tonnes");
        unit("volume", 1, "l", "liter", "liters", "litre", "litres");
        unit("volume", 0.001, "ml", "milliliter", "milliliters");
        unit("volume", 3.785411784, "gal", "gallon", "gallons");
        unit("volume", 0.946352946, "qt", "quart", "quarts");
        unit("volume", 0.2365882365, "cup", "cups");
        unit("volume", 0.0295735295625, "floz");
        unit("time", 1, "s", "sec", "second", "seconds");
        unit("time", 60, "min", "minute", "minutes");
        unit("time", 3600, "h", "hr", "hour", "hours");
        unit("time", 86400, "d", "day", "days");
        unit("time", 604800, "week", "weeks");
        unit("speed", 1, "m/s", "mps");
        unit("speed", 1000.0 / 3600, "km/h", "kmh", "kph");
        unit("speed", 0.44704, "mph");
        unit("speed", 0.514444, "kn", "knot", "knots");
        unit("data", 1, "b", "byte", "bytes");
        unit("data", 1024, "kb");
        unit("data", 1024.0 * 1024, "mb");
        unit("data", 1024.0 * 1024 * 1024, "gb");
        unit("data", 1024.0 * 1024 * 1024 * 1024, "tb");
        unit("temp", 0, "c", "°c", "celsius");
        unit("temp", 0, "f", "°f", "fahrenheit");
        unit("temp", 0, "k", "kelvin");
    }

    private static Answer convert(String s) {
        Matcher m = CONVERT.matcher(s);
        if (!m.matches()) return null;
        double value = Double.parseDouble(m.group(1));
        String from = m.group(2).toLowerCase(Locale.ROOT), to = m.group(3).toLowerCase(Locale.ROOT);
        Object[] a = UNITS.get(from), b = UNITS.get(to);
        if (a == null || b == null || !a[0].equals(b[0])) return null;

        double result;
        if (a[0].equals("temp")) {
            double celsius = toCelsius(value, from);
            result = fromCelsius(celsius, to);
        } else {
            result = value * (double) a[1] / (double) b[1];
        }
        return new Answer(format(result) + " " + m.group(3), s);
    }

    private static double toCelsius(double v, String unit) {
        if (unit.startsWith("f")) return (v - 32) * 5.0 / 9.0;
        if (unit.startsWith("k")) return v - 273.15;
        return v;
    }

    private static double fromCelsius(double c, String unit) {
        if (unit.startsWith("f")) return c * 9.0 / 5.0 + 32;
        if (unit.startsWith("k")) return c + 273.15;
        return c;
    }
}
