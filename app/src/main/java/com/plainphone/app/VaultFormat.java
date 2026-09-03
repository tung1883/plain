package com.plainphone.app;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

/**
 * Reads and writes {@code vault/header.pv} — a small JSON blob holding the KDF
 * parameters and the passphrase-wrapped master key. No file content passes
 * through here.
 */
final class VaultFormat {

    private VaultFormat() {}

    static final String MAGIC = "PPV1";
    static final int PBKDF2_ITERATIONS = 600_000;
    static final int SALT_LEN = 32;
    static final int MASTER_KEY_LEN = 32;

    static final class WrongPassphrase extends Exception {}

    static class Header {
        int iterations;
        byte[] salt;
        byte[] wrappedMasterKey;
    }

    static boolean exists(File vaultRoot) {
        return headerFile(vaultRoot).isFile();
    }

    private static File headerFile(File vaultRoot) {
        return new File(vaultRoot, "header.pv");
    }

    static void createVault(File vaultRoot, char[] passphrase)
            throws IOException, GeneralSecurityException {
        createVault(vaultRoot, passphrase, null);
    }

    static void createVault(File vaultRoot, char[] passphrase, VaultCrypto.Progress progress)
            throws IOException, GeneralSecurityException {
        VaultCrypto.zeroize(createVaultKey(vaultRoot, passphrase, progress));
    }

    /**
     * Generate salt + master key, wrap the master key under the passphrase, write the
     * header, and return the live master key so the caller can adopt it without a
     * second KDF pass (an unlock right after creation used to re-derive the whole
     * PBKDF2 — ~4 s wasted at 100%).
     */
    static byte[] createVaultKey(File vaultRoot, char[] passphrase, VaultCrypto.Progress progress)
            throws IOException, GeneralSecurityException {
        if (!vaultRoot.isDirectory() && !vaultRoot.mkdirs()) {
            throw new IOException("Can't create vault dir " + vaultRoot);
        }
        new File(vaultRoot, "d").mkdirs();

        byte[] salt = VaultCrypto.randomBytes(SALT_LEN);
        byte[] masterKey = VaultCrypto.randomBytes(MASTER_KEY_LEN);
        byte[] passRaw = VaultCrypto.deriveKey(passphrase, salt, PBKDF2_ITERATIONS, progress);
        byte[] wrapped;
        try {
            wrapped = VaultCrypto.wrap(VaultCrypto.aesKey(passRaw), masterKey);
        } finally {
            VaultCrypto.zeroize(passRaw);
        }

        JSONObject json = new JSONObject();
        try {
            json.put("magic", MAGIC);
            json.put("kdf", "PBKDF2WithHmacSHA256");
            json.put("iterations", PBKDF2_ITERATIONS);
            json.put("salt", b64(salt));
            json.put("wrappedMasterKey", b64(wrapped));
        } catch (JSONException e) {
            throw new IOException(e);
        }
        writeAtomically(headerFile(vaultRoot), json.toString().getBytes(StandardCharsets.UTF_8));
        return masterKey;
    }

    static Header readHeader(File vaultRoot) throws IOException {
        byte[] raw = Files.readAllBytes(headerFile(vaultRoot).toPath());
        try {
            JSONObject json = new JSONObject(new String(raw, StandardCharsets.UTF_8));
            if (!MAGIC.equals(json.optString("magic"))) throw new IOException("Bad vault header");
            Header header = new Header();
            header.iterations = json.getInt("iterations");
            header.salt = unb64(json.getString("salt"));
            header.wrappedMasterKey = unb64(json.getString("wrappedMasterKey"));
            return header;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    static byte[] unwrapMasterKey(Header header, char[] passphrase)
            throws GeneralSecurityException, WrongPassphrase {
        return unwrapMasterKey(header, passphrase, null);
    }

    /** Derive the pass key and unwrap the master key, or throw {@link WrongPassphrase}. */
    static byte[] unwrapMasterKey(Header header, char[] passphrase, VaultCrypto.Progress progress)
            throws GeneralSecurityException, WrongPassphrase {
        byte[] passRaw = VaultCrypto.deriveKey(passphrase, header.salt, header.iterations, progress);
        try {
            return VaultCrypto.unwrap(VaultCrypto.aesKey(passRaw), header.wrappedMasterKey);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new WrongPassphrase();
        } finally {
            VaultCrypto.zeroize(passRaw);
        }
    }

    static void changePassphrase(File vaultRoot, byte[] masterKey, char[] newPassphrase)
            throws IOException, GeneralSecurityException {
        changePassphrase(vaultRoot, masterKey, newPassphrase, null);
    }

    /** Re-wrap the same master key under a new passphrase (phase 2). */
    static void changePassphrase(File vaultRoot, byte[] masterKey, char[] newPassphrase,
                                 VaultCrypto.Progress progress)
            throws IOException, GeneralSecurityException {
        byte[] salt = VaultCrypto.randomBytes(SALT_LEN);
        byte[] passRaw = VaultCrypto.deriveKey(newPassphrase, salt, PBKDF2_ITERATIONS, progress);
        byte[] wrapped;
        try {
            wrapped = VaultCrypto.wrap(VaultCrypto.aesKey(passRaw), masterKey);
        } finally {
            VaultCrypto.zeroize(passRaw);
        }
        JSONObject json = new JSONObject();
        try {
            json.put("magic", MAGIC);
            json.put("kdf", "PBKDF2WithHmacSHA256");
            json.put("iterations", PBKDF2_ITERATIONS);
            json.put("salt", b64(salt));
            json.put("wrappedMasterKey", b64(wrapped));
        } catch (JSONException e) {
            throw new IOException(e);
        }
        writeAtomically(headerFile(vaultRoot), json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeAtomically(File target, byte[] data) throws IOException {
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        Files.write(tmp.toPath(), data);
        if (!tmp.renameTo(target)) {
            Files.write(target.toPath(), data);
            tmp.delete();
        }
    }

    private static String b64(byte[] b) {
        return Base64.encodeToString(b, Base64.NO_WRAP);
    }

    private static byte[] unb64(String s) {
        return Base64.decode(s, Base64.NO_WRAP);
    }
}
