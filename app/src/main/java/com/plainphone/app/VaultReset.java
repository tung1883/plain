package com.plainphone.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;

/**
 * Nuke the vault — the escape hatch for a forgotten password. Everything encrypted
 * is deleted (no way to recover it without the password anyway), the vault falls
 * back to "not set up", and the file-picker provider is disabled again.
 *
 * <p>Run as a persistent job by {@link VaultJobService}: the delete is idempotent,
 * so an interrupted wipe simply resumes and finishes on next launch. Gated in the
 * UI by a device-credential prompt.
 */
final class VaultReset {

    private VaultReset() {}

    interface Progress {
        void onProgress(int deleted, int total);
        boolean cancelled();
    }

    /** Lock the vault so the wipe isn't racing live crypto. Call before starting the job. */
    static void lockForWipe(Context context) {
        if (VaultSession.get().isUnlocked()) {
            VaultUnlockService.stop(context);
            VaultSession.get().lock(context.getApplicationContext());
        }
    }

    static void wipe(Context context, Progress progress) {
        File[] targets = {
                VaultSession.vaultRoot(context),
                VaultSession.defaultVaultRoot(context),   // in case a custom location was set
                VaultOpenFiles.openDir(context),          // any decrypt temps
        };

        int total = 0;
        for (File t : targets) total += count(t);
        int[] done = {0};

        for (File t : targets) {
            deleteRecursively(t, progress, done, Math.max(total, 1));
            if (progress.cancelled()) return;
        }

        Config.setVaultLocationPath(context, null);
        Config.setVaultHiddenFromHome(context, false);

        context.getPackageManager().setComponentEnabledSetting(
                new ComponentName(context, VaultDocumentsProvider.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        progress.onProgress(Math.max(total, 1), Math.max(total, 1));
    }

    private static int count(File file) {
        if (file == null || !file.exists()) return 0;
        int n = 1;
        File[] kids = file.listFiles();
        if (kids != null) for (File kid : kids) n += count(kid);
        return n;
    }

    private static void deleteRecursively(File file, Progress progress, int[] done, int total) {
        if (file == null || !file.exists()) return;
        if (progress.cancelled()) return;
        File[] kids = file.listFiles();
        if (kids != null) {
            for (File kid : kids) {
                deleteRecursively(kid, progress, done, total);
                if (progress.cancelled()) return;
            }
        }
        file.delete();
        done[0]++;
        if ((done[0] & 0x3F) == 0) progress.onProgress(done[0], total);
    }
}
