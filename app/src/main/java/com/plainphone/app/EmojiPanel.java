package com.plainphone.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emoji by category plus recents, then the keyboard's own: text faces (built in, plus any the user
 * saves from the clipboard) and the user's own images. The emoji lists are code-point ranges
 * filtered by what the system font can draw, so a missing glyph never shows as an empty box.
 */
final class EmojiPanel extends FrameLayout {

    private static final String RECENT_KEY = "emoji_recent", FACES_KEY = "faces_custom";
    private static final int MAX_RECENT = 24, COLUMNS = 8;

    private static final String[] CATS = {"Rec", "Smi", "Peo", "Nat", "Fod", "Obj", "Sym", "Txt", "Img"};
    private static final int[][] RANGES_SMILEYS = {{0x1F600, 0x1F637}, {0x1F910, 0x1F92F}, {0x1F970, 0x1F97A}};
    private static final int[][] RANGES_PEOPLE = {{0x1F440, 0x1F450}, {0x1F64C, 0x1F64C}, {0x1F64F, 0x1F64F},
            {0x1F4AA, 0x1F4AA}, {0x1F9E0, 0x1F9E0}, {0x1F9D1, 0x1F9DD}};
    private static final int[][] RANGES_NATURE = {{0x1F400, 0x1F43E}, {0x1F330, 0x1F344}, {0x1F980, 0x1F9AE},
            {0x2600, 0x2603}, {0x26A1, 0x26A1}, {0x1F30D, 0x1F320}};
    private static final int[][] RANGES_FOOD = {{0x1F345, 0x1F37F}, {0x1F950, 0x1F96B}};
    private static final int[][] RANGES_OBJECTS = {{0x1F4A1, 0x1F4A1}, {0x1F4BB, 0x1F4FF}, {0x1F50B, 0x1F52E},
            {0x1F527, 0x1F529}, {0x231A, 0x231B}, {0x23F0, 0x23F0}};
    private static final int[][] RANGES_SYMBOLS = {{0x2705, 0x2705}, {0x274C, 0x274C}, {0x26A0, 0x26A0},
            {0x2757, 0x2757}, {0x2753, 0x2753}, {0x2795, 0x2797}, {0x2B50, 0x2B50}, {0x1F525, 0x1F525},
            {0x1F4AF, 0x1F4AF}, {0x1F4A5, 0x1F4A5}, {0x1F389, 0x1F389}, {0x1F680, 0x1F680},
            {0x1F41B, 0x1F41B}, {0x1F6D1, 0x1F6D1}, {0x1F7E0, 0x1F7EB}, {0x1F534, 0x1F535}, {0x2764, 0x2764}};
    private static final int[] DEFAULT_RECENT = {0x1F642, 0x1F44D, 0x2705, 0x1F680, 0x1F525, 0x26A0, 0x1F41B, 0x1F4A1};

    private static Map<String, List<String>> cache;
    private static List<String> builtInFaces;

    private final KeyboardRoot.Host host;
    private final GridLayout grid;
    private final LinearLayout main, tabs, actions;
    private final FrameLayout sheet;       // a small sheet over the panel: move to, merge into, confirm delete

    // selecting in the image tab: 0 none, 1 images, 2 collections
    private int selMode;
    private final Set<String> pickImg = new LinkedHashSet<>();    // image file paths
    private final Set<String> pickColl = new LinkedHashSet<>();   // collection names
    private String cat = "Rec";
    private final ComposeSession.Starter composer;
    private final TextView searchBar;
    private String query = "";     // non-empty: the grid shows matches from every category
    private boolean secure;        // password field: no search

