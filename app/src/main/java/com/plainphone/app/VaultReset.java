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
 * <p>Gated in the UI by a device-credential prompt, so a passerby with your
 * unlocked phone can't wipe it.
 */
final class VaultReset {

    private VaultReset() {}

    static void wipe(Context context) {
        if (VaultSession.get().isUnlocked()) {
            VaultUnlockService.stop(context);
            VaultSession.get().lock(context.getApplicationContext());
        }

        deleteRecursively(VaultSession.vaultRoot(context));
        deleteRecursively(VaultSession.defaultVaultRoot(context));   // in case a custom location was set
        deleteRecursively(VaultOpenFiles.openDir(context));          // any decrypt temps

        Config.setVaultLocationPath(context, null);
        Config.setVaultHiddenFromHome(context, false);

        context.getPackageManager().setComponentEnabledSetting(
                new ComponentName(context, VaultDocumentsProvider.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        File[] kids = file.listFiles();
        if (kids != null) {
            for (File kid : kids) deleteRecursively(kid);
        }
        file.delete();
    }
}
