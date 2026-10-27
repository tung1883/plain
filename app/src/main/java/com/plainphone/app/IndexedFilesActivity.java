package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every file the content index knows, as a folder tree you can open and close; a file not read shows why.
 * The index status box (with Pause) is on top, then a search box that swaps the tree for the matching files.
 */
public class IndexedFilesActivity extends Activity {

    private static final int GRAY = 0xFF808080, REASON = 0xFFB0B0B0, INDENT = 40, MAX_MATCHES = 300;

    private Typeface font;
    private ListView list;
    private List<IndexedTree.Node> tree = new ArrayList<>();
    private final Set<String> flipped = new HashSet<>();       // folders whose open state differs from the default
    private List<IndexedTree.Row> rows = new ArrayList<>();
    private List<ContentDb.Entry> all = new ArrayList<>();
    private List<ContentDb.Entry> matches;                  // non-null while the search box has text
    private String query = "";
    private TextView count;
    private IndexStatusCard.Ticker ticker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);
        list = new ListView(this);
        list.setBackgroundColor(Color.BLACK);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.BLACK);
        FrameLayout cardHolder = new FrameLayout(this);
        ticker = new IndexStatusCard.Ticker(cardHolder, () -> IndexStatusCard.build(this, true, ticker::start, this::load));
        page.addView(cardHolder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(searchBox(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        count = text(13, GRAY);
        count.setPadding(48, 0, 48, UiKit.dp(this, 6));
        count.setVisibility(View.GONE);
        page.addView(count);
        page.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        UiKit.screen(this, "Indexed files", page);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, i, id) -> {
            if (matches != null || i >= rows.size()) return;
            IndexedTree.Row r = rows.get(i);
            if (r.folder == null) return;
            if (!flipped.remove(r.folder.key)) flipped.add(r.folder.key);
            rebuildRows();
        });
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ticker.start();       // draws the status box now, and keeps it current while a run goes on
    }

    @Override
    protected void onPause() {
        super.onPause();
        ticker.stop();
    }

    private void load() {
        new Thread(() -> {
            List<ContentDb.Entry> found = ContentDb.get(this).listFiles();
            // the tree starts at the folders the user chose, not at the whole storage
            List<StorageLocations.Volume> volumes = Volumes.list(this);
            List<String[]> roots = new ArrayList<>();
            java.util.Map<String, Integer> uses = new java.util.HashMap<>();
            List<StorageLocations.Location> chosen = new ArrayList<>();
            for (String stored : Config.getContentFolders(this)) {
                StorageLocations.Location loc = StorageLocations.parse(stored);
                chosen.add(loc);
                String n = StorageLocations.entryName(loc, volumes);
                uses.put(n, uses.containsKey(n) ? uses.get(n) + 1 : 1);
            }
            for (StorageLocations.Location loc : chosen) {
                File dir = StorageLocations.resolve(loc, volumes);
                if (dir == null) continue;
                String name = StorageLocations.entryName(loc, volumes);
                if (uses.get(name) > 1) {          // two chosen folders with the same name: say which storage
                    for (StorageLocations.Volume v : volumes) if (v.id.equals(loc.volumeId)) name = v.label + ": " + name;
                }
                roots.add(new String[]{dir.getAbsolutePath(), name});
            }
            List<IndexedTree.Node> built = IndexedTree.build(found, roots);
            runOnUiThread(() -> {
                all = found;
                tree = built;
                applyQuery();
            });
        }).start();
    }

    private void rebuildRows() {
        rows = IndexedTree.rows(tree, flipped);
        adapter.notifyDataSetChanged();
    }

    /** Show the tree (empty box) or the files that match what is typed. */
    private void applyQuery() {
        if (query.trim().isEmpty()) {
            matches = null;
            count.setVisibility(View.GONE);
            rebuildRows();
            return;
        }
        matches = IndexedTree.filter(all, query, MAX_MATCHES);
        count.setText(matches.isEmpty() ? "No indexed file matches \"" + query.trim() + "\""
                : matches.size() >= MAX_MATCHES ? "First " + MAX_MATCHES + " files" : matches.size() + (matches.size() == 1 ? " file" : " files"));
        count.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        list.setSelection(0);
    }

    private View searchBox() {
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setBackground(UiKit.rounded(this, Color.BLACK, 0xFF2C2C2C, 1f, UiKit.R_MD));
        box.setPadding(UiKit.dp(this, 14), 0, UiKit.dp(this, 6), 0);
        EditText input = new EditText(this);
        input.setHint("Search indexed files");
        input.setHintTextColor(0xFF555555);
        input.setTextColor(Color.WHITE);
        input.setTextSize(15);
        input.setTypeface(font);
        input.setSingleLine(true);
        input.setBackground(null);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setPadding(0, UiKit.dp(this, 12), 0, UiKit.dp(this, 12));
        TextView clear = text(20, GRAY);
        clear.setText("\u00d7");
        clear.setGravity(Gravity.CENTER);
        clear.setPadding(UiKit.dp(this, 10), 0, UiKit.dp(this, 10), 0);
        clear.setVisibility(View.GONE);
        clear.setOnClickListener(v -> input.setText(""));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}

            @Override
            public void afterTextChanged(Editable e) {
                query = e.toString();
                clear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                applyQuery();
            }
        });
        box.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(clear, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(48, UiKit.dp(this, 2), 48, UiKit.dp(this, 10));
        wrap.addView(box, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return wrap;
    }

    private final BaseAdapter adapter = new BaseAdapter() {
        @Override public int getCount() { return matches != null ? matches.size() : rows.size(); }
        @Override public Object getItem(int i) { return matches != null ? matches.get(i) : rows.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public int getViewTypeCount() { return 3; }
        @Override public int getItemViewType(int i) { return matches != null ? 2 : rows.get(i).folder != null ? 0 : 1; }
        @Override public boolean isEnabled(int i) { return matches == null && rows.get(i).folder != null; }

        @Override
        public View getView(int i, View convert, ViewGroup parent) {
            if (matches != null) return matchRow(matches.get(i), convert);
            IndexedTree.Row r = rows.get(i);
            return r.folder != null ? folderRow(r, convert) : fileRow(r, convert);
        }
    };

    private View folderRow(IndexedTree.Row r, View convert) {
        LinearLayout row;
        ImageView arrow;
        TextView name, count;
        if (convert instanceof LinearLayout) {
            row = (LinearLayout) convert;
            arrow = (ImageView) row.getChildAt(0);
            name = (TextView) row.getChildAt(1);
            count = (TextView) row.getChildAt(2);
        } else {
            row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(PlainDialog.rowBackground());
            int chev = UiKit.dp(this, 18);
            arrow = new ImageView(this);
            arrow.setImageDrawable(MiniIcons.chevron(chev, 0xFF999999));
            row.addView(arrow, new LinearLayout.LayoutParams(chev, chev));
            name = text(18, Color.WHITE);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setPadding(UiKit.dp(this, 8), 0, 0, 0);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            count = text(14, GRAY);
            count.setSingleLine(true);
            count.setPadding(UiKit.dp(this, 12), 0, 0, 0);
            row.addView(count);
        }
        row.setPadding(48 + r.depth * INDENT, 28, 48, 28);
        arrow.setRotation(r.open ? 90f : 0f);
        name.setText(r.folder.name);
        count.setText(r.folder.bad > 0 ? r.folder.total + " · " + r.folder.bad + " not read" : String.valueOf(r.folder.total));
        count.setTextColor(r.folder.bad > 0 ? REASON : GRAY);
        return row;
    }

    private View fileRow(IndexedTree.Row r, View convert) {
        LinearLayout row;
        TextView name, why;
        if (convert instanceof LinearLayout) {
            row = (LinearLayout) convert;
            name = (TextView) row.getChildAt(0);
            why = (TextView) row.getChildAt(1);
        } else {
            row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            name = text(16, Color.WHITE);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            why = text(13, REASON);
            why.setMaxLines(3);
            why.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(name);
            row.addView(why);
        }
        // lines up with the folder name above it: past the chevron
        row.setPadding(48 + r.depth * INDENT + UiKit.dp(this, 26), 14, 48, 14);
        ContentDb.Entry e = r.file;
        name.setText(new File(e.path).getName());
        String reason = e.status.equals("ok") ? e.error : SearchSettingsText.reason(e.status, e.error, limitMb(e.ext));
        why.setText(reason == null ? "" : reason);
        why.setVisibility(reason == null ? View.GONE : View.VISIBLE);
        return row;
    }

    /** A file found by the search box: its name with the typed words marked, the folder, and why it was not read. */
    private View matchRow(ContentDb.Entry e, View convert) {
        LinearLayout row;
        TextView name, where, why;
        if (convert instanceof LinearLayout) {
            row = (LinearLayout) convert;
            name = (TextView) row.getChildAt(0);
            where = (TextView) row.getChildAt(1);
            why = (TextView) row.getChildAt(2);
        } else {
            row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            name = text(16, Color.WHITE);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            where = text(12, 0xFF666666);
            where.setSingleLine(true);
            where.setEllipsize(TextUtils.TruncateAt.START);
            why = text(13, REASON);
            why.setMaxLines(3);
            why.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(name);
            row.addView(where);
            row.addView(why);
        }
        row.setPadding(48, 14, 48, 14);
        File f = new File(e.path);
        name.setText(marked(f.getName(), query));
        where.setText(DeviceSearch.describeLocation(f.getParentFile()));
        String reason = e.status.equals("ok") ? e.error : SearchSettingsText.reason(e.status, e.error, limitMb(e.ext));
        why.setText(reason == null ? "" : reason);
        why.setVisibility(reason == null ? View.GONE : View.VISIBLE);
        return row;
    }

    /** {@code text} with each typed word that appears in it shown inverted (black on white). */
    private static CharSequence marked(String text, String typed) {
        SpannableString out = new SpannableString(text);
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        for (String w : typed.trim().toLowerCase(java.util.Locale.ROOT).split("\\s+")) {
            if (w.isEmpty()) continue;
            int at = lower.indexOf(w);
            if (at < 0) continue;
            out.setSpan(new BackgroundColorSpan(Color.WHITE), at, at + w.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            out.setSpan(new ForegroundColorSpan(Color.BLACK), at, at + w.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return out;
    }

    private static long limitMb(String ext) {
        String group = Extractors.groupOf(ext == null ? "" : ext);
        return group == null ? 0 : Extractors.sizeLimit(group) / (1024 * 1024);
    }

    private TextView text(float sp, int color) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(font);
        return t;
    }
}
