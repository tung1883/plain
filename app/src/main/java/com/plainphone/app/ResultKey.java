package com.plainphone.app;

import android.content.pm.ResolveInfo;

/** A stable identity for a search result, so what the user picks can be remembered ({@link PickHistory}). */
final class ResultKey {

    private ResultKey() {}

    /** The key for {@code r}, or null for results that are not worth learning (web, actions, to-dos ...). */
    static String of(SearchResult r) {
        Object p = r.payload;
        if (p instanceof ResolveInfo) return "app:" + ((ResolveInfo) p).activityInfo.packageName;
        if (p instanceof Note) return "note:" + ((Note) p).id;
        if (p instanceof Recording) return "rec:" + ((Recording) p).id;
        if (p instanceof FileIndex.Entry) return "file:" + ((FileIndex.Entry) p).file.getAbsolutePath();
        if (p instanceof ContentHit) return "file:" + ((ContentHit) p).file.getAbsolutePath();
        if (p instanceof DeviceSearch.MediaFile) return "media:" + ((DeviceSearch.MediaFile) p).uri;
        switch (r.kind) {
            case PLAIN:
            case SYSTEM:
            case CONTACT:
                return r.kind.name().toLowerCase(java.util.Locale.ROOT) + ":" + r.title;
            default:
                return null;
        }
    }
}
