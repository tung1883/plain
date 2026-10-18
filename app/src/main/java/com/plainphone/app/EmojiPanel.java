package com.plainphone.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emoji by category plus recents, then the keyboard's own: text faces (built in, plus any the user
 * saves from the clipboard) and the user's own images. The emoji lists are code-point ranges
 * filtered by what the system font can draw, so a missing glyph never shows as an empty box.
 */
final class EmojiPanel extends LinearLayout {

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
    private final LinearLayout tabs;
    private String cat = "Rec";

    EmojiPanel(Context ctx, KeyboardRoot.Host host) {
        super(ctx);
        this.host = host;
        setOrientation(VERTICAL);

        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        grid = new GridLayout(ctx);
        scroll.addView(grid, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        tabs = new LinearLayout(ctx);
        tabs.setPadding(0, PanelKit.dp(ctx, 3), 0, 0);
        addView(tabs, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    void refresh() {
        drawTabs();
        drawGrid();
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
                refresh();
            });
            LayoutParams lp = new LayoutParams(0, PanelKit.dp(ctx, 30), 1f);
            lp.rightMargin = PanelKit.dp(ctx, 2);
            tabs.addView(t, lp);
        }
    }

    private void drawGrid() {
        grid.removeAllViews();
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
    private String confirmDelete;                       // a collection chip waiting for its second tap

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

        // one chip per collection; hold one to delete it (tap the "Delete?" chip to confirm)
        android.widget.HorizontalScrollView strip = new android.widget.HorizontalScrollView(ctx);
        strip.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(ctx);
        for (String name : names) {
            boolean deleting = name.equals(confirmDelete);
            TextView chip = PanelKit.chipSmall(ctx, deleting ? "Delete " + name + "?" : name,
                    !deleting && name.equals(collection), v -> {
                        if (deleting) {
                            StickerStore.deleteCollection(ctx, name);
                            confirmDelete = null;
                            collection = StickerStore.DEFAULT;
                        } else {
                            collection = name;
                            confirmDelete = null;
                        }
                        refresh();
                    });
            chip.setOnLongClickListener(v -> {
                confirmDelete = name;
                refresh();
                postDelayed(() -> {
                    if (name.equals(confirmDelete)) {
                        confirmDelete = null;
                        if ("Img".equals(cat)) refresh();
                    }
                }, 3000);
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = PanelKit.dp(ctx, 4);
            chips.addView(chip, lp);
        }
        strip.addView(chips);

        // one row: the collections scroll on the left, a + button stays on the right
        LinearLayout top = new LinearLayout(ctx);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(strip, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView add = new TextView(ctx);   // plain white text, no button background
        add.setText("+ Add");
        add.setTextSize(12);
        add.setTextColor(0xFFFFFFFF);
        add.setTypeface(Fonts.cascadiaMono(ctx));
        add.setGravity(Gravity.CENTER);
        add.setSingleLine(true);
        add.setPadding(PanelKit.dp(ctx, 12), 0, PanelKit.dp(ctx, 8), 0);
        add.setOnClickListener(v -> host.addSticker(collection));
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, PanelKit.dp(ctx, 32));
        top.addView(add, addLp);
        GridLayout.LayoutParams topLp = cell(4, 0);
        topLp.setMargins(0, 0, 0, PanelKit.dp(ctx, 4));
        grid.addView(top, topLp);

        List<File> files = StickerStore.list(ctx, collection);
        if (files.isEmpty()) {
            TextView hint = PanelKit.caption(ctx, "NO IMAGES IN " + collection.toUpperCase());
            GridLayout.LayoutParams hLp = cell(4, 0);
            hLp.setMargins(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 8), 0, 0);
            grid.addView(hint, hLp);
            return;
        }
        TextView caption = PanelKit.caption(ctx, "TAP TO SEND - HOLD TO DELETE");
        GridLayout.LayoutParams cLp = cell(4, 0);
        cLp.setMargins(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 2), 0, PanelKit.dp(ctx, 4));
        grid.addView(caption, cLp);
        for (File f : files) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setPadding(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 4));
            iv.setBackground(UiKit.pressable(ctx, 0x00000000, PanelKit.DOWN, 0, 0f, 8));
            iv.setImageBitmap(thumb(f, size));
            iv.setOnClickListener(v -> host.sendSticker(f));
            iv.setOnLongClickListener(v -> {
                StickerStore.delete(f);
                refresh();
                return true;
            });
            grid.addView(iv, cell(1, size));
        }
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
