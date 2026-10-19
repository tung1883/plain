package com.plainphone.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** The keyboard's clipboard history: the last copies (text or images), newest first, plus pinned
 * ones. Images are copied into the app's private files, scaled down. Local only. */
final class ClipboardHistory {

    static final int MAX_UNPINNED = 20;
    private static final String KEY = "clips";
    private static final int MAX_LEN = 5000;
    private static final int MAX_SIDE = 512;

    static final class Item {
        final String text;   // null for an image item
        final String image;  // absolute file path; null for a text item
        boolean pinned;

        Item(String text, String image, boolean pinned) {
            this.text = text;
            this.image = image;
            this.pinned = pinned;
        }

        boolean isImage() {
            return image != null;
        }

        boolean sameAs(Item other) {
            return isImage() ? image.equals(other.image) : other.text != null && text.equals(other.text);
        }
    }

    private ClipboardHistory() {}

    private static File imageDir(Context c) {
        File d = new File(c.getFilesDir(), "clip_images");
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    /** Pinned first, then newest first. */
    static List<Item> load(Context c) {
        List<Item> all = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(PanelKit.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                String text = o.has("t") ? o.getString("t") : null;
                String image = o.has("i") ? o.getString("i") : null;
                all.add(new Item(text, image, o.optBoolean("p")));
            }
        } catch (JSONException ignored) {
        }
        List<Item> out = new ArrayList<>();
        for (Item i : all) if (i.pinned) out.add(i);
        for (Item i : all) if (!i.pinned) out.add(i);
        return out;
    }

    private static void save(Context c, List<Item> items) {
        JSONArray a = new JSONArray();
        try {
            for (Item i : items) {
                JSONObject o = new JSONObject().put("p", i.pinned);
                if (i.isImage()) o.put("i", i.image);
                else o.put("t", i.text);
                a.put(o);
            }
        } catch (JSONException ignored) {
        }
        PanelKit.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    /** Record a copy: moves it to the front if already there, keeps its pin. Prunes old unpinned
     * entries past {@link #MAX_UNPINNED}, deleting any image file that falls off. */
    private static void record(Context c, Item fresh) {
        List<Item> items = load(c);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).sameAs(fresh)) {
                fresh.pinned = items.get(i).pinned;
                items.remove(i);
                break;
            }
        }
        items.add(0, fresh);
        int unpinned = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).pinned) continue;
            if (++unpinned > MAX_UNPINNED) {
                Item dropped = items.remove(i--);
                deleteFile(dropped);
            }
        }
        save(c, items);
    }

    /** Record a copied text clip. */
    static void add(Context c, CharSequence raw) {
        if (raw == null) return;
        String text = raw.toString();
        if (text.trim().isEmpty()) return;
        if (text.length() > MAX_LEN) text = text.substring(0, MAX_LEN);
        record(c, new Item(text, null, false));
    }

    /** Record a copied image clip: copies it in, scaled down to at most 512 px, as a PNG. */
    static void addImage(Context c, Uri source) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = c.getContentResolver().openInputStream(source)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return;
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bmp;
            try (InputStream in = c.getContentResolver().openInputStream(source)) {
                bmp = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bmp == null) return;
            float scale = Math.min(1f, (float) MAX_SIDE / Math.max(bmp.getWidth(), bmp.getHeight()));
            if (scale < 1f) {
                bmp = Bitmap.createScaledBitmap(bmp, Math.max(1, Math.round(bmp.getWidth() * scale)),
                        Math.max(1, Math.round(bmp.getHeight() * scale)), true);
            }
            File out = new File(imageDir(c), System.currentTimeMillis() + ".png");
            try (FileOutputStream os = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            record(c, new Item(null, out.getAbsolutePath(), false));
        } catch (IOException | RuntimeException ignored) {
        }
    }

    static void togglePin(Context c, Item item) {
        List<Item> items = load(c);
        for (Item i : items) if (i.sameAs(item)) i.pinned = !i.pinned;
        save(c, items);
    }

    /** Move a clip to the front of the history (keeping its pin), as if it were just copied. */
    static void bump(Context c, Item item) {
        List<Item> items = load(c);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).sameAs(item)) {
                Item moved = items.remove(i);
                items.add(0, moved);
                save(c, items);
                return;
            }
        }
    }

    /** Delete these clips (and any image files they own). */
    static void remove(Context c, java.util.Collection<Item> targets) {
        List<Item> items = load(c);
        for (int i = items.size() - 1; i >= 0; i--) {
            for (Item t : targets) {
                if (items.get(i).sameAs(t)) {
                    deleteFile(items.remove(i));
                    break;
                }
            }
        }
        save(c, items);
    }

    /** Pin or unpin all of these clips. */
    static void setPinned(Context c, java.util.Collection<Item> targets, boolean pinned) {
        List<Item> items = load(c);
        for (Item i : items) {
            for (Item t : targets) if (i.sameAs(t)) i.pinned = pinned;
        }
        save(c, items);
    }

    static void clearUnpinned(Context c) {
        List<Item> items = load(c);
        for (int i = items.size() - 1; i >= 0; i--) {
            if (!items.get(i).pinned) deleteFile(items.remove(i));
        }
        save(c, items);
    }

    private static void deleteFile(Item i) {
        if (i.isImage()) {
            //noinspection ResultOfMethodCallIgnored
            new File(i.image).delete();
        }
    }
}
