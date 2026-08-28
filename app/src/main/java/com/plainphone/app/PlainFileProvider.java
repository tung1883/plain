package com.plainphone.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;

public class PlainFileProvider extends ContentProvider {

    static Uri uriFor(String authority, File file) {
        return new Uri.Builder()
                .scheme("content")
                .authority(authority)
                .appendPath(file.getAbsolutePath())
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private File fileFrom(Uri uri) throws FileNotFoundException {
        String path = uri.getPath();
        if (TextUtils.isEmpty(path)) throw new FileNotFoundException("No path in " + uri);

        File file = new File(path);
        if (!file.isFile()) throw new FileNotFoundException("Not a file: " + path);
        return file;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {

        return ParcelFileDescriptor.open(fileFrom(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (TextUtils.isEmpty(extension)) return "application/octet-stream";
        String mime = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.toLowerCase());
        return TextUtils.isEmpty(mime) ? "application/octet-stream" : mime;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        File file;
        try {
            file = fileFrom(uri);
        } catch (FileNotFoundException e) {
            return null;
        }

        String[] columns = projection != null ? projection
                : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String column : columns) {
            if (OpenableColumns.DISPLAY_NAME.equals(column)) {
                row.add(file.getName());
            } else if (OpenableColumns.SIZE.equals(column)) {
                row.add(file.length());
            } else {
                row.add(null);
            }
        }
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }
}

