package com.plainphone.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The vault browser. Locked → {@link PassphraseView} gate (creating the vault on
 * first run). Unlocked → a folder listing with image thumbnails, recursive
 * search, multi-select (move / delete / export), sort, and the built-in viewers.
 */
public class VaultActivity extends Activity {

    private static final int REQ_ADD = 7101;
    private static final int REQ_EXPORT = 7102;
    private static final int REQ_IMPORT_TREE = 7103;
    private static final int REQ_EXPORT_TREE = 7104;

    private final Handler main = new Handler(Looper.getMainLooper());

    private FrameLayout root;
    private ListView list;
    private EntryAdapter adapter;
    private TextView crumb;
    private LinearLayout actionRows;
    private LinearLayout selectionBar;
    private EditText searchField;
    private View busyOverlay;
    private TextView busyLabel;

    private String currentDocId = VaultStore.ROOT_DOC_ID;
    private final Deque<String> stack = new ArrayDeque<>();
    private List<VaultStore.Entry> entries = new ArrayList<>();
    private List<VaultStore.Entry> searchHits = new ArrayList<>();
    private String query = "";

    private boolean selecting;
    private final Set<String> selected = new LinkedHashSet<>();

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
        VaultSession.get().addListener(lockListener);
        if (VaultSession.get().isUnlocked()) {
            VaultUnlockService.touch(this);
            if (list != null && query.isEmpty()) loadListing();   // pick up outside edits
        } else if (VaultFormat.exists(VaultSession.vaultRoot(this))
                && root.getChildCount() > 0 && !(root.getChildAt(0) instanceof PassphraseView)) {
            finish();   // locked out from under a live browser
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        VaultSession.get().removeListener(lockListener);
    }

    private void renderState() {
        root.removeAllViews();
        busyOverlay = null;
        if (!VaultFormat.exists(VaultSession.vaultRoot(this))) {
            showGate("Choose a vault password", true);
        } else if (!VaultSession.get().isUnlocked()) {
            showGate("Enter your vault password", false);
        } else {
            applyStartDocId();
            showBrowser();
        }
    }

