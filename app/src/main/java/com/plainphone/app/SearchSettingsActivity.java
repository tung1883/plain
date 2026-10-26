package com.plainphone.app;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Search settings: what is searched, and the index of file contents. */
public class SearchSettingsActivity extends Activity {

    private static final int DIM = 0xFF666666, MUTED = 0xFF999999, GRAY = 0xFF808080, EDGE = 0xFF2C2C2C, HAIR = 0xFF1C1C1C;
    private static final int INDENT = 40;

    private static final String[][] ENGINES = {
            {"Google", "https://www.google.com/search?q=%s"},
            {"DuckDuckGo", "https://duckduckgo.com/?q=%s"},
            {"Bing", "https://www.bing.com/search?q=%s"},
            {"Brave", "https://search.brave.com/search?q=%s"},
            {"Startpage", "https://www.startpage.com/do/search?q=%s"},
            {"Ecosia", "https://www.ecosia.org/search?q=%s"},
            {"Yahoo", "https://search.yahoo.com/search?p=%s"},
    };

    private LinearLayout root;
    private Typeface font;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private FrameLayout cardHolder;
    private boolean ticking;

    // Folders popup state
    private final Set<String> open = new HashSet<>();      // expanded tree nodes, "<volume>|<path>"
    private String focus;                                  // node to scroll to and flash
    private ScrollView treeScroll;

