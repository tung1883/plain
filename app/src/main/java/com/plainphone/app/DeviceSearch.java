package com.plainphone.app;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class DeviceSearch {

    private DeviceSearch() {}

    static class MediaFile {
        final Uri uri;
        final String mime;

        MediaFile(Uri uri, String mime) {
            this.uri = uri;
            this.mime = mime;
        }
    }

    private static final int MAX_FILES = 8;
    private static final int MAX_CONTACTS = 8;

    static final int REQUEST_FILES = 2001;
    static final int REQUEST_CONTACTS = 2002;

    static void requestFullFileAccess(Activity host) {
        if (Build.VERSION.SDK_INT >= 30) {
            requestAllFilesAccess(host);
        } else {
            host.requestPermissions(
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_FILES);
        }
    }

    static void requestAllFilesAccess(Activity host) {
        if (Build.VERSION.SDK_INT < 30) return;
        Intent settings = new Intent(
                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + host.getPackageName()));
        try {
            host.startActivity(settings);
        } catch (Exception e) {

            host.startActivity(new Intent(
                    android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        }
    }

    static String[] filePermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[]{
                    "android.permission.READ_MEDIA_IMAGES",
                    "android.permission.READ_MEDIA_VIDEO",
                    "android.permission.READ_MEDIA_AUDIO",
            };
        }
        return new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    static boolean canSearchFiles(Context context) {
        for (String permission : filePermissions()) {
            if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    static boolean canSearchContacts(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    static List<SearchResult> files(Activity host, TextMatch.Query query) {
        if (query.empty) return new ArrayList<>();
        if (FileIndex.canWalk(host)) return indexedFiles(host, query);
        if (!canSearchFiles(host)) return new ArrayList<>();
        return mediaStoreFiles(host, query);
    }

    private static List<SearchResult> indexedFiles(Activity host, TextMatch.Query query) {
        List<SearchResult> results = new ArrayList<>();
        for (FileIndex.Scored hit : FileIndex.search(host, query, MAX_FILES)) {
            FileIndex.Entry entry = hit.entry;
            String subtitle = (entry.directory ? "Folder · " : "File · ")
                    + describeLocation(entry.file.getParentFile());
            results.add(new SearchResult(SearchResult.Kind.FILE, entry.name, subtitle,
                    hit.score, () -> open(host, entry.file, entry.directory), entry));
        }
        return results;
    }

    private static List<SearchResult> mediaStoreFiles(Activity host, TextMatch.Query query) {
        String needle = query.raw;
        List<SearchResult> results = new ArrayList<>();
        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
        };

        for (String volume : mediaStoreVolumes(host)) {
            if (results.size() >= MAX_FILES) break;
            Uri collection = MediaStore.Files.getContentUri(volume);

            Cursor cursor = null;
            try {
                cursor = host.getContentResolver().query(
                        collection,
                        projection,
                        MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ? ESCAPE '\\'",
                        new String[]{"%" + escapeLike(needle) + "%"},
                        MediaStore.MediaColumns.DATE_MODIFIED + " DESC");
                if (cursor == null) continue;

                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                int mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);

                while (cursor.moveToNext() && results.size() < MAX_FILES) {
                    String name = cursor.getString(nameColumn);
                    if (TextUtils.isEmpty(name)) continue;

                    long id = cursor.getLong(idColumn);
                    String mime = cursor.getString(mimeColumn);
                    Uri fileUri = ContentUris.withAppendedId(collection, id);

                    results.add(new SearchResult(SearchResult.Kind.FILE, name, describe(mime),
                            TextMatch.score(name, query), () -> openFile(host, fileUri, mime),
                            new MediaFile(fileUri, mime)));
                }
            } catch (Exception ignored) {

            } finally {
                if (cursor != null) cursor.close();
            }
        }
        return results;
    }

    private static List<String> mediaStoreVolumes(Activity host) {
        List<String> volumes = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                volumes.addAll(MediaStore.getExternalVolumeNames(host));
            } catch (Exception ignored) {

            }
        }
        if (volumes.isEmpty()) volumes.add("external");
        return volumes;
    }

    static List<SearchResult> contacts(Activity host, TextMatch.Query query) {
        List<SearchResult> results = new ArrayList<>();
        if (query.empty || !canSearchContacts(host)) return results;

        String needle = query.raw;

        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI,
                Uri.encode(needle));
        String[] projection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
        };

        Cursor cursor = null;
        try {
            cursor = host.getContentResolver().query(uri, projection, null, null,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC");
            if (cursor == null) return results;

            int idColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
            int lookupColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY);
            int nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
            int hasPhoneColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER);

            while (cursor.moveToNext() && results.size() < MAX_CONTACTS) {
                String name = cursor.getString(nameColumn);
                if (TextUtils.isEmpty(name)) continue;

                long id = cursor.getLong(idColumn);
                String lookupKey = cursor.getString(lookupColumn);
                boolean hasPhone = cursor.getInt(hasPhoneColumn) > 0;
                Uri contactUri = ContactsContract.Contacts.getLookupUri(id, lookupKey);

                results.add(new SearchResult(SearchResult.Kind.CONTACT, name,
                        hasPhone ? "Contact · has a phone number" : "Contact",
                        TextMatch.score(name, query),
                        () -> openContact(host, contactUri)));
            }
        } catch (Exception ignored) {

        } finally {
            if (cursor != null) cursor.close();
        }
        return results;
    }

    private static void open(Activity host, File file, boolean directory) {
        if (directory) {
            openFolder(host, file);
            return;
        }
        try {
            Uri uri = PlainFileProvider.uriFor(host.getPackageName() + ".files", file);
            openFile(host, uri, guessMime(file));
        } catch (Exception e) {
            android.widget.Toast.makeText(host, file.getAbsolutePath(),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    static void openWithChooser(Activity host, File file) {
        try {
            Uri uri = PlainFileProvider.uriFor(host.getPackageName() + ".files", file);
            openWithChooser(host, uri, guessMime(file));
        } catch (Exception e) {
            android.widget.Toast.makeText(host, "No app can open this file",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    static void openWithChooser(Activity host, Uri uri, String mime) {
        try {
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, TextUtils.isEmpty(mime) ? "*/*" : mime);
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            host.startActivity(Intent.createChooser(view, "Open with"));
        } catch (Exception e) {
            android.widget.Toast.makeText(host, "No app can open this file",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    static void revealInFileManager(Activity host, File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            android.widget.Toast.makeText(host, file.getAbsolutePath(),
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        openFolder(host, parent);
    }

    private static final String[] FOLDER_MIMES = {"resource/folder", "vnd.android.document/directory", "*/*"};

    private static boolean tryOpen(Activity host, Uri uri, String mime) {
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, mime);
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (view.resolveActivity(host.getPackageManager()) == null) return false;
        try {
            host.startActivity(view);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Uri documentUriFor(Activity host, File directory) {
        if (Build.VERSION.SDK_INT < 30) return null;

        android.os.storage.StorageManager storage =
                host.getSystemService(android.os.storage.StorageManager.class);
        if (storage == null) return null;

        String path = directory.getAbsolutePath();
        for (android.os.storage.StorageVolume volume : storage.getStorageVolumes()) {
            File root = volume.getDirectory();
            if (root == null) continue;
            String rootPath = root.getAbsolutePath();
            if (!path.equals(rootPath) && !path.startsWith(rootPath + "/")) continue;

            String volumeId = volume.isPrimary() ? "primary" : volume.getUuid();
            if (volumeId == null) continue;

            String relative = path.equals(rootPath) ? "" : path.substring(rootPath.length() + 1);
            String documentId = volumeId + ":" + relative;
            return android.provider.DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents", documentId);
        }
        return null;
    }

    private static void copyToClipboard(Activity host, String text) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) host.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Folder path", text));
    }

    private static void openFolder(Activity host, File directory) {

        Uri documentUri = documentUriFor(host, directory);
        if (documentUri != null && tryOpen(host, documentUri, "vnd.android.document/directory")) {
            return;
        }

        for (String mime : FOLDER_MIMES) {
            if (tryOpen(host, Uri.parse(directory.getAbsolutePath()), mime)) return;
        }

        copyToClipboard(host, directory.getAbsolutePath());
        android.widget.Toast.makeText(host,
                "Path copied:\n" + directory.getAbsolutePath(),
                android.widget.Toast.LENGTH_LONG).show();
    }

    private static String guessMime(File file) {
        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(
                Uri.fromFile(file).toString());
        if (TextUtils.isEmpty(extension)) return "*/*";
        String mime = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.toLowerCase());
        return TextUtils.isEmpty(mime) ? "*/*" : mime;
    }

    private static String describeLocation(File parent) {
        if (parent == null) return "Storage";
        String path = parent.getAbsolutePath();
        if (path.startsWith("/storage/emulated/0")) {
            String rest = path.substring("/storage/emulated/0".length());
            return rest.isEmpty() || rest.equals("/") ? "Internal storage" : "Internal" + rest;
        }
        if (path.startsWith("/storage/")) {

            String rest = path.substring("/storage/".length());
            int slash = rest.indexOf('/');
            return slash < 0 ? "SD card" : "SD card/" + rest.substring(slash + 1);
        }
        return path;
    }

    private static void openFile(Activity host, Uri uri, String mime) {
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, TextUtils.isEmpty(mime) ? "*/*" : mime);
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startOrToast(host, view, "No app can open this file");
    }

    private static void openContact(Activity host, Uri uri) {
        startOrToast(host, new Intent(Intent.ACTION_VIEW, uri), "No contacts app to open this");
    }

    private static void startOrToast(Activity host, Intent intent, String failureMessage) {
        try {
            host.startActivity(intent);
        } catch (Exception e) {
            android.widget.Toast.makeText(host, failureMessage,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private static String describe(String mime) {
        if (TextUtils.isEmpty(mime)) return "File";
        int slash = mime.indexOf('/');
        if (slash < 0) return mime;

        String category = mime.substring(0, slash);
        String subtype = mime.substring(slash + 1).toUpperCase();
        String label;
        switch (category) {
            case "image": label = "Image"; break;
            case "video": label = "Video"; break;
            case "audio": label = "Audio"; break;
            case "text": label = "Text"; break;
            default: label = "File"; break;
        }
        return label + " · " + subtype;
    }

    private static String escapeLike(String needle) {
        return needle.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

