package com.plainphone.app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The readable text of an HTML / XHTML / XML document: tags dropped, entities decoded. */
final class HtmlText {

    private HtmlText() {}

    private static final Pattern SCRIPT = Pattern.compile("(?is)<(script|style|head)\\b.*?</\\1\\s*>");
    private static final Pattern COMMENT = Pattern.compile("(?s)<!--.*?-->");
    private static final Pattern BLOCK = Pattern.compile(
            "(?i)</?(p|div|br|li|ul|ol|tr|table|section|article|h[1-6]|blockquote|pre|hr)\\b[^>]*>");
    private static final Pattern TAG = Pattern.compile("(?s)<[^>]*>");
    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);");

    static String strip(String html) {
        if (html == null) return "";
        String s = SCRIPT.matcher(html).replaceAll(" ");
        s = COMMENT.matcher(s).replaceAll(" ");
        s = BLOCK.matcher(s).replaceAll("\n");
        s = TAG.matcher(s).replaceAll(" ");
        s = decode(s);
        s = s.replaceAll("[ \\t\\u00a0]+", " ").replaceAll(" ?\\n ?", "\n").replaceAll("\\n{3,}", "\n\n");
        return s.trim();
    }

    static String decode(String s) {
        Matcher m = ENTITY.matcher(s);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String e = m.group(1);
            String rep;
            try {
                if (e.startsWith("#x") || e.startsWith("#X")) rep = new String(Character.toChars(Integer.parseInt(e.substring(2), 16)));
                else if (e.startsWith("#")) rep = new String(Character.toChars(Integer.parseInt(e.substring(1))));
                else rep = named(e);
            } catch (RuntimeException ex) {
                rep = " ";
            }
            m.appendReplacement(out, Matcher.quoteReplacement(rep));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String named(String e) {
        switch (e) {
            case "amp": return "&";
            case "lt": return "<";
            case "gt": return ">";
            case "quot": return "\"";
            case "apos": return "'";
            case "nbsp": return " ";
            case "ndash": return "-";
            case "mdash": return "-";
            case "hellip": return "...";
            case "lsquo": case "rsquo": return "'";
            case "ldquo": case "rdquo": return "\"";
            case "copy": return "(c)";
            default: return " ";
        }
    }
}
