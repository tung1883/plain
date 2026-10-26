package com.plainphone.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Text, markdown, logs, data files, source code and HTML. */
final class PlainTextExtractor implements TextExtractor {

    /** Never read more than this much of one file. */
    static final int MAX_BYTES = 8 * 1024 * 1024;

    private static final Set<String> MARKDOWN = new HashSet<>(Arrays.asList("md", "markdown", "mdown"));
    private static final Set<String> MARKUP = new HashSet<>(Arrays.asList("html", "htm", "xhtml", "xml"));
    static final Set<String> EXTENSIONS = new HashSet<>(Arrays.asList(
            "txt", "text", "md", "markdown", "mdown", "log", "csv", "tsv", "json", "jsonl", "yaml", "yml", "ini",
            "conf", "cfg", "toml", "tex", "rst", "org", "srt", "vtt", "html", "htm", "xhtml", "xml",
            "java", "kt", "kts", "py", "js", "ts", "tsx", "jsx", "c", "h", "cc", "cpp", "hpp", "cs", "go", "rs", "rb",
            "php", "swift", "sh", "bash", "zsh", "bat", "ps1", "sql", "css", "scss", "lua", "r", "pl", "scala",
            "gradle", "properties", "dart", "vue", "svelte"));

    private static final Pattern TITLE = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern HEADING = Pattern.compile("^\\s{0,3}#{1,6}\\s+(.+?)\\s*#*\\s*$");

    @Override
    public boolean supports(String extension) {
        return EXTENSIONS.contains(extension);
    }

    @Override
    public void extract(File file, Sink sink) throws IOException {
        String ext = extensionOf(file.getName());
        String text = read(file);
        if (MARKUP.contains(ext)) {
            Matcher t = TITLE.matcher(text);
            if (t.find()) sink.title(HtmlText.decode(t.group(1)).trim());
            sink.chunk("", HtmlText.strip(text));
            return;
        }

        boolean markdown = MARKDOWN.contains(ext);
        boolean titled = false;
        StringBuilder cur = new StringBuilder();
        String heading = null;
        int startLine = 1;
        int line = 0;
        for (String l : text.split("\r?\n", -1)) {
            line++;
            if (sink.stop()) return;
            if (markdown) {
                Matcher h = HEADING.matcher(l);
                if (h.matches()) {
                    flush(sink, cur, heading, startLine, true);
                    heading = h.group(1);
                    if (!titled) {
                        sink.title(heading);
                        titled = true;
                    }
                    startLine = line;
                }
            }
            if (cur.length() == 0) startLine = line;
            cur.append(l).append('\n');
            if (cur.length() >= Chunker.TARGET) flush(sink, cur, heading, startLine, markdown);
        }
        flush(sink, cur, heading, startLine, markdown);
    }

    private static void flush(Sink sink, StringBuilder cur, String heading, int startLine, boolean markdown) {
        if (cur.toString().trim().isEmpty()) {
            cur.setLength(0);
            return;
        }
        String locator = markdown && heading != null ? "§ " + heading : "L" + startLine;
        sink.chunk(locator, cur.toString());
        cur.setLength(0);
    }

    static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** The file's text, decoded as UTF-8 / UTF-16 (by byte-order mark) or else Windows-1252. */
    static String read(File file) throws IOException {
        byte[] data = readBytes(file);
        if (data.length == 0) throw new Unreadable("empty", "empty file");
        Charset bom = null;
        int skip = 0;
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            bom = StandardCharsets.UTF_8;
            skip = 3;
        } else if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE) {
            bom = StandardCharsets.UTF_16LE;
            skip = 2;
        } else if (data.length >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF) {
            bom = StandardCharsets.UTF_16BE;
            skip = 2;
        }
        if (bom == null && looksBinary(data)) throw new Unreadable("failed", "binary file");
        if (bom != null) return new String(data, skip, data.length - skip, bom);
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            return new String(data, Charset.forName("windows-1252"));
        }
    }

    private static byte[] readBytes(File file) throws IOException {
        long size = Math.min(file.length(), MAX_BYTES);
        byte[] buf = new byte[(int) size];
        try (InputStream in = new FileInputStream(file)) {
            int n = 0;
            while (n < buf.length) {
                int r = in.read(buf, n, buf.length - n);
                if (r < 0) break;
                n += r;
            }
            return n == buf.length ? buf : Arrays.copyOf(buf, n);
        }
    }

    /** NUL bytes, or many control characters, in the first few KB mean this is not text. */
    static boolean looksBinary(byte[] data) {
        int n = Math.min(data.length, 4096);
        int control = 0;
        for (int i = 0; i < n; i++) {
            int b = data[i] & 0xFF;
            if (b == 0) return true;
            if (b < 9 || (b > 13 && b < 32 && b != 27)) control++;
        }
        return n > 0 && control * 50 > n;
    }
}
