package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * The document-picker round-trips a home-plugin section needs (import files,
 * pick the todo file / notes folder), shared so a workspace {@link PluginPanel}
 * gets the same behaviour as the home screen. The hosting {@code Activity} routes
 * its {@code onActivityResult} here.
 */
final class SectionImports {

    private SectionImports() {}

    static final int NOTES_IMPORT = 0x5C10;
    static final int TODOS_IMPORT = 0x5C11;
    static final int RECORDER_IMPORT = 0x5C12;
    static final int TODO_FILE = 0x5C13;
    static final int NOTES_FOLDER = 0x5C14;

    static void pickImport(Activity a, HomeMode section, String mime) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(mime)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        a.startActivityForResult(i, importCode(section));
    }

    private static int importCode(HomeMode s) {
        return s == HomeMode.NOTES ? NOTES_IMPORT
                : s == HomeMode.TODOS ? TODOS_IMPORT
                : RECORDER_IMPORT;
    }

    /** @return true if this result was one of ours (and handled). */
    static boolean onResult(Activity a, int req, int res, Intent data, Runnable after) {
        if (res != Activity.RESULT_OK) return req >= NOTES_IMPORT && req <= NOTES_FOLDER;
        switch (req) {
            case NOTES_IMPORT:    startImport(a, HomeMode.NOTES, data); break;
            case TODOS_IMPORT:    startImport(a, HomeMode.TODOS, data); break;
            case RECORDER_IMPORT: startImport(a, HomeMode.RECORDER, data); break;
            case TODO_FILE: {
                String msg = Todos.handleFilePick(a, data);
                if (msg != null) Toast.makeText(a, msg, Toast.LENGTH_SHORT).show();
                break;
            }
            case NOTES_FOLDER:
                Notes.saveFolderPick(a, data);
                break;
            default:
                return false;
        }
        if (after != null) after.run();
        return true;
    }

    private static void startImport(Activity a, HomeMode section, Intent data) {
        List<Uri> uris = urisFrom(data);
        if (uris.isEmpty()) return;
        for (Uri u : uris) {
            try {
                a.getContentResolver().takePersistableUriPermission(
                        u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
            }
        }
        int n = uris.size();
        ImportJobs.start(a, section, uris, n + (n == 1 ? " file" : " files"));
    }

    private static List<Uri> urisFrom(Intent data) {
        List<Uri> out = new ArrayList<>();
        if (data == null) return out;
        android.content.ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri u = clip.getItemAt(i).getUri();
                if (u != null) out.add(u);
            }
        } else if (data.getData() != null) {
            out.add(data.getData());
        }
        return out;
    }
}
