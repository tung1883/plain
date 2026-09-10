package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The home Notes section, as a reusable block of rows. {@link MainActivity} and
 * a workspace {@link NotesPanel} both call {@link #render}; they differ only in
 * {@code openNote} (full-screen editor vs. in-panel editor). Lock gating and
 * multi-select stay with the caller.
 */
final class NotesSection {

    private NotesSection() {}

    static void render(SectionHost host, Consumer<String> openNote, List<Object> rows) {
        Activity a = host.activity();
        SearchResult.Kind K = SearchResult.Kind.NOTE;

        boolean open = host.settingsOpen(HomeMode.NOTES);
        rows.add(new SearchResult(K, (open ? "▾  " : "▸  ") + "Notes settings", null, -1,
                () -> { host.setSettingsOpen(HomeMode.NOTES, !open); host.refresh(); }));
        if (open) {
            rows.add(new SearchResult(K, "Export folder: " + Notes.exportFolderLabel(a), null, -1,
                    host::pickNotesFolder));
            rows.add(new SearchResult(K, "Locked: " + (Lock.NOTES.isLocked(a) ? "On" : "Off"), null, -1,
                    () -> Lock.NOTES.toggleLock(a, host::refresh)));
            int plainCount = Config.getNotes(a).size();
            if (plainCount > 0 && VaultFormat.exists(VaultSession.vaultRoot(a))) {
                rows.add(new SearchResult(K, "Move all notes to vault", null, -1,
                        () -> moveAllToVault(host, plainCount)));
            }
        }

        if (SectionJobs.pendingFor(a, HomeMode.NOTES)) {
            rows.add(SectionHost.inert(K, SectionJobs.progressLine(a, HomeMode.NOTES)));
        } else if (ImportJobs.pendingForPlugin(a, HomeMode.NOTES)) {
            rows.add(SectionHost.inert(K, ImportJobs.progressLine(a, HomeMode.NOTES)));
        } else {
            rows.add(new SearchResult(K, "+ Import files", null, -1,
                    () -> host.pickImport(HomeMode.NOTES, "text/*")));
        }

        rows.add(new SearchResult(K, "+ New note", null, -1, () -> {
            Note n = Note.create();
            List<Note> all = Config.getNotes(a);
            all.add(n);
            Config.setNotes(a, all);
            openNote.accept(n.id);
        }));

        List<Note> notes = new ArrayList<>(Config.getNotes(a));
        notes.addAll(Notes.vaultNotes(a));
        Collections.sort(notes, (x, y) -> Long.compare(y.updatedAt, x.updatedAt));
        for (Note note : notes) {
            final String id = note.id;
            rows.add(new SearchResult(K, note.title(), note.preview(), -1, () -> {
                if (Notes.isVaulted(id)) {
                    a.startActivity(new Intent(a, VaultTextViewerActivity.class)
                            .putExtra("docId", Notes.docIdOf(id))
                            .putExtra("name", Notes.vaultNoteName(a, id)));
                } else {
                    openNote.accept(id);
                }
            }, note));
        }

        if (!VaultSession.get().isUnlocked() && VaultFormat.exists(VaultSession.vaultRoot(a))
                && Config.getVaultNoteCount(a) > 0) {
            int n = Config.getVaultNoteCount(a);
            rows.add(new SearchResult(K, n + (n == 1 ? " note in the vault" : " notes in the vault"),
                    "Unlock to read", -1, host::unlockVault));
        }
    }

    static String selectionId(Object payload) {
        return payload instanceof Note ? ((Note) payload).id : null;
    }

    static void renderSelection(SelectionHost host, List<Object> rows) {
        Activity a = host.activity();
        SearchResult.Kind K = SearchResult.Kind.NOTE;

        List<Note> list = new ArrayList<>(Config.getNotes(a));
        list.addAll(Notes.vaultNotes(a));
        Collections.sort(list, (x, y) -> Long.compare(y.updatedAt, x.updatedAt));

        List<String> ids = new ArrayList<>();
        for (Note n : list) ids.add(n.id);
        List<Note> sel = new ArrayList<>();
        for (Note n : list) if (host.selection().contains(n.id)) sel.add(n);
        int locals = 0, vaulted = 0;
        for (Note n : sel) { if (Notes.isVaulted(n.id)) vaulted++; else locals++; }

        List<BarAction> actions = new ArrayList<>();
        if (locals > 0 && VaultFormat.exists(VaultSession.vaultRoot(a))) {
            actions.add(new BarAction("Move to vault", () -> {
                if (!VaultSession.get().isUnlocked()) { host.unlockVault(); return; }
                List<String> mv = new ArrayList<>();
                for (Note n : sel) if (!Notes.isVaulted(n.id)) mv.add(n.id);
                SectionJobs.startNotesToVault(a, mv);
                Toast.makeText(a, "Move queued", Toast.LENGTH_SHORT).show();
                host.exitSelection();
            }));
        }
        if (vaulted > 0) {
            actions.add(new BarAction("Move out", () -> {
                List<String> mv = new ArrayList<>();
                for (Note n : sel) if (Notes.isVaulted(n.id)) mv.add(n.id);
                SectionJobs.startNotesFromVault(a, mv);
                Toast.makeText(a, "Move queued", Toast.LENGTH_SHORT).show();
                host.exitSelection();
            }));
        }
        if (locals > 0) {
            actions.add(new BarAction("Export", () -> {
                List<android.net.Uri> uris = new ArrayList<>();
                for (Note n : sel) {
                    if (Notes.isVaulted(n.id)) continue;
                    try {
                        java.io.File f = new java.io.File(a.getCacheDir(), Notes.exportFileName(n));
                        try (java.io.OutputStream os = new java.io.FileOutputStream(f)) {
                            os.write(Notes.exportText(n).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        }
                        uris.add(PlainFileProvider.uriFor(a.getPackageName() + ".files", f));
                    } catch (Exception ignored) {
                    }
                }
                Sharing.sendFiles(a, uris, Notes.EXPORT_MIME, "Send notes");
            }));
        }
        actions.add(new BarAction("Delete", () -> VaultUi.confirm(a,
                "Delete " + sel.size() + " note" + (sel.size() == 1 ? "" : "s") + "?",
                null, "Delete", () -> {
                    List<Note> keep = Config.getNotes(a);
                    keep.removeIf(n -> host.selection().contains(n.id));
                    Config.setNotes(a, keep);
                    for (Note n : sel) if (Notes.isVaulted(n.id)) Notes.deleteVaultNote(a, n.id);
                    host.exitSelection();
                }, "Cancel", null)));
        host.selectionBar().bind(host, ids, actions);

        for (Note n : list) {
            final String id = n.id;
            rows.add(new SearchResult(K, n.title(), n.preview(), -1,
                    () -> host.toggle(id), n).check(host.selection().contains(id)));
        }
    }

    private static void moveAllToVault(SectionHost host, int count) {
        Activity a = host.activity();
        if (!VaultSession.get().isUnlocked()) { host.unlockVault(); return; }
        VaultUi.confirm(a, "Move " + count + " note" + (count == 1 ? "" : "s") + " to the vault?",
                "They'll be encrypted and only readable while the vault is unlocked.",
                "Move", () -> {
                    List<String> ids = new ArrayList<>();
                    for (Note n : Config.getNotes(a)) if (!n.isBlank()) ids.add(n.id);
                    SectionJobs.startNotesToVault(a, ids);
                    Toast.makeText(a, "Move queued", Toast.LENGTH_SHORT).show();
                    host.refresh();
                }, "Cancel", null);
    }
}
