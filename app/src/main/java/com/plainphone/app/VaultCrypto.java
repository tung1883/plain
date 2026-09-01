package com.plainphone.app;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Zero-dependency crypto for the file vault. Everything here is {@code javax.crypto}
 * on the platform (minSdk 26).
 *
 * <ul>
 *   <li>KDF: PBKDF2-HMAC-SHA256, iteration count stored in the header.
 *   <li>Key wrap / filename cipher / content chunks: AES-256-GCM.
 *   <li>Sub-keys: HKDF-SHA256(masterKey, "content" | "name").
 *   <li>Content blob: {@code fileNoncePrefix(4) | chunk...}, each chunk is
 *       {@code ciphertext | 16-byte tag} over a 64 KiB plaintext window. The GCM
 *       nonce is {@code prefix ‖ big-endian chunk counter}; the AAD binds the
 *       chunk index and a last-chunk flag so truncation is detectable.
 * </ul>
 */
final class VaultCrypto {

    private VaultCrypto() {}

    static final int GCM_NONCE_LEN = 12;
    static final int GCM_TAG_BITS = 128;
    static final int GCM_TAG_LEN = 16;
    static final int CHUNK_PLAIN = 64 * 1024;
    static final int FILE_PREFIX_LEN = 4;

    private static final SecureRandom RANDOM = new SecureRandom();

    static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }

    static void zeroize(byte[] b) {
        if (b != null) Arrays.fill(b, (byte) 0);
    }

    // --- KDF ---------------------------------------------------------------

    /** PBKDF2-HMAC-SHA256 → 256-bit key material. Caller owns {@code passphrase}. */
    static byte[] deriveKey(char[] passphrase, byte[] salt, int iterations)
            throws GeneralSecurityException {
        KeySpec spec = new PBEKeySpec(passphrase, salt, iterations, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey key = factory.generateSecret(spec);
        return key.getEncoded();
    }

    // --- HKDF-SHA256 -----------------------------------------------------

    static SecretKey subKey(byte[] masterKey, String info) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(new byte[32], "HmacSHA256"));
            byte[] prk = mac.doFinal(masterKey);              // extract, salt = zeros

            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            mac.update(info.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0x01);
            byte[] okm = mac.doFinal();                       // expand, one block = 32 bytes
            zeroize(prk);
            return new SecretKeySpec(okm, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    // --- key wrap ------------------------------------------------------

    /** {@code nonce(12) | ciphertext | tag(16)}. */
    static byte[] wrap(SecretKey wrappingKey, byte[] plaintext) throws GeneralSecurityException {
        byte[] nonce = randomBytes(GCM_NONCE_LEN);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey, new GCMParameterSpec(GCM_TAG_BITS, nonce));
        byte[] ct = cipher.doFinal(plaintext);
        return concat(nonce, ct);
    }

    static byte[] unwrap(SecretKey wrappingKey, byte[] wrapped) throws GeneralSecurityException {
        byte[] nonce = Arrays.copyOfRange(wrapped, 0, GCM_NONCE_LEN);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey, new GCMParameterSpec(GCM_TAG_BITS, nonce));
        return cipher.doFinal(wrapped, GCM_NONCE_LEN, wrapped.length - GCM_NONCE_LEN);
    }

    static SecretKey aesKey(byte[] raw) {
        return new SecretKeySpec(raw, "AES");
    }

    // --- filename cipher ------------------------------------------------

    static String encryptName(SecretKey nameKey, String name) {
        try {
            byte[] wrapped = wrap(nameKey, name.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(wrapped, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING)
                    + ".pv";
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Returns the plaintext name, or {@code null} if this is not one of ours. */
    static String decryptName(SecretKey nameKey, String encName) {
        if (encName == null || !encName.endsWith(".pv")) return null;
        try {
            byte[] wrapped = Base64.decode(encName.substring(0, encName.length() - 3),
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return new String(unwrap(nameKey, wrapped), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    // --- content stream ------------------------------------------------

    /** Plaintext size a blob of {@code blobLen} bytes decrypts to. */
    static long plaintextSize(long blobLen) {
        long body = blobLen - FILE_PREFIX_LEN;
        if (body <= 0) return 0;
        long cipherChunk = CHUNK_PLAIN + GCM_TAG_LEN;
        long fullChunks = body / cipherChunk;
        long remainder = body % cipherChunk;
        long size = fullChunks * CHUNK_PLAIN;
        if (remainder > 0) size += remainder - GCM_TAG_LEN;
        return size;
    }

    private static byte[] nonce(byte[] prefix, long chunkIndex) {
        ByteBuffer buf = ByteBuffer.allocate(GCM_NONCE_LEN);
        buf.put(prefix);
        buf.putLong(chunkIndex);
        return buf.array();
    }

    private static byte[] aad(long chunkIndex, boolean last) {
        ByteBuffer buf = ByteBuffer.allocate(9);
        buf.putLong(chunkIndex);
        buf.put((byte) (last ? 1 : 0));
        return buf.array();
    }

    /** Encrypt {@code in} into {@code out} as a vault content blob, then close both. */
    static void encryptStream(InputStream in, OutputStream out, SecretKey contentKey)
            throws IOException, GeneralSecurityException {
        byte[] prefix = randomBytes(FILE_PREFIX_LEN);
        out.write(prefix);

        byte[] current = new byte[CHUNK_PLAIN];
        int currentLen = readFully(in, current);
        long index = 0;
        while (true) {
            // Read the following window so we know whether the current one is last
            // (the AAD flag binds it, so truncation past a chunk boundary is caught).
            byte[] next = new byte[CHUNK_PLAIN];
            int nextLen = currentLen == CHUNK_PLAIN ? readFully(in, next) : 0;
            boolean last = nextLen == 0;

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, contentKey,
                    new GCMParameterSpec(GCM_TAG_BITS, nonce(prefix, index)));
            cipher.updateAAD(aad(index, last));
            out.write(cipher.doFinal(current, 0, currentLen));
            index++;

            if (last) break;
            current = next;
            currentLen = nextLen;
        }

        out.flush();
        out.close();
        in.close();
        zeroize(prefix);
    }

    /** Decrypt a whole vault content blob into memory. */
    static byte[] decryptFully(InputStream in, SecretKey contentKey)
            throws IOException, GeneralSecurityException {
        try (InputStream stream = in) {
            byte[] prefix = new byte[FILE_PREFIX_LEN];
            if (readFully(stream, prefix) != FILE_PREFIX_LEN) return new byte[0];

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] cipherChunk = new byte[CHUNK_PLAIN + GCM_TAG_LEN];
            long index = 0;
            int got = readFully(stream, cipherChunk);
            while (got > 0) {
                byte[] following = new byte[CHUNK_PLAIN + GCM_TAG_LEN];
                int nextGot = got == cipherChunk.length ? readFully(stream, following) : 0;
                boolean last = nextGot == 0;

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, contentKey,
                        new GCMParameterSpec(GCM_TAG_BITS, nonce(prefix, index)));
                cipher.updateAAD(aad(index, last));
                out.write(cipher.doFinal(cipherChunk, 0, got));
                index++;

                if (last) break;
                cipherChunk = following;
                got = nextGot;
            }
            return out.toByteArray();
        }
    }

    private static int readFully(InputStream in, byte[] buf) throws IOException {
        int total = 0;
        while (total < buf.length) {
            int n = in.read(buf, total, buf.length - total);
            if (n < 0) break;
            total += n;
        }
        return total;
    }

    // --- misc ---------------------------------------------------------

    static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
