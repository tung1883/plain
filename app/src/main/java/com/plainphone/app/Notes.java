package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

/** Shared note actions used by both the home screen and the editor. */
class Notes {

    private Notes() {}

    static void confirmDelete(Activity host, Note note, Runnable after) {
        Typeface font = Fonts.current(host);

        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 32, 0, 8);

        TextView title = new TextView(host);
        title.setText("Delete this note?");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(font);
        title.setPadding(48, 0, 48, 8);
        root.addView(title);

        TextView body = new TextView(host);
        body.setText(note.title());
        body.setTextColor(Color.GRAY);
        body.setTextSize(14);
        body.setTypeface(font);
        body.setSingleLine(true);
        body.setEllipsize(android.text.TextUtils.TruncateAt.END);
        body.setPadding(48, 0, 48, 16);
        root.addView(body);

        AlertDialog dialog = new AlertDialog.Builder(host).setView(root).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        root.addView(optionRow(host, font, "Delete", v -> {
            dialog.dismiss();
            List<Note> notes = Config.getNotes(host);
            Iterator<Note> it = notes.iterator();
            while (it.hasNext()) {
                if (it.next().id.equals(note.id)) it.remove();
            }
            Config.setNotes(host, notes);
            if (after != null) after.run();
        }));
        root.addView(optionRow(host, font, "Cancel", v -> dialog.dismiss()));

        dialog.show();
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (host.getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    static String exportBaseName(Note note) {
        String base = note.title()
                .replaceAll("[^A-Za-z0-9-_ ]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .toLowerCase();
        return base.isEmpty() ? "note" : base;
    }

    static final String EXPORT_MIME = "text/markdown";

    static String exportFileName(Note note) {
        return exportBaseName(note) + ".md";
    }

    // --- default export folder ---------------------------------------------

    static boolean hasExportFolder(Context context) {
        return Config.getNotesExportTree(context) != null;
    }

    static String exportFolderLabel(Context context) {
        String stored = Config.getNotesExportTree(context);
        if (stored == null) return "Not set";
        try {
            String docId = DocumentsContract.getTreeDocumentId(Uri.parse(stored));
            String tail = docId.contains(":") ? docId.substring(docId.indexOf(':') + 1) : docId;
            if (tail.isEmpty()) return "Root";
            int slash = tail.lastIndexOf('/');
            return slash >= 0 ? tail.substring(slash + 1) : tail;
        } catch (Exception e) {
            return "Folder";
        }
    }

    static void pickExportFolder(Activity host, int requestCode) {
        host.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), requestCode);
    }

    static void saveFolderPick(Context context, Intent data) {
        if (data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            context.getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }
        Config.setNotesExportTree(context, uri.toString());
    }

    static void showFolderOptions(Activity host, int pickRequestCode, Runnable after) {
        Typeface font = Fonts.current(host);
        boolean set = hasExportFolder(host);

        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 32, 0, 8);

        TextView title = new TextView(host);
        title.setText("Default export folder");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(font);
        title.setPadding(48, 0, 48, 16);
        root.addView(title);

        AlertDialog dialog = new AlertDialog.Builder(host).setView(root).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        root.addView(optionRow(host, font, set ? "Change folder" : "Choose folder", v -> {
            dialog.dismiss();
            pickExportFolder(host, pickRequestCode);
        }));
        if (set) {
            root.addView(optionRow(host, font, "Clear folder", v -> {
                dialog.dismiss();
                Config.setNotesExportTree(host, null);
                if (after != null) after.run();
            }));
        }
        root.addView(optionRow(host, font, "Cancel", v -> dialog.dismiss()));

        dialog.show();
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (host.getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    static boolean exportToTree(Context context, Note note) {
        String stored = Config.getNotesExportTree(context);
        if (stored == null) return false;
        try {
            Uri tree = Uri.parse(stored);
            Uri parent = DocumentsContract.buildDocumentUriUsingTree(
                    tree, DocumentsContract.getTreeDocumentId(tree));
            Uri child = DocumentsContract.createDocument(
                    context.getContentResolver(), parent, EXPORT_MIME, exportBaseName(note));
            if (child == null) return false;
            try (OutputStream os = context.getContentResolver().openOutputStream(child, "w")) {
                if (os == null) return false;
                os.write(exportText(note).getBytes(StandardCharsets.UTF_8));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static void startCreateDocument(Activity host, Note note, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(EXPORT_MIME);
        intent.putExtra(Intent.EXTRA_TITLE, exportFileName(note));
        host.startActivityForResult(intent, requestCode);
    }

    static boolean sendToApp(Activity host, Note note) {
        try {
            File file = new File(host.getCacheDir(), exportFileName(note));
            try (OutputStream os = new java.io.FileOutputStream(file)) {
                os.write(exportText(note).getBytes(StandardCharsets.UTF_8));
            }
            Uri uri = PlainFileProvider.uriFor(host.getPackageName() + ".files", file);

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType(EXPORT_MIME);
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, note.title().trim());
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            host.startActivity(Intent.createChooser(send, "Send note"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static void showExportOptions(Activity host, Note note, int createDocRequestCode) {
        Typeface font = Fonts.current(host);

        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 32, 0, 8);

        TextView title = new TextView(host);
        title.setText("Export note");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(font);
        title.setPadding(48, 0, 48, 16);
        root.addView(title);

        AlertDialog dialog = new AlertDialog.Builder(host).setView(root).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (hasExportFolder(host)) {
            root.addView(optionRow(host, font, "Export to " + exportFolderLabel(host), v -> {
                dialog.dismiss();
                boolean ok = exportToTree(host, note);
                Toast.makeText(host, ok ? "Exported" : "Export failed", Toast.LENGTH_SHORT).show();
            }));
        }
        root.addView(optionRow(host, font, "Choose location…", v -> {
            dialog.dismiss();
            startCreateDocument(host, note, createDocRequestCode);
        }));
        root.addView(optionRow(host, font, "Send to app…", v -> {
            dialog.dismiss();
            if (!sendToApp(host, note)) {
                Toast.makeText(host, "Export failed", Toast.LENGTH_SHORT).show();
            }
        }));
        root.addView(optionRow(host, font, "Cancel", v -> dialog.dismiss()));

        dialog.show();
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (host.getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    static String exportText(Note note) {
        return "# " + note.title().trim() + "\n\n" + note.text;
    }

    static boolean writeNote(Context context, Uri uri, Note note) {
        try (OutputStream os = context.getContentResolver().openOutputStream(uri, "w")) {
            if (os == null) return false;
            os.write(exportText(note).getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static TextView optionRow(Context context, Typeface font, String label,
                                      View.OnClickListener listener) {
        TextView row = new TextView(context);
        row.setText(label);
        row.setTextColor(Color.WHITE);
        row.setTextSize(20);
        row.setTypeface(font);
        row.setPadding(48, 32, 48, 32);
        row.setGravity(Gravity.START);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.setOnClickListener(listener);
        return row;
    }

    private static GradientDrawable popupBackground() {
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.BLACK);
        return box;
    }
}
