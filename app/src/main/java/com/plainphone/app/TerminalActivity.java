package com.plainphone.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.TextPaint;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Locale;

/**
 * A terminal-style reader for the manuals bundled with the Sandbox (see {@link ManIndex}).
 * {@code man <topic> [section]} opens a {@code less}-style pager at the top; {@code man -k words}
 * is a ranked search whose results open the pager at the hit. Select text in the pager and choose
 * Insert to send it back to the Sandbox editor. Opened from Sandbox → Man.
 */
public class TerminalActivity extends Activity {

    static final String EXTRA_TOPIC = "topic";
    /** Result extra: text the user selected in a page, to insert into the Sandbox editor. */
    static final String EXTRA_SNIPPET = "snippet";

    private static final int COL_TEXT = 0xFFCFCFCF, COL_DIM = 0xFF7A7A7A, COL_HEAD = 0xFFFFD27F,
            COL_LINK = 0xFF7CB7FF, COL_MATCH = 0xFF5A4A00, COL_CURRENT = 0xFFB88A00;
    private static final int MENU_INSERT = 1;

    // terminal log
    private TextView out;
    private ScrollView logScroll;
    private final SpannableStringBuilder log = new SpannableStringBuilder();
    /** Numbered choices from the last list; typing the number picks one. */
    private final List<Runnable> picks = new ArrayList<>();

    // pager
    private TextView pager;
    private ScrollView pageScroll;
    private TextView status;
    private HorizontalScrollView keys;
    private ManIndex.Page cur;
    private int[] lineStart = new int[0];
    private String fullLower = "";
    private String[] terms = new String[0];
    private final List<Integer> matches = new ArrayList<>();
    private final List<BackgroundColorSpan> matchSpans = new ArrayList<>();
    private int matchIdx = -1;

    private EditText input;
    private TextView dollar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        int pad = dp(10);
        root.setPadding(pad, pad, pad, pad);

