package com.plainphone.app;

import android.content.Context;
import android.net.Uri;
import android.view.View;

/** Builds the home-art view for the current selection: off / a scene / a gallery item. */
final class ArtKit {

    private ArtKit() {}

    /** The view for `artFrame`, or null when art is off / nothing is selected. */
    static View homeArt(Context context) {
        if ("off".equals(Config.getArtMode(context))) return null;
        return viewFor(context, ArtGallery.currentId(context));
    }

    /** Build the view for one selection id ("scene:NAME" or a gallery item id). */
    static View viewFor(Context context, String id) {
        if (id == null) return null;
        if (id.startsWith("scene:")) {
            try {
                GifScene s = GifScene.valueOf(id.substring(6));
                float[] c = Config.getSceneCrop(context, s);
                return new GifArtView(context, s, c[0], c[1], c[2], c[3] != 0f);
            } catch (Exception e) {
                return null;
            }
        }
        ArtItem it = ArtGallery.get(context, id);
        if (it == null) return null;
        return it.isGif()
                ? new GifArtView(context, it.file(context), it.fx, it.fy, it.zoom, it.gray)
                : new PhotoArtView(context, Uri.fromFile(it.file(context)),
                        it.fx, it.fy, it.zoom, it.gray);
    }
}