    /** Redraws only the status card while a run is going. */
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (cardHolder != null) {
                cardHolder.removeAllViews();
                cardHolder.addView(buildCard());
            }
            ticking = ContentIndexer.isRunning();
            if (ticking) ui.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, "Search", scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ui.removeCallbacks(tick);
        ticking = false;
    }

    // ------------------------------------------------------------------- screen

    private void render() {
        font = Fonts.current(this);
        root.removeAllViews();
        cardHolder = null;

        root.addView(sectionHeader("Results"));
        boolean web = Config.isWebSearchEnabled(this);
        root.addView(onOff("Search the web", null, web, () -> {
            Config.setWebSearchEnabled(this, !web);
            render();
        }));
        root.addView(row("Search engine", null, engineName(), MUTED, this::showEngines));
        root.addView(row("My web searches", null, String.valueOf(Config.getWebTargets(this).size()), MUTED,
                () -> startActivity(new Intent(this, WebTargetsActivity.class))));

        root.addView(sectionHeader("On this phone"));
        boolean contacts = Config.isContactSearchEnabled(this);
        root.addView(onOff("Contacts", null, contacts, () -> {
            Config.setContactSearchEnabled(this, !contacts);
            if (!contacts && !DeviceSearch.canSearchContacts(this)) {
                requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS},
                        DeviceSearch.REQUEST_CONTACTS);
            }
            render();
        }));
        boolean fileGranted = FileIndex.canWalk(this);
        boolean fileEnabled = Config.isFileSearchEnabled(this);
        root.addView(onOff("File names", null, fileGranted && fileEnabled, () -> {
            if (!fileGranted) {
                Config.setFileSearchEnabled(this, true);
                DeviceSearch.requestFullFileAccess(this);
            } else {
                Config.setFileSearchEnabled(this, !fileEnabled);
            }
            render();
        }));
        boolean content = Config.isContentSearchEnabled(this);
        root.addView(onOff("File content", null, content, () -> {
            Config.setContentSearchEnabled(this, !content);
            if (!content) {
                if (!FileIndex.canWalk(this)) DeviceSearch.requestFullFileAccess(this);
                else SearchJobs.startContentIndex(this);
            }
            render();
        }));
        if (content) addContentBlock();

        root.addView(sectionHeader("Privacy"));
        root.addView(onOff("Lock search", "Ask for your PIN before searching", Lock.SEARCH.isLocked(this),
                () -> Lock.SEARCH.toggleLock(this, this::render)));

        root.addView(new View(this), new LinearLayout.LayoutParams(1, UiKit.dp(this, 40)));
        if (content && ContentIndexer.isRunning() && !ticking) {
            ticking = true;
            ui.postDelayed(tick, 500);
        }
    }

    private void addContentBlock() {
        root.addView(sectionHeader("File content"));
        root.addView(row("Folders", null, String.valueOf(Config.getContentFolders(this).size()), MUTED, this::openFolders));
        root.addView(row("File types", null, SearchSettingsText.typeSummary(Config.getContentTypes(this)), MUTED, this::showTypes));
        boolean low = Config.isContentAllowLowBattery(this);
        root.addView(onOff("Allow when low battery", "Keep reading under 20%", low, () -> {
            Config.setContentAllowLowBattery(this, !low);
            render();
        }));
        root.addView(row("Index size limit", null, SearchSettingsText.limitLabel(Config.getContentMaxMb(this)), MUTED, this::showSizes));
        cardHolder = new FrameLayout(this);
        cardHolder.addView(buildCard());
        root.addView(cardHolder);
    }

    // -------------------------------------------------------------- status box

    private View buildCard() {
        ContentDb.Stats stats = ContentDb.get(this).stats();
        boolean running = ContentIndexer.isRunning();
        int maxMb = Config.getContentMaxMb(this);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.rounded(this, Color.BLACK, EDGE, 1f, UiKit.R_MD));
        card.setPadding(1, 1, 1, 1);     // keep the children off the border

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = UiKit.dp(this, 16);
        body.setPadding(pad, pad, pad, pad);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.BOTTOM);
        top.addView(text(String.valueOf(stats.count("ok")), 30, Color.WHITE));
        TextView small = text("files indexed", 14, GRAY);
        small.setPadding(UiKit.dp(this, 6), 0, 0, UiKit.dp(this, 4));
        top.addView(small);
        body.addView(top);

        ThinBar bar = new ThinBar(this);
        bar.setFraction(running && ContentIndexer.progressTotal() > 0
                ? ContentIndexer.progressDone() / (float) ContentIndexer.progressTotal()
                : SearchSettingsText.usedFraction(stats.bytes, maxMb));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 6));
        bp.topMargin = UiKit.dp(this, 12);
        body.addView(bar, bp);

        LinearLayout meta = new LinearLayout(this);
        meta.setPadding(0, UiKit.dp(this, 6), 0, 0);
        String left;
        if (running) {
            left = SearchSettingsText.progressLine(ContentIndexer.progressDone(), ContentIndexer.progressTotal());
        } else if (Config.isContentCapped(this)) {
            left = "Full at " + SearchSettingsText.limitLabel(maxMb) + ", more files are waiting";
        } else if (!ContentIndexer.policyAllows(this, Config.isContentAllowLowBattery(this))) {
            left = "Waiting: battery is low";
        } else {
            left = SearchSettingsText.sizeLine(stats.bytes, maxMb);
        }
        meta.addView(text(left, 12, GRAY), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!running) {
            meta.addView(text(SearchSettingsText.lastRun(Config.getContentLastRun(this), System.currentTimeMillis()), 12, GRAY));
        }
        body.addView(meta);

        List<String> skipped = SearchSettingsText.skippedPills(stats.byStatus);
        if (!running && !skipped.isEmpty()) {
            HorizontalScrollView scroll = new HorizontalScrollView(this);
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout pills = new LinearLayout(this);
            for (String s : skipped) {
                TextView p = text(s, 12, 0xFFB0B0B0);
                p.setPadding(UiKit.dp(this, 10), UiKit.dp(this, 3), UiKit.dp(this, 10), UiKit.dp(this, 3));
                p.setBackground(UiKit.rounded(this, Color.TRANSPARENT, EDGE, 1f, 12));
                LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                pp.rightMargin = UiKit.dp(this, 6);
                pills.addView(p, pp);
            }
            scroll.addView(pills);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sp.topMargin = UiKit.dp(this, 12);
            body.addView(scroll, sp);
        }
        card.addView(body);

        card.addView(hairline(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        LinearLayout actions = new LinearLayout(this);
        actions.addView(action(running ? "Reading…" : "Index now", running ? DIM : Color.WHITE, this::indexNow));
        actions.addView(hairline(), new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        actions.addView(action("Files", Color.WHITE, () -> startActivity(new Intent(this, IndexedFilesActivity.class))));
        actions.addView(hairline(), new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        actions.addView(action("Clear", Color.WHITE, this::confirmClear));
        card.addView(actions);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(48, UiKit.dp(this, 10), 48, UiKit.dp(this, 10));
        wrap.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return wrap;
    }

    private TextView action(String label, int color, Runnable run) {
        TextView t = text(label, 16, color);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setPadding(0, UiKit.dp(this, 14), 0, UiKit.dp(this, 14));
        // no resting fill and no clipping: a fill or a clip would cut the box border at its corners
        android.graphics.drawable.StateListDrawable bg = new android.graphics.drawable.StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, UiKit.pressedFill());
        t.setBackground(bg);
        t.setOnClickListener(v -> run.run());
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return t;
    }

    private View hairline() {
        View v = new View(this);
        v.setBackgroundColor(HAIR);
        return v;
    }

    private void indexNow() {
        if (ContentIndexer.isRunning()) return;
        if (!FileIndex.canWalk(this)) {
            DeviceSearch.requestFullFileAccess(this);
            return;
        }
        SearchJobs.startContentIndex(this);
        Toast.makeText(this, "Reading documents in the background", Toast.LENGTH_SHORT).show();
        if (!ticking) {
            ticking = true;
            ui.postDelayed(tick, 700);
        }
    }

    private void confirmClear() {
        VaultUi.confirm(this, "Clear the index?", "Search in files will be empty until it reads again.",
                "Clear", () -> new Thread(() -> {
                    ContentIndexer.stopAndWait();   // a run in progress would keep writing to the old file
                    try {
                        ContentDb.get(this).clearAll();
                    } finally {
                        ContentIndexer.allowRuns();
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Index cleared", Toast.LENGTH_SHORT).show();
                        render();
                    });
                }).start(), "Cancel", null);
    }

    // ------------------------------------------------------------ folders popup

    private void openFolders() {
        PlainDialog.show(this, null, this::drawFolders, () -> {
            treeScroll = null;
            SearchJobs.startContentIndex(this);
            render();
        });
    }

    private void drawFolders(PlainDialog d) {
        List<StorageLocations.Volume> volumes = Volumes.list(this);
        Set<String> stored = Config.getContentFolders(this);
        int treeY = treeScroll == null ? 0 : treeScroll.getScrollY();

        TextView caption = text("CURRENT FOLDERS", 12, GRAY);
        caption.setLetterSpacing(0.15f);
        caption.setPadding(48, UiKit.dp(this, 4), 48, UiKit.dp(this, 4));

        TextView storage = text("STORAGE", 12, GRAY);
        storage.setLetterSpacing(0.15f);
        storage.setPadding(48, 0, 48, UiKit.dp(this, 4));

        LinearLayout current = new LinearLayout(this);
        current.setOrientation(LinearLayout.VERTICAL);
        List<String> sorted = new ArrayList<>(stored);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        for (String s : sorted) current.addView(currentRow(d, s, volumes));
        if (sorted.isEmpty()) {
            TextView none = text("None", 16, DIM);
            none.setPadding(48, UiKit.dp(this, 9), 48, UiKit.dp(this, 9));
            current.addView(none);
        }
        ScrollView currentScroll = new ScrollView(this);
        currentScroll.setVerticalScrollBarEnabled(false);
        currentScroll.addView(current);

        LinearLayout tree = new LinearLayout(this);
        tree.setOrientation(LinearLayout.VERTICAL);
        final View[] flash = {null};
        for (StorageLocations.Volume v : volumes) addNode(d, tree, v, "*", v.label, 0, stored, flash);
        treeScroll = new ScrollView(this);
        treeScroll.setVerticalScrollBarEnabled(false);
        treeScroll.addView(tree);

        LinearLayout.LayoutParams curLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 114));
        LinearLayout.LayoutParams treeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 260));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lineLp.topMargin = UiKit.dp(this, 14);
        lineLp.bottomMargin = UiKit.dp(this, 8);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.addView(caption);
        body.addView(currentScroll, curLp);
        body.addView(hairline(), lineLp);
        body.addView(storage);
        body.addView(treeScroll, treeLp);
        body.addView(d.option("Done", null, 0, d::dismiss));
        d.setBody(body);

        final ScrollView ts = treeScroll;
        ts.post(() -> {
            if (flash[0] != null) {
                ts.scrollTo(0, Math.max(0, flash[0].getTop() - ts.getHeight() / 2 + flash[0].getHeight() / 2));
                ValueAnimator a = ValueAnimator.ofArgb(0xFF2A2A2A, Color.BLACK);
                a.setDuration(1400);
                final View row = flash[0];
                a.addUpdateListener(x -> row.setBackgroundColor((int) x.getAnimatedValue()));
                a.start();
                focus = null;
            } else {
                ts.scrollTo(0, treeY);
            }
        });
    }

    /** One chosen folder: its name, Show (find it in the tree) and Remove (asks first). */
    private View currentRow(PlainDialog d, String stored, List<StorageLocations.Volume> volumes) {
        StorageLocations.Location loc = StorageLocations.parse(stored);
        String name = StorageLocations.entryName(loc, volumes);
        boolean known = false;
        for (StorageLocations.Volume v : volumes) if (v.id.equals(loc.volumeId)) known = true;

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(48, 0, 48, 0);
        TextView n = text(name, 16, Color.WHITE);
        n.setSingleLine(true);
        n.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(n, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (known) row.addView(smallLink("Show", () -> {
            open.add(loc.volumeId + "|*");
            String path = loc.path;
            for (int i = path.indexOf('/'); i > 0; i = path.indexOf('/', i + 1)) open.add(loc.volumeId + "|" + path.substring(0, i));
            focus = loc.volumeId + "|" + loc.path;
            drawFolders(d);
        }));
        row.addView(smallLink("Remove", () -> VaultUi.confirm(this, "Remove folder?", name, "Remove", () -> {
            Config.setContentFolders(this, StorageLocations.unchoose(Config.getContentFolders(this), loc.volumeId, loc.path));
            drawFolders(d);
        }, "Cancel", null)));
        row.setMinimumHeight(UiKit.dp(this, 38));
        return row;
    }

    private TextView smallLink(String label, Runnable run) {
        TextView t = text(label, 14, MUTED);
        t.setPadding(UiKit.dp(this, 12), UiKit.dp(this, 10), 0, UiKit.dp(this, 10));
        t.setOnClickListener(v -> run.run());
        return t;
    }

    private void addNode(PlainDialog d, LinearLayout tree, StorageLocations.Volume v, String path, String name,
                         int depth, Set<String> stored, View[] flash) {
        String key = v.id + "|" + path;
        File dir = v.root == null ? null : path.equals("*") ? v.root : new File(v.root, path);
        List<String> kids = childFolders(dir, v.primary && path.equals("*"));
        boolean chosen = StorageLocations.isChosen(stored, v.id, path);
        boolean covered = !chosen && StorageLocations.coveredByParent(stored, v.id, path);
        boolean isOpen = open.contains(key) && !kids.isEmpty();

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(48 + depth * INDENT, 0, 48, 0);
        row.setBackground(PlainDialog.rowBackground());
        row.setMinimumHeight(UiKit.dp(this, depth == 0 ? 52 : 46));

        LinearLayout left = new LinearLayout(this);
        left.setGravity(Gravity.CENTER_VERTICAL);
        int chev = UiKit.dp(this, 18);
        if (!kids.isEmpty()) {
            ImageView arrow = new ImageView(this);
            arrow.setImageDrawable(MiniIcons.chevron(chev, MUTED));
            arrow.setRotation(isOpen ? 90f : 0f);
            left.addView(arrow, new LinearLayout.LayoutParams(chev, chev));
        } else {
            left.addView(new View(this), new LinearLayout.LayoutParams(chev, chev));
        }
        TextView label = text(name, depth == 0 ? 20 : 18, v.root == null ? DIM : Color.WHITE);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setPadding(UiKit.dp(this, 8), 0, 0, 0);
        left.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        if (!kids.isEmpty()) {
            left.setOnClickListener(x -> {
                if (!open.remove(key)) open.add(key);
                drawFolders(d);
            });
        }

        if (v.root != null) {
            TextView state = text(chosen || covered ? "On" : "Off", 14, chosen ? Color.WHITE : covered ? 0xFF777777 : DIM);
            state.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            state.setPadding(UiKit.dp(this, 16), 0, 0, 0);
            row.addView(state, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            state.setOnClickListener(x -> {
                if (covered) return;
                Config.setContentFolders(this, chosen
                        ? StorageLocations.unchoose(Config.getContentFolders(this), v.id, path)
                        : StorageLocations.choose(Config.getContentFolders(this), v.id, path));
                drawFolders(d);
            });
        } else {
            row.addView(text("Not available", 14, DIM));
        }
        tree.addView(row);
        if (key.equals(focus)) flash[0] = row;

        if (isOpen) {
            for (String kid : kids) {
                addNode(d, tree, v, path.equals("*") ? kid : path + "/" + kid, kid, depth + 1, stored, flash);
            }
        }
    }

    private static List<String> childFolders(File dir, boolean primaryRoot) {
        List<String> names = new ArrayList<>();
        File[] list = dir == null ? null : dir.listFiles();
        if (list != null) {
            for (File f : list) {
                String n = f.getName();
                if (f.isDirectory() && !n.startsWith(".") && !(primaryRoot && n.equals("Android"))) names.add(n);
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    // ------------------------------------------------------------- other popups

    private void showTypes() {
        PlainDialog.show(this, "File types to read", this::drawTypes, () -> {
            SearchJobs.startContentIndex(this);
            render();
        });
    }

    private void drawTypes(PlainDialog d) {
        String[][] kinds = {{Extractors.TEXT, "Text and code"}, {Extractors.OFFICE, "Office documents"},
                {Extractors.EPUB, "E-books"}, {Extractors.PDF, "PDF (text layer only)"}};
        Set<String> chosen = Config.getContentTypes(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (String[] k : kinds) {
            boolean on = chosen.contains(k[0]);
            list.addView(d.option(k[1], on ? "On" : "Off", on ? Color.WHITE : DIM, () -> {
                Set<String> next = new HashSet<>(Config.getContentTypes(this));
                if (!next.remove(k[0])) next.add(k[0]);
                Config.setContentTypes(this, next);
                drawTypes(d);
            }));
        }
        list.addView(d.option("Done", null, 0, d::dismiss));
        d.setBody(list);
    }

    private void showSizes() {
        int current = Config.getContentMaxMb(this);
        PlainDialog.show(this, "Index size limit", d -> {
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            for (int mb : new int[]{50, 150, 500, 1000, 0}) {
                list.addView(d.option(SearchSettingsText.limitLabel(mb), mb == current ? "Selected" : null, Color.WHITE, () -> {
                    Config.setContentMaxMb(this, mb);
                    Config.setContentCapped(this, false);
                    SearchJobs.startContentIndex(this);
                    d.dismiss();
                }));
            }
            d.setBody(list);
        }, this::render);
    }

    private void showEngines() {
        String current = Config.getSearchEngine(this);
        PlainDialog.show(this, "Search engine", d -> {
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            for (String[] engine : ENGINES) {
                list.addView(d.option(engine[0], engine[1].equals(current) ? "Selected" : null, Color.WHITE, () -> {
                    Config.setSearchEngine(this, engine[1]);
                    d.dismiss();
                }));
            }
            d.setBody(list);
        }, this::render);
    }

    private String engineName() {
        String current = Config.getSearchEngine(this);
        for (String[] engine : ENGINES) if (engine[1].equals(current)) return engine[0];
        return "Custom";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0) return;

        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) granted = true;
        }
        if (granted) return;

        if (requestCode == DeviceSearch.REQUEST_FILES) {
            Config.setFileSearchEnabled(this, false);
        } else if (requestCode == DeviceSearch.REQUEST_CONTACTS) {
            Config.setContactSearchEnabled(this, false);
        }
    }

    // ------------------------------------------------------------------ building blocks

    private TextView text(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(font);
        return t;
    }

    private TextView sectionHeader(String label) {
        TextView header = text(label.toUpperCase(), 13, Color.GRAY);
        header.setLetterSpacing(0.15f);
        header.setPadding(48, 36, 48, 12);
        return header;
    }

    private View onOff(String label, String sub, boolean on, Runnable click) {
        return row(label, sub, on ? "On" : "Off", on ? Color.WHITE : DIM, click);
    }

    /** The Locker screen's row: label on the left (20), its state or value on the right (16). */
    private View row(String label, String sub, String state, int stateColor, Runnable click) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(48, 36, 48, 36);
        row.setBackground(PlainDialog.rowBackground());
        row.setOnClickListener(v -> click.run());

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(label, 20, Color.WHITE));
        if (sub != null) {
            TextView s = text(sub, 13, GRAY);
            s.setPadding(0, UiKit.dp(this, 3), 0, 0);
            col.addView(s);
        }
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView value = text(state, 16, stateColor);
        value.setSingleLine(true);
        value.setEllipsize(TextUtils.TruncateAt.END);
        value.setMaxWidth(UiKit.dp(this, 170));
        value.setPadding(UiKit.dp(this, 12), 0, 0, 0);
        row.addView(value);
        return row;
    }
}