    private void applyStartDocId() {
        String start = getIntent().getStringExtra("startDocId");
        getIntent().removeExtra("startDocId");
        if (start == null || start.equals(VaultStore.ROOT_DOC_ID)) return;
        try {
            VaultStore.stat(this, start);   // validate it exists
        } catch (Exception e) {
            return;
        }
        stack.clear();
        // startDocId = d/a/b  → push d, d/a ; current = d/a/b
        String[] parts = start.split("/");
        StringBuilder acc = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            stack.push(acc.toString());
            acc.append('/').append(parts[i]);
        }
        currentDocId = start;
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
        VaultCrypto.Progress progress = (done, total) ->
                main.post(() -> gate.setProgress(done, total));
        new Thread(() -> {
            boolean wrong = false;
            String otherError = null;
            try {
                if (creating) {
                    VaultFormat.createVault(VaultSession.vaultRoot(this), passphrase, progress);
                    enableProvider(this);
                }
                VaultSession.get().unlock(this, passphrase, creating ? null : progress);
            } catch (VaultFormat.WrongPassphrase e) {
                wrong = true;
            } catch (Exception e) {
                otherError = "Couldn't open vault: " + e.getMessage();
            } finally {
                java.util.Arrays.fill(passphrase, '\0');
            }
            boolean isWrong = wrong;
            String message = otherError;
            main.post(() -> {
                if (isWrong) {
                    gate.onAttemptFailed();
                    return;
                }
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

    private void confirmLock() {
        VaultUi.confirm(this, "Lock the vault?", null,
                "Lock", this::lockAndClose, "Cancel", null);
    }

    private void lockAndClose() {
        showBusy("Locking…");
        new Thread(() -> {
            VaultUnlockService.stop(this);
            VaultSession.get().lock(this);
            main.post(this::finish);
        }).start();
    }

    private final VaultSession.Listener lockListener = () -> {
        if (!VaultSession.get().isUnlocked()) main.post(this::finish);
    };

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
        crumb.setSingleLine(true);
        crumb.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        crumb.setPadding(48, 36, 24, 12);
        bar.addView(crumb, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        bar.addView(actionText(font, "SORT", v -> showSortMenu()));
        bar.addView(actionText(font, "LOCK", v -> confirmLock()));
        bar.addView(actionText(font, "•••", v ->
                startActivity(new Intent(this, VaultSettingsActivity.class))));
        column.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        searchField = new EditText(this);
        searchField.setHint("Search vault");
        searchField.setHintTextColor(Color.GRAY);
        searchField.setTextColor(Color.WHITE);
        searchField.setTypeface(font);
        searchField.setTextSize(15);
        searchField.setSingleLine(true);
        searchField.setBackgroundColor(Color.BLACK);
        searchField.setPadding(48, 20, 48, 20);
        searchField.setInputType(InputType.TYPE_CLASS_TEXT);
        searchField.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {
                onQueryChanged(s.toString().trim());
            }
        });
        column.addView(searchField, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        actionRows = new LinearLayout(this);
        actionRows.setOrientation(LinearLayout.VERTICAL);
        actionRows.addView(actionRow(font, "+ Add files", v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(pick, REQ_ADD);
        }));
        actionRows.addView(actionRow(font, "+ New file", v -> promptNewFile()));
        actionRows.addView(actionRow(font, "+ New folder", v -> promptNewFolder()));
        actionRows.addView(actionRow(font, "+ Import folder", v -> startActivityForResult(
                new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQ_IMPORT_TREE)));
        column.addView(actionRows, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        selectionBar = new LinearLayout(this);
        selectionBar.setOrientation(LinearLayout.HORIZONTAL);
        selectionBar.setGravity(Gravity.CENTER_VERTICAL);
        selectionBar.setVisibility(View.GONE);
        column.addView(selectionBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        list = new ListView(this);
        list.setBackgroundColor(Color.BLACK);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        adapter = new EntryAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> onRowTapped(pos));
        list.setOnItemLongClickListener((p, v, pos, id) -> onRowLongPressed(pos));
        column.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(column, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        loadListing();
    }

    private void onQueryChanged(String q) {
        query = q;
        if (!q.isEmpty()) {
            exitSelection();
            searchHits = VaultStore.searchAll(this, q);
        }
        refreshChrome();
        adapter.notifyDataSetChanged();
    }

    private void loadListing() {
        VaultUnlockService.touch(this);
        try {
            entries = VaultStore.list(this, currentDocId);
            VaultStore.sort(entries, Config.getVaultSort(this), Config.isVaultSortDesc(this));
        } catch (Exception e) {
            entries = new ArrayList<>();
        }
        refreshChrome();
        adapter.notifyDataSetChanged();
    }

    private void refreshChrome() {
        crumb.setText(headerLabel());
        boolean browsing = query.isEmpty() && !selecting;
        actionRows.setVisibility(browsing ? View.VISIBLE : View.GONE);
        selectionBar.setVisibility(selecting ? View.VISIBLE : View.GONE);
        if (selecting) rebuildSelectionBar();
    }

    private String headerLabel() {
        if (!query.isEmpty()) return "SEARCH";
        if (currentDocId.equals(VaultStore.ROOT_DOC_ID)) return "VAULT";
        return shortNameOf(currentDocId).toUpperCase();
    }

    private String shortNameOf(String docId) {
        try {
            return VaultStore.stat(this, docId).name;
        } catch (Exception e) {
            return "…";
        }
    }

    // --- list model ---------------------------------------------

    private List<VaultStore.Entry> shown() {
        return query.isEmpty() ? entries : searchHits;
    }

    private boolean hasUpRow() {
        return query.isEmpty() && !stack.isEmpty();
    }

    private void onRowTapped(int pos) {
        VaultUnlockService.touch(this);
        if (hasUpRow() && pos == 0) {
            goUp();
            return;
        }
        VaultStore.Entry entry = shown().get(pos - (hasUpRow() ? 1 : 0));
        if (selecting) {
            toggleSelected(entry.docId);
            return;
        }
        if (entry.isDir) {
            if (!query.isEmpty()) {
                searchField.setText("");
            }
            stack.push(currentDocId);
            currentDocId = entry.docId;
            loadListing();
            return;
        }
        openViewer(entry);
    }

    private boolean onRowLongPressed(int pos) {
        if (hasUpRow() && pos == 0) return false;
        VaultStore.Entry entry = shown().get(pos - (hasUpRow() ? 1 : 0));
        if (!selecting) {
            selecting = true;
            selected.clear();
        }
        toggleSelected(entry.docId);
        refreshChrome();
        return true;
    }

    private void toggleSelected(String docId) {
        if (!selected.remove(docId)) selected.add(docId);
        rebuildSelectionBar();
        adapter.notifyDataSetChanged();
    }

    private void toggleSelectAll() {
        boolean allSelected = !shown().isEmpty();
        for (VaultStore.Entry e : shown()) {
            if (!selected.contains(e.docId)) {
                allSelected = false;
                break;
            }
        }
        selected.clear();
        if (!allSelected) {
            for (VaultStore.Entry e : shown()) selected.add(e.docId);
        }
        rebuildSelectionBar();
        adapter.notifyDataSetChanged();
    }

    private void exitSelection() {
        selecting = false;
        selected.clear();
        refreshChrome();
        if (adapter != null) adapter.notifyDataSetChanged();
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
        if (busyOverlay != null) return;
        if (selecting) {
            exitSelection();
        } else if (!query.isEmpty()) {
            searchField.setText("");
        } else if (VaultSession.get().isUnlocked() && !stack.isEmpty()) {
            goUp();
        } else {
            super.onBackPressed();
        }
    }

    private void openViewer(VaultStore.Entry entry) {
        String mime = entry.mimeType == null ? "" : entry.mimeType;
        Class<?> viewer = mime.startsWith("image/")
                ? VaultImageViewerActivity.class
                : VaultTextViewerActivity.class;   // everything else → text
        Intent intent = new Intent(this, viewer);
        intent.putExtra("docId", entry.docId);
        intent.putExtra("name", entry.name);
        startActivity(intent);
    }

    // --- selection actions -------------------------------------

    private void rebuildSelectionBar() {
        Typeface font = Fonts.current(this);
        selectionBar.removeAllViews();

        TextView count = new TextView(this);
        count.setText(selected.size() + " selected");
        count.setTextColor(Color.WHITE);
        count.setTextSize(13);
        count.setTypeface(font);
        count.setPadding(48, 36, 8, 12);
        selectionBar.addView(count, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        selectionBar.addView(actionText(font, "ALL", v -> toggleSelectAll()));
        selectionBar.addView(actionText(font, "MOVE", v -> {
            if (!selected.isEmpty()) pickMoveDestination();
        }));
        selectionBar.addView(actionText(font, "EXPORT", v -> {
            if (!selected.isEmpty()) {
                startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQ_EXPORT_TREE);
            }
        }));
        selectionBar.addView(actionText(font, "DEL", v -> {
            if (!selected.isEmpty()) confirmDeleteSelected();
        }));
        selectionBar.addView(actionText(font, "✕", v -> exitSelection()));
    }

    private void confirmDeleteSelected() {
        List<String> ids = new ArrayList<>(selected);
        confirm("Delete " + ids.size() + " item(s)?", null, "Delete", () -> {
            for (String id : ids) {
                try {
                    VaultStore.delete(this, id);
                } catch (Exception ignored) {
                }
            }
            exitSelection();
            loadListing();
        });
    }

    private void pickMoveDestination() {
        pickMoveDestination(new ArrayList<>(selected));
    }

    private LinearLayout moveSelectedRow;
    private String moveSelectedDocId;
    private TextView moveFooter;

    private void pickMoveDestination(List<String> ids) {
        Typeface font = Fonts.current(this);
        moveSelectedRow = null;
        moveSelectedDocId = null;

        LinearLayout box = popupBox();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("MOVE " + ids.size() + " ITEM" + (ids.size() == 1 ? "" : "S") + " TO…");
        title.setTextColor(Color.GRAY);
        title.setTextSize(13);
        title.setLetterSpacing(0.12f);
        title.setTypeface(font);
        title.setPadding(48, 8, 24, 16);
        header.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(header);
        box.addView(hairline());

        android.widget.ScrollView scroller = new android.widget.ScrollView(this);
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(options);
        box.addView(scroller, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(360)));

        android.app.AlertDialog dialog = popupDialog(box);

        // disabled targets: the items themselves, their descendants, and the
        // folder they already sit in ("— here").
        options.addView(moveRow(font, "Vault", VaultStore.ROOT_DOC_ID, 0,
                currentDocId.equals(VaultStore.ROOT_DOC_ID) ? RowState.CURRENT : RowState.NORMAL));
        for (VaultStore.Entry folder : VaultStore.allFolders(this)) {
            int depth = folder.docId.split("/").length - 1;
            RowState state;
            if (folder.docId.equals(currentDocId)) {
                state = RowState.CURRENT;
            } else if (isBlocked(folder.docId, ids) || ids.contains(folder.docId)) {
                state = RowState.DISABLED;
            } else {
                state = RowState.NORMAL;
            }
            options.addView(moveRow(font, folder.name, folder.docId, depth, state));
        }

        box.addView(hairline());
        moveFooter = new TextView(this);
        moveFooter.setTextColor(Color.DKGRAY);
        moveFooter.setTextSize(15);
        moveFooter.setTypeface(font);
        moveFooter.setText("Select a folder");
        moveFooter.setPadding(48, 30, 48, 32);
        moveFooter.setBackground(rowBg());
        moveFooter.setOnClickListener(v -> {
            if (moveSelectedDocId == null) return;
            dialog.dismiss();
            doMove(ids, moveSelectedDocId);
        });
        box.addView(moveFooter);

        showPopup(dialog);
    }

    private enum RowState { NORMAL, CURRENT, DISABLED }

    private boolean isBlocked(String docId, List<String> ids) {
        for (String id : ids) {
            if (docId.startsWith(id + "/")) return true;
        }
        return false;
    }

    private LinearLayout moveRow(Typeface font, String name, String docId, int depth,
                                 RowState state) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(rowBg());

        View bar = new View(this);
        bar.setBackgroundColor(Color.TRANSPARENT);
        row.addView(bar, new LinearLayout.LayoutParams(dp(3), LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(18) + depth * dp(26), dp(15), dp(20), dp(15));

        int color = state == RowState.NORMAL ? Color.WHITE
                : state == RowState.CURRENT ? 0xFF6E6E6E : 0xFF3D3D3D;
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(MiniIcons.folder(dp(16), color));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(16), dp(16));
        iconLp.rightMargin = dp(14);
        content.addView(icon, iconLp);

        TextView label = new TextView(this);
        label.setText(state == RowState.CURRENT ? name + "  (here)" : name);
        label.setTextColor(color);
        label.setTextSize(15);
        label.setTypeface(font);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        content.addView(label);

        row.addView(content, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));

        if (state == RowState.NORMAL) {
            row.setOnClickListener(v -> selectMoveRow(font, row, bar, label, docId));
        }
        row.setLayoutParams(rowLp);
        return row;
    }

    private void selectMoveRow(Typeface font, LinearLayout row, View bar, TextView label,
                               String docId) {
        if (moveSelectedRow != null && moveSelectedRow != row) {
            moveSelectedRow.setBackground(rowBg());
            View prevBar = moveSelectedRow.getChildAt(0);
            prevBar.setBackgroundColor(Color.TRANSPARENT);
            View prevContent = moveSelectedRow.getChildAt(1);
            ((TextView) ((LinearLayout) prevContent).getChildAt(1)).setTypeface(font, Typeface.NORMAL);
        }
        moveSelectedRow = row;
        moveSelectedDocId = docId;
        row.setBackgroundColor(0xFF161616);
        bar.setBackgroundColor(Color.WHITE);
        label.setTypeface(font, Typeface.BOLD);

        String name = docId.equals(VaultStore.ROOT_DOC_ID) ? "Vault" : shortNameOf(docId);
        moveFooter.setText("Move to " + name);
        moveFooter.setTextColor(Color.WHITE);
    }

    private View hairline() {
        View line = new View(this);
        line.setBackgroundColor(0xFF222222);
        line.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return line;
    }

    private void doMove(List<String> ids, String destParent) {
        int ok = 0;
        for (String id : ids) {
            try {
                VaultStore.move(this, id, destParent);
                ok++;
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(this, "Moved " + ok + " item(s)", Toast.LENGTH_SHORT).show();
        exitSelection();
        loadListing();
    }

    private void exportSelectedInto(Uri treeUri) {
        List<String> ids = new ArrayList<>(selected);
        exitSelection();
        showBusy("Exporting…");
        new Thread(() -> {
            String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId);
            int ok = 0;
            for (String id : ids) {
                try {
                    VaultStore.Entry entry = VaultStore.stat(this, id);
                    if (entry.isDir) continue;
                    Uri fileUri = DocumentsContract.createDocument(getContentResolver(), dirUri,
                            entry.mimeType == null ? "application/octet-stream" : entry.mimeType,
                            entry.name);
                    if (fileUri == null) continue;
                    try (OutputStream out = getContentResolver().openOutputStream(fileUri)) {
                        if (out != null) {
                            VaultStore.exportStream(this, id, out);
                            ok++;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            int done = ok;
            main.post(() -> {
                hideBusy();
                Toast.makeText(this, "Exported " + done + " file(s)", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    // --- single-entry options (long-press disabled in favour of select;
    //     kept reachable via a tap on a selected single item's row? no —
    //     rename/single-delete live here, opened from the ••• of one item) ---

    private void showEntryOptions(VaultStore.Entry entry) {
        Typeface font = Fonts.current(this);
        LinearLayout box = popupBox();
        box.addView(popupTitle(font, entry.name));
        android.app.AlertDialog dialog = popupDialog(box);

        if (!entry.isDir) {
            box.addView(option(font, "Export", v -> {
                dialog.dismiss();
                pendingExportDocId = entry.docId;
                startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(entry.mimeType == null ? "application/octet-stream" : entry.mimeType)
                        .putExtra(Intent.EXTRA_TITLE, entry.name), REQ_EXPORT);
            }));
        }
        box.addView(option(font, "Move", v -> {
            dialog.dismiss();
            List<String> one = new ArrayList<>();
            one.add(entry.docId);
            pickMoveDestination(one);
        }));
        box.addView(option(font, "Rename", v -> {
            dialog.dismiss();
            promptText("Rename", entry.name, "Untitled", "Rename", name -> {
                try {
                    VaultStore.rename(this, entry.docId, name.isEmpty() ? "Untitled" : name);
                } catch (Exception e) {
                    Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show();
                }
                loadListing();
            });
        }));
        box.addView(option(font, "Delete", v -> {
            dialog.dismiss();
            confirm("Delete " + entry.name + "?", null, "Delete", () -> {
                try {
                    VaultStore.delete(this, entry.docId);
                } catch (Exception e) {
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                }
                loadListing();
            });
        }));
        showPopup(dialog);
    }

    private void promptNewFile() {
        promptText("New file", "", "Untitled", "Create", name -> {
            String finalName = name.isEmpty() ? "Untitled" : name;
            try {
                VaultStore.createDocument(this, currentDocId, finalName, VaultStore.mimeOf(finalName));
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't create file", Toast.LENGTH_SHORT).show();
            }
            loadListing();
        });
    }

    private void promptNewFolder() {
        promptText("New folder", "", "New Folder", "Create", name -> {
            String finalName = name.isEmpty() ? "New Folder" : name;
            try {
                VaultStore.createDocument(this, currentDocId, finalName,
                        DocumentsContract.Document.MIME_TYPE_DIR);
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't create folder", Toast.LENGTH_SHORT).show();
            }
            loadListing();
        });
    }

    private void showSortMenu() {
        Typeface font = Fonts.current(this);
        LinearLayout box = popupBox();
        box.addView(popupTitle(font, "Sort by"));
        android.app.AlertDialog dialog = popupDialog(box);
        String current = Config.getVaultSort(this);
        boolean desc = Config.isVaultSortDesc(this);
        String[][] opts = {{"name", "Name"}, {"size", "Size"}, {"date", "Date modified"}};
        for (String[] o : opts) {
            String key = o[0];
            String mark = key.equals(current) ? (desc ? "  ↓" : "  ↑") : "";
            box.addView(option(font, o[1] + mark, v -> {
                dialog.dismiss();
                if (key.equals(current)) {
                    Config.setVaultSortDesc(this, !desc);
                } else {
                    Config.setVaultSort(this, key);
                    Config.setVaultSortDesc(this, false);
                }
                loadListing();
            }));
        }
        showPopup(dialog);
    }

    // --- SAF results --------------------------------------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQ_ADD) importFrom(data);
        else if (requestCode == REQ_EXPORT && data.getData() != null) exportTo(data.getData());
        else if (requestCode == REQ_IMPORT_TREE && data.getData() != null) importTree(data.getData());
        else if (requestCode == REQ_EXPORT_TREE && data.getData() != null) {
            exportSelectedInto(data.getData());
        }
    }

    private void importTree(Uri treeUri) {
        try {
            getContentResolver().takePersistableUriPermission(treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }
        String dest = currentDocId;
        showBusy("Importing…");
        new Thread(() -> {
            VaultImport.Result result = VaultImport.run(this, treeUri, dest,
                    (fd, ft, bd, bt) -> main.post(() -> {
                        int pct = ft > 0 ? (int) (100L * fd / ft) : 0;
                        String msg = pct + "%   " + fd + "/" + ft + " files";
                        if (bt > 0) msg += "   " + humanSize(bd) + " / " + humanSize(bt);
                        setBusy(msg);
                    }));
            main.post(() -> {
                hideBusy();
                loadListing();
                showImportResult(result);
            });
        }).start();
    }

    private void showImportResult(VaultImport.Result result) {
        String summary = result.filesAdded + " added"
                + (result.filesSkipped > 0 ? ", " + result.filesSkipped + " already there" : "")
                + (result.errors > 0 ? ", " + result.errors + " failed" : "");
        if (result.importedSources.isEmpty()) {
            confirm("Import done", summary, "OK", null);
            return;
        }
        confirm("Import done", summary + "\n\nDelete the " + result.importedSources.size()
                        + " imported file(s) from the source folder?", "Delete originals", () -> {
            showBusy("Deleting originals…");
            new Thread(() -> {
                int gone = VaultImport.shredSources(this, result.importedSources);
                main.post(() -> {
                    hideBusy();
                    Toast.makeText(this, "Deleted " + gone + " from source",
                            Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
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
        showBusy("Adding files…");
        new Thread(() -> {
            int ok = 0;
            for (Uri uri : uris) {
                String name = queryName(uri);
                int done = ok;
                main.post(() -> setBusy("Adding files… " + done + "/" + uris.size()));
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) continue;
                    VaultStore.importStream(this, parent, name, in);
                    ok++;
                } catch (Exception ignored) {
                }
            }
            int added = ok;
            main.post(() -> {
                hideBusy();
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
        showBusy("Exporting…");
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
            main.post(() -> {
                hideBusy();
                Toast.makeText(this, done ? "Exported" : "Export failed",
                        Toast.LENGTH_SHORT).show();
            });
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

    // --- busy overlay ------------------------------------------

    private void showBusy(String label) {
        if (busyOverlay != null) {
            setBusy(label);
            return;
        }
        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        overlay.setBackgroundColor(Color.BLACK);
        overlay.setClickable(true);

        overlay.addView(UiKit.spinner(this));

        busyLabel = new TextView(this);
        busyLabel.setText(label);
        busyLabel.setTextColor(Color.WHITE);
        busyLabel.setTypeface(Fonts.current(this));
        busyLabel.setTextSize(15);
        busyLabel.setGravity(Gravity.CENTER);
        busyLabel.setPadding(0, 32, 0, 0);
        overlay.addView(busyLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        busyOverlay = overlay;
    }

    private void setBusy(String label) {
        if (busyLabel != null) busyLabel.setText(label);
    }

    private void hideBusy() {
        if (busyOverlay != null) {
            root.removeView(busyOverlay);
            busyOverlay = null;
            busyLabel = null;
        }
    }

    // --- styled popups ----------------------------------------

    private void promptText(String title, String initial, String hint, String okLabel,
                            java.util.function.Consumer<String> onOk) {
        Typeface font = Fonts.current(this);
        LinearLayout box = popupBox();
        box.addView(popupTitle(font, title));

        EditText input = new EditText(this);
        input.setText(initial == null ? "" : initial);
        if (hint != null) input.setHint(hint);
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setTypeface(font);
        input.setTextSize(18);
        input.setSingleLine(true);
        input.setBackground(null);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setPadding(48, 8, 48, 24);
        input.setSelectAllOnFocus(true);
        box.addView(input);

        android.app.AlertDialog dialog = popupDialog(box);
        Runnable go = () -> {
            String name = input.getText().toString().trim();
            dialog.dismiss();
            onOk.accept(name);
        };
        input.setOnEditorActionListener((v, actionId, e) -> {
            go.run();
            return true;
        });
        box.addView(option(font, okLabel, v -> go.run()));
        box.addView(option(font, "Cancel", v -> dialog.dismiss()));
        showPopup(dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private void confirm(String title, String message, String okLabel, Runnable onOk) {
        Typeface font = Fonts.current(this);
        LinearLayout box = popupBox();
        box.addView(popupTitle(font, title));
        if (message != null) {
            TextView body = new TextView(this);
            body.setText(message);
            body.setTextColor(Color.GRAY);
            body.setTextSize(14);
            body.setTypeface(font);
            body.setPadding(48, 0, 48, 16);
            box.addView(body);
        }
        android.app.AlertDialog dialog = popupDialog(box);
        box.addView(option(font, okLabel, v -> {
            dialog.dismiss();
            if (onOk != null) onOk.run();
        }));
        if (onOk != null) box.addView(option(font, "Cancel", v -> dialog.dismiss()));
        showPopup(dialog);
    }

    private LinearLayout popupBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.BLACK);
        box.setBackground(bg);
        box.setPadding(0, 24, 0, 8);
        return box;
    }

    private TextView popupTitle(Typeface font, String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(font);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        title.setPadding(48, 8, 48, 16);
        return title;
    }

    private android.app.AlertDialog popupDialog(View content) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(content).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    private void showPopup(android.app.AlertDialog dialog) {
        dialog.show();
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    // --- small ui helpers --------------------------------------

    private TextView actionRow(Typeface font, String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(18);
        view.setTypeface(font);
        view.setPadding(48, 28, 48, 28);
        view.setBackground(rowBg());
        view.setOnClickListener(listener);
        return view;
    }

    private TextView actionText(Typeface font, String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.GRAY);
        view.setTextSize(13);
        view.setTypeface(font);
        view.setPadding(20, 36, 20, 12);
        view.setBackground(rowBg());
        view.setOnClickListener(listener);
        return view;
    }

    private TextView option(Typeface font, String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20);
        view.setTypeface(font);
        view.setPadding(48, 28, 48, 28);
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

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }


    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(java.util.Locale.US, "%.1f MB", bytes / 1024f / 1024f);
    }

    // --- list adapter with image thumbnails -------------------

    private class EntryAdapter extends BaseAdapter {

        private final int thumbPx = (int) (44 * getResources().getDisplayMetrics().density);

        @Override
        public int getCount() {
            return shown().size() + (hasUpRow() ? 1 : 0);
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView != null ? convertView : buildRow();
            Holder holder = (Holder) row.getTag();

            if (hasUpRow() && position == 0) {
                holder.thumb.setVisibility(View.GONE);
                holder.check.setVisibility(View.GONE);
                holder.more.setVisibility(View.GONE);
                holder.name.setText("↑  up");
                holder.name.setTextColor(Color.GRAY);
                holder.sub.setVisibility(View.GONE);
                return row;
            }

            VaultStore.Entry entry = shown().get(position - (hasUpRow() ? 1 : 0));
            holder.thumb.setVisibility(View.VISIBLE);
            holder.more.setVisibility(selecting ? View.GONE : View.VISIBLE);
            holder.name.setText(entry.name);
            holder.name.setTextColor(Color.WHITE);
            if (entry.isDir) {
                holder.sub.setVisibility(View.GONE);
            } else {
                holder.sub.setText(humanSize(entry.size));
                holder.sub.setVisibility(View.VISIBLE);
            }

            holder.check.setVisibility(selecting ? View.VISIBLE : View.GONE);
            holder.check.setText(selected.contains(entry.docId) ? "[x]" : "[ ]");

            holder.thumb.setTag(entry.docId);
            holder.thumb.setImageDrawable(null);
            boolean isImage = !entry.isDir && entry.mimeType != null
                    && entry.mimeType.startsWith("image/");
            if (entry.isDir) {
                holder.thumb.setBackground(null);
                holder.thumb.setImageDrawable(MiniIcons.folder(thumbPx, Color.GRAY));
            } else if (isImage) {
                holder.thumb.setBackground(iconBox());
                Bitmap hit = VaultThumbs.cached(entry.docId);
                if (hit != null) {
                    holder.thumb.setBackground(null);
                    holder.thumb.setImageBitmap(hit);
                } else {
                    String want = entry.docId;
                    ImageView target = holder.thumb;
                    VaultThumbs.load(VaultActivity.this, entry.docId, blobSize(entry),
                            thumbPx, bitmap -> {
                        if (bitmap != null && want.equals(target.getTag())) {
                            target.setBackground(null);
                            target.setImageBitmap(bitmap);
                        }
                    });
                }
            } else {
                holder.thumb.setBackground(null);
                holder.thumb.setImageDrawable(MiniIcons.file(thumbPx, Color.DKGRAY));
            }
            return row;
        }

        private long blobSize(VaultStore.Entry entry) {
            try {
                return VaultStore.resolve(VaultActivity.this, entry.docId).length();
            } catch (Exception e) {
                return 0;
            }
        }

        private View buildRow() {
            Typeface font = Fonts.current(VaultActivity.this);
            LinearLayout row = new LinearLayout(VaultActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(48, 20, 48, 20);
            row.setBackground(rowBg());

            TextView check = new TextView(VaultActivity.this);
            check.setTextColor(Color.WHITE);
            check.setTextSize(15);
            check.setTypeface(font);
            check.setVisibility(View.GONE);
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            checkParams.rightMargin = 24;
            row.addView(check, checkParams);

            ImageView thumb = new ImageView(VaultActivity.this);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(thumbPx, thumbPx);
            thumbParams.rightMargin = 32;
            row.addView(thumb, thumbParams);

            LinearLayout text = new LinearLayout(VaultActivity.this);
            text.setOrientation(LinearLayout.VERTICAL);

            TextView name = new TextView(VaultActivity.this);
            name.setTextColor(Color.WHITE);
            name.setTextSize(17);
            name.setTypeface(font);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            text.addView(name);

            TextView sub = new TextView(VaultActivity.this);
            sub.setTextColor(Color.GRAY);
            sub.setTextSize(12);
            sub.setTypeface(font);
            text.addView(sub);

            row.addView(text, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView more = new TextView(VaultActivity.this);
            more.setText("•••");
            more.setTextColor(Color.GRAY);
            more.setTextSize(14);
            more.setTypeface(font);
            more.setPadding(24, 16, 8, 16);
            more.setOnClickListener(v -> {
                Object tag = thumb.getTag();
                if (tag == null) return;
                for (VaultStore.Entry e : shown()) {
                    if (e.docId.equals(tag)) {
                        showEntryOptions(e);
                        return;
                    }
                }
            });
            row.addView(more);

            Holder holder = new Holder();
            holder.thumb = thumb;
            holder.name = name;
            holder.sub = sub;
            holder.check = check;
            holder.more = more;
            row.setTag(holder);
            return row;
        }

        private GradientDrawable iconBox() {
            GradientDrawable box = new GradientDrawable();
            box.setColor(Color.BLACK);
            box.setStroke(2, Color.DKGRAY);
            return box;
        }
    }

    private static class Holder {
        ImageView thumb;
        TextView name;
        TextView sub;
        TextView check;
        TextView more;
    }
}
