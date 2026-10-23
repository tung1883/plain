package com.plainphone.app;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** Rich Text Format: control words dropped, escaped characters decoded. Good enough to search, not to display. */
final class RtfExtractor implements TextExtractor {

    private static final Set<String> SKIPPED_GROUPS = new HashSet<>(Arrays.asList(
            "fonttbl", "colortbl", "stylesheet", "info", "pict", "header", "footer", "themedata",
            "colorschememapping", "datastore", "latentstyles", "listtable", "listoverridetable", "generator"));

    @Override
    public boolean supports(String ext) {
        return ext.equals("rtf");
    }

    @Override
    public void extract(File file, Sink sink) throws IOException {
        byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
        if (data.length > PlainTextExtractor.MAX_BYTES) data = Arrays.copyOf(data, PlainTextExtractor.MAX_BYTES);
        String text = strip(new String(data, Charset.forName("windows-1252")));
        if (text.isEmpty()) throw new Unreadable("empty", "no text");
        sink.chunk("", text);
    }

    /** The plain text of an RTF document (also used by tests). */
    static String strip(String rtf) {
        StringBuilder out = new StringBuilder();
        Deque<Boolean> skip = new ArrayDeque<>();
        boolean skipping = false;
        int i = 0, n = rtf.length();
        while (i < n) {
            char c = rtf.charAt(i);
            if (c == '{') {
                skip.push(skipping);
                i++;
                // a group that starts with \* or a known destination is not body text
                if (i < n && rtf.charAt(i) == '\\') {
                    if (i + 1 < n && rtf.charAt(i + 1) == '*') {
                        skipping = true;
                    } else {
                        int j = i + 1;
                        while (j < n && Character.isLetter(rtf.charAt(j))) j++;
                        if (SKIPPED_GROUPS.contains(rtf.substring(i + 1, j))) skipping = true;
                    }
                }
            } else if (c == '}') {
                if (!skip.isEmpty()) skipping = skip.pop();
                i++;
            } else if (c == '\\') {
                i++;
                if (i >= n) break;
                char d = rtf.charAt(i);
                if (d == '\\' || d == '{' || d == '}') {
                    if (!skipping) out.append(d);
                    i++;
                } else if (d == '\'') {
                    if (i + 3 <= n) {
                        try {
                            int v = Integer.parseInt(rtf.substring(i + 1, i + 3), 16);
                            if (!skipping) out.append(new String(new byte[]{(byte) v}, Charset.forName("windows-1252")));
                        } catch (RuntimeException ignored) {
                            // not a hex escape
                        }
                    }
                    i += 3;
                } else if (Character.isLetter(d)) {
                    int j = i;
                    while (j < n && Character.isLetter(rtf.charAt(j))) j++;
                    String word = rtf.substring(i, j);
                    int k = j;
                    if (k < n && (rtf.charAt(k) == '-' || Character.isDigit(rtf.charAt(k)))) {
                        k++;
                        while (k < n && Character.isDigit(rtf.charAt(k))) k++;
                    }
                    String param = rtf.substring(j, k);
                    if (k < n && rtf.charAt(k) == ' ') k++;       // the space ending a control word
                    if (!skipping) {
                        switch (word) {
                            case "par": case "line": case "row": case "sect": out.append('\n'); break;
                            case "tab": case "cell": out.append(' '); break;
                            case "u": {
                                try {
                                    int cp = Integer.parseInt(param);
                                    out.append((char) (cp < 0 ? cp + 65536 : cp));
                                } catch (NumberFormatException ignored) {
                                    // bad parameter
                                }
                                if (k < n) k++;                   // skip the fallback character that follows
                                break;
                            }
                            default:
                        }
                    }
                    i = k;
                } else {
                    i++;
                }
            } else {
                if (!skipping && c != '\r' && c != '\n') out.append(c);
                i++;
            }
        }
        return out.toString().replaceAll("[ \\t]+", " ").replaceAll(" ?\\n ?", "\n").trim();
    }
}
