package com.plainphone.app;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Random-access <b>read</b> view over one encrypted content blob — decrypts only
 * the 64 KiB chunks a read actually touches, with a small LRU of recent chunks.
 * Backs {@code openProxyFileDescriptor} so a viewer opening a vault file gets no
 * plaintext temp on disk and an instant open regardless of file size.
 *
 * <p>Read-only: {@code ProxyFileDescriptorCallback} has no truncate, so writable
 * opens still go through the decrypt-to-temp path in {@link VaultDocumentsProvider}.
 */
final class VaultCryptoFile implements java.io.Closeable {

    private static final int MAX_CACHED_CHUNKS = 8;

    private final RandomAccessFile raf;
    private final long blobLen;
    private final SecretKey key;
    private final byte[] prefix;
    private final long plainSize;
    private final int chunkCount;

    private final Map<Integer, byte[]> cache =
            new LinkedHashMap<Integer, byte[]>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, byte[]> eldest) {
                    return size() > MAX_CACHED_CHUNKS;
                }
            };

    static VaultCryptoFile open(File blob, SecretKey contentKey) throws IOException {
        return new VaultCryptoFile(blob, contentKey);
    }

    private VaultCryptoFile(File blob, SecretKey contentKey) throws IOException {
        this.raf = new RandomAccessFile(blob, "r");
        this.blobLen = raf.length();
        this.key = contentKey;
        this.prefix = new byte[VaultCrypto.FILE_PREFIX_LEN];
        if (blobLen < VaultCrypto.FILE_PREFIX_LEN) {
            raf.close();
            throw new IOException("blob too short");
        }
        raf.readFully(prefix);
        this.plainSize = VaultCrypto.plaintextSize(blobLen);
        this.chunkCount = (int) Math.max(1,
                (plainSize + VaultCrypto.CHUNK_PLAIN - 1) / VaultCrypto.CHUNK_PLAIN);
    }

    long size() {
        return plainSize;
    }

    /** Fill {@code out[0..len)} from plaintext {@code [offset, offset+len)}. Returns bytes read. */
    synchronized int read(long offset, int len, byte[] out) throws IOException {
        if (offset < 0 || offset >= plainSize) return 0;
        long end = Math.min(offset + len, plainSize);
        int total = 0;
        long pos = offset;
        while (pos < end) {
            int ci = (int) (pos / VaultCrypto.CHUNK_PLAIN);
            byte[] chunk = chunk(ci);
            int within = (int) (pos % VaultCrypto.CHUNK_PLAIN);
            int n = (int) Math.min(chunk.length - within, end - pos);
            if (n <= 0) break;
            System.arraycopy(chunk, within, out, total, n);
            total += n;
            pos += n;
        }
        return total;
    }

    private byte[] chunk(int index) throws IOException {
        byte[] hit = cache.get(index);
        if (hit != null) return hit;

        long off = (long) VaultCrypto.FILE_PREFIX_LEN
                + (long) index * (VaultCrypto.CHUNK_PLAIN + VaultCrypto.GCM_TAG_LEN);
        int cipherLen = index < chunkCount - 1
                ? VaultCrypto.CHUNK_PLAIN + VaultCrypto.GCM_TAG_LEN
                : (int) (blobLen - off);
        if (cipherLen <= 0) throw new IOException("chunk " + index + " past end");

        byte[] ct = new byte[cipherLen];
        raf.seek(off);
        raf.readFully(ct);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(
                    VaultCrypto.GCM_TAG_BITS, VaultCrypto.chunkNonce(prefix, index)));
            cipher.updateAAD(VaultCrypto.chunkAad(index, index == chunkCount - 1));
            byte[] plain = cipher.doFinal(ct);
            cache.put(index, plain);
            return plain;
        } catch (GeneralSecurityException e) {
            throw new IOException("chunk " + index + " decrypt failed", e);
        }
    }

    @Override
    public synchronized void close() {
        cache.clear();
        try {
            raf.close();
        } catch (IOException ignored) {
        }
    }
}
