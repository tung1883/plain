package com.plainphone.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * A PIN gate over one feature area of the launcher. Each value owns two prefs
 * keys ({@code <area>_locked}, {@code <area>_unlock_until}) and the copy shown on
 * its gate screens. The generic {@link LockChangeActivity} and
 * {@link LockPinGateActivity} render the right text from the {@code EXTRA_LOCK}
 * name they are launched with.
 */
enum Lock {
    NOTES("notes", "Unlocking Notes"),
    TODOS("todos", "Unlocking To-do"),
    RECORDER("recorder", "Unlocking the recorder"),
    APPS("apps", "Unlocking the app list"),
    SEARCH("search", "Unlocking search");

    static final String EXTRA_LOCK = "lock";

    final String area;
    final String frictionLabel;

    Lock(String area, String frictionLabel) {
        this.area = area;
        this.frictionLabel = frictionLabel;
    }

    static Lock from(Intent intent) {
        return valueOf(intent.getStringExtra(EXTRA_LOCK));
    }

    /** The section lock with this prefs area, or null (e.g. "applock" / "settings"). */
    static Lock byArea(String area) {
        for (Lock lock : values()) {
            if (lock.area.equals(area)) return lock;
        }
        return null;
    }

    /** How long this section stays unlocked after you leave it, in millis. */
    long graceMs(Context context) {
        return Config.getRelockSeconds(context, area) * 1000L;
    }

    /**
     * Turn every launcher section's lock on and expire all grace windows, section
     * and per-app. The vault is a plugin with pausable tasks — it is locked
     * separately through {@link PluginLock#requestLockAll}.
     */
    static void lockAllSections(Context context) {
        for (Lock lock : values()) {
            if (lock.importing(context)) continue;   // a pending import keeps its section unlocked
            lock.setLocked(context, true);
            Config.setUnlockUntil(context, lock.area, 0L);
        }
        Config.clearAppUnlocks(context);
    }

    /** True while this section has a background import queued or running. */
    boolean importing(Context context) {
        switch (this) {
            case NOTES:    return ImportJobs.pendingForPlugin(context, HomeMode.NOTES);
            case TODOS:    return ImportJobs.pendingForPlugin(context, HomeMode.TODOS);
            case RECORDER: return ImportJobs.pendingForPlugin(context, HomeMode.RECORDER);
            default:       return false;
        }
    }

    boolean isLocked(Context context) {
        return Config.isLocked(context, area);
    }

    void setLocked(Context context, boolean locked) {
        Config.setLocked(context, area, locked);
    }

    boolean isUnlocked(Context context) {
        return System.currentTimeMillis() < Config.getUnlockUntil(context, area);
    }

    /** Call on a successful PIN entry, and while the area is actively shown. */
    void keepUnlocked(Context context) {
        long until = System.currentTimeMillis() + graceMs(context);
        if (until - Config.getUnlockUntil(context, area) > 5_000L) {
            Config.setUnlockUntil(context, area, until);
        }
    }

    boolean gateActive(Context context) {
        return isLocked(context) && !isUnlocked(context) && Config.isPinSet(context, area)
                && Config.isLocksEnabled(context) && !importing(context);
    }

    Intent pinGate(Context context) {
        return new Intent(context, LockPinGateActivity.class).putExtra(EXTRA_LOCK, name());
    }

    /** Settings-row toggle: straight flip both ways — reaching this row already
     *  required the section's PIN. */
    void toggleLock(Activity host, Runnable after) {
        if (isLocked(host)) {
            setLocked(host, false);
            Config.setUnlockUntil(host, area, 0L);
            if (after != null) after.run();
        } else if (!Config.isPinSet(host, area)) {
            Toast.makeText(host, "Set a master PIN first (Settings → Lock settings)",
                    Toast.LENGTH_LONG).show();
        } else {
            setLocked(host, true);
            Config.setUnlockUntil(host, area, 0L);
            if (after != null) after.run();
        }
    }
}