        out = mono(12);
        out.setMovementMethod(LinkMovementMethod.getInstance());
        logScroll = new ScrollView(this);
        logScroll.addView(out, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(logScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        pager = mono(12);
        pager.setTextIsSelectable(true);
        pager.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override public boolean onCreateActionMode(ActionMode m, Menu menu) {
                menu.add(0, MENU_INSERT, 0, "Insert");
                return true;
            }
            @Override public boolean onPrepareActionMode(ActionMode m, Menu menu) { return true; }
            @Override public boolean onActionItemClicked(ActionMode m, MenuItem item) {
                if (item.getItemId() != MENU_INSERT) return false;
                insertSelection();
                m.finish();
                return true;
            }
            @Override public void onDestroyActionMode(ActionMode m) { }
        });
        pageScroll = new ScrollView(this);
        pageScroll.addView(pager, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pageScroll.setOnScrollChangeListener((v, x, y, ox, oy) -> updateStatus());
        pageScroll.setVisibility(View.GONE);
        root.addView(pageScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        status = new TextView(this);
        status.setTypeface(Typeface.MONOSPACE);
        status.setTextSize(11);
        status.setTextColor(0xFF000000);
        status.setBackgroundColor(0xFFCFCFCF);
        status.setSingleLine(true);
        status.setPadding(dp(6), dp(2), dp(6), dp(2));
        status.setVisibility(View.GONE);
        root.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        keys = new HorizontalScrollView(this);
        keys.setHorizontalScrollBarEnabled(false);
        LinearLayout keyRow = new LinearLayout(this);
        keyRow.setOrientation(LinearLayout.HORIZONTAL);
        addKey(keyRow, "top", () -> pagerCommand("g"));
        addKey(keyRow, "prev", () -> pagerCommand("b"));
        addKey(keyRow, "next", () -> pagerCommand("f"));
        addKey(keyRow, "/", this::startSearch);
        addKey(keyRow, "n", () -> pagerCommand("n"));
        addKey(keyRow, "N", () -> pagerCommand("N"));
        addKey(keyRow, "toc", () -> pagerCommand("toc"));
        addKey(keyRow, "q", () -> pagerCommand("q"));
        keys.addView(keyRow);
        keys.setVisibility(View.GONE);
        root.addView(keys, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout prompt = new LinearLayout(this);
        prompt.setOrientation(LinearLayout.HORIZONTAL);
        prompt.setGravity(Gravity.CENTER_VERTICAL);
        dollar = new TextView(this);
        dollar.setText("$ ");
        dollar.setTypeface(Typeface.MONOSPACE);
        dollar.setTextSize(13);
        dollar.setTextColor(0xFF7CFC7C);
        prompt.addView(dollar);

        input = new EditText(this);
        input.setTypeface(Typeface.MONOSPACE);
        input.setTextSize(13);
        input.setTextColor(0xFFFFFFFF);
        input.setBackground(null);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        input.setImeOptions(EditorInfo.IME_ACTION_GO);
        input.setOnEditorActionListener((v, id, e) -> {
            String line = input.getText().toString();
            input.setText("");
            submit(line);
            return true;
        });
        prompt.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(prompt, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        // Build the index off the UI thread; the first command waits for it if needed.
        new Thread(() -> ManIndex.get(this), "man-index").start();
        println("sandbox manuals. type 'help' for commands.");
        String topic = getIntent().getStringExtra(EXTRA_TOPIC);
        if (topic != null) run("man " + topic);
    }

    // ---- input ----------------------------------------------------------------------------------

    private void submit(String line) {
        if (pageScroll.getVisibility() == View.VISIBLE && pagerCommand(line.trim())) return;
        run(line);
    }

    private void run(String line) {
        String cmd = line.trim();
        if (pageScroll.getVisibility() == View.VISIBLE) showLog();
        println("$ " + cmd);
        if (cmd.isEmpty()) return;
        if (cmd.matches("\\d+")) {
            int n = Integer.parseInt(cmd);
            if (n >= 1 && n <= picks.size()) picks.get(n - 1).run();
            else println("no choice " + n);
            return;
        }
        String[] parts = cmd.split("\\s+");
        switch (parts[0]) {
            case "help":
                println("man                list the manuals");
                println("man <topic>        opens a manual, or lists its pages (man linux, man c)");
                println("man <function>     e.g. man printf, man strlen, man c malloc");
                println("man <page>         e.g. man ts.classes, man py.str");
                println("man <topic> <word> jump to a section: man ts generics, man c 2.1, man py join");
                println("man -k <words>     ranked search of every manual; tap a result or type its number");
                println("pack list          downloadable manuals: rust, go, linux, git, web, sql ...");
                println("pack install <id>  download;  pack remove <id>  delete;  pack update");
                println("clear              clear the screen");
                println("exit               leave the terminal");
                println("");
                println("in a manual:");
                println("  enter/f next page   b prev page   g top   G end   volume keys page");
                println("  /word search   n next match   N previous match   toc sections   q quit");
                println("  select text, then Insert to put it in the Sandbox editor");
                break;
            case "man":
                man(parts);
                break;
            case "pack":
                pack(parts);
                break;
            case "apropos":
                search(join(parts, 1));
                break;
            case "clear":
                log.clear();
                out.setText(log);
                break;
            case "exit":
                finish();
                break;
            default:
                println(parts[0] + ": command not found (try 'help')");
        }
    }

    private void man(String[] a) {
        ManIndex ix = ManIndex.get(this);
        if (ix.isEmpty()) {
            println("no manuals in this build");
            return;
        }
        if (a.length == 1) {
            println("usage: man <topic> [section]    man -k <words>");
            listTopics(ix);
            return;
        }
        if (a[1].equals("-k")) {
            search(join(a, 2));
            return;
        }
        String key = a[1].toLowerCase(Locale.ROOT);
        String rest = join(a, 2);
        ManIndex.Page p = ix.page(key);
        if (p == null && key.contains(".")) { // man py.str.join -> page py.str, section join
            String k = key;
            while (p == null && k.contains(".")) {
                k = k.substring(0, k.lastIndexOf('.'));
                p = ix.page(k);
            }
            if (p != null) rest = (key.substring(k.length() + 1) + " " + rest).trim();
        }
        if (p == null && ix.subPages(key).isEmpty()) p = ix.byName(key); // man sprintf
        if (p != null) {
            openQuery(ix, p, rest);
        } else if (!ix.subPages(key).isEmpty()) {
            if (rest.isEmpty()) {
                listTopic(ix, key);
            } else {
                String first = a[2].toLowerCase(Locale.ROOT);
                ManIndex.Page sub = ix.page(key + "." + first);
                if (sub == null) { // man c sprintf: an alias of c.printf
                    ManIndex.Page alias = ix.byName(first);
                    if (alias != null && alias.topic.equals(key)) sub = alias;
                }
                if (sub != null) {
                    openQuery(ix, sub, join(a, 3));
                } else {
                    ManIndex.Hit h = ix.findInTopic(key, rest);
                    if (h == null) println("no section '" + rest + "' in " + key + " (try: man -k " + rest + ")");
                    else openPage(h.page, h.line, new String[0]);
                }
            }
        } else {
            needDocs(key, join(a, 0));
        }
    }

    private void openQuery(ManIndex ix, ManIndex.Page p, String q) {
        if (q.isEmpty()) {
            openPage(p, 0, new String[0]);
            return;
        }
        int s = ix.findSection(p, q);
        if (s < 0) {
            println("no section '" + q + "' in " + p.id + "; opening the top (toc lists sections)");
            openPage(p, 0, new String[0]);
        } else {
            openPage(p, p.sections().get(s).line, new String[0]);
        }
    }

    private void listTopics(ManIndex ix) {
        picks.clear();
        for (String t : ix.topics()) {
            final String topic = t;
            boolean multi = !ix.subPages(t).isEmpty();
            link("  " + t + (multi ? "  (" + ix.subPages(t).size() + " pages)" : ""), () -> run("man " + topic));
        }
    }

    private void listTopic(ManIndex ix, String topic) {
        picks.clear();
        println(topic + ": " + ix.subPages(topic).size() + " pages (type a number, or man " + topic + " <page>)");
        int n = 1;
        for (ManIndex.Page p : ix.subPages(topic)) {
            final ManIndex.Page page = p;
            link(String.format(Locale.US, "%3d. %-16s %s", n++, p.id, p.title().toLowerCase(Locale.ROOT)),
                    () -> openPage(page, 0, new String[0]));
        }
    }

    private void search(String query) {
        if (query.isEmpty()) {
            println("usage: man -k <words>");
            return;
        }
        println("searching...", COL_DIM);
        new Thread(() -> {
            final List<ManIndex.Hit> found = ManIndex.get(this).search(query, 30);
            runOnUiThread(() -> showHits(query, found));
        }, "man-search").start();
    }

    private void showHits(String query, List<ManIndex.Hit> hits) {
        if (hits.isEmpty()) {
            println("nothing found for '" + query + "'");
            missingPacksHint();
            return;
        }
        picks.clear();
        final String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        int n = 1;
        for (ManIndex.Hit h : hits) {
            final ManIndex.Hit hit = h;
            String title = h.page.sections().get(h.section).title;
            link(String.format(Locale.US, "%2d. %s › %s", n++, h.page.id, title),
                    () -> openPage(hit.page, hit.line, words));
            if (!h.snippet.isEmpty()) println("      " + h.snippet, COL_DIM);
        }
        if (hits.size() >= 30) println("[top 30 shown]", COL_DIM);
        missingPacksHint();
    }

    // ---- packs ----------------------------------------------------------------------------------

    private void notInstalled(PackStore.Pack p, String rerun) {
        PackUi.ensure(this, p, () -> run(rerun), null);
    }

    /** Runs {@code r} on the UI thread once the pack list is known (downloaded the first time). */
    private void withRegistry(Runnable r) {
        if (PackStore.hasRegistry(this)) {
            r.run();
            return;
        }
        println("loading the list of manuals...", COL_DIM);
        new Thread(() -> {
            String err = null;
            try {
                PackStore.refreshRegistry(this);
            } catch (IOException e) {
                err = e.getMessage();
            }
            final String msg = err;
            runOnUiThread(() -> {
                if (msg != null) println(msg, 0xFFE57373);
                r.run();
            });
        }, "pack-registry").start();
    }

    /** {@code key} is no local page: maybe a doc pack that is not installed has it. */
    private void needDocs(String key, String rerun) {
        final String topic = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
        withRegistry(() -> {
            PackStore.Pack need = PackStore.docPackForTopic(this, topic);
            if (need != null && !PackStore.installed(this, need)) {
                notInstalled(need, rerun);
                return;
            }
            println("no manual for '" + key + "'");
            listTopics(ManIndex.get(this));
            missingPacksHint();
        });
    }

    private void missingPacksHint() {
        if (!PackStore.hasRegistry(this)) {
            println("[more manuals can be downloaded; see 'pack list']", COL_DIM);
            return;
        }
        for (PackStore.Pack p : PackStore.docPacks(this)) {
            if (!PackStore.installed(this, p)) {
                println("[more manuals can be downloaded; see 'pack list']", COL_DIM);
                return;
            }
        }
    }

    private void pack(String[] a) {
        final String sub = a.length > 1 ? a[1] : "list";
        if (sub.equals("list")) {
            withRegistry(() -> {
                picks.clear();
                for (PackStore.Pack p : PackStore.docPacks(this)) {
                    final PackStore.Pack pack = p;
                    int have = PackStore.installedVersion(this, p);
                    if (have == p.version && PackStore.installed(this, p)) {
                        println(String.format(Locale.US, "  %-11s installed", p.id));
                    } else {
                        String state = have >= 0 ? "update available" : "not installed";
                        link(String.format(Locale.US, "  %-11s %-16s %s download", p.id, state, PackStore.mb(p.zipBytes)),
                                () -> run("pack install " + pack.id));
                    }
                    println("      " + p.desc, COL_DIM);
                }
                println("  py: a small built-in guide is always available.", COL_DIM);
            });
            return;
        }
        if (sub.equals("remove")) {
            List<String> ids = new ArrayList<>();
            for (int i = 2; i < a.length; i++) ids.add(a[i]);
            if (ids.isEmpty()) {
                println("usage: pack remove <id>... | all");
                return;
            }
            boolean all = ids.contains("all");
            for (PackStore.Pack p : PackStore.installedDocs(this)) {
                if (all || ids.contains(p.id)) {
                    PackStore.remove(this, p);
                    println(p.id + " removed");
                }
            }
            return;
        }
        if (sub.equals("install") || sub.equals("update")) {
            final List<String> ids = new ArrayList<>();
            for (int i = 2; i < a.length; i++) ids.add(a[i]);
            if (sub.equals("install") && ids.isEmpty()) {
                println("usage: pack install <id>... | all     (pack list shows the ids)");
                return;
            }
            withRegistry(() -> {
                List<PackStore.Pack> todo = new ArrayList<>();
                if (sub.equals("update")) {
                    for (PackStore.Pack p : PackStore.installedDocs(this)) {
                        PackStore.Pack reg = PackStore.docPack(this, p.id);
                        if (reg != null && PackStore.installedVersion(this, reg) != reg.version) todo.add(reg);
                    }
                    if (todo.isEmpty()) println("everything is up to date");
                } else if (ids.contains("all")) {
                    for (PackStore.Pack p : PackStore.docPacks(this)) if (!PackStore.installed(this, p)) todo.add(p);
                    if (todo.isEmpty()) println("all packs are already installed");
                } else {
                    for (String id : ids) {
                        PackStore.Pack p = PackStore.docPack(this, id);
                        if (p == null) println("no pack '" + id + "' (pack list shows the ids)");
                        else if (PackStore.installed(this, p)) println(id + " is already installed");
                        else todo.add(p);
                    }
                }
                if (!todo.isEmpty()) installPacks(todo);
            });
            return;
        }
        println("usage: pack list | install <id>... | remove <id>... | update");
    }

    private void installPacks(List<PackStore.Pack> packs) {
        new Thread(() -> {
            for (PackStore.Pack p : packs) {
                runOnUiThread(() -> println("downloading " + p.id + " (" + PackStore.mb(p.zipBytes) + ")..."));
                final int[] last = {-1};
                try {
                    PackStore.install(this, p, (done, total, phase) -> {
                        int step = total > 0 ? (int) (done * 4 / total) : 99; // report at 25% steps
                        if (step == last[0]) return;
                        last[0] = step;
                        String msg = total > 0 ? phase + " " + (step * 25) + "%" : "installing...";
                        runOnUiThread(() -> println(msg, COL_DIM));
                    }, new AtomicBoolean());
                    runOnUiThread(() -> println(p.id + " installed"));
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        println(e.getMessage(), 0xFFE57373);
                        picks.clear();
                        link("  pack install " + p.id + "   (retry)", () -> run("pack install " + p.id));
                    });
                    return;
                }
            }
        }, "pack-install").start();
    }

    private static String join(String[] a, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < a.length; i++) sb.append(i > from ? " " : "").append(a[i]);
        return sb.toString();
    }

