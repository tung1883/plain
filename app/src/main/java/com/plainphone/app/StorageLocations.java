package com.plainphone.app;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * The places content search reads, as stored in the settings. A location is a folder on a storage
 * volume. On the main storage it is just the path below it ({@code Documents}, {@code Books/Sub}) or
 * {@code *} for everything, which is what older versions stored. On any other volume it is
 * {@code <volume id>:<path>} or {@code <volume id>:*}. Pure Java; {@link Volumes} lists the real volumes.
 */
final class StorageLocations {

    private StorageLocations() {}

    /** The id of the main (internal) storage volume. */
    static final String PRIMARY = "primary";

    /** A storage volume: the internal storage or a removable card. */
    static final class Volume {
        final String id;
        final String label;
        final File root;          // null when it is not mounted
        final boolean primary;

        Volume(String id, String label, File root, boolean primary) {
            this.id = id;
            this.label = label;
            this.root = root;
            this.primary = primary;
        }
    }

    /** One stored location, taken apart. {@code path} is "*" for the whole volume. */
    static final class Location {
        final String volumeId;
        final String path;

        Location(String volumeId, String path) {
            this.volumeId = volumeId;
            this.path = path;
        }

        boolean whole() {
            return path.equals("*");
        }
    }

    static Location parse(String stored) {
        int colon = stored.indexOf(':');
        if (colon > 0) return new Location(stored.substring(0, colon), clean(stored.substring(colon + 1)));
        return new Location(PRIMARY, clean(stored));
    }

    static String encode(String volumeId, String path) {
        String p = clean(path);
        return PRIMARY.equals(volumeId) ? p : volumeId + ":" + p;
    }

    /** Slashes trimmed, empty meaning the whole volume. */
    private static String clean(String path) {
        String p = path.replaceAll("^/+|/+$", "");
        return p.isEmpty() ? "*" : p;
    }

    /** The folder a location points to, or null when its volume is missing / not mounted or the path is not allowed. */
    static File resolve(Location loc, List<Volume> volumes) {
        for (Volume v : volumes) {
            if (!v.id.equals(loc.volumeId)) continue;
            if (v.root == null) return null;
            if (loc.whole()) return v.root;
            for (String part : loc.path.split("/")) if (part.equals("..")) return null;
            return new File(v.root, loc.path);
        }
        return null;
    }

    /** What one stored location is called in lists: its folder name, or "Internal: all" / "SD card: all". */
    static String entryName(Location loc, List<Volume> volumes) {
        if (!loc.whole()) return loc.path.substring(loc.path.lastIndexOf('/') + 1);
        for (Volume v : volumes) if (v.id.equals(loc.volumeId)) return (v.primary ? "Internal" : v.label) + ": all";
        return "other storage: all";
    }

    /** True when the folder itself is chosen ({@code path} "*" is the whole volume). */
    static boolean isChosen(Set<String> stored, String volumeId, String path) {
        return stored.contains(encode(volumeId, path));
    }

    /** True when a folder above it, or the whole volume, is chosen: it is read as part of that. */
    static boolean coveredByParent(Set<String> stored, String volumeId, String path) {
        String p = clean(path);
        if (p.equals("*")) return false;
        if (stored.contains(encode(volumeId, "*"))) return true;
        for (int i = p.indexOf('/'); i > 0; i = p.indexOf('/', i + 1)) {
            if (stored.contains(encode(volumeId, p.substring(0, i)))) return true;
        }
        return false;
    }

    /** Choose a folder: everything chosen inside it is dropped, it now covers them. */
    static Set<String> choose(Set<String> stored, String volumeId, String path) {
        Set<String> out = new java.util.LinkedHashSet<>();
        String p = clean(path);
        for (String s : stored) {
            Location loc = parse(s);
            boolean inside = loc.volumeId.equals(volumeId) && !loc.path.equals(p)
                    && (p.equals("*") || loc.path.startsWith(p + "/"));
            if (!inside) out.add(s);
        }
        out.add(encode(volumeId, p));
        return out;
    }

    static Set<String> unchoose(Set<String> stored, String volumeId, String path) {
        Set<String> out = new java.util.LinkedHashSet<>(stored);
        out.remove(encode(volumeId, path));
        return out;
    }

    /**
     * The location for a folder picked with the system folder picker, from the tree's document id
     * ("primary:Books/Sub", "1234-ABCD:Backup", "primary:" for the root); null for ids we cannot read.
     */
    static String fromTreeDocumentId(String docId) {
        if (docId == null) return null;
        int colon = docId.indexOf(':');
        if (colon <= 0) return null;
        String volume = docId.substring(0, colon), path = docId.substring(colon + 1);
        if (volume.equals("home") || volume.equals("raw") || volume.equals("downloads")) return null;
        return encode(volume.equals("primary") ? PRIMARY : volume, path);
    }
}
