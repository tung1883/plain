package com.plainphone.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Small AES-GCM secret box backed by the Android keystore — the app's first use
 * of it, kept deliberately tiny. Values are the Dev-plugin pairing tokens, which
 * are LAN/Tailscale-scoped and low value; if the keystore is unavailable the
 * value is stored obfuscated in plain prefs rather than failing the pairing.
 *
 * <p>Ciphertext layout in prefs: {@code base64(iv || ciphertext)}, 12-byte IV.
 */
final class SecretStore {

    private SecretStore() {}

    private static final String PREFS = "dev_secrets";
    private static final String KEY_ALIAS = "plain_dev_secret_key";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    static void put(Context context, String name, String value) {
        String stored;
        try {
            SecretKey key = key();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();
            byte[] ct = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] joined = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, joined, 0, iv.length);
            System.arraycopy(ct, 0, joined, iv.length, ct.length);
            stored = "v1:" + Base64.encodeToString(joined, Base64.NO_WRAP);
        } catch (Exception e) {
            stored = "p0:" + Base64.encodeToString(
                    xor(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        }
        prefs(context).edit().putString(name, stored).apply();
    }

    static String get(Context context, String name) {
        String stored = prefs(context).getString(name, null);
        if (stored == null) return null;
        try {
            if (stored.startsWith("p0:")) {
                return new String(xor(Base64.decode(stored.substring(3), Base64.NO_WRAP)),
                        StandardCharsets.UTF_8);
            }
            byte[] joined = Base64.decode(stored.substring(3), Base64.NO_WRAP);
            byte[] iv = new byte[IV_LEN];
            byte[] ct = new byte[joined.length - IV_LEN];
            System.arraycopy(joined, 0, iv, 0, IV_LEN);
            System.arraycopy(joined, IV_LEN, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    static void remove(Context context, String name) {
        prefs(context).edit().remove(name).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) ks.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator gen = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return gen.generateKey();
    }

    /** Last-resort obfuscation when the keystore is unavailable — not real crypto. */
    private static byte[] xor(byte[] data) {
        byte[] pad = "plainphone-dev".getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) out[i] = (byte) (data[i] ^ pad[i % pad.length]);
        return out;
    }
}
