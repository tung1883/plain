package com.plainphone.app;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.crypto.SecretKey;

/**
 * Process-wide unlocked state for the vault. Holds the master key and the two
 * sub-keys in memory only while unlocked; {@link #lock()} zeroes them and tells
 * the system file picker to repopulate.
 *
 * <p>If the process dies, this state goes with it — the vault is then locked.
 * {@link VaultUnlockService} keeps the process alive while a file is in use.
 */
final class VaultSession {

    interface Listener {
        void onVaultLockStateChanged();
    }

    private static final VaultSession INSTANCE = new VaultSession();

    static VaultSession get() {
        return INSTANCE;
    }

    private byte[] masterKey;
    private SecretKey contentKey;
    private SecretKey nameKey;

    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private VaultSession() {}

    static File vaultRoot(Context context) {
        return new File(context.getFilesDir(), "vault");
    }

    synchronized boolean isUnlocked() {
        return masterKey != null;
    }

    synchronized void unlock(Context context, char[] passphrase)
            throws GeneralSecurityException, VaultFormat.WrongPassphrase, java.io.IOException {
        unlock(context, passphrase, null);
    }

    synchronized void unlock(Context context, char[] passphrase, VaultCrypto.Progress progress)
            throws GeneralSecurityException, VaultFormat.WrongPassphrase, java.io.IOException {
        VaultFormat.Header header = VaultFormat.readHeader(vaultRoot(context));
        byte[] key = VaultFormat.unwrapMasterKey(header, passphrase, progress);
        adoptKey(key);
        notifyChanged(context);
    }

    synchronized void adoptKey(byte[] key) {
        this.masterKey = key;
        this.contentKey = VaultCrypto.subKey(key, "content");
        this.nameKey = VaultCrypto.subKey(key, "name");
    }

    /** The still-unwrapped master key, for a passphrase change. Null when locked. */
    synchronized byte[] masterKey() {
        return masterKey;
    }

    synchronized void lock(Context context) {
        if (masterKey == null) return;
        VaultCrypto.zeroize(masterKey);
        masterKey = null;
        contentKey = null;
        nameKey = null;
        VaultOpenFiles.get().shredAll();
        VaultThumbs.clear();
        notifyChanged(context);
    }

    synchronized SecretKey contentKey() {
        return contentKey;
    }

    synchronized SecretKey nameKey() {
        return nameKey;
    }

    void addListener(Listener l) {
        listeners.addIfAbsent(l);
    }

    void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void notifyChanged(Context context) {
        Uri roots = DocumentsContract.buildRootsUri(VaultDocumentsProvider.AUTHORITY);
        context.getContentResolver().notifyChange(roots, null);
        for (Listener l : listeners) l.onVaultLockStateChanged();
    }
}
