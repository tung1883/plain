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

/**
 * File and contact search, both backed by content providers and both permission-gated.
 * Every query here touches disk, so callers must run these off the main thread; the
 * permission checks alone are cheap and safe to call from anywhere.
 */
class DeviceSearch {

    private DeviceSearch() {}

    /** Enough rows to be useful without burying the other result groups. */
    private static final int MAX_FILES = 8;
    private static final int MAX_CONTACTS = 8;

    static final int REQUEST_FILES = 2001;
    static final int REQUEST_CONTACTS = 2002;

    /**
     * Sends the user to the system's "All files access" toggle, the only way to grant
     * MANAGE_EXTERNAL_STORAGE — it's a special permission with a settings screen of its
     * own rather than a runtime dialog, so there's no requestPermissions() equivalent.
     */
    static void requestAllFilesAccess(Activity host) {
        if (Build.VERSION.SDK_INT < 30) return;
        Intent settings = new Intent(
                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + host.getPackageName()));
        try {
            host.startActivity(settings);
        } catch (Exception e) {
            // Some OEM builds only ship the all-apps list screen, not the per-app one.
            host.startActivity(new Intent(
                    android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        }
    }

    /**
     * Android 13 split storage reads into per-media-type permissions and dropped the broad
     * one, so which permissions to ask for depends on the OS version. On 13+ this means
     * only media files are reachable — documents live behind the picker, which isn't a
     * search surface — so a plain "Files" search finds photos, video, and audio.
     */
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

    /** True once at least one file permission is granted — partial access still searches. */
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

    /**
     * File and folder results, from whichever source the granted permissions allow.
     *
     * <p>With All-files access the filesystem index is used — the only source that can see
     * folders and non-media files (documents, archives, downloads) at all. Without it,
     * MediaStore is the fallback, and only images, video, and audio exist as far as search
     * is concerned.
     */
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
                    hit.score, () -> open(host, entry.file, entry.directory)));
        }
        return results;
    }

    /**
     * MediaStore matches with SQL LIKE against the stored filename, which is both
     * diacritic- and separator-sensitive — so this fallback can't do the folding or fuzzy
     * matching the index does. It searches on the query as typed and re-ranks what comes
     * back; getting the flexible behaviour everywhere is what All-files access buys.
     */
    private static List<SearchResult> mediaStoreFiles(Activity host, TextMatch.Query query) {
        String needle = query.raw;
        List<SearchResult> results = new ArrayList<>();
        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
        };

        // Every mounted volume, not just "external" — that name means primary storage only,
        // so querying it alone silently ignores everything on an SD card.
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

                // Capped by walking the cursor rather than a LIMIT in the sort-order string:
                // that trick isn't part of the provider contract and misbehaves on newer
                // Android, while stopping early costs nothing since rows are lazily fetched.
                while (cursor.moveToNext() && results.size() < MAX_FILES) {
                    String name = cursor.getString(nameColumn);
                    if (TextUtils.isEmpty(name)) continue;

                    long id = cursor.getLong(idColumn);
                    String mime = cursor.getString(mimeColumn);
                    Uri fileUri = ContentUris.withAppendedId(collection, id);

                    results.add(new SearchResult(SearchResult.Kind.FILE, name, describe(mime),
                            TextMatch.score(name, query), () -> openFile(host, fileUri, mime)));
                }
            } catch (Exception ignored) {
                // A revoked permission or an OEM provider that rejects the query shouldn't
                // take the whole search down — other volumes and groups still count.
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
                // Fall through to the legacy single-volume name below.
            }
        }
        if (volumes.isEmpty()) volumes.add("external");
        return volumes;
    }

    static List<SearchResult> contacts(Activity host, TextMatch.Query query) {
        List<SearchResult> results = new ArrayList<>();
        if (query.empty || !canSearchContacts(host)) return results;
        // The contacts provider does its own name normalization, so it's given the query as
        // typed rather than the folded form.
        String needle = query.raw;

        // CONTENT_FILTER_URI does the name matching in the provider, which also picks up
        // matches a raw LIKE on the display name would miss (nicknames, organizations,
        // and the provider's own normalization of accents).
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
            // Same reasoning as files: degrade to no contact results, not a broken search.
        } finally {
            if (cursor != null) cursor.close();
        }
        return results;
    }

    /**
     * Opens an indexed path. A raw file:// Uri would trip FileUriExposedException, so the
     * handoff goes through a content:// Uri from this app's FileProvider — the receiving
     * app gets read access to that one file and nothing else.
     *
     * <p>Folders have no such universal mechanism: Android defines no standard intent for
     * "open this directory", and which of the conventional MIME types works depends
     * entirely on which file manager is installed, hence trying several and telling the
     * user the path when none of them stick.
     */
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

    private static void openFolder(Activity host, File directory) {
        String[] folderMimes = {"resource/folder", "vnd.android.document/directory", "*/*"};
        for (String mime : folderMimes) {
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(Uri.parse(directory.getAbsolutePath()), mime);
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (view.resolveActivity(host.getPackageManager()) == null) continue;
            try {
                host.startActivity(view);
                return;
            } catch (Exception ignored) {
                // Try the next convention.
            }
        }
        // No file manager took it — the path itself is still the useful answer, since
        // knowing where a folder lives is usually why it was searched for.
        android.widget.Toast.makeText(host, directory.getAbsolutePath(),
                android.widget.Toast.LENGTH_LONG).show();
    }

    /** MIME type from the file extension, since an indexed file carries no provider metadata. */
    private static String guessMime(File file) {
        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(
                Uri.fromFile(file).toString());
        if (TextUtils.isEmpty(extension)) return "*/*";
        String mime = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.toLowerCase());
        return TextUtils.isEmpty(mime) ? "*/*" : mime;
    }

    /** Where a result lives, with the unhelpful storage prefixes traded for readable names. */
    private static String describeLocation(File parent) {
        if (parent == null) return "Storage";
        String path = parent.getAbsolutePath();
        if (path.startsWith("/storage/emulated/0")) {
            String rest = path.substring("/storage/emulated/0".length());
            return rest.isEmpty() || rest.equals("/") ? "Internal storage" : "Internal" + rest;
        }
        if (path.startsWith("/storage/")) {
            // Anything else mounted under /storage is a removable card, named by its
            // volume id — "6364-3862/DCIM" means nothing, "SD card/DCIM" does.
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

    /** Human-readable file kind from a MIME type, e.g. "image/jpeg" to "Image · JPEG". */
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

    /**
     * LIKE treats % and _ as wildcards, so a query containing either would otherwise match
     * far more than the user typed. \ is the escape character declared by the ESCAPE clause
     * callers pair with this, and has to be escaped first so it doesn't double-escape the
     * backslashes this method itself adds.
     */
    private static String escapeLike(String needle) {
        return needle.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
