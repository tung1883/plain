package com.plainphone.app;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** The storage volumes of this phone: the internal storage first, then SD cards and USB drives. */
final class Volumes {

    private Volumes() {}

    static List<StorageLocations.Volume> list(Context context) {
        List<StorageLocations.Volume> out = new ArrayList<>();
        out.add(new StorageLocations.Volume(StorageLocations.PRIMARY, "Internal only",
                Environment.getExternalStorageDirectory(), true));

        if (Build.VERSION.SDK_INT >= 30) {
            StorageManager storage = context.getSystemService(StorageManager.class);
            if (storage != null) {
                for (StorageVolume v : storage.getStorageVolumes()) {
                    if (v.isPrimary() || !v.isRemovable()) continue;
                    String uuid = v.getUuid();
                    if (uuid == null) continue;
                    boolean mounted = Environment.MEDIA_MOUNTED.equals(v.getState()) && v.getDirectory() != null;
                    String label = v.getDescription(context);
                    out.add(new StorageLocations.Volume(uuid,
                            label == null || label.isEmpty() ? "SD card" : label, mounted ? v.getDirectory() : null, false));
                }
            }
            return out;
        }

        // before Android 11: the card's root is four levels above its app folder (/storage/ABCD-1234/Android/data/<pkg>/files)
        File[] dirs = context.getExternalFilesDirs(null);
        for (int i = 1; i < dirs.length; i++) {
            File root = dirs[i];
            for (int up = 0; up < 4 && root != null; up++) root = root.getParentFile();
            if (root != null && root.canRead()) out.add(new StorageLocations.Volume(root.getName(), "SD card", root, false));
        }
        return out;
    }
}