    EmojiPanel(Context ctx, KeyboardRoot.Host host, ComposeSession.Starter composer) {
        super(ctx);
        this.host = host;
        this.composer = composer;
        main = new LinearLayout(ctx);
        main.setOrientation(LinearLayout.VERTICAL);
        addView(main, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        searchBar = new TextView(ctx);
        searchBar.setTextSize(12);
        searchBar.setTypeface(Fonts.cascadiaMono(ctx));
        searchBar.setSingleLine(true);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setPadding(PanelKit.dp(ctx, 12), 0, PanelKit.dp(ctx, 12), 0);
        searchBar.setBackground(UiKit.rounded(ctx, PanelKit.KEY, PanelKit.LINE, 1f, 9));
        searchBar.setOnClickListener(v -> composer.start(typing, query));
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, PanelKit.dp(ctx, 32));
        sLp.bottomMargin = PanelKit.dp(ctx, 4);
        main.addView(searchBar, sLp);

        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        grid = new GridLayout(ctx);
        scroll.addView(grid, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        main.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        actions = new LinearLayout(ctx);   // while selecting: replaces the category tabs
        actions.setPadding(0, PanelKit.dp(ctx, 4), 0, 0);
        actions.setVisibility(GONE);
        main.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        tabs = new LinearLayout(ctx);
        tabs.setPadding(0, PanelKit.dp(ctx, 3), 0, 0);
        main.addView(tabs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        sheet = new FrameLayout(ctx);
        sheet.setVisibility(GONE);
        addView(sheet, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** Typing the search: the grid follows each key, behind the keys, and shows when typing ends. */
    private final ComposeSession typing = new ComposeSession() {
        @Override
        public String hint() {
            return "Search emoji";
        }

        @Override
        public void onChange(String text) {
            query = text;
        }

        @Override
        public void onDone(String text) {
            query = text.trim();
            refresh();
        }
    };

    void setSecure(boolean secure) {
        this.secure = secure;
    }

    private void drawSearchBar() {
        boolean show = !secure && selMode == 0;
        searchBar.setVisibility(show ? VISIBLE : GONE);
        if (query.isEmpty()) {
            searchBar.setText("Search emoji");
            searchBar.setTextColor(PanelKit.DIM);
        } else {
            searchBar.setText(query);
            searchBar.setTextColor(Color.WHITE);
        }
    }

    void refresh() {
        if (secure) query = "";
        if (!"Img".equals(cat)) {   // selecting only exists in the image tab
            selMode = 0;
            pickImg.clear();
            pickColl.clear();
        }
        drawTabs();
        drawSearchBar();
        drawGrid();
        drawBars();
    }

    /** Open on one tab, e.g. the image tab after adding a picture. */
    void showCategory(String c) {
        cat = c;
        refresh();
    }

    private static final int[] CAT_ICONS = {KeyIcons.CLOCK, KeyIcons.EMOJI, KeyIcons.PEOPLE, KeyIcons.LEAF,
            KeyIcons.CUP, KeyIcons.BOX, KeyIcons.HASH, KeyIcons.FACE, KeyIcons.IMAGE};

    private void drawTabs() {
        Context ctx = getContext();
        tabs.removeAllViews();
        for (int i = 0; i < CATS.length; i++) {
            final String c = CATS[i];
            PanelKit.IconView t = new PanelKit.IconView(ctx, CAT_ICONS[i]);
            t.setTab(c.equals(cat));
            t.setOnClickListener(v -> {
                cat = c;
                query = "";
                refresh();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, PanelKit.dp(ctx, 30), 1f);
            lp.rightMargin = PanelKit.dp(ctx, 2);
            tabs.addView(t, lp);
        }
    }

    private void drawGrid() {
        stopAnims();   // the old thumbnails are about to go
        grid.removeAllViews();
        if (!query.isEmpty() && !secure) {
            drawMatches();
            return;
        }
        if ("Txt".equals(cat)) {
            drawFaces();
            return;
        }
        if ("Img".equals(cat)) {
            drawImages();
            return;
        }
        Context ctx = getContext();
        grid.setColumnCount(COLUMNS);
        List<String> list = "Rec".equals(cat) ? recents(ctx) : categories().get(cat);
        if (list == null) return;
        addEmoji(ctx, list);
    }

    /** The search result: every emoji matching the query, with a label, whatever the tab. */
    private void drawMatches() {
        Context ctx = getContext();
        grid.setColumnCount(COLUMNS);
        List<String> found = EmojiKeywords.search(ctx, query);
        TextView label = PanelKit.caption(ctx, "MATCHES FOR \"" + query + "\"");
        GridLayout.LayoutParams lp = cell(COLUMNS, 0);
        lp.setMargins(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 4), 0, PanelKit.dp(ctx, 2));
        grid.addView(label, lp);
        if (found.isEmpty()) {
            TextView none = new TextView(ctx);
            none.setText("No emoji found");
            none.setTextSize(12);
            none.setTextColor(PanelKit.DIM);
            none.setTypeface(Fonts.cascadiaMono(ctx));
            none.setGravity(Gravity.CENTER);
            none.setPadding(0, PanelKit.dp(ctx, 24), 0, 0);
            grid.addView(none, cell(COLUMNS, 0));
            return;
        }
        addEmoji(ctx, found);
    }

    private void addEmoji(Context ctx, List<String> list) {
        for (String e : list) {
            TextView t = new TextView(ctx);
            t.setText(e);
            t.setTextSize(22);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, PanelKit.dp(ctx, 5), 0, PanelKit.dp(ctx, 5));
            t.setBackground(UiKit.pressable(ctx, 0x00000000, PanelKit.DOWN, 0, 0f, 8));
            t.setOnClickListener((View v) -> {
                host.insertText(e);
                remember(ctx, e);
            });
            grid.addView(t, cell(1, 0));
        }
    }

    /** A grid cell {@code span} columns wide with equal weight; {@code heightPx} 0 = wrap. */
    private static GridLayout.LayoutParams cell(int span, int heightPx) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, span, 1f));
        lp.width = 0;
        if (heightPx > 0) lp.height = heightPx;
        return lp;
    }

