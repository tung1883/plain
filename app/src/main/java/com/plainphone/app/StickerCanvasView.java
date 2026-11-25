package com.plainphone.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * The sticker editor's canvas: a photo in a rounded frame with items on top (drawn items, emoji and
 * text). Touch edits everything: drag to move (snapping to guides), a corner handle to resize and
 * rotate (or pinch and twist), an arc handle to bend text, drop a text on an item to attach it.
 * Every item has its own border (style, colour, width); the picture has one too. The scene is
 * laid out in a 320 x 320 unit square, like the design mockup.
 */
final class StickerCanvasView extends View {

    static final float W = 320f, EDGE = 4f, THR = 7f, MARGIN = 16f, HIT = 20f;
    static final int[] COLORS = {0xFFFF5C9A, 0xFFFFFFFF, 0xFFFFC53D, 0xFF5CC8FF, 0xFFA78BFA, 0xFF3A3A3A, 0xFF5B3A29};
    // the photo frame: a wide margin is left around it so items can hang out past the picture
    static final float FR_X = 60f, FR_Y = 60f, FR_W = 200f, FR_H = 200f, FR_RX = 26f;
    static final int K_ART = 0, K_TEXT = 1, K_EMOJI = 2;
    private static final int M_NONE = 0, M_MOVE = 1, M_SCALE = 2, M_BEND = 3, M_PINCH = 4;
    private static final float TEXT_PPU = 4f, FONT = 34f;

    interface Listener {
        /** The selection changed; {@code it} is null when nothing is selected. */
        void onSelect(Item it);
    }

    /** A border: style white / color / neon / none, a colour (for color and neon) and a width in the item's units. */
    static final class Border {
        String style = "white";
        int color = COLORS[0];
        float width = 3f;
    }

    static final class Item {
        int kind;
        StickerLib.Def def;
        final StickerLib.Fill fill = new StickerLib.Fill();   // the item's colours (drawn items only)
        StickerGfx.Sprite sprite;
        StickerGfx.Halo halo;
        float x, y, rot, s = 1f;
        final Border border = new Border();
        Item parent;                 // a text attached to an item follows it
        float rx, ry, rr, rs;        // where it sits in its parent's frame
        // text
        String text = "";
        int font, color;
        float bend;
    }

    /** The picture's own border: style white / color / neon / dotted / none. */
    static final class PicBorder {
        String style = "white";
        int color = COLORS[0];
        float width = 6f;
    }

    private final List<Item> items = new ArrayList<>();
    private final PicBorder pic = new PicBorder();
    private Listener listener;
    private Item sel;
    private boolean locked;

    private Bitmap photo, cut;
    private StickerGfx.Sprite picSprite;
    private StickerGfx.Halo picHalo;

    // touch
    private int mode = M_NONE;
    private final List<Integer> ptrIds = new ArrayList<>();
    private final android.util.SparseArray<float[]> ptrs = new android.util.SparseArray<>();
    private float offX, offY, st0, st1, st2, st3, st4;   // gesture start values
    private long lastBendTap;
    private Item dropTarget;
    private final List<float[]> guides = new ArrayList<>();   // active snap lines: {0 vertical | 1 horizontal, value}
    private boolean dragging;

    private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint ov = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dst = new RectF();

    StickerCanvasView(Context c) {
        super(c);
        setClickable(true);
    }

    void setListener(Listener l) {
        listener = l;
    }

    PicBorder pic() {
        return pic;
    }

    Item selected() {
        return sel;
    }

    List<Item> items() {
        return items;
    }

    /** A locked canvas shows the picture only (an animated GIF is saved as it is). */
    void setLocked(boolean on) {
        locked = on;
        invalidate();
    }

    // ============================================================================================ the picture

    /** Start over with a new picture (items cleared); {@code lock} shows it without editing (an animated GIF). */
    void reset(Bitmap b, boolean lock) {
        items.clear();
        sel = null;
        guides.clear();
        dropTarget = null;
        mode = M_NONE;
        dragging = false;
        locked = lock;
        setPhoto(b);
        if (listener != null) listener.onSelect(null);
    }

    void setPhoto(Bitmap b) {
        photo = b;
        cut = null;
        rebuildPicture();
    }

    void setCut(Bitmap b) {
        cut = b;
        rebuildPicture();
    }

    boolean hasCut() {
        return cut != null;
    }

