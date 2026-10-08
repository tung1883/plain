package com.plainphone.app;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Index of the manuals: the built-in pages under {@code assets/man} ({@code man/py/str.txt} is page
 * {@code py.str}) plus every installed doc pack. Doc packs are the ones of the pman project (see
 * {@link PackStore}): a {@code bundle.json} listing pages (id, title, aliases, byte range) and a
 * {@code bundle.txt} holding their text. Only the listing is read at startup; a page is read and
 * parsed when first used. Pages are re-flowed for a phone-width screen and split into sections at their headings, so the
 * pager can jump to a section and {@code man -k} can rank by heading.
 */
final class ManIndex {

    static final class Section {
        final String title;
        final int line;
        Section(String title, int line) { this.title = title; this.line = line; }
    }

    /** The parsed text of a page. */
    static final class Body {
        String title;
        final List<String> lines = new ArrayList<>();
        final List<String> lower = new ArrayList<>();
        final List<Section> sections = new ArrayList<>();
        final List<String> aliases = new ArrayList<>();
    }

    static final class Page {
        final String id;
        final String topic;
        private String title;
        /** Other names this page documents (the NAME line of a man page). */
        private final List<String> aliases = new ArrayList<>();
        private final File file;
        private final long off;
        private final long len;
        private SoftReference<Body> body;
        private Body pinned; // built-in pages have no file to reload from

        Page(String id, String topic, String title, List<String> aliases, File file, long off, long len) {
            this.id = id;
            this.topic = topic;
            this.title = title;
            if (aliases != null) this.aliases.addAll(aliases);
            this.file = file;
            this.off = off;
            this.len = len;
        }

        /** Built-in page: parsed now. */
        Page(String id, String topic, String text) {
            this(id, topic, null, null, null, 0, 0);
            Body b = new Body();
            parse(b, id, text);
            this.title = b.title;
            this.aliases.addAll(b.aliases);
            this.pinned = b;
        }

        synchronized Body body() {
            if (pinned != null) return pinned;
            Body b = body == null ? null : body.get();
            if (b == null) {
                b = new Body();
                parse(b, id, readRange(file, off, len));
                body = new SoftReference<>(b);
            }
            return b;
        }

        String title() { return title == null ? id : title; }
        List<String> aliases() { return aliases; }
        List<String> lines() { return body().lines; }
        List<String> lower() { return body().lower; }
        List<Section> sections() { return body().sections; }

        /** Index of the last section starting at or before {@code line}. */
        int sectionAt(int line) {
            List<Section> secs = sections();
            int best = 0;
            for (int i = 0; i < secs.size(); i++) if (secs.get(i).line <= line) best = i;
            return best;
        }
    }

    static final class Hit {
        final Page page;
        final int section;
        final int line;
        final String snippet;
        final int score;
        Hit(Page page, int section, int line, String snippet, int score) {
            this.page = page; this.section = section; this.line = line;
            this.snippet = snippet; this.score = score;
        }
    }

    private static final Pattern CAPS = Pattern.compile("[A-Z0-9][A-Z0-9 ,&'()./:_+#-]*");
    private static final Pattern NUMBERED = Pattern.compile("\\d+(\\.\\d+)*\\s+\\S.*");
    private static final int BASE_INDENT = 7;

    private static ManIndex instance;

    private final Map<String, Page> pages = new TreeMap<>();
    private final List<File> bundles = new ArrayList<>();

    static synchronized ManIndex get(Context ctx) {
        if (instance == null) instance = new ManIndex(ctx.getApplicationContext());
        return instance;
    }

    /** Call after a pack is installed or removed so the next {@link #get} rescans. */
    static synchronized void invalidate() {
        instance = null;
    }

