package com.plainphone.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Bitmap helpers for the sticker editor: sprites (an item's picture and where it sits), trimming, emoji,
 * and the sticker border, an exact round grow of any shape (items, text, emoji and the photo alike).
 */
final class StickerGfx {

    private StickerGfx() {}

    /** An item's picture: {@code bmp} drawn into the box (x0, y0, x0 + w, y0 + h) given in the item's own units. */
    static final class Sprite {
        final Bitmap bmp;
        final float ppu;      // bitmap pixels per unit
        final float x0, y0;   // top-left, in units

        Sprite(Bitmap bmp, float ppu, float x0, float y0) {
            this.bmp = bmp;
            this.ppu = ppu;
            this.x0 = x0;
            this.y0 = y0;
        }

        float w() {
            return bmp.getWidth() / ppu;
        }

        float h() {
            return bmp.getHeight() / ppu;
        }
    }

    /** A border bitmap and the box (in units) it is drawn into. */
    static final class Halo {
        final Bitmap bmp;
        final float x0, y0, w, h;

        Halo(Bitmap bmp, float x0, float y0, float w, float h) {
            this.bmp = bmp;
            this.x0 = x0;
            this.y0 = y0;
            this.w = w;
            this.h = h;
        }
    }

    /** Crop {@code src} to the pixels that are not (nearly) transparent. Null when nothing is drawn. */
    static Sprite trim(Bitmap src, float ppu, float x0Units, float y0Units) {
        int w = src.getWidth(), h = src.getHeight();
        int[] row = new int[w];
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            src.getPixels(row, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x++) {
                if ((row[x] >>> 24) > 8) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    maxY = y;
                }
            }
        }
        if (maxX < 0) return null;
        Bitmap out = Bitmap.createBitmap(src, minX, minY, maxX - minX + 1, maxY - minY + 1);
        return new Sprite(out, ppu, x0Units + minX / ppu, y0Units + minY / ppu);
    }

    /** An emoji as a sprite about 46 units wide, centred on the origin. Null if this system cannot draw it. */
    static Sprite emoji(String ch) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(200);
        p.setTextAlign(Paint.Align.CENTER);
        if (!p.hasGlyph(ch)) return null;
        Bitmap b = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint.FontMetrics fm = p.getFontMetrics();
        c.drawText(ch, 160, 160 - (fm.ascent + fm.descent) / 2f, p);
        Sprite raw = trim(b, 1f, 0, 0);
        if (raw == null) return null;
        float ppu = Math.max(raw.bmp.getWidth(), raw.bmp.getHeight()) / 46f;
        return new Sprite(raw.bmp, ppu, -raw.bmp.getWidth() / 2f / ppu, -raw.bmp.getHeight() / 2f / ppu);
    }

    // ------------------------------------------------------------------------------------------ the border

    /**
     * The border of a sprite: every pixel within {@code rUnits} of the shape, painted {@code color} with a
     * clean anti-aliased edge (a distance transform, so it is exactly round, even at sharp tips). With
     * {@code glow} a soft glow fades out beyond the border (the neon look).
     */
    static Halo halo(Sprite sp, float rUnits, int color, boolean glow) {
        if (rUnits <= 0.05f) return null;
        // work at a modest resolution so dragging the width slider stays smooth
        float k = Math.min(1f, 320f / Math.max(sp.bmp.getWidth(), sp.bmp.getHeight()));
        int sw = Math.max(1, Math.round(sp.bmp.getWidth() * k)), sh = Math.max(1, Math.round(sp.bmp.getHeight() * k));
        Bitmap small = k < 1f ? Bitmap.createScaledBitmap(sp.bmp, sw, sh, true) : sp.bmp;
        float ppu = sp.ppu * k, rPx = rUnits * ppu, gPx = glow ? 5f * ppu : 0f;
        int pad = (int) Math.ceil(rPx + gPx) + 2;
        int W = sw + 2 * pad, H = sh + 2 * pad;

        double[] f = new double[W * H];
        java.util.Arrays.fill(f, INF);
        int[] row = new int[sw];
        boolean any = false;
        for (int y = 0; y < sh; y++) {
            small.getPixels(row, 0, sw, 0, y, sw, 1);
            for (int x = 0; x < sw; x++) {
                if ((row[x] >>> 24) >= 128) { f[(y + pad) * W + (x + pad)] = 0; any = true; }
            }
        }
        if (small != sp.bmp) small.recycle();
        if (!any) return null;
        edt(f, W, H);

        int rgb = color & 0xFFFFFF;
        int[] out = new int[W * H];
        for (int i = 0; i < out.length; i++) {
            double d = Math.sqrt(f[i]);
            double a = rPx - d + 0.5;
            a = a < 0 ? 0 : Math.min(a, 1);
            if (glow && d > rPx) {
                double g = 1 - (d - rPx) / gPx;
                if (g > 0) a = Math.max(a, g * g * 0.8);
            }
            int ai = (int) Math.round(a * 255);
            out[i] = ai == 0 ? 0 : (ai << 24) | rgb;
        }
        Bitmap bmp = Bitmap.createBitmap(out, W, H, Bitmap.Config.ARGB_8888);
        return new Halo(bmp, sp.x0 - pad / ppu, sp.y0 - pad / ppu, W / ppu, H / ppu);
    }

    // exact squared Euclidean distance transform (Felzenszwalb & Huttenlocher)
    private static final double INF = 1e15;

    private static void edt(double[] f, int w, int h) {
        int n = Math.max(w, h);
        double[] d = new double[n], o = new double[n], z = new double[n + 1];
        int[] v = new int[n];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) d[y] = f[y * w + x];
            dt1(d, o, h, v, z);
            for (int y = 0; y < h; y++) f[y * w + x] = o[y];
        }
        for (int y = 0; y < h; y++) {
            int off = y * w;
            for (int x = 0; x < w; x++) d[x] = f[off + x];
            dt1(d, o, w, v, z);
            for (int x = 0; x < w; x++) f[off + x] = o[x];
        }
    }

    /** One-dimensional squared distance transform of {@code f[0..n)} into {@code o}. */
    private static void dt1(double[] f, double[] o, int n, int[] v, double[] z) {
        int k = 0;
        v[0] = 0;
        z[0] = -INF;
        z[1] = INF;
        for (int q = 1; q < n; q++) {
            double s;
            while (true) {
                int p = v[k];
                s = ((f[q] + (double) q * q) - (f[p] + (double) p * p)) / (2.0 * q - 2.0 * p);
                if (s <= z[k] && k > 0) k--; else break;
            }
            k++;
            v[k] = q;
            z[k] = s;
            z[k + 1] = INF;
        }
        k = 0;
        for (int q = 0; q < n; q++) {
            while (z[k + 1] < q) k++;
            double dq = q - v[k];
            o[q] = dq * dq + f[v[k]];
        }
    }
}
