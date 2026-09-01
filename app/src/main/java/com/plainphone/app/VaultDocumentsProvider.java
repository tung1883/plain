package com.plainphone.app;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Exposes the unlocked vault as a location ("PlainPhone Vault") in the system
 * file picker. Locked → {@link #queryRoots} returns an empty cursor and the
 * location disappears. Content is decrypted to a no-backup temp on open and
 * re-encrypted when a writable descriptor closes ({@link VaultOpenFiles}).
 */
public class VaultDocumentsProvider extends DocumentsProvider {

    static final String AUTHORITY = "com.plainphone.app.vault";
    private static final String ROOT_ID = "vault";

    private static final String[] DEFAULT_ROOT_COLUMNS = {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID, Root.COLUMN_ICON,
    };
    private static final String[] DEFAULT_DOC_COLUMNS = {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE,
            Document.COLUMN_FLAGS, Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED,
    };

    private HandlerThread callbackThread;
    private Handler callbackHandler;

    @Override
    public boolean onCreate() {
        callbackThread = new HandlerThread("vault-fd");
        callbackThread.start();
        callbackHandler = new Handler(callbackThread.getLooper());
        return true;
    }

    private boolean locked() {
        return !VaultSession.get().isUnlocked();
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_COLUMNS);
        if (locked() || !VaultFormat.exists(VaultSession.vaultRoot(getContext()))) {
            return cursor;   // no rows → the location is not offered
        }
        cursor.newRow()
                .add(Root.COLUMN_ROOT_ID, ROOT_ID)
                .add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_IS_CHILD)
                .add(Root.COLUMN_TITLE, "PlainPhone Vault")
                .add(Root.COLUMN_DOCUMENT_ID, VaultStore.ROOT_DOC_ID)
                .add(Root.COLUMN_ICON, R.mipmap.ic_launcher);
        return cursor;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        requireUnlocked();
        MatrixCursor cursor = new MatrixCursor(projection != null ? projection : DEFAULT_DOC_COLUMNS);
        addRow(cursor, VaultStore.stat(getContext(), documentId));
        return cursor;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection,
                                      String sortOrder) throws FileNotFoundException {
        requireUnlocked();
        MatrixCursor cursor = new MatrixCursor(projection != null ? projection : DEFAULT_DOC_COLUMNS);
        for (VaultStore.Entry entry : VaultStore.list(getContext(), parentDocumentId)) {
            addRow(cursor, entry);
        }
        return cursor;
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        return VaultStore.isChild(parentDocumentId, documentId);
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        requireUnlocked();
        return VaultStore.stat(getContext(), documentId).mimeType;
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName)
            throws FileNotFoundException {
        requireUnlocked();
        try {
            return VaultStore.createDocument(getContext(), parentDocumentId, displayName, mimeType);
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        requireUnlocked();
        VaultStore.delete(getContext(), documentId);
        notifyRoots();
    }

    @Override
    public String renameDocument(String documentId, String displayName)
            throws FileNotFoundException {
        requireUnlocked();
        try {
            String newId = VaultStore.rename(getContext(), documentId, displayName);
            notifyRoots();
            return newId;
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode,
                                             CancellationSignal signal) throws FileNotFoundException {
        requireUnlocked();
        final boolean writable = mode.indexOf('w') != -1;
        File plainTmp = new File(VaultOpenFiles.openDir(getContext()), UUID.randomUUID().toString());
        try {
            VaultStore.decryptToFile(getContext(), documentId, plainTmp);
        } catch (IOException e) {
            plainTmp.delete();
            throw wrap(e);
        }
        VaultOpenFiles.get().track(getContext(), documentId, plainTmp, writable);
        int pfdMode = ParcelFileDescriptor.parseMode(writable ? "rw" : "r");
        try {
            return ParcelFileDescriptor.open(plainTmp, pfdMode, callbackHandler,
                    error -> VaultOpenFiles.get().onClosed(plainTmp));
        } catch (IOException e) {
            VaultOpenFiles.get().onClosed(plainTmp);
            throw wrap(e);
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint,
                                                     CancellationSignal signal)
            throws FileNotFoundException {
        requireUnlocked();
        int target = Math.max(sizeHint.x, sizeHint.y);
        if (target <= 0) target = 256;
        try {
            byte[] plain = VaultStore.decryptToMemory(getContext(), documentId);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(plain, 0, plain.length, bounds);
            int sample = 1;
            int longest = Math.max(bounds.outWidth, bounds.outHeight);
            while (longest / sample > target * 2) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bitmap = BitmapFactory.decodeByteArray(plain, 0, plain.length, opts);
            if (bitmap == null) throw new IOException("decode failed");

            File thumb = new File(VaultOpenFiles.openDir(getContext()),
                    "thumb-" + UUID.randomUUID());
            try (FileOutputStream out = new FileOutputStream(thumb)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            }
            bitmap.recycle();
            VaultOpenFiles.get().track(getContext(), documentId, thumb, false);
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(thumb,
                    ParcelFileDescriptor.MODE_READ_ONLY, callbackHandler,
                    error -> VaultOpenFiles.get().onClosed(thumb));
            return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    // --- helpers ----------------------------------------------------

    private void requireUnlocked() throws FileNotFoundException {
        if (locked()) throw new FileNotFoundException("vault locked");
    }

    private void notifyRoots() {
        getContext().getContentResolver().notifyChange(
                android.provider.DocumentsContract.buildRootsUri(AUTHORITY), null);
    }

    private static FileNotFoundException wrap(IOException e) {
        FileNotFoundException fnf = new FileNotFoundException(e.getMessage());
        fnf.initCause(e);
        return fnf;
    }

    private void addRow(MatrixCursor cursor, VaultStore.Entry entry) {
        int flags = 0;
        if (entry.isDir) {
            flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
        } else {
            flags |= Document.FLAG_SUPPORTS_WRITE;
            if (entry.mimeType != null && entry.mimeType.startsWith("image/")) {
                flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
            }
        }
        flags |= Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_RENAME;
        cursor.newRow()
                .add(Document.COLUMN_DOCUMENT_ID, entry.docId)
                .add(Document.COLUMN_DISPLAY_NAME, entry.name)
                .add(Document.COLUMN_MIME_TYPE, entry.mimeType)
                .add(Document.COLUMN_FLAGS, flags)
                .add(Document.COLUMN_SIZE, entry.isDir ? null : entry.size)
                .add(Document.COLUMN_LAST_MODIFIED, entry.lastModified);
    }
}