    private void rebuildPicture() {
        picSprite = null;
        if (photo != null) {
            float ppu = 3f;
            int px = Math.round(FR_W * ppu);
            Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            if (cut != null) {   // the subject on a transparent background, fitted inside the frame
                float k = Math.min((float) px / cut.getWidth(), (float) px / cut.getHeight());
                float w = cut.getWidth() * k, h = cut.getHeight() * k;
                c.drawBitmap(cut, null, new RectF((px - w) / 2f, (px - h) / 2f, (px + w) / 2f, (px + h) / 2f), bmpPaint);
                picSprite = StickerGfx.trim(b, ppu, FR_X, FR_Y);
            } else {            // the whole photo, centre-cropped to the rounded frame
                Path clip = new Path();
                clip.addRoundRect(new RectF(0, 0, px, px), FR_RX * ppu, FR_RX * ppu, Path.Direction.CW);
                c.clipPath(clip);
                float k = Math.max((float) px / photo.getWidth(), (float) px / photo.getHeight());
                float w = photo.getWidth() * k, h = photo.getHeight() * k;
                c.drawBitmap(photo, null, new RectF((px - w) / 2f, (px - h) / 2f, (px + w) / 2f, (px + h) / 2f), bmpPaint);
                picSprite = new StickerGfx.Sprite(b, ppu, FR_X, FR_Y);
            }
        }
        rebuildPicHalo();
    }

    /** Call after changing {@link #pic()}. */
    void picChanged() {
        rebuildPicHalo();
    }

    private void rebuildPicHalo() {
        picHalo = null;
        if (picSprite != null) {
            String st = pic.style;
            if (st.equals("white")) picHalo = StickerGfx.halo(picSprite, pic.width, 0xFFFFFFFF, false);
            else if (st.equals("color")) picHalo = StickerGfx.halo(picSprite, pic.width, pic.color, false);
            else if (st.equals("neon")) picHalo = StickerGfx.halo(picSprite, pic.width, pic.color, true);
        }
        invalidate();
    }

    // ============================================================================================ items

    Item addArt(StickerLib.Def d, float x, float y) {
        StickerGfx.Sprite sp = StickerLib.sprite(getContext(), d);
        if (sp == null) return null;
        Item it = new Item();
        it.kind = K_ART;
        it.def = d;
        it.sprite = sp;
        it.x = x;
        it.y = y;
        it.s = d.scale;
        return finishAdd(it);
    }

    Item addEmoji(String ch, float x, float y) {
        StickerGfx.Sprite sp = StickerGfx.emoji(ch);
        if (sp == null) return null;
        Item it = new Item();
        it.kind = K_EMOJI;
        it.sprite = sp;
        it.x = x;
        it.y = y;
        it.s = 1.6f;
        return finishAdd(it);
    }

    Item addText(String text, int font, int color, float x, float y, float scale, float bend) {
        Item it = new Item();
        it.kind = K_TEXT;
        it.text = text;
        it.font = font;
        it.color = color;
        it.bend = bend;
        it.x = x;
        it.y = y;
        it.s = scale;
        it.border.width = 5f;
        renderText(it);
        if (it.sprite == null) return null;
        items.add(it);
        fitInside(it);
        select(it);
        invalidate();
        return it;
    }

    private Item finishAdd(Item it) {
        items.add(it);
        applyBorder(it);
        select(it);
        return it;
    }

    void remove(Item it) {
        for (Item c : items) if (c.parent == it) c.parent = null;
        items.remove(it);
        if (sel == it) select(null);
        invalidate();
    }

    void select(Item it) {
        sel = it;
        if (listener != null) listener.onSelect(it);
        invalidate();
    }

    /** Attach a text to the item it sits on (used by the combos). */
    void attach(Item text) {
        Item t = findTarget(text);
        if (t != null) {
            text.parent = t;
            setRel(text);
        }
        invalidate();
    }

    /** Recolour a drawn item after its fill changed. */
    void setFill(Item it) {
        if (it.kind != K_ART) return;
        StickerGfx.Sprite sp = StickerLib.sprite(getContext(), it.def, it.fill);
        if (sp != null) it.sprite = sp;
        applyBorder(it);
    }

    // ---- text

    void setText(Item it, String text) {
        it.text = text;
        renderText(it);
        invalidate();
    }

    void setFont(Item it, int font) {
        it.font = font;
        renderText(it);
        invalidate();
    }

    void setTextColor(Item it, int color) {
        it.color = color;
        renderText(it);
        invalidate();
    }