    // ---- pager ----------------------------------------------------------------------------------

    private void openPage(ManIndex.Page p, int line, String[] highlight) {
        cur = p;
        SpannableStringBuilder sb = new SpannableStringBuilder();
        lineStart = new int[p.lines().size()];
        boolean[] heading = new boolean[p.lines().size()];
        for (ManIndex.Section s : p.sections()) if (s.line < heading.length) heading[s.line] = true;
        for (int i = 0; i < p.lines().size(); i++) {
            lineStart[i] = sb.length();
            sb.append(p.lines().get(i)).append('\n');
            if (heading[i]) {
                sb.setSpan(new StyleSpan(Typeface.BOLD), lineStart[i], sb.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(COL_HEAD), lineStart[i], sb.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (i == 0) {
                sb.setSpan(new ForegroundColorSpan(COL_DIM), 0, sb.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        fullLower = sb.toString().toLowerCase(Locale.ROOT);
        pager.setText(sb, TextView.BufferType.SPANNABLE);
        showPager();
        setTerms(highlight);
        int target = Math.max(0, Math.min(line, lineStart.length - 1));
        if (highlight.length > 0 && !matches.isEmpty()) {
            matchIdx = firstMatchAtOrAfter(lineStart[target]);
            if (matchIdx < 0) matchIdx = 0;
            paintMatches();
        }
        scrollToLine(target, 0);
    }

    private void setTerms(String[] t) {
        Spannable sp = (Spannable) pager.getText();
        for (BackgroundColorSpan s : matchSpans) sp.removeSpan(s);
        matchSpans.clear();
        matches.clear();
        matchIdx = -1;
        terms = t;
        List<int[]> ranges = new ArrayList<>();
        for (String term : t) {
            if (term.isEmpty()) continue;
            int at = fullLower.indexOf(term);
            while (at >= 0 && ranges.size() < 3000) {
                ranges.add(new int[]{at, term.length()});
                at = fullLower.indexOf(term, at + term.length());
            }
        }
        ranges.sort((x, y) -> x[0] - y[0]);
        for (int[] r : ranges) {
            matches.add(r[0]);
            BackgroundColorSpan span = new BackgroundColorSpan(COL_MATCH);
            sp.setSpan(span, r[0], r[0] + r[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            matchSpans.add(span);
        }
    }

    /** Re-colours the spans so only the current match is bright. */
    private void paintMatches() {
        Spannable sp = (Spannable) pager.getText();
        for (int i = 0; i < matchSpans.size(); i++) {
            BackgroundColorSpan old = matchSpans.get(i);
            int s = sp.getSpanStart(old), e = sp.getSpanEnd(old);
            if (s < 0) continue;
            sp.removeSpan(old);
            BackgroundColorSpan span = new BackgroundColorSpan(i == matchIdx ? COL_CURRENT : COL_MATCH);
            sp.setSpan(span, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            matchSpans.set(i, span);
        }
    }

    private int firstMatchAtOrAfter(int offset) {
        for (int i = 0; i < matches.size(); i++) if (matches.get(i) >= offset) return i;
        return -1;
    }

    private void scrollToLine(int line, int tries) {
        pager.post(() -> {
            android.text.Layout l = pager.getLayout();
            if (l == null || pager.getHeight() == 0) {
                if (tries < 10) scrollToLine(line, tries + 1);
                return;
            }
            int off = Math.min(lineStart[line], pager.length());
            pageScroll.scrollTo(0, Math.max(0, l.getLineTop(l.getLineForOffset(off)) - dp(4)));
            updateStatus();
        });
    }

    private void scrollToOffset(int off) {
        android.text.Layout l = pager.getLayout();
        if (l == null) return;
        pageScroll.scrollTo(0, Math.max(0, l.getLineTop(l.getLineForOffset(off)) - pageScroll.getHeight() / 3));
        updateStatus();
    }

    private int topOffset() {
        android.text.Layout l = pager.getLayout();
        if (l == null) return 0;
        return l.getLineStart(l.getLineForVertical(pageScroll.getScrollY()));
    }

    private void updateStatus() {
        if (cur == null || pageScroll.getVisibility() != View.VISIBLE) return;
        android.text.Layout l = pager.getLayout();
        if (l == null) return;
        int idx = Arrays.binarySearch(lineStart, topOffset());
        int line = idx >= 0 ? idx : Math.max(0, -idx - 2);
        int max = Math.max(1, pager.getHeight() - pageScroll.getHeight());
        int pct = Math.min(100, pageScroll.getScrollY() * 100 / max);
        StringBuilder sb = new StringBuilder(cur.id).append(" · ")
                .append(cur.sections().get(cur.sectionAt(line)).title.toLowerCase(Locale.ROOT))
                .append(" · ").append(pct).append('%');
        if (!matches.isEmpty()) {
            sb.append(" · match ").append(matchIdx < 0 ? "-" : String.valueOf(matchIdx + 1))
                    .append('/').append(matches.size());
        }
        status.setText(sb);
    }

    /** Handles a pager key; false when the text is not a pager command. */
    private boolean pagerCommand(String c) {
        if (cur == null) return false;
        if (c.startsWith("/")) {
            String q = c.substring(1).trim().toLowerCase(Locale.ROOT);
            if (q.isEmpty()) return true;
            int from = topOffset();
            setTerms(new String[]{q});
            if (matches.isEmpty()) {
                status.setText("pattern not found: " + q);
                return true;
            }
            matchIdx = firstMatchAtOrAfter(from + 1);
            if (matchIdx < 0) matchIdx = 0;
            paintMatches();
            scrollToOffset(matches.get(matchIdx));
            return true;
        }
        switch (c) {
            case "":
            case "f":
                pageScroll.scrollBy(0, pageScroll.getHeight() - pager.getLineHeight());
                return true;
            case "b":
                pageScroll.scrollBy(0, -(pageScroll.getHeight() - pager.getLineHeight()));
                return true;
            case "g":
                pageScroll.scrollTo(0, 0);
                return true;
            case "G":
                pageScroll.scrollTo(0, pager.getHeight());
                return true;
            case "n":
            case "N":
                if (matches.isEmpty()) {
                    status.setText("no search pattern (use /word)");
                    return true;
                }
                int step = c.equals("n") ? 1 : -1;
                matchIdx = ((matchIdx < 0 ? 0 : matchIdx) + step + matches.size()) % matches.size();
                paintMatches();
                scrollToOffset(matches.get(matchIdx));
                return true;
            case "toc":
                listSections();
                return true;
            case "ins":
                insertSelection();
                return true;
            case "q":
                showLog();
                return true;
            default:
                return false;
        }
    }

    private void listSections() {
        final ManIndex.Page p = cur;
        showLog();
        picks.clear();
        println(p.id + ": " + p.sections().size() + " sections (tap, or type a number)");
        int n = 1;
        for (ManIndex.Section s : p.sections()) {
            final int line = s.line;
            link(String.format(Locale.US, "%3d. %s", n++, s.title), () -> openPage(p, line, new String[0]));
        }
    }

    private void startSearch() {
        input.setText("/");
        input.setSelection(1);
        input.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
    }

    private void insertSelection() {
        int s = pager.getSelectionStart(), e = pager.getSelectionEnd();
        if (s < 0 || e <= s) {
            Toast.makeText(this, "Select some text in the page first (long-press)", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = pager.getText().subSequence(s, e).toString().trim();
        if (text.isEmpty()) return;
        if (getCallingActivity() != null) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_SNIPPET, text));
            finish();
        } else {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("snippet", text));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPager() {
        logScroll.setVisibility(View.GONE);
        pageScroll.setVisibility(View.VISIBLE);
        status.setVisibility(View.VISIBLE);
        keys.setVisibility(View.VISIBLE);
        dollar.setText(": ");
    }

    private void showLog() {
        pageScroll.setVisibility(View.GONE);
        status.setVisibility(View.GONE);
        keys.setVisibility(View.GONE);
        logScroll.setVisibility(View.VISIBLE);
        dollar.setText("$ ");
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void onBackPressed() {
        if (pageScroll.getVisibility() == View.VISIBLE) showLog();
        else super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (pageScroll.getVisibility() == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) { pagerCommand("f"); return true; }
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) { pagerCommand("b"); return true; }
        }
        return super.onKeyDown(keyCode, event);
    }

    // ---- log ------------------------------------------------------------------------------------

    private void println(String s) {
        println(s, COL_TEXT);
    }

    private void println(String s, int color) {
        int start = log.length();
        log.append(s).append('\n');
        log.setSpan(new ForegroundColorSpan(color), start, log.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        flush();
    }

    /** A tappable line that is also the next numbered choice. */
    private void link(String s, Runnable action) {
        picks.add(action);
        int start = log.length();
        log.append(s).append('\n');
        log.setSpan(new ClickableSpan() {
            @Override public void onClick(View v) {
                if (pageScroll.getVisibility() == View.VISIBLE) return;
                action.run();
            }
            @Override public void updateDrawState(TextPaint ds) {
                ds.setColor(COL_LINK);
                ds.setUnderlineText(false);
            }
        }, start, log.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        flush();
    }

    private void flush() {
        out.setText(log);
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    // ---- view helpers ---------------------------------------------------------------------------

    private TextView mono(int sp) {
        TextView t = new TextView(this);
        t.setTypeface(Typeface.MONOSPACE);
        t.setTextSize(sp);
        t.setTextColor(COL_TEXT);
        return t;
    }

    private void addKey(LinearLayout row, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTypeface(Typeface.MONOSPACE);
        b.setTextSize(12);
        b.setTextColor(0xFF9E9E9E);
        b.setBackground(null);
        b.setStateListAnimator(null);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setOnClickListener(v -> action.run());
        row.addView(b);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