    /** Built-in pages from assets (Python), overridden by the pages of every installed doc pack. */
    private ManIndex(Context ctx) {
        try {
            String[] top = ctx.getAssets().list("man");
            if (top != null) {
                for (String name : top) {
                    if (name.endsWith(".txt")) {
                        addText(name.substring(0, name.length() - 4), readAsset(ctx, "man/" + name));
                    } else {
                        String[] kids = ctx.getAssets().list("man/" + name);
                        if (kids == null) continue;
                        for (String k : kids) {
                            if (k.endsWith(".txt")) {
                                addText(name + "." + k.substring(0, k.length() - 4), readAsset(ctx, "man/" + name + "/" + k));
                            }
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            // an empty index just means "no manuals in this build"
        }
        for (PackStore.Pack pack : PackStore.installedDocs(ctx)) loadBundle(PackStore.dir(ctx, pack));
    }

    private void loadBundle(File dir) {
        String json = readFile(new File(dir, "bundle.json"));
        if (json == null) return;
        File text = new File(dir, "bundle.txt");
        try {
            JSONArray arr = new JSONObject(json).getJSONArray("pages");
            bundles.add(text);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String id = o.getString("id");
                int dot = id.indexOf('.');
                List<String> al = new ArrayList<>();
                JSONArray aa = o.optJSONArray("aliases");
                if (aa != null) for (int k = 0; k < aa.length(); k++) al.add(aa.getString(k));
                pages.put(id, new Page(id, dot < 0 ? id : id.substring(0, dot), o.optString("title", id), al,
                        text, o.getLong("off"), o.getLong("len")));
            }
        } catch (org.json.JSONException ignored) {
            // a damaged pack contributes nothing; reinstalling fixes it
        }
    }

    private void addText(String id, String text) {
        if (text == null) return;
        int dot = id.indexOf('.');
        pages.put(id, new Page(id, dot < 0 ? id : id.substring(0, dot), text));
    }

    /**
     * Packs {@code <dir>/man/**.txt} (the layout of a downloaded doc pack) into one {@code bundle.txt}
     * plus a {@code bundle.json} listing (id, title, aliases, byte range), then deletes the loose
     * files. The result is what pman writes on a PC, so startup reads one small file.
     */
    static void buildBundle(File dir) throws IOException {
        File man = new File(dir, "man");
        List<File> files = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        File[] top = man.listFiles();
        if (top == null) throw new IOException("empty pack");
        java.util.Arrays.sort(top);
        for (File f : top) {
            if (f.isFile() && f.getName().endsWith(".txt")) {
                files.add(f);
                ids.add(f.getName().substring(0, f.getName().length() - 4));
            } else if (f.isDirectory()) {
                File[] kids = f.listFiles();
                if (kids == null) continue;
                java.util.Arrays.sort(kids);
                for (File k : kids) {
                    if (k.isFile() && k.getName().endsWith(".txt")) {
                        files.add(k);
                        ids.add(f.getName() + "." + k.getName().substring(0, k.getName().length() - 4));
                    }
                }
            }
        }
        JSONArray arr = new JSONArray();
        long off = 0;
        try (java.io.OutputStream out = new java.io.BufferedOutputStream(
                new java.io.FileOutputStream(new File(dir, "bundle.txt")), 64 * 1024)) {
            for (int i = 0; i < files.size(); i++) {
                byte[] bytes;
                try (InputStream in = new FileInputStream(files.get(i))) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    byte[] b = new byte[16 * 1024];
                    int n;
                    while ((n = in.read(b)) != -1) buf.write(b, 0, n);
                    bytes = buf.toByteArray();
                }
                Body body = new Body();
                parse(body, ids.get(i), new String(bytes, StandardCharsets.UTF_8));
                out.write(bytes);
                try {
                    JSONObject o = new JSONObject();
                    o.put("id", ids.get(i));
                    o.put("title", body.title);
                    JSONArray al = new JSONArray();
                    for (String a : body.aliases) al.put(a.toLowerCase(Locale.ROOT));
                    o.put("aliases", al);
                    o.put("off", off);
                    o.put("len", bytes.length);
                    arr.put(o);
                } catch (org.json.JSONException e) {
                    throw new IOException(e);
                }
                off += bytes.length;
            }
        }
        try {
            java.io.FileOutputStream meta = new java.io.FileOutputStream(new File(dir, "bundle.json"));
            meta.write(new JSONObject().put("pages", arr).toString().getBytes(StandardCharsets.UTF_8));
            meta.close();
        } catch (org.json.JSONException e) {
            throw new IOException(e);
        }
        deleteTree(man);
    }

    private static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    private static String readRange(File f, long off, long len) {
        try (RandomAccessFile r = new RandomAccessFile(f, "r")) {
            byte[] b = new byte[(int) len];
            r.seek(off);
            r.readFully(b);
            return new String(b, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String readAsset(Context ctx, String path) {
        try (InputStream in = ctx.getAssets().open(path)) {
            return readAll(in);
        } catch (IOException e) {
            return null;
        }
    }

    private static String readFile(File f) {
        try (InputStream in = new FileInputStream(f)) {
            return readAll(in);
        } catch (IOException e) {
            return null;
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[16 * 1024];
        int n;
        while ((n = in.read(b)) != -1) buf.write(b, 0, n);
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Strips the 7-column indent and joins hard-wrapped paragraph lines (the source wraps at ~80
     * columns, which wraps twice on a phone). Bullets, quotes and extra-indented lines (code,
     * signatures) stay as their own lines. Headings are recorded as sections.
     */
    private static void parse(Body p, String id, String text) {
        String[] raw = text.split("\n", -1);
        // 0 = blank/none, 1 = flowing text, 2 = bullet/quote (takes continuations), 3 = verbatim
        int prev = 0;
        boolean firstHeading = true;
        for (int i = 0; i < raw.length; i++) {
            String r = raw[i];
            if (i == raw.length - 1 && r.isEmpty()) break;
            if (r.trim().isEmpty()) {
                emit(p, "", prev != 0);
                prev = 0;
                continue;
            }
            if (!r.startsWith(" ") || r.length() <= BASE_INDENT) { // header and footer lines
                emit(p, r.trim(), false);
                prev = 3;
                continue;
            }
            String t = r.substring(BASE_INDENT);
            String trimmed = t.trim();
            // Numbered headings may be stacked ("2 Usage" directly above "2.1 Install").
            boolean blankBefore = i == 0 || raw[i - 1].trim().isEmpty() || isNumbered(raw[i - 1]);
            boolean blankAfter = i + 1 >= raw.length || raw[i + 1].trim().isEmpty() || isNumbered(raw[i + 1]);
            boolean heading = (blankBefore && blankAfter && isHeading(trimmed)) || (blankBefore && strongNumbered(trimmed));
            if (t.charAt(0) == '\u0002') { // explicit heading (converted markdown)
                String h = trimmed.substring(1).trim();
                p.sections.add(new Section(h, p.lines.size()));
                if (firstHeading) { p.title = h; firstHeading = false; }
                emit(p, h, false);
                prev = 3;
                continue;
            }
            if (t.charAt(0) != ' ' && heading) {
                p.sections.add(new Section(trimmed, p.lines.size()));
                if (firstHeading) { p.title = trimmed; firstHeading = false; }
                emit(p, trimmed, false);
                prev = 3;
                continue;
            }
            boolean bullet = trimmed.startsWith("* ") || trimmed.startsWith("> ");
            if (bullet) {
                emit(p, t, false);
                prev = 2;
            } else if (prev == 2 && leadingSpaces(t) >= 2) {
                join(p, trimmed);
            } else if (t.charAt(0) == ' ') {
                emit(p, t, false);
                prev = 3;
            } else if (prev == 1) {
                join(p, trimmed);
            } else {
                emit(p, t, false);
                prev = 1;
            }
        }
        // man-page style "NAME" section: "printf, fprintf - formatted output" gives aliases + title.
        for (int s = 0; s < p.sections.size(); s++) {
            if (!p.sections.get(s).title.equals("NAME")) continue;
            for (int i = p.sections.get(s).line + 1; i < p.lines.size(); i++) {
                String l = p.lines.get(i).trim();
                if (l.isEmpty()) continue;
                int dash = l.indexOf(" - ");
                String names = dash < 0 ? l : l.substring(0, dash);
                for (String n : names.split(",")) {
                    n = n.trim();
                    if (n.matches("[\\w.+-]+")) p.aliases.add(n.toLowerCase(Locale.ROOT));
                }
                if (dash >= 0) p.title = l.substring(dash + 3).trim();
                break;
            }
            break;
        }
        if (p.title == null) p.title = id;
        if (p.sections.isEmpty() || p.sections.get(0).line > 0) {
            p.sections.add(0, new Section(p.title, 0));
        }
    }

    /** A numbered heading ("2.1 Quick start") counts even when text follows on the next line. */
    private static boolean strongNumbered(String s) {
        if (!NUMBERED.matcher(s).matches() || s.length() > 60 || s.endsWith(".") || s.endsWith(":")) return false;
        int i = 0;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return i < s.length() && Character.isUpperCase(s.charAt(i));
    }

    private static boolean isNumbered(String raw) {
        return raw.length() > BASE_INDENT && raw.startsWith(" ") && raw.charAt(BASE_INDENT) != ' '
                && NUMBERED.matcher(raw.substring(BASE_INDENT).trim()).matches();
    }

    private static boolean isHeading(String s) {
        if (s.length() < 2 || s.length() > 70) return false;
        if (NUMBERED.matcher(s).matches()) return true;
        boolean letter = false;
        for (int i = 0; i < s.length(); i++) if (Character.isLetter(s.charAt(i))) { letter = true; break; }
        return letter && CAPS.matcher(s).matches();
    }

    private static int leadingSpaces(String s) {
        int n = 0;
        while (n < s.length() && s.charAt(n) == ' ') n++;
        return n;
    }

    /** Adds a line; collapses runs of blank lines. */
    private static void emit(Body p, String line, boolean allowBlank) {
        if (line.isEmpty()) {
            if (!allowBlank || p.lines.isEmpty() || p.lines.get(p.lines.size() - 1).isEmpty()) return;
        }
        p.lines.add(line);
        p.lower.add(line.toLowerCase(Locale.ROOT));
    }

    private static void join(Body p, String more) {
        int last = p.lines.size() - 1;
        String joined = p.lines.get(last) + " " + more;
        p.lines.set(last, joined);
        p.lower.set(last, joined.toLowerCase(Locale.ROOT));
    }

    // ---- lookup -------------------------------------------------------------------------------

    Page page(String id) { return pages.get(id); }

    /** Topics whose page is the fullest reference for a shared name (kill: linux, c, cmd); cmd comes last. */
    private static final List<String> RANK = java.util.Arrays.asList("linux", "bash", "git", "posix", "powershell", "c");

    private static int rank(String topic) {
        int i = RANK.indexOf(topic);
        return i >= 0 ? i : topic.equals("cmd") ? 99 : 20;
    }

    /** Page documenting {@code name}: a page id, or an alias such as sprintf for c.printf. */
    Page byName(String name) {
        Page p = pages.get(name);
        if (p != null) return p;
        Page best = null;
        for (Page q : pages.values()) {
            if (q.aliases.contains(name) && (best == null || rank(q.topic) < rank(best.topic))) best = q;
        }
        return best;
    }

    boolean isEmpty() { return pages.isEmpty(); }

    /** Topics in name order. */
    List<String> topics() {
        List<String> out = new ArrayList<>();
        for (Page p : pages.values()) if (!out.contains(p.topic)) out.add(p.topic);
        return out;
    }

    /** Pages of a topic that has sub-pages ({@code ts}, {@code py}); empty for plain pages. */
    List<Page> subPages(String topic) {
        List<Page> out = new ArrayList<>();
        for (Page p : pages.values()) if (p.topic.equals(topic) && !p.id.equals(topic)) out.add(p);
        return out;
    }

    /** Best section in {@code p} for {@code q} ("generics", "2.1", "join"); -1 if none. */
    int findSection(Page p, String q) {
        q = q.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) return -1;
        List<Section> secs = p.sections();
        int starts = -1, ends = -1, contains = -1;
        for (int i = 0; i < secs.size(); i++) {
            String t = secs.get(i).title.toLowerCase(Locale.ROOT);
            if (t.equals(q)) return i;
            if (starts < 0 && t.startsWith(q)) starts = i;
            if (ends < 0 && t.endsWith("." + q)) ends = i;
            if (contains < 0 && t.contains(q)) contains = i;
        }
        return starts >= 0 ? starts : ends >= 0 ? ends : contains;
    }

    /** Best (page, section) across the pages of a topic, for {@code man ts generics}. */
    Hit findInTopic(String topic, String q) {
        List<Page> candidates = subPages(topic);
        if (candidates.isEmpty() && pages.containsKey(topic)) candidates.add(pages.get(topic));
        String needle = q.toLowerCase(Locale.ROOT).trim();
        Hit best = null;
        for (Page p : candidates) {
            List<Section> secs = p.sections();
            for (int i = 0; i < secs.size(); i++) {
                String t = secs.get(i).title.toLowerCase(Locale.ROOT);
                int score = t.equals(needle) ? 4 : t.endsWith("." + needle) ? 3
                        : t.startsWith(needle) ? 2 : t.contains(needle) ? 1 : 0;
                if (score > 0 && (best == null || score > best.score)) {
                    best = new Hit(p, i, secs.get(i).line, secs.get(i).title, score);
                }
            }
        }
        return best;
    }

    // ---- man -k ---------------------------------------------------------------------------------

    /** Cheap check on the raw text of a pack page, so only pages that can match are parsed. */
    private static boolean mayMatch(Page p, byte[][] toks, Map<File, RandomAccessFile> open, byte[] buf) {
        if (p.file == null) return true;
        try {
            RandomAccessFile r = open.get(p.file);
            if (r == null) {
                r = new RandomAccessFile(p.file, "r");
                open.put(p.file, r);
            }
            byte[] b = p.len <= buf.length ? buf : new byte[(int) p.len];
            r.seek(p.off);
            r.readFully(b, 0, (int) p.len);
            int n = (int) p.len;
            for (int i = 0; i < n; i++) if (b[i] >= 'A' && b[i] <= 'Z') b[i] += 32;
            for (byte[] t : toks) if (indexOf(b, n, t) < 0) return false;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static int indexOf(byte[] hay, int n, byte[] needle) {
        if (needle.length == 0) return 0;
        byte first = needle[0];
        outer:
        for (int i = 0; i <= n - needle.length; i++) {
            if (hay[i] != first) continue;
            for (int k = 1; k < needle.length; k++) if (hay[i + k] != needle[k]) continue outer;
            return i;
        }
        return -1;
    }

    /** Sections containing every word, headings weighted over body text, best first. */
    List<Hit> search(String query, int max) {
        String[] tokens = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        List<Hit> hits = new ArrayList<>();
        if (tokens.length == 0 || tokens[0].isEmpty()) return hits;
        byte[][] toks = new byte[tokens.length][];
        for (int k = 0; k < tokens.length; k++) toks[k] = tokens[k].getBytes(StandardCharsets.UTF_8);
        Map<File, RandomAccessFile> open = new java.util.HashMap<>();
        byte[] buf = new byte[256 * 1024];
        try {
            for (Page p : pages.values()) {
                if (!mayMatch(p, toks, open, buf)) continue;
                searchPage(p, tokens, hits);
            }
        } finally {
            for (RandomAccessFile r : open.values()) {
                try { r.close(); } catch (IOException ignored) { }
            }
        }
        Collections.sort(hits, (a, b) -> b.score - a.score);
        return hits.size() > max ? new ArrayList<>(hits.subList(0, max)) : hits;
    }

    private static void searchPage(Page p, String[] tokens, List<Hit> hits) {
        List<Section> secs = p.sections();
        List<String> lines = p.lines();
        List<String> lower = p.lower();
        for (int s = 0; s < secs.size(); s++) {
            int from = secs.get(s).line;
            int to = s + 1 < secs.size() ? secs.get(s + 1).line : lines.size();
            String title = secs.get(s).title.toLowerCase(Locale.ROOT);
            int[] count = new int[tokens.length];
            int[] firstLine = new int[tokens.length];
            java.util.Arrays.fill(firstLine, -1);
            for (int i = from; i < to; i++) {
                String l = lower.get(i);
                for (int k = 0; k < tokens.length; k++) {
                    int at = l.indexOf(tokens[k]);
                    while (at >= 0 && count[k] < 50) {
                        count[k]++;
                        if (firstLine[k] < 0 && i > from) firstLine[k] = i;
                        at = l.indexOf(tokens[k], at + tokens[k].length());
                    }
                }
            }
            boolean all = true, headAll = true;
            int score = 0;
            for (int k = 0; k < tokens.length; k++) {
                if (count[k] == 0) { all = false; break; }
                if (title.contains(tokens[k])) score += 20; else headAll = false;
                score += Math.min(count[k], 10);
            }
            if (!all) continue;
            if (headAll) score += 100;
            if (title.equals(String.join(" ", tokens)) || title.endsWith("." + String.join(" ", tokens))) {
                score += 50;
            }
            int line = secs.get(s).line;
            String snippet = "";
            if (!headAll) {
                for (int k = 0; k < tokens.length; k++) {
                    if (firstLine[k] >= 0) { line = firstLine[k]; snippet = lines.get(line).trim(); break; }
                }
            } else if (from + 1 < to) {
                for (int i = from + 1; i < to; i++) {
                    String l = lines.get(i).trim();
                    if (!l.isEmpty()) { snippet = l; break; }
                }
            }
            if (snippet.length() > 90) snippet = snippet.substring(0, 90) + "…";
            hits.add(new Hit(p, s, line, snippet, score));
        }
    }
}
