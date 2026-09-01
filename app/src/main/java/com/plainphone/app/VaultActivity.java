package com.plainphone.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The vault browser. Locked → {@link PassphraseView} gate (creating the vault on
 * first run). Unlocked → a folder listing with Add / Export / Rename / Delete.
 * Tapping a file opens a built-in in-memory viewer; other types offer Export.
 */
public class VaultActivity extends Activity {

    private static final int REQ_ADD = 7101;
    private static final int REQ_EXPORT = 7102;

    private final Handler main = new Handler(Looper.getMainLooper());

    private FrameLayout root;
    private ListView list;
    private TextView crumb;
    private String currentDocId = VaultStore.ROOT_DOC_ID;
    private final Deque<String> stack = new ArrayDeque<>();
    private List<VaultStore.Entry> entries = new ArrayList<>();
    private String pendingExportDocId;

    static void enableProvider(Context context) {
        context.getPackageManager().setComponentEnabledSetting(
                new android.content.ComponentName(context, VaultDocumentsProvider.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        renderState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (VaultSession.get().isUnlocked()) VaultUnlockService.touch(this);
    }

    private void renderState() {
        root.removeAllViews();
        if (!VaultFormat.exists(VaultSession.vaultRoot(this))) {
            showGate("Create a vault passphrase", true);
        } else if (!VaultSession.get().isUnlocked()) {
            showGate("Unlock the vault", false);
        } else {
            showBrowser();
        }
    }

    // --- gate ------------------------------------------------------

    private void showGate(String prompt, boolean creating) {
        PassphraseView gate = new PassphraseView(this, prompt, creating, new PassphraseView.Listener() {
            @Override
            public void onPassphrase(char[] passphrase) {
                handlePassphrase(passphrase, creating);
            }

            @Override
            public void onCancel() {
                finish();
            }
        });
        root.addView(gate, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void handlePassphrase(char[] passphrase, boolean creating) {
        PassphraseView gate = (PassphraseView) root.getChildAt(0);
        gate.showBusy(true);
        new Thread(() -> {
            String failure = null;
            try {
                if (creating) {
                    VaultFormat.createVault(VaultSession.vaultRoot(this), passphrase);
                    enableProvider(this);
                }
                VaultSession.get().unlock(this, passphrase);
            } catch (VaultFormat.WrongPassphrase e) {
                failure = "Wrong passphrase";
            } catch (Exception e) {
                failure = "Couldn't open vault: " + e.getMessage();
            } finally {
                java.util.Arrays.fill(passphrase, '\0');
            }
            String message = failure;
            main.post(() -> {
                if (message != null) {
                    gate.reject(message);
                    return;
                }
                VaultUnlockService.start(this);
                currentDocId = VaultStore.ROOT_DOC_ID;
                stack.clear();
                renderState();
            });
        }).start();
    }

    // --- browser --------------------------------------------------

    private void showBrowser() {
        Typeface font = Fonts.current(this);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(Color.BLACK);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        crumb = new TextView(this);
        crumb.setTextColor(Color.GRAY);
        crumb.setTextSize(13);
        crumb.setLetterSpacing(0.1f);
        crumb.setTypeface(font);
        crumb.setPadding(48, 36, 24, 12);
        bar.addView(crumb, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView lock = actionText(font, "LOCK");
        lock.setOnClickListener(v -> {
            VaultUnlockService.stop(this);
            VaultSession.get().lock(this);
            renderState();
        });
        bar.addView(lock);

        TextView settings = actionText(font, "•••");
        settings.setOnClickListener(v ->
                startActivity(new Intent(this, VaultAutoLockActivity.class)));
        bar.addView(settings);
        column.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView add = row(font, "+ Add files");
        add.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(pick, REQ_ADD);
        });
        column.addView(add, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView newFolder = row(font, "+ New folder");
        newFolder.setOnClickListener(v -> promptNewFolder());
        column.addView(newFolder, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        list = new ListView(this);
        list.setBackgroundColor(Color.BLACK);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setOnItemClickListener((p, v, pos, id) -> onEntryTapped(entries.get(pos)));
        list.setOnItemLongClickListener((p, v, pos, id) -> {
            showEntryOptions(entries.get(pos));
            return true;
        });
        column.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(column, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        loadListing();
    }

    private void loadListing() {
        VaultUnlockService.touch(this);
        try {
            entries = VaultStore.list(this, currentDocId);
        } catch (Exception e) {
            entries = new ArrayList<>();
        }
        List<String> labels = new ArrayList<>();
        labels.add(stack.isEmpty() ? ".  (root)" : "..  up");
        for (VaultStore.Entry entry : entries) {
            labels.add((entry.isDir ? "[ ] " : "    ") + entry.name
                    + (entry.isDir ? "" : "   " + humanSize(entry.size)));
        }
        crumb.setText(pathLabel());

        Typeface font = Fonts.current(this);
        list.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(position == 0 ? Color.GRAY : Color.WHITE);
                view.setTypeface(font);
                view.setTextSize(17);
                view.setPadding(48, 32, 48, 32);
                view.setBackground(rowBg());
                return view;
            }
        });
        list.setOnItemClickListener((p, v, pos, id) -> {
            if (pos == 0) {
                goUp();
            } else {
                onEntryTapped(entries.get(pos - 1));
            }
        });
        list.setOnItemLongClickListener((p, v, pos, id) -> {
            if (pos == 0) return false;
            showEntryOptions(entries.get(pos - 1));
            return true;
        });
    }

    private String pathLabel() {
        StringBuilder sb = new StringBuilder("VAULT");
        for (String docId : stack) {
            sb.append(" / ").append(shortNameOf(docId));
        }
        if (!currentDocId.equals(VaultStore.ROOT_DOC_ID)) {
            sb.append(" / ").append(shortNameOf(currentDocId));
        }
        return sb.toString();
    }

    private String shortNameOf(String docId) {
        try {
            return VaultStore.stat(this, docId).name;
        } catch (Exception e) {
            return "…";
        }
    }

    private void goUp() {
        if (stack.isEmpty()) {
            finish();
            return;
        }
        currentDocId = stack.pop();
        loadListing();
    }

    @Override
    public void onBackPressed() {
        if (VaultSession.get().isUnlocked() && !stack.isEmpty()) {
            goUp();
        } else {
            super.onBackPressed();
        }
    }

    private void onEntryTapped(VaultStore.Entry entry) {
        VaultUnlockService.touch(this);
        if (entry.isDir) {
            stack.push(currentDocId);
            currentDocId = entry.docId;
            loadListing();
            return;
        }
        String mime = entry.mimeType == null ? "" : entry.mimeType;
        if (mime.startsWith("image/")) {
            open(VaultImageViewerActivity.class, entry);
        } else if (mime.startsWith("text/") || mime.equals("application/json")) {
            open(VaultTextViewerActivity.class, entry);
        } else {
            Toast.makeText(this, "No built-in viewer — long-press to Export", Toast.LENGTH_SHORT).show();
        }
    }

    private void open(Class<?> viewer, VaultStore.Entry entry) {
        Intent intent = new Intent(this, viewer);
        intent.putExtra("docId", entry.docId);
        intent.putExtra("name", entry.name);
        startActivity(intent);
    }

    // --- entry options -------------------------------------------

    private void showEntryOptions(VaultStore.Entry entry) {
        Typeface font = Fonts.current(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.BLACK);
        box.setBackground(bg);
        box.setPadding(0, 8, 0, 8);

        TextView title = new TextView(this);
        title.setText(entry.name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(font);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        title.setPadding(48, 32, 48, 24);
        box.addView(title);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(box).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (!entry.isDir) {
            box.addView(option(font, "Export", v -> {
                dialog.dismiss();
                pendingExportDocId = entry.docId;
                Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(entry.mimeType == null ? "application/octet-stream" : entry.mimeType)
                        .putExtra(Intent.EXTRA_TITLE, entry.name);
                startActivityForResult(create, REQ_EXPORT);
            }));
        }
        box.addView(option(font, "Rename", v -> {
            dialog.dismiss();
            promptRename(entry);
        }));
        box.addView(option(font, "Delete", v -> {
            dialog.dismiss();
            confirmDelete(entry);
        }));

        dialog.show();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private void confirmDelete(VaultStore.Entry entry) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete " + entry.name + "?")
                .setMessage(entry.isDir ? "The folder and everything in it." : null)
                .setPositiveButton("Delete", (d, w) -> {
                    try {
                        VaultStore.delete(this, entry.docId);
                    } catch (Exception e) {
                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                    loadListing();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptRename(VaultStore.Entry entry) {
        EditText input = new EditText(this);
        input.setText(entry.name);
        input.setSelectAllOnFocus(true);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("Rename", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    try {
                        VaultStore.rename(this, entry.docId, name);
                    } catch (Exception e) {
                        Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show();
                    }
                    loadListing();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptNewFolder() {
        EditText input = new EditText(this);
        input.setHint("Folder name");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new android.app.AlertDialog.Builder(this)
                .setTitle("New folder")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    try {
                        VaultStore.createDocument(this, currentDocId, name,
                                android.provider.DocumentsContract.Document.MIME_TYPE_DIR);
                    } catch (Exception e) {
                        Toast.makeText(this, "Couldn't create folder", Toast.LENGTH_SHORT).show();
                    }
                    loadListing();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- SAF results --------------------------------------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQ_ADD) {
            importFrom(data);
        } else if (requestCode == REQ_EXPORT && data.getData() != null) {
            exportTo(data.getData());
        }
    }

    private void importFrom(Intent data) {
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                uris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        if (uris.isEmpty()) return;
        String parent = currentDocId;
        new Thread(() -> {
            int ok = 0;
            for (Uri uri : uris) {
                String name = queryName(uri);
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) continue;
                    VaultStore.importStream(this, parent, name, in);
                    ok++;
                } catch (Exception ignored) {
                }
            }
            int added = ok;
            main.post(() -> {
                Toast.makeText(this, "Added " + added + " file" + (added == 1 ? "" : "s"),
                        Toast.LENGTH_SHORT).show();
                loadListing();
            });
        }).start();
    }

    private void exportTo(Uri dest) {
        String docId = pendingExportDocId;
        pendingExportDocId = null;
        if (docId == null) return;
        new Thread(() -> {
            boolean ok = false;
            try (OutputStream out = getContentResolver().openOutputStream(dest)) {
                if (out != null) {
                    VaultStore.exportStream(this, docId, out);
                    ok = true;
                }
            } catch (Exception ignored) {
            }
            boolean done = ok;
            main.post(() -> Toast.makeText(this, done ? "Exported" : "Export failed",
                    Toast.LENGTH_SHORT).show());
        }).start();
    }

    private String queryName(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(uri,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
        } catch (Exception ignored) {
        }
        String last = uri.getLastPathSegment();
        return last != null ? last : "file";
    }

    // --- small ui helpers --------------------------------------

    private TextView row(Typeface font, String label) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(18);
        view.setTypeface(font);
        view.setPadding(48, 32, 48, 32);
        view.setBackground(rowBg());
        return view;
    }

    private TextView actionText(Typeface font, String label) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.GRAY);
        view.setTextSize(13);
        view.setTypeface(font);
        view.setPadding(24, 36, 32, 12);
        return view;
    }

    private TextView option(Typeface font, String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20);
        view.setTypeface(font);
        view.setPadding(48, 32, 48, 32);
        view.setBackground(rowBg());
        view.setOnClickListener(listener);
        return view;
    }

    private StateListDrawable rowBg() {
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return bg;
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(java.util.Locale.US, "%.1f MB", bytes / 1024f / 1024f);
    }
}