    private void renderText(Item it) {
        float ppu = TEXT_PPU;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setTypeface(Fonts.caption(getContext(), it.font));
        int wt = Fonts.captionWeight(it.font);
        if (wt > 0) p.setFontVariationSettings("'wght' " + wt);
        p.setColor(COLORS[Math.max(0, Math.min(it.color, COLORS.length - 1))]);
        String text = it.text == null || it.text.isEmpty() ? " " : it.text;
        p.setTextSize(FONT * ppu);
        float tw = p.measureText(text), maxW = 500f * ppu;
        if (tw > maxW) {   // never longer than the arc: shrink to fit
            p.setTextSize(FONT * ppu * maxW / tw);
            tw = maxW;
        }
        float sag = it.bend / 100f * 150f * ppu;
        Path path = arcPath(260f * ppu, sag);
        PathMeasure pm = new PathMeasure(path, false);
        float len = pm.getLength(), start = (len - tw) / 2f;
        float minX = 1e9f, minY = 1e9f, maxX = -1e9f, maxY = -1e9f;
        float[] pos = new float[2];
        for (int i = 0; i <= 8; i++) {
            pm.getPosTan(Math.max(0, Math.min(len, start + tw * i / 8f)), pos, null);
            minX = Math.min(minX, pos[0]);
            maxX = Math.max(maxX, pos[0]);
            minY = Math.min(minY, pos[1]);
            maxY = Math.max(maxY, pos[1]);
        }
        float pad = p.getTextSize() * 1.5f;
        int bw = Math.max(8, (int) Math.ceil(maxX - minX + 2 * pad)), bh = Math.max(8, (int) Math.ceil(maxY - minY + 2 * pad));
        Bitmap b = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate(pad - minX, pad - minY);
        c.drawTextOnPath(text, path, start, 0, p);
        it.sprite = StickerGfx.trim(b, ppu, (minX - pad) / ppu, (minY - pad) / ppu);
        if (it.sprite == null) {   // a blank text: keep a tiny invisible sprite so it can still be selected
            it.sprite = new StickerGfx.Sprite(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888), ppu, -2f / ppu, -2f / ppu);
        }
        applyBorder(it);
    }

    /** An arc through (+-half, sag) whose middle (apex) is at the origin; flat when sag is 0. */
    private static Path arcPath(float half, float sag) {
        Path path = new Path();
        if (Math.abs(sag) < 0.5f) {
            path.moveTo(-half, 0);
            path.lineTo(half, 0);
            return path;
        }
        float r = (half * half + sag * sag) / (2f * Math.abs(sag)), cy = Math.signum(sag) * r;
        float a0 = (float) Math.toDegrees(Math.atan2(sag - cy, -half)), a1 = (float) Math.toDegrees(Math.atan2(sag - cy, half));
        path.arcTo(new RectF(-r, cy - r, r, cy + r), a0, a1 - a0, true);
        return path;
    }

    // ---- borders

    /** Rebuild an item's border after its style, colour or width changed. */
    void applyBorder(Item it) {
        boolean none = "none".equals(it.border.style);
        it.halo = none ? null : StickerGfx.halo(it.sprite, it.border.width, haloColor(it), "neon".equals(it.border.style));
        fitInside(it);
        invalidate();
    }

    private int haloColor(Item it) {
        Border b = it.border;
        boolean coloured = b.style.equals("color") || b.style.equals("neon");
        if (it.kind == K_TEXT) return coloured ? b.color : (COLORS[it.color] == 0xFFFFFFFF ? 0xFF3A3A3A : 0xFFFFFFFF);
        return coloured ? b.color : 0xFFFFFFFF;
    }

    // ============================================================================================ geometry

    private float[] toCanvas(Item it, float lx, float ly) {
        double a = Math.toRadians(it.rot);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new float[]{it.x + it.s * (c * lx - s * ly), it.y + it.s * (s * lx + c * ly)};
    }

    private float[] toLocal(Item it, float px, float py) {
        double a = Math.toRadians(it.rot);
        float c = (float) Math.cos(a), s = (float) Math.sin(a), dx = px - it.x, dy = py - it.y;
        return new float[]{(c * dx + s * dy) / it.s, (-s * dx + c * dy) / it.s};
    }

    /** How far the selection box reaches past the shape: the border plus 4 canvas units of air. */
    private float padLocal(Item it) {
        float r = "none".equals(it.border.style) ? 0f : it.border.width;
        return r + 4f / it.s;
    }

    private float[] lbox(Item it) {
        StickerGfx.Sprite sp = it.sprite;
        float p = padLocal(it);
        return new float[]{sp.x0 - p, sp.y0 - p, sp.x0 + sp.w() + p, sp.y0 + sp.h() + p};
    }

    private float[][] corners(Item it) {
        float[] b = lbox(it);
        return new float[][]{toCanvas(it, b[0], b[1]), toCanvas(it, b[2], b[1]), toCanvas(it, b[2], b[3]), toCanvas(it, b[0], b[3])};
    }

    private float[] aabb(Item it) {
        float[][] c = corners(it);
        float x0 = 1e9f, y0 = 1e9f, x1 = -1e9f, y1 = -1e9f;
        for (float[] p : c) {
            x0 = Math.min(x0, p[0]);
            x1 = Math.max(x1, p[0]);
            y0 = Math.min(y0, p[1]);
            y1 = Math.max(y1, p[1]);
        }
        return new float[]{x0, y0, x1, y1};
    }

    float[] centerCanvas(Item it) {
        float[] b = lbox(it);
        return toCanvas(it, (b[0] + b[2]) / 2f, (b[1] + b[3]) / 2f);
    }

    /** Change scale and rotation but keep the item's visual centre where it is. */
    private void setScaleRot(Item it, float ns, float nr) {
        float[] b = lbox(it), cl = {(b[0] + b[2]) / 2f, (b[1] + b[3]) / 2f};
        float[] c0 = toCanvas(it, cl[0], cl[1]);
        it.s = Math.max(.3f, Math.min(6f, ns));
        it.rot = nr;
        float[] c1 = toCanvas(it, cl[0], cl[1]);
        it.x += c0[0] - c1[0];
        it.y += c0[1] - c1[1];
        moved(it, false);
    }

    // ---- texts attached to an item follow it

    private void setRel(Item c) {
        Item q = c.parent;
        float[] l = toLocal(q, c.x, c.y);
        c.rx = l[0];
        c.ry = l[1];
        c.rr = c.rot - q.rot;
        c.rs = c.s / q.s;
    }

    /** An item changed place: remember where it sits on its item, and carry everything attached to it. */
    private void moved(Item it, boolean fromParent) {
        if (it.parent != null && !fromParent) setRel(it);
        for (Item c : items) {
            if (c.parent == it) {
                float[] p = toCanvas(it, c.rx, c.ry);
                c.x = p[0];
                c.y = p[1];
                c.rot = it.rot + c.rr;
                c.s = it.s * c.rs;
                moved(c, true);
            }
        }
    }

    /** The item a text sits on: the top-most drawn item or emoji whose box holds the text's centre. */
    private Item findTarget(Item text) {
        float[] ctr = centerCanvas(text);
        for (int i = items.size() - 1; i >= 0; i--) {
            Item it = items.get(i);
            if (it == text || it.kind == K_TEXT) continue;
            float[] l = toLocal(it, ctr[0], ctr[1]), b = lbox(it);
            if (l[0] >= b[0] && l[0] <= b[2] && l[1] >= b[1] && l[1] <= b[3]) return it;
        }
        return null;
    }

    // ---- keep an item (and the texts attached to it) fully inside the canvas, border included

    private float[] groupBox(Item it) {
        float[] g = aabb(it);
        for (Item c : items) {
            if (c.parent == it) {
                float[] b = aabb(c);
                g[0] = Math.min(g[0], b[0]);
                g[1] = Math.min(g[1], b[1]);
                g[2] = Math.max(g[2], b[2]);
                g[3] = Math.max(g[3], b[3]);
            }
        }
        return g;
    }

    private void fitInside(Item it) {
        for (int k = 0; k < 3; k++) {
            float[] b = groupBox(it);
            float w = b[2] - b[0], h = b[3] - b[1], lim = W - 2 * EDGE;
            if (w > lim + .01f || h > lim + .01f) {   // too big for the canvas: shrink
                setScaleRot(it, it.s * Math.min(lim / w, lim / h), it.rot);
                b = groupBox(it);
            }
            float dx = b[0] < EDGE ? EDGE - b[0] : b[2] > W - EDGE ? W - EDGE - b[2] : 0f;
            float dy = b[1] < EDGE ? EDGE - b[1] : b[3] > W - EDGE ? W - EDGE - b[3] : 0f;
            if (dx == 0f && dy == 0f) break;
            it.x += dx;
            it.y += dy;
            moved(it, false);
        }
    }

    // ---- snapping

    /** One axis of snapping: returns the (possibly adjusted) offset and the line it caught in {@code line[0]} (NaN if none). */
    private float snapAxis(float lo, float hi, float d, float itemC, float[] line) {
        float c = (lo + hi) / 2f + d, l = lo + d, r = hi + d;
        float[][] cand = {
                {W / 2f - c, W / 2f, 1f}, {W / 3f - c, W / 3f, 1.35f}, {2f * W / 3f - c, 2f * W / 3f, 1.35f},
                {MARGIN - l, MARGIN, 1.2f}, {W - MARGIN - r, W - MARGIN, 1.2f},
                {Float.isNaN(itemC) ? 1e9f : itemC - c, itemC, .9f}};
        float[] best = null;
        for (float[] k : cand) {
            if (Math.abs(k[0]) <= THR && (best == null || Math.abs(k[0]) * k[2] < Math.abs(best[0]) * best[2])) best = k;
        }
        if (best == null) {
            line[0] = Float.NaN;
            return d;
        }
        line[0] = best[1];
        return d + best[0];
    }

    // ============================================================================================ drawing

    @Override
    protected void onMeasure(int w, int h) {
        // Square, full width when there is room. When the window gets short (the keyboard is up) it shrinks
        // so the panel and the Save button below it stay visible: leave ~300dp for them, but never go below 170dp.
        float d = getResources().getDisplayMetrics().density;
        int left = MeasureSpec.getMode(h) == MeasureSpec.UNSPECIFIED ? Integer.MAX_VALUE : MeasureSpec.getSize(h);
        int room = left == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max((int) (170 * d), left - (int) (300 * d));
        int size = Math.min(MeasureSpec.getSize(w), room);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float u = getWidth() / W;
        canvas.save();
        canvas.scale(u, u);
        drawChecker(canvas);
        drawScene(canvas);
        if (!locked) drawOverlay(canvas);
        canvas.restore();
    }

    private void drawChecker(Canvas c) {
        ov.setStyle(Paint.Style.FILL);
        ov.setColor(0xFF0D0D0D);
        c.drawRect(0, 0, W, W, ov);
        ov.setColor(0xFF181818);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 40; x++) {
                if (((x + y) & 1) == 0) c.drawRect(x * 8, y * 8, x * 8 + 8, y * 8 + 8, ov);
            }
        }
    }

    /** The picture, then every item (texts above the drawn items), the same way for the screen and for saving. */
    private void drawScene(Canvas c) {
        if (picSprite != null) {
            if (picHalo != null) drawHalo(c, picHalo);
            drawSprite(c, picSprite);
            if (pic.style.equals("dotted")) drawDots(c);
        }
        for (Item it : items) if (it.kind != K_TEXT) drawItem(c, it);
        for (Item it : items) if (it.kind == K_TEXT) drawItem(c, it);
    }

    private void drawHalo(Canvas c, StickerGfx.Halo h) {
        dst.set(h.x0, h.y0, h.x0 + h.w, h.y0 + h.h);
        c.drawBitmap(h.bmp, null, dst, bmpPaint);
    }

    private void drawSprite(Canvas c, StickerGfx.Sprite s) {
        dst.set(s.x0, s.y0, s.x0 + s.w(), s.y0 + s.h());
        c.drawBitmap(s.bmp, null, dst, bmpPaint);
    }

    private void drawItem(Canvas c, Item it) {
        c.save();
        c.translate(it.x, it.y);
        c.rotate(it.rot);
        c.scale(it.s, it.s);
        if (it.halo != null) drawHalo(c, it.halo);
        drawSprite(c, it.sprite);
        c.restore();
    }

    /** The dotted picture border: evenly spaced round dots (diameter = width) around the picture. */
    private void drawDots(Canvas c) {
        float d = pic.width, off = d * .9f;
        StickerGfx.Sprite s = picSprite;
        float rx = cut != null ? Math.min(20f, Math.min(s.w(), s.h()) / 4f) : FR_RX;
        RectF r = new RectF(s.x0 - off, s.y0 - off, s.x0 + s.w() + off, s.y0 + s.h() + off);
        Path p = new Path();
        p.addRoundRect(r, rx + off, rx + off, Path.Direction.CW);
        PathMeasure pm = new PathMeasure(p, true);
        float len = pm.getLength();
        int n = Math.max(8, Math.round(len / (d * 2.3f)));
        ov.setStyle(Paint.Style.FILL);
        ov.setColor(pic.color);
        float[] pos = new float[2];
        for (int i = 0; i < n; i++) {
            pm.getPosTan(len * i / n, pos, null);
            c.drawCircle(pos[0], pos[1], d / 2f, ov);
        }
    }

    // ---- the editing overlay (never saved)

    private float[][] handlePoints(Item it) {
        float[][] c = corners(it);
        float[] b = lbox(it);
        float[] bend = it.kind == K_TEXT ? toCanvas(it, (b[0] + b[2]) / 2f, b[3] + 17f / it.s) : null;
        // a handle on a box corner at the canvas edge would be half outside the canvas: keep each one fully inside, where it can be grabbed
        float lo = 13f, hi = W - 13f;
        float[] del = {Math.max(lo, Math.min(hi, c[0][0])), Math.max(lo, Math.min(hi, c[0][1]))};
        float[] scl = {Math.max(lo, Math.min(hi, c[2][0])), Math.max(lo, Math.min(hi, c[2][1]))};
        float[] bnd = bend == null ? null : new float[]{Math.max(lo, Math.min(hi, bend[0])), Math.max(lo, Math.min(hi, bend[1]))};
        return new float[][]{del, scl, bnd};   // delete, scale, bend
    }

    private void polygon(Canvas c, float[][] p, Paint paint) {
        Path path = new Path();
        path.moveTo(p[0][0], p[0][1]);
        for (int i = 1; i < p.length; i++) path.lineTo(p[i][0], p[i][1]);
        path.close();
        c.drawPath(path, paint);
    }

    private void drawOverlay(Canvas c) {
        // guides while dragging
        if (dragging) {
            ov.setStyle(Paint.Style.STROKE);
            ov.setStrokeWidth(1f);
            ov.setPathEffect(new DashPathEffect(new float[]{3f, 4f}, 0));
            ov.setColor(0x73FFFFFF);
            c.drawLine(W / 2f, 0, W / 2f, W, ov);
            c.drawLine(0, W / 2f, W, W / 2f, ov);
            ov.setStrokeWidth(1.6f);
            ov.setColor(0xFFFF5C9A);
            for (float[] g : guides) {
                if (g[0] == 0) c.drawLine(g[1], 0, g[1], W, ov);
                else c.drawLine(0, g[1], W, g[1], ov);
            }
            ov.setPathEffect(null);
        }
        if (sel == null) return;
        // what is linked to the selection
        ov.setStyle(Paint.Style.STROKE);
        ov.setStrokeWidth(1.4f);
        ov.setColor(0xF2FF5C9A);
        ov.setPathEffect(new DashPathEffect(new float[]{3f, 3f}, 0));
        if (sel.parent != null) polygon(c, corners(sel.parent), ov);
        else for (Item ch : items) if (ch.parent == sel) polygon(c, corners(ch), ov);
        // the dotted box on the item a dragged text would attach to
        if (dropTarget != null) {
            ov.setStyle(Paint.Style.FILL);
            ov.setColor(0x1AFF5C9A);
            polygon(c, corners(dropTarget), ov);
            ov.setStyle(Paint.Style.STROKE);
            ov.setStrokeWidth(2f);
            ov.setStrokeCap(Paint.Cap.ROUND);
            ov.setColor(0xFFFF5C9A);
            ov.setPathEffect(new DashPathEffect(new float[]{2f, 4f}, 0));
            polygon(c, corners(dropTarget), ov);
            ov.setStrokeCap(Paint.Cap.BUTT);
        }
        // the selection box
        ov.setStrokeWidth(1.2f);
        ov.setColor(0xE6FFFFFF);
        ov.setPathEffect(new DashPathEffect(new float[]{4f, 3f}, 0));
        polygon(c, corners(sel), ov);
        ov.setPathEffect(null);
        // handles
        float[][] hp = handlePoints(sel);
        handle(c, hp[0], 0);
        handle(c, hp[1], 1);
        if (hp[2] != null) handle(c, hp[2], 2);
    }

    /** A round white handle with a small icon: 0 delete, 1 resize + rotate, 2 bend. */
    private void handle(Canvas c, float[] p, int icon) {
        ov.setPathEffect(null);
        ov.setStyle(Paint.Style.FILL);
        ov.setColor(0xFFFFFFFF);
        c.drawCircle(p[0], p[1], 11f, ov);
        ov.setStyle(Paint.Style.STROKE);
        ov.setStrokeWidth(1.5f);
        ov.setColor(0xFF000000);
        c.drawCircle(p[0], p[1], 11f, ov);
        ov.setStrokeWidth(1.9f);
        ov.setStrokeCap(Paint.Cap.ROUND);
        float x = p[0], y = p[1];
        if (icon == 0) {
            c.drawLine(x - 4, y - 4, x + 4, y + 4, ov);
            c.drawLine(x + 4, y - 4, x - 4, y + 4, ov);
        } else if (icon == 1) {
            c.drawLine(x - 4.5f, y + 4.5f, x + 4.5f, y - 4.5f, ov);
            c.drawLine(x + 4.5f, y - 4.5f, x + 1.5f, y - 4.5f, ov);
            c.drawLine(x + 4.5f, y - 4.5f, x + 4.5f, y - 1.5f, ov);
            c.drawLine(x - 4.5f, y + 4.5f, x - 1.5f, y + 4.5f, ov);
            c.drawLine(x - 4.5f, y + 4.5f, x - 4.5f, y + 1.5f, ov);
        } else {
            Path a = new Path();
            a.moveTo(x - 5.5f, y + 2.5f);
            a.quadTo(x, y - 6.5f, x + 5.5f, y + 2.5f);
            c.drawPath(a, ov);
        }
        ov.setStrokeCap(Paint.Cap.BUTT);
    }

    // ============================================================================================ touch

    private boolean near(float[] a, float x, float y) {
        return a != null && Math.hypot(a[0] - x, a[1] - y) <= HIT;
    }

    private Item hitItem(float x, float y) {
        for (int pass = 0; pass < 2; pass++) {   // texts first, then drawn items and emoji
            for (int i = items.size() - 1; i >= 0; i--) {
                Item it = items.get(i);
                if ((it.kind == K_TEXT) != (pass == 0)) continue;
                float[] l = toLocal(it, x, y), b = lbox(it);
                float pad = 5f / it.s;
                if (l[0] >= b[0] - pad && l[0] <= b[2] + pad && l[1] >= b[1] - pad && l[1] <= b[3] + pad) return it;
            }
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (locked) return false;
        float u = getWidth() / W;
        int act = e.getActionMasked();
        int pi = e.getActionIndex(), pid = e.getPointerId(pi);
        switch (act) {
            case MotionEvent.ACTION_DOWN:
                ptrIds.clear();
                ptrs.clear();
                ptrIds.add(pid);
                ptrs.put(pid, new float[]{e.getX(pi) / u, e.getY(pi) / u});
                onDown(e.getX(pi) / u, e.getY(pi) / u);
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                ptrIds.add(pid);
                ptrs.put(pid, new float[]{e.getX(pi) / u, e.getY(pi) / u});
                if (sel != null && ptrIds.size() == 2) startPinch();
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    float[] p = ptrs.get(e.getPointerId(i));
                    if (p != null) {
                        p[0] = e.getX(i) / u;
                        p[1] = e.getY(i) / u;
                    }
                }
                onMove(ptrs.get(ptrIds.isEmpty() ? -1 : ptrIds.get(0)));
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                endPointer(pid, act == MotionEvent.ACTION_CANCEL);
                break;
            default:
                break;
        }
        return true;
    }

    private void onDown(float x, float y) {
        Item it = sel;
        if (it != null) {
            float[][] hp = handlePoints(it);
            if (near(hp[0], x, y)) {   // delete
                remove(it);
                mode = M_NONE;
                return;
            }
            if (near(hp[2], x, y)) {   // bend (double-tap straightens)
                long now = System.currentTimeMillis();
                if (now - lastBendTap < 350) {
                    it.bend = 0;
                    renderText(it);
                    lastBendTap = 0;
                    invalidate();
                    return;
                }
                lastBendTap = now;
                mode = M_BEND;
                st0 = it.bend;
                st1 = toLocal(it, x, y)[1];
                return;
            }
            if (near(hp[1], x, y)) {   // resize + rotate
                float[] ctr = centerCanvas(it);
                mode = M_SCALE;
                st0 = ctr[0];
                st1 = ctr[1];
                st2 = (float) Math.hypot(x - ctr[0], y - ctr[1]);
                st3 = (float) Math.atan2(y - ctr[1], x - ctr[0]);
                st4 = it.s;
                offX = it.rot;
                return;
            }
        }
        Item hit = hitItem(x, y);
        if (hit != null) {
            if (sel != hit) select(hit);
            mode = M_MOVE;
            offX = x - hit.x;
            offY = y - hit.y;
            dragging = true;
        } else {
            select(null);
            mode = M_NONE;
        }
        invalidate();
    }

    private void startPinch() {
        float[] a = ptrs.get(ptrIds.get(0)), b = ptrs.get(ptrIds.get(1));
        if (a == null || b == null || sel == null) return;
        mode = M_PINCH;
        st0 = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
        st1 = (float) Math.atan2(b[1] - a[1], b[0] - a[0]);
        st2 = sel.s;
        st3 = sel.rot;
        dragging = false;
        guides.clear();
        dropTarget = null;
    }

    private void onMove(float[] p) {
        Item it = sel;
        if (it == null || mode == M_NONE) return;
        if (mode == M_PINCH) {
            if (ptrIds.size() < 2) return;
            float[] a = ptrs.get(ptrIds.get(0)), b = ptrs.get(ptrIds.get(1));
            if (a == null || b == null) return;
            float d = (float) Math.hypot(b[0] - a[0], b[1] - a[1]), ang = (float) Math.atan2(b[1] - a[1], b[0] - a[0]);
            setScaleRot(it, st2 * d / Math.max(8f, st0), st3 + (float) Math.toDegrees(ang - st1));
            fitInside(it);
            invalidate();
            return;
        }
        if (p == null) return;
        float x = p[0], y = p[1];
        if (mode == M_MOVE) {
            float nx = x - offX, ny = y - offY;
            guides.clear();
            float[] snapped = snapMove(it, nx, ny);
            it.x = snapped[0];
            it.y = snapped[1];
            moved(it, false);
            fitInside(it);
            dropTarget = it.kind == K_TEXT ? findTarget(it) : null;
            invalidate();
        } else if (mode == M_SCALE) {
            float d = (float) Math.hypot(x - st0, y - st1), a = (float) Math.atan2(y - st1, x - st0);
            float r = offX + (float) Math.toDegrees(a - st3);
            float q = Math.round(r / 45f) * 45f;
            if (Math.abs(r - q) < 4f) r = q;   // snap to 0 / 45 / 90 ...
            setScaleRot(it, st4 * d / Math.max(8f, st2), r);
            fitInside(it);
            invalidate();
        } else if (mode == M_BEND) {
            float ly = toLocal(it, x, y)[1];
            it.bend = Math.round(Math.max(-100f, Math.min(100f, st0 + (st1 - ly) * 100f / 70f)));
            renderText(it);
            fitInside(it);
            invalidate();
        }
    }

    /** Snap a proposed position of {@code it}; returns {x, y} and fills the active guide lines. */
    private float[] snapMove(Item it, float nx, float ny) {
        float[] b = aabb(it);
        float dx = nx - it.x, dy = ny - it.y;
        Item tgt = it.kind == K_TEXT ? findTarget(it) : null;
        float[] ic = tgt != null ? centerCanvas(tgt) : null;
        float[] line = new float[1];
        float rx = snapAxis(b[0], b[2], dx, ic != null ? ic[0] : Float.NaN, line);
        if (!Float.isNaN(line[0])) guides.add(new float[]{0, line[0]});
        float ry = snapAxis(b[1], b[3], dy, ic != null ? ic[1] : Float.NaN, line);
        if (!Float.isNaN(line[0])) guides.add(new float[]{1, line[0]});
        return new float[]{it.x + rx, it.y + ry};
    }

    private void endPointer(int pid, boolean cancel) {
        if (mode == M_MOVE && sel != null && sel.kind == K_TEXT && !cancel) finishDrop(sel);
        ptrs.remove(pid);
        ptrIds.remove(Integer.valueOf(pid));
        if (ptrIds.size() < 2 && mode == M_PINCH) mode = M_NONE;
        if (ptrIds.isEmpty()) {
            mode = M_NONE;
            dragging = false;
            guides.clear();
            dropTarget = null;
        }
        invalidate();
    }

    /** A text dropped on an item attaches to it by itself; dropped off every item it lets go. */
    private void finishDrop(Item text) {
        dropTarget = null;
        Item t = findTarget(text);
        if (t != null && text.parent != t) {
            text.parent = t;
            setRel(text);
        } else if (t == null && text.parent != null) {
            text.parent = null;
        }
    }

    // ============================================================================================ saving

    /**
     * The sticker as a transparent PNG bitmap: everything drawn (picture, items, borders) cropped to
     * what is there, no guides or handles, at most {@code maxSide} pixels on the long side.
     */
    Bitmap export(int maxSide) {
        float x0 = 1e9f, y0 = 1e9f, x1 = -1e9f, y1 = -1e9f;
        if (picSprite != null) {
            StickerGfx.Halo h = picHalo;
            float ex = pic.style.equals("dotted") ? pic.width * 1.9f : 0f;
            float a = (h != null ? h.x0 : picSprite.x0) - ex, b = (h != null ? h.y0 : picSprite.y0) - ex;
            float c = (h != null ? h.x0 + h.w : picSprite.x0 + picSprite.w()) + ex, d = (h != null ? h.y0 + h.h : picSprite.y0 + picSprite.h()) + ex;
            x0 = a;
            y0 = b;
            x1 = c;
            y1 = d;
        }
        for (Item it : items) {
            StickerGfx.Halo h = it.halo;
            float a = h != null ? h.x0 : it.sprite.x0, b = h != null ? h.y0 : it.sprite.y0;
            float c = h != null ? h.x0 + h.w : it.sprite.x0 + it.sprite.w(), d = h != null ? h.y0 + h.h : it.sprite.y0 + it.sprite.h();
            for (float[] p : new float[][]{toCanvas(it, a, b), toCanvas(it, c, b), toCanvas(it, c, d), toCanvas(it, a, d)}) {
                x0 = Math.min(x0, p[0]);
                x1 = Math.max(x1, p[0]);
                y0 = Math.min(y0, p[1]);
                y1 = Math.max(y1, p[1]);
            }
        }
        x0 = Math.max(0, x0 - 1);
        y0 = Math.max(0, y0 - 1);
        x1 = Math.min(W, x1 + 1);
        y1 = Math.min(W, y1 + 1);
        if (x1 <= x0 || y1 <= y0) return null;
        float sc = Math.min(maxSide / Math.max(x1 - x0, y1 - y0), 4f);
        Bitmap out = Bitmap.createBitmap(Math.max(1, Math.round((x1 - x0) * sc)), Math.max(1, Math.round((y1 - y0) * sc)), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.scale(sc, sc);
        c.translate(-x0, -y0);
        drawScene(c);
        return out;
    }
}
