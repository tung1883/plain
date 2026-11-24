package com.plainphone.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGExternalFileResolver;
import com.caverock.androidsvg.SVGParseException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The sticker editor's library: vector items (from assets/stickers/items.json) and their ready-made combos. */
final class StickerLib {

    /** One drawn item: SVG drawn around the origin, and the scale it is added at. */
    static final class Def {
        final String id, cat, label, svg;
        final float scale;
        String attr, primary;   // the colour that "fill" changes: its attribute (fill / stroke) and hex; null when the item has none

        Def(String id, String cat, String label, String svg, float scale) {
            this.id = id;
            this.cat = cat;
            this.label = label;
            this.svg = svg;
            this.scale = scale;
            findPrimary();
        }

        /** The item's main colour: its most used fill (or, when it has no fills, its most used stroke). */
        private void findPrimary() {
            for (String a : new String[]{"fill", "stroke"}) {
                Map<String, Integer> n = new HashMap<>();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b" + a + "=\"(#[0-9a-fA-F]{6})\"").matcher(svg);
                String best = null;
                while (m.find()) {
                    String h = m.group(1).toLowerCase(java.util.Locale.US);
                    int c = n.containsKey(h) ? n.get(h) + 1 : 1;
                    n.put(h, c);
                    if (best == null || c > n.get(best)) best = h;
                }
                if (best != null) {
                    attr = a;
                    primary = best;
                    return;
                }
            }
        }
    }

    /** How an item is coloured: its own colours (0), one solid colour (1) or a two-colour gradient (2) in a direction (0 down, 1 across, 2 diagonal). */
    static final class Fill {
        int mode;
        int a = 0xFFFF5C9A, b = 0xFF5CC8FF;
        int dir;
    }

    /** An item with a caption already attached. */
    static final class Combo {
        final String label, def, text;
        final int color;
        final float ts;

        Combo(String label, String def, String text, int color, float ts) {
            this.label = label;
            this.def = def;
            this.text = text;
            this.color = color;
            this.ts = ts;
        }
    }

    /** Pixels per unit the items are drawn at (an item is up to ~180 units across). */
    private static final float PPU = 4f;

    static final List<String> cats = new ArrayList<>();
    static final List<Def> items = new ArrayList<>();
    static final List<Combo> combos = new ArrayList<>();
    private static final Map<String, StickerGfx.Sprite> sprites = new HashMap<>();
    private static boolean loaded;

    private StickerLib() {}

    static synchronized void load(Context c) {
        if (loaded) return;
        final Context app = c.getApplicationContext();
        try (InputStream in = app.getAssets().open("stickers/items.json")) {
            byte[] buf = new byte[in.available()];
            int n = 0;
            while (n < buf.length) {
                int r = in.read(buf, n, buf.length - n);
                if (r < 0) break;
                n += r;
            }
            JSONObject root = new JSONObject(new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8));
            JSONArray cs = root.getJSONArray("cats");
            for (int i = 0; i < cs.length(); i++) cats.add(cs.getString(i));
            JSONArray its = root.getJSONArray("items");
            for (int i = 0; i < its.length(); i++) {
                JSONObject o = its.getJSONObject(i);
                items.add(new Def(o.getString("id"), o.getString("cat"), o.getString("label"), o.getString("svg"), (float) o.optDouble("scale", 1.3)));
            }
            JSONArray cb = root.getJSONArray("combos");
            for (int i = 0; i < cb.length(); i++) {
                JSONObject o = cb.getJSONObject(i);
                combos.add(new Combo(o.getString("label"), o.getString("def"), o.getString("text"), o.getInt("color"), (float) o.getDouble("ts")));
            }
        } catch (Exception e) {
            android.util.Log.e("StickerLib", "items.json", e);
        }
        // the "?", "!", "zZ" marks are text: draw them in the caption font
        SVG.registerExternalFileResolver(new SVGExternalFileResolver() {
            @Override
            public Typeface resolveFont(String fontFamily, int fontWeight, String fontStyle) {
                return Fonts.caption(app, 0);
            }
        });
        loaded = true;
    }

    static Def def(String id) {
        for (Def d : items) if (d.id.equals(id)) return d;
        return null;
    }

    /** The item drawn once and kept (shared by every copy of it on the canvas). */
    static synchronized StickerGfx.Sprite sprite(Context c, Def d) {
        StickerGfx.Sprite s = sprites.get(d.id);
        if (s != null) return s;
        float box = 180f;
        int px = Math.round(box * PPU);
        String doc = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"-90 -90 180 180\" width=\"" + px + "\" height=\"" + px + "\">" + d.svg + "</svg>";
        try {
            SVG svg = SVG.getFromString(doc);
            Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            svg.renderToCanvas(new Canvas(b));
            s = StickerGfx.trim(b, PPU, -box / 2f, -box / 2f);
            if (s != null) sprites.put(d.id, s);
        } catch (SVGParseException e) {
            android.util.Log.e("StickerLib", "svg " + d.id, e);
        }
        return s;
    }

    private static final Map<String, StickerGfx.Sprite> filled = new HashMap<>();

    /** The item in the colours of {@code fill}; the same size and place as the plain sprite, so its border and box do not move. */
    static synchronized StickerGfx.Sprite sprite(Context c, Def d, Fill fill) {
        StickerGfx.Sprite base = sprite(c, d);
        if (base == null || fill == null || fill.mode == 0 || d.primary == null) return base;
        String key = d.id + "|" + fill.mode + "|" + fill.a + "|" + (fill.mode == 2 ? fill.b + "|" + fill.dir : "");
        StickerGfx.Sprite s = filled.get(key);
        if (s != null) return s;
        if (filled.size() > 80) filled.clear();
        int left = Math.round((base.x0 + 90f) * PPU), top = Math.round((base.y0 + 90f) * PPU), w = base.bmp.getWidth(), h = base.bmp.getHeight();
        try {
            if (fill.mode == 1) {
                Bitmap full = renderBody(recolor(d, hex(fill.a), hex(dark(fill.a))));
                s = new StickerGfx.Sprite(crop(full, left, top, w, h), PPU, base.x0, base.y0);
            } else {
                // two renders, the colour replaced by black and by white, tell per pixel how much of it is that colour: the gradient goes there
                int mid = mix(fill.a, fill.b, .5f);
                Bitmap rb = crop(renderBody(recolor(d, "#000000", hex(dark(mid)))), left, top, w, h);
                Bitmap rw = crop(renderBody(recolor(d, "#ffffff", hex(dark(mid)))), left, top, w, h);
                w = rb.getWidth();
                h = rb.getHeight();
                int[] pb = new int[w * h], pw = new int[w * h];
                rb.getPixels(pb, 0, w, 0, 0, w, h);
                rw.getPixels(pw, 0, w, 0, 0, w, h);
                double dx = fill.dir == 1 ? 1 : fill.dir == 2 ? Math.sqrt(.5) : 0, dy = fill.dir == 0 ? 1 : fill.dir == 2 ? Math.sqrt(.5) : 0;
                double len = Math.abs(w * dx) + Math.abs(h * dy);
                int[] out = new int[w * h];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int i = y * w + x, p = pb[i], q = pw[i];
                        int al = p >>> 24;
                        if (al == 0) continue;
                        float k = (((q >> 16 & 255) - (p >> 16 & 255)) + ((q >> 8 & 255) - (p >> 8 & 255)) + ((q & 255) - (p & 255))) / (3f * 255f);
                        k = Math.max(0f, Math.min(1f, k));
                        double t = ((x + .5 - w / 2.0) * dx + (y + .5 - h / 2.0) * dy) / len + .5;
                        int g = mix(fill.a, fill.b, (float) Math.max(0, Math.min(1, t)));
                        int r = clamp255((p >> 16 & 255) + k * (g >> 16 & 255)), gr = clamp255((p >> 8 & 255) + k * (g >> 8 & 255)), bl = clamp255((p & 255) + k * (g & 255));
                        out[i] = (al << 24) | (r << 16) | (gr << 8) | bl;
                    }
                }
                s = new StickerGfx.Sprite(Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888), PPU, base.x0, base.y0);
            }
        } catch (RuntimeException | OutOfMemoryError e) {
            android.util.Log.e("StickerLib", "fill " + d.id, e);
            return base;
        }
        filled.put(key, s);
        return s;
    }

    private static Bitmap renderBody(String body) {
        int px = Math.round(180f * PPU);
        String doc = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"-90 -90 180 180\" width=\"" + px + "\" height=\"" + px + "\">" + body + "</svg>";
        try {
            SVG svg = SVG.getFromString(doc);
            Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            svg.renderToCanvas(new Canvas(b));
            return b;
        } catch (SVGParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static Bitmap crop(Bitmap full, int left, int top, int w, int h) {
        left = Math.max(0, Math.min(left, full.getWidth() - 1));
        top = Math.max(0, Math.min(top, full.getHeight() - 1));
        return Bitmap.createBitmap(full, left, top, Math.min(w, full.getWidth() - left), Math.min(h, full.getHeight() - top));
    }

    /** The item's SVG with its main colour replaced (and, on the shapes that use it, the matching darker edge colour). */
    private static String recolor(Def d, String fillHex, String edgeHex) {
        java.util.regex.Matcher tags = java.util.regex.Pattern.compile("<[a-zA-Z][^>]*>").matcher(d.svg);
        StringBuffer out = new StringBuffer();
        while (tags.find()) {
            String tag = tags.group();
            String pat = d.attr + "=\"" + d.primary + "\"";
            if (tag.toLowerCase(java.util.Locale.US).contains(pat)) {
                tag = tag.replaceAll("(?i)" + d.attr + "=\"" + d.primary + "\"", d.attr + "=\"" + fillHex + "\"");
                if (d.attr.equals("fill")) {   // a coloured edge goes with the colour; the brown outlines stay
                    java.util.regex.Matcher st = java.util.regex.Pattern.compile("stroke=\"(#[0-9a-fA-F]{6})\"").matcher(tag);
                    if (st.find() && !st.group(1).equalsIgnoreCase("#5b3a29") && !st.group(1).equalsIgnoreCase("#ffffff")) {
                        tag = tag.substring(0, st.start()) + "stroke=\"" + edgeHex + "\"" + tag.substring(st.end());
                    }
                }
            }
            tags.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(tag));
        }
        tags.appendTail(out);
        return out.toString();
    }

    private static String hex(int c) {
        return String.format(java.util.Locale.US, "#%06x", c & 0xFFFFFF);
    }

    private static int dark(int c) {
        return 0xFF000000 | clamp255((c >> 16 & 255) * .74f) << 16 | clamp255((c >> 8 & 255) * .74f) << 8 | clamp255((c & 255) * .74f);
    }

    private static int mix(int a, int b, float t) {
        return 0xFF000000 | clamp255((a >> 16 & 255) + t * ((b >> 16 & 255) - (a >> 16 & 255))) << 16
                | clamp255((a >> 8 & 255) + t * ((b >> 8 & 255) - (a >> 8 & 255))) << 8 | clamp255((a & 255) + t * ((b & 255) - (a & 255)));
    }

    private static int clamp255(float v) {
        return v < 0 ? 0 : v > 255 ? 255 : Math.round(v);
    }

    /** A small picture of an item for the tray. */
    static Bitmap icon(Context c, Def d, int px) {
        StickerGfx.Sprite s = sprite(c, d);
        if (s == null) return null;
        float k = (px * 0.82f) / Math.max(s.bmp.getWidth(), s.bmp.getHeight());
        int w = Math.max(1, Math.round(s.bmp.getWidth() * k)), h = Math.max(1, Math.round(s.bmp.getHeight() * k));
        Bitmap out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
        cv.drawBitmap(Bitmap.createScaledBitmap(s.bmp, w, h, true), (px - w) / 2f, (px - h) / 2f, p);
        return out;
    }
}
