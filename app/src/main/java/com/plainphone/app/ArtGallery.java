package com.plainphone.app;

import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The home-art gallery: user-added images / GIFs stored as files under
 * {@code filesDir/art/} with metadata in the {@code art_gallery} pref (JSON,
 * same pattern as {@link Config#getNotes}). The checked subset drives the home
 * art — one item static, two or more a crossfade slideshow whose rotation reuses
 * the Tips mechanism ({@link Tips#maybeAutoAdvance}).
 */
final class ArtGallery {

    private ArtGallery() {}

    private static final int MAX_BYTES = 25 * 1024 * 1024;

    static File dir(Context context) {
        File d = new File(context.getFilesDir(), "art");
        d.mkdirs();
        return d;
    }

    // --- list ------------------------------------------------

    static List<ArtItem> all(Context context) {
        List<ArtItem> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(Config.getArtGalleryJson(context));
            for (int i = 0; i < arr.length(); i++) out.add(ArtItem.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {
        }
        return out;
    }

    static void save(Context context, List<ArtItem> items) {
        JSONArray arr = new JSONArray();
        for (ArtItem it : items) arr.put(it.toJson());
        Config.setArtGalleryJson(context, arr.toString());
    }

    static ArtItem get(Context context, String id) {
        for (ArtItem it : all(context)) if (it.id.equals(id)) return it;
        return null;
    }

    static void replace(Context context, ArtItem updated) {
        List<ArtItem> items = all(context);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(updated.id)) {
                items.set(i, updated);
                save(context, items);
                return;
            }
        }
    }

    static void updateCrop(Context context, String id, float fx, float fy, float zoom) {
        ArtItem it = get(context, id);
        if (it == null) return;
        it.fx = fx; it.fy = fy; it.zoom = zoom;
        replace(context, it);
    }

    static void setGray(Context context, String id, boolean gray) {
        ArtItem it = get(context, id);
        if (it == null) return;
        it.gray = gray;
        replace(context, it);
    }

    // --- add / delete --------------------------------------

    static ArtItem addFromUri(Context context, Uri uri, boolean isGif) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            return in == null ? null
                    : addFromStream(context, in, isGif, displayName(context, uri));
        } catch (Exception e) {
            return null;
        }
    }

    static ArtItem addFromFile(Context context, File src, boolean isGif,
                               float fx, float fy, float zoom, boolean gray) {
        try (InputStream in = new java.io.FileInputStream(src)) {
            ArtItem it = addFromStream(context, in, isGif, src.getName());
            if (it != null) {
                it.fx = fx; it.fy = fy; it.zoom = zoom; it.gray = gray;
                replace(context, it);
            }
            return it;
        } catch (Exception e) {
            return null;
        }
    }

    private static ArtItem addFromStream(Context context, InputStream in, boolean isGif, String name) {
        String id = UUID.randomUUID().toString();
        String fileName = id + (isGif ? ".gif" : ".img");
        File dst = new File(dir(context), fileName);
        long total = 0;
        try (OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_BYTES) throw new java.io.IOException("too big");
                out.write(buf, 0, n);
            }
        } catch (Exception e) {
            dst.delete();
            return null;
        }

        ArtItem it = new ArtItem();
        it.id = id;
        it.kind = isGif ? "gif" : "photo";
        it.file = fileName;
        it.name = name != null ? name : (isGif ? "GIF" : "Image");
        List<ArtItem> items = all(context);
        items.add(it);
        save(context, items);
        return it;
    }

    static void delete(Context context, String id) {
        List<ArtItem> items = all(context);
        items.removeIf(it -> {
            if (it.id.equals(id)) {
                new File(dir(context), it.file).delete();
                return true;
            }
            return false;
        });
        save(context, items);

        List<String> sel = Config.getArtSelected(context);
        if (sel.remove(id)) Config.setArtSelected(context, sel);
        if (selectedIds(context).isEmpty()) Config.setArtMode(context, "off");
    }

    // --- selection ----------------------------------------

    /**
     * Selected ids in a stable order — gallery items (that still exist) then scenes.
     * A scene id is {@code "scene:<NAME>"}.
     */
    static List<String> selectedIds(Context context) {
        List<String> want = Config.getArtSelected(context);
        List<String> out = new ArrayList<>();
        for (ArtItem it : all(context)) if (want.contains(it.id)) out.add(it.id);
        for (String id : want) if (id.startsWith("scene:")) out.add(id);
        return out;
    }

    static boolean isSelected(Context context, String id) {
        return Config.getArtSelected(context).contains(id);
    }

    static void toggle(Context context, String id) {
        List<String> sel = Config.getArtSelected(context);
        if (!sel.remove(id)) sel.add(id);
        Config.setArtSelected(context, sel);
    }

    // --- slideshow ---------------------------------------

    /** The id (gallery id or "scene:NAME") to show right now, or null when nothing is selected. */
    static String currentId(Context context) {
        List<String> sel = selectedIds(context);
        if (sel.isEmpty()) return null;
        int idx = Math.floorMod(Config.getArtSlideIndex(context), sel.size());
        return sel.get(idx);
    }

    static void advance(Context context) {
        Config.setArtSlideIndex(context, Config.getArtSlideIndex(context) + 1);
        Config.setArtSlideLastAdvance(context, System.currentTimeMillis());
    }

    /** Tips-style catch-up: advance by whole elapsed intervals with no drift. */
    static boolean maybeAdvance(Context context) {
        if ("off".equals(Config.getArtMode(context))) return false;
        if (selectedIds(context).size() < 2) return false;
        int minutes = Config.getArtSlideshowMinutes(context);
        if (minutes <= 0) return false;

        long now = System.currentTimeMillis();
        long last = Config.getArtSlideLastAdvance(context);
        if (last <= 0L || last > now) {
            Config.setArtSlideLastAdvance(context, now);
            return false;
        }
        long intervalMs = minutes * 60_000L;
        long elapsed = now - last;
        if (elapsed < intervalMs) return false;

        int steps = (int) Math.min(elapsed / intervalMs, 10_000);
        Config.setArtSlideIndex(context, Config.getArtSlideIndex(context) + steps);
        Config.setArtSlideLastAdvance(context, last + steps * intervalMs);
        return true;
    }

    // --- helpers ----------------------------------------

    private static String displayName(Context context, Uri uri) {
        try (android.database.Cursor c = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
        } catch (Exception ignored) {
        }
        return null;
    }
}