    // ------------------------------------------------------------ text faces

    private void drawFaces() {
        Context ctx = getContext();
        grid.setColumnCount(2);

        TextView add = PanelKit.chip(ctx, "+ Save clipboard as a face", true, v -> {
            String clip = clipboardText();
            if (clip == null) return;
            saveFace(ctx, clip);
            refresh();
        });
        GridLayout.LayoutParams addLp = cell(2, 0);
        addLp.setMargins(0, 0, 0, PanelKit.dp(ctx, 4));
        grid.addView(add, addLp);

        List<String> mine = customFaces(ctx);
        if (!mine.isEmpty()) {
            TextView caption = PanelKit.caption(ctx, "MINE - HOLD ONE TO DELETE");
            GridLayout.LayoutParams cLp = cell(2, 0);
            cLp.setMargins(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 2), 0, PanelKit.dp(ctx, 4));
            grid.addView(caption, cLp);
            for (String face : mine) grid.addView(faceTile(ctx, face, true), faceLp(ctx));
        }
        for (String face : builtIn(ctx)) grid.addView(faceTile(ctx, face, false), faceLp(ctx));
    }

    private GridLayout.LayoutParams faceLp(Context ctx) {
        GridLayout.LayoutParams lp = cell(1, 0);
        lp.setMargins(0, 0, PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 4));
        return lp;
    }

    private TextView faceTile(Context ctx, String face, boolean custom) {
        TextView t = PanelKit.chip(ctx, face, false, v -> host.insertText(face));
        t.setTypeface(null);                 // the system font has the symbols these faces use
        t.setTextSize(14);
        t.setPadding(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 10));
        if (custom) {
            t.setOnLongClickListener(v -> {
                deleteFace(ctx, face);
                refresh();
                return true;
            });
        }
        return t;
    }

    private String clipboardText() {
        try {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = cm == null ? null : cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return null;
            CharSequence text = clip.getItemAt(0).coerceToText(getContext());
            String s = text == null ? "" : text.toString().trim();
            return s.isEmpty() || s.length() > 60 || s.contains("\n") ? null : s;
        } catch (SecurityException e) {
            return null;
        }
    }

    private static List<String> builtIn(Context ctx) {
        if (builtInFaces != null) return builtInFaces;
        List<String> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                ctx.getAssets().open("kb/faces.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) if (!line.trim().isEmpty()) out.add(line.trim());
        } catch (IOException ignored) {
        }
        builtInFaces = out;
        return out;
    }

    private static List<String> customFaces(Context c) {
        List<String> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(PanelKit.prefs(c).getString(FACES_KEY, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
        } catch (JSONException ignored) {
        }
        return out;
    }

    private static void storeFaces(Context c, List<String> faces) {
        PanelKit.prefs(c).edit().putString(FACES_KEY, new JSONArray(faces).toString()).apply();
    }

    private static void saveFace(Context c, String face) {
        List<String> faces = customFaces(c);
        faces.remove(face);
        faces.add(0, face);
        storeFaces(c, faces);
    }

    private static void deleteFace(Context c, String face) {
        List<String> faces = customFaces(c);
        faces.remove(face);
        storeFaces(c, faces);
    }

    // ----------------------------------------------------------------- images

    private String collection = StickerStore.DEFAULT;   // the image collection on show

    private void drawImages() {
        Context ctx = getContext();
        grid.setColumnCount(4);
        int size = PanelKit.dp(ctx, 78);

        if (StickerStore.pendingCollection != null) {   // just added: open on that collection
            collection = StickerStore.pendingCollection;
            StickerStore.pendingCollection = null;
        }
        List<String> names = StickerStore.collections(ctx);
        if (!names.contains(collection)) collection = StickerStore.DEFAULT;
        List<File> files = StickerStore.list(ctx, collection);

        // forget picks of things that are gone
        pickColl.retainAll(names);
        Set<String> have = new java.util.HashSet<>();
        for (File f : files) have.add(f.getAbsolutePath());
        pickImg.retainAll(have);

        if (selMode == 1) {
            grid.addView(selectHeader(ctx, pickImg.size() + " SELECTED", files.size()), cell(4, 0));
        } else {
            if (selMode == 2) {
                grid.addView(selectHeader(ctx, pickColl.size() + (pickColl.size() == 1 ? " COLLECTION SELECTED"
                        : " COLLECTIONS SELECTED"), names.size()), cell(4, 0));
            }
            GridLayout.LayoutParams topLp = cell(4, 0);
            topLp.setMargins(0, 0, 0, PanelKit.dp(ctx, 4));
            grid.addView(collectionRow(ctx, names), topLp);
        }

        if (files.isEmpty()) {
            TextView hint = PanelKit.caption(ctx, "NO IMAGES IN " + collection.toUpperCase());
            GridLayout.LayoutParams hLp = cell(4, 0);
            hLp.setMargins(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 8), 0, 0);
            grid.addView(hint, hLp);
            return;
        }
        if (selMode == 0) {
            TextView caption = PanelKit.caption(ctx, "TAP TO SEND - HOLD TO SELECT");
            GridLayout.LayoutParams cLp = cell(4, 0);
            cLp.setMargins(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 2), 0, PanelKit.dp(ctx, 4));
            grid.addView(caption, cLp);
        }
        for (File f : files) grid.addView(imageCell(ctx, f, size), cell(1, size));
    }

    /** "3 SELECTED" with All and Done, shown while selecting. */
    private View selectHeader(Context ctx, String title, int total) {
        LinearLayout head = new LinearLayout(ctx);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(PanelKit.dp(ctx, 4), 0, 0, PanelKit.dp(ctx, 2));
        TextView cap = PanelKit.caption(ctx, title);
        head.addView(cap, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(PanelKit.link(ctx, "All", v -> {
            if (selMode == 1) {
                for (File f : StickerStore.list(ctx, collection)) pickImg.add(f.getAbsolutePath());
            } else {
                pickColl.addAll(StickerStore.collections(ctx));
            }
            refresh();
        }));
        head.addView(PanelKit.link(ctx, "Done", v -> stopSelecting()));
        return head;
    }

    private void stopSelecting() {
        selMode = 0;
        pickImg.clear();
        pickColl.clear();
        refresh();
    }

    /** The collection chips: tap to open one; while selecting collections, tap to pick. */
    private View collectionRow(Context ctx, List<String> names) {
        android.widget.HorizontalScrollView strip = new android.widget.HorizontalScrollView(ctx);
        strip.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(ctx);
        for (String name : names) {
            boolean picked = pickColl.contains(name);
            String label = name + "  " + StickerStore.list(ctx, name).size();
            if (selMode == 2) label = (picked ? "● " : "○ ") + label;
            TextView chip = PanelKit.chipSmall(ctx, label,
                    selMode == 2 ? picked : name.equals(collection), v -> {
                        if (selMode == 2) {
                            if (!pickColl.remove(name)) pickColl.add(name);
                        } else {
                            collection = name;
                        }
                        refresh();
                    });
            chip.setOnLongClickListener(v -> {   // hold a collection to start selecting collections
                if (selMode == 1) return false;
                selMode = 2;
                pickColl.add(name);
                refresh();
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = PanelKit.dp(ctx, 4);
            chips.addView(chip, lp);
        }
        strip.addView(chips);

        // one row: the collections scroll on the left, "+ Add" stays on the right (not while selecting)
        LinearLayout top = new LinearLayout(ctx);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(strip, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (selMode == 0) {
            TextView add = new TextView(ctx);   // plain white text, no button background
            add.setText("+ Add");
            add.setTextSize(12);
            add.setTextColor(0xFFFFFFFF);
            add.setTypeface(Fonts.cascadiaMono(ctx));
            add.setGravity(Gravity.CENTER);
            add.setSingleLine(true);
            add.setPadding(PanelKit.dp(ctx, 12), 0, PanelKit.dp(ctx, 8), 0);
            add.setOnClickListener(v -> host.addSticker(collection));
            top.addView(add, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, PanelKit.dp(ctx, 32)));
        }
        return top;
    }

    /** One image: tap sends it (or picks it while selecting), hold starts selecting. */
    private View imageCell(Context ctx, File f, int size) {
        boolean picked = pickImg.contains(f.getAbsolutePath());
        FrameLayout cell = new FrameLayout(ctx);
        ImageView iv = new ImageView(ctx);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setPadding(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 4));
        iv.setBackground(picked ? UiKit.rounded(ctx, 0x00000000, Color.WHITE, PanelKit.dp(ctx, 1.5f), 10)
                : UiKit.pressable(ctx, 0x00000000, PanelKit.DOWN, 0, 0f, 8));
        if (!animate(iv, f, size)) iv.setImageBitmap(thumb(f, size));
        if (selMode == 1 && !picked) iv.setAlpha(0.55f);
        if (selMode == 2) iv.setAlpha(0.45f);   // collections are being picked: the images wait
        cell.addView(iv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (selMode == 1) {
            View ring = new View(ctx);   // a ring in the corner, filled when picked
            android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dot.setColor(picked ? Color.WHITE : 0xAA000000);
            dot.setStroke(PanelKit.dp(ctx, 1), picked ? Color.WHITE : 0xFF777777);
            ring.setBackground(dot);
            FrameLayout.LayoutParams rLp = new FrameLayout.LayoutParams(PanelKit.dp(ctx, 15), PanelKit.dp(ctx, 15),
                    Gravity.TOP | Gravity.END);
            rLp.setMargins(0, PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 4), 0);
            cell.addView(ring, rLp);
            if (picked) {
                TextView check = new TextView(ctx);
                check.setText("✓");
                check.setTextSize(9);
                check.setTextColor(Color.BLACK);
                check.setGravity(Gravity.CENTER);
                cell.addView(check, rLp);
            }
        }
        if (StickerStore.isGifFile(f) && selMode != 2) {
            TextView tag = new TextView(ctx);
            tag.setText("GIF");
            tag.setTextSize(7.5f);
            tag.setTextColor(Color.BLACK);
            tag.setTypeface(Fonts.cascadiaMono(ctx));
            tag.setGravity(Gravity.CENTER);
            tag.setBackground(UiKit.rounded(ctx, Color.WHITE, Color.WHITE, 0f, 4));
            cell.addView(tag, new FrameLayout.LayoutParams(PanelKit.dp(ctx, 20), PanelKit.dp(ctx, 12),
                    Gravity.BOTTOM | Gravity.START));
        }

        cell.setOnClickListener(v -> {
            if (selMode == 1) {
                if (!pickImg.remove(f.getAbsolutePath())) pickImg.add(f.getAbsolutePath());
                refresh();
            } else if (selMode == 0) {
                host.sendSticker(f);
            }
        });
        cell.setOnLongClickListener(v -> {   // hold an image to start selecting images
            if (selMode == 2) return false;
            selMode = 1;
            pickImg.add(f.getAbsolutePath());
            refresh();
            return true;
        });
        return cell;
    }

    // ------------------------------------------------------- actions and sheets

    /** The bottom row: category tabs normally, the selection's actions while selecting. */
    private void drawBars() {
        Context ctx = getContext();
        drawSearchBar();
        boolean selecting = selMode != 0 && "Img".equals(cat);
        tabs.setVisibility(selecting ? GONE : VISIBLE);
        actions.setVisibility(selecting ? VISIBLE : GONE);
        actions.removeAllViews();
        if (!selecting) return;
        int n = selMode == 1 ? pickImg.size() : pickColl.size();
        boolean canFirst = selMode == 1 ? n > 0 : n > 1;   // merging needs two or more
        TextView first = PanelKit.chip(ctx, selMode == 1 ? "Move to..." : "Merge into...", false, v -> {
            if (canFirst) openSheet(selMode == 1 ? 1 : 2);
        });
        TextView delete = PanelKit.chip(ctx, "Delete" + (n > 0 ? " " + n : ""), true, v -> {
            if (n > 0) openSheet(selMode == 1 ? 3 : 4);
        });
        first.setAlpha(canFirst ? 1f : 0.4f);
        delete.setAlpha(n > 0 ? 1f : 0.4f);
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, PanelKit.dp(ctx, 38), 1f);
        a.rightMargin = PanelKit.dp(ctx, 4);
        actions.addView(first, a);
        actions.addView(delete, new LinearLayout.LayoutParams(0, PanelKit.dp(ctx, 38), 1f));
    }

    private List<File> pickedFiles() {
        List<File> out = new ArrayList<>();
        for (String path : pickImg) out.add(new File(path));
        return out;
    }

    /** 1 move to, 2 merge into, 3 confirm deleting images, 4 confirm deleting collections. */
    private void openSheet(int kind) {
        Context ctx = getContext();
        sheet.removeAllViews();
        View scrim = new View(ctx);
        scrim.setBackgroundColor(0x99000000);
        scrim.setOnClickListener(v -> closeSheet());
        sheet.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(UiKit.rounded(ctx, 0xFF101010, 0xFF333333, 1f, 14));
        box.setPadding(PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 12));
        box.setClickable(true);   // taps on the sheet itself do not reach the scrim

        if (kind == 1) {
            box.addView(sheetTitle(ctx, "MOVE " + pickImg.size() + " TO"));
            if (StickerStore.collections(ctx).size() < 2) {
                TextView none = PanelKit.caption(ctx, "NO OTHER COLLECTION - ADD IMAGES TO A NEW ONE FIRST");
                none.setPadding(PanelKit.dp(ctx, 2), PanelKit.dp(ctx, 6), 0, PanelKit.dp(ctx, 6));
                box.addView(none);
            }
            for (String name : StickerStore.collections(ctx)) {
                if (name.equals(collection)) continue;
                box.addView(optionRow(ctx, name, StickerStore.list(ctx, name).size(), () -> {
                    int moved = StickerStore.move(ctx, pickedFiles(), name);
                    done("Moved " + moved + " to " + name);
                }));
            }
        } else if (kind == 2) {
            box.addView(sheetTitle(ctx, "MERGE " + pickColl.size() + " INTO"));
            for (String name : pickColl) {
                box.addView(optionRow(ctx, name, StickerStore.list(ctx, name).size(), () -> {
                    int moved = StickerStore.merge(ctx, new ArrayList<>(pickColl), name);
                    collection = name;
                    done("Merged " + moved + " images into " + name);
                }));
            }
        } else {
            int images = 0;
            if (kind == 4) for (String c : pickColl) images += StickerStore.list(ctx, c).size();
            TextView msg = new TextView(ctx);
            msg.setTextSize(12);
            msg.setTextColor(0xFFE8E8E8);
            msg.setTypeface(Fonts.cascadiaMono(ctx));
            msg.setLineSpacing(0, 1.25f);
            msg.setPadding(0, 0, 0, PanelKit.dp(ctx, 10));
            msg.setText(kind == 3
                    ? "Delete " + pickImg.size() + (pickImg.size() == 1 ? " image" : " images") + " from " + collection + "?"
                    : "Delete " + pickColl.size() + (pickColl.size() == 1 ? " collection" : " collections") + " and the "
                            + images + (images == 1 ? " image" : " images") + " in "
                            + (pickColl.size() == 1 ? "it" : "them") + "? This cannot be undone.");
            box.addView(msg);
            LinearLayout row = new LinearLayout(ctx);
            LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, PanelKit.dp(ctx, 38), 1f);
            a.rightMargin = PanelKit.dp(ctx, 4);
            row.addView(PanelKit.chip(ctx, "Cancel", false, v -> closeSheet()), a);
            row.addView(PanelKit.chip(ctx, "Delete", true, v -> {
                if (kind == 3) {
                    int k = 0;
                    for (File f : pickedFiles()) {
                        StickerStore.delete(f);
                        k++;
                    }
                    done("Deleted " + k);
                } else {
                    int k = pickColl.size();
                    for (String c : new ArrayList<>(pickColl)) StickerStore.deleteCollection(ctx, c);
                    collection = StickerStore.DEFAULT;
                    done("Deleted " + k + (k == 1 ? " collection" : " collections"));
                }
            }), new LinearLayout.LayoutParams(0, PanelKit.dp(ctx, 38), 1f));
            box.addView(row);
        }
        sheet.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        sheet.setVisibility(VISIBLE);
    }

    /** True while a sheet or select mode is open: Back should close that, not the keyboard. */
    boolean canGoBack() {
        return sheet.getVisibility() == VISIBLE || selMode != 0;
    }

    void goBack() {
        if (sheet.getVisibility() == VISIBLE) closeSheet();
        else if (selMode != 0) stopSelecting();
    }

    private void closeSheet() {
        sheet.setVisibility(GONE);
        sheet.removeAllViews();
    }

    /** An action finished: close the sheet, leave select mode, say what happened. */
    private void done(String message) {
        closeSheet();
        android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        stopSelecting();
    }

    private TextView sheetTitle(Context ctx, String text) {
        TextView t = PanelKit.caption(ctx, text);
        t.setPadding(PanelKit.dp(ctx, 2), 0, 0, PanelKit.dp(ctx, 6));
        return t;
    }

    /** A line in a sheet: a ring, a name, its image count on the right. */
    private View optionRow(Context ctx, String name, int count, Runnable pick) {
        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(PanelKit.dp(ctx, 6), PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 6), PanelKit.dp(ctx, 10));
        row.setBackground(UiKit.pressable(ctx, 0x00000000, PanelKit.DOWN, 0, 0f, 8));
        View ring = new View(ctx);
        android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
        dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        dot.setColor(0x00000000);
        dot.setStroke(PanelKit.dp(ctx, 1.5f), 0xFF555555);
        ring.setBackground(dot);
        row.addView(ring, new LinearLayout.LayoutParams(PanelKit.dp(ctx, 14), PanelKit.dp(ctx, 14)));
        TextView label = new TextView(ctx);
        label.setText(name);
        label.setTextSize(13);
        label.setTextColor(PanelKit.FG);
        label.setTypeface(Fonts.cascadiaMono(ctx));
        label.setPadding(PanelKit.dp(ctx, 10), 0, 0, 0);
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView n = new TextView(ctx);
        n.setText(String.valueOf(count));
        n.setTextSize(10);
        n.setTextColor(PanelKit.DIM);
        n.setTypeface(Fonts.cascadiaMono(ctx));
        row.addView(n);
        row.setOnClickListener(v -> pick.run());
        return row;
    }

    // GIF thumbnails play (Android 9+); they stop whenever the panel is left or redrawn
    private final List<android.graphics.drawable.AnimatedImageDrawable> anims = new ArrayList<>();

    /** Show a GIF as a playing thumbnail; false if it is not a GIF or cannot be played here. */
    private boolean animate(ImageView view, File f, int size) {
        if (!StickerStore.isGifFile(f) || android.os.Build.VERSION.SDK_INT < 28) return false;
        try {
            android.graphics.drawable.Drawable d = android.graphics.ImageDecoder.decodeDrawable(
                    android.graphics.ImageDecoder.createSource(f), (decoder, info, src) -> {
                        int side = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                        decoder.setTargetSampleSize(Math.max(1, side / size));
                    });
            view.setImageDrawable(d);
            if (d instanceof android.graphics.drawable.AnimatedImageDrawable) {
                android.graphics.drawable.AnimatedImageDrawable a = (android.graphics.drawable.AnimatedImageDrawable) d;
                a.setRepeatCount(android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE);
                a.start();
                anims.add(a);
            }
            return true;
        } catch (java.io.IOException | RuntimeException e) {
            return false;
        }
    }

    private void stopAnims() {
        for (android.graphics.drawable.AnimatedImageDrawable a : anims) a.stop();
        anims.clear();
    }

    /** Called when the keyboard leaves this panel: stop the GIFs and drop any selection. */
    void release() {
        stopAnims();
        query = "";
        selMode = 0;
        pickImg.clear();
        pickColl.clear();
        sheet.setVisibility(GONE);
        sheet.removeAllViews();
    }

    private static Bitmap thumb(File f, int size) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= size) sample *= 2;
        BitmapFactory.Options d = new BitmapFactory.Options();
        d.inSampleSize = sample;
        return BitmapFactory.decodeFile(f.getAbsolutePath(), d);
    }

    // ---------------------------------------------------------------- emoji data

    private static synchronized Map<String, List<String>> categories() {
        if (cache != null) return cache;
        Paint probe = new Paint();
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("Smi", build(probe, RANGES_SMILEYS));
        m.put("Peo", build(probe, RANGES_PEOPLE));
        m.put("Nat", build(probe, RANGES_NATURE));
        m.put("Fod", build(probe, RANGES_FOOD));
        m.put("Obj", build(probe, RANGES_OBJECTS));
        m.put("Sym", build(probe, RANGES_SYMBOLS));
        cache = m;
        return m;
    }

    private static List<String> build(Paint probe, int[][] ranges) {
        List<String> out = new ArrayList<>();
        for (int[] r : ranges) {
            for (int cp = r[0]; cp <= r[1]; cp++) {
                String s = new String(Character.toChars(cp));
                if (probe.hasGlyph(s)) out.add(s);
            }
        }
        return out;
    }

    private static List<String> recents(Context c) {
        List<String> out = new ArrayList<>();
        String raw = PanelKit.prefs(c).getString(RECENT_KEY, "");
        for (String part : raw.split(",")) {
            if (part.isEmpty()) continue;
            try {
                out.add(new String(Character.toChars(Integer.parseInt(part, 16))));
            } catch (NumberFormatException ignored) {
            }
        }
        if (out.isEmpty()) {
            for (int cp : DEFAULT_RECENT) out.add(new String(Character.toChars(cp)));
        }
        return out;
    }

    private static void remember(Context c, String emoji) {
        List<String> list = recents(c);
        list.remove(emoji);
        list.add(0, emoji);
        while (list.size() > MAX_RECENT) list.remove(list.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (String e : list) {
            if (sb.length() > 0) sb.append(',');
            sb.append(Integer.toHexString(e.codePointAt(0)));
        }
        PanelKit.prefs(c).edit().putString(RECENT_KEY, sb.toString()).apply();
    }
}
