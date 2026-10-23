package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
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

/** Every file the content index knows, as a folder tree you can open and close; a file not read shows why. */
public class IndexedFilesActivity extends Activity {

    private static final int GRAY = 0xFF808080, REASON = 0xFFB0B0B0, INDENT = 40;

    private Typeface font;
    private ListView list;
    private List<IndexedTree.Node> tree = new ArrayList<>();
    private final Set<String> flipped = new HashSet<>();       // folders whose open state differs from the default
    private List<IndexedTree.Row> rows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);
        list = new ListView(this);
        list.setBackgroundColor(Color.BLACK);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        UiKit.screen(this, "Indexed files", list);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, i, id) -> {
            IndexedTree.Row r = rows.get(i);
            if (r.folder == null) return;
            if (!flipped.remove(r.folder.key)) flipped.add(r.folder.key);
            rebuildRows();
        });
        new Thread(() -> {
            List<ContentDb.Entry> all = ContentDb.get(this).listFiles();
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
            List<IndexedTree.Node> built = IndexedTree.build(all, roots);
            runOnUiThread(() -> {
                tree = built;
                rebuildRows();
            });
        }).start();
    }

    private void rebuildRows() {
        rows = IndexedTree.rows(tree, flipped);
        adapter.notifyDataSetChanged();
    }

    private final BaseAdapter adapter = new BaseAdapter() {
        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int i) { return rows.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int i) { return rows.get(i).folder != null ? 0 : 1; }
        @Override public boolean isEnabled(int i) { return rows.get(i).folder != null; }

        @Override
        public View getView(int i, View convert, ViewGroup parent) {
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
