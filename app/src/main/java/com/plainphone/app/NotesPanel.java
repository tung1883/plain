package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import java.util.List;

/** A notes window: the note list, rendered like the home Notes section. */
final class NotesPanel implements PanelContent {

    private Context ctx;
    private SectionListView list;

    @Override public String kind() { return "notes"; }

    @Override public String title() { return "Notes"; }

    @Override
    public View onCreate(Context ctx) {
        this.ctx = ctx;
        list = new SectionListView(ctx);
        list.setProvider(this::fill);
        return list;
    }

    @Override public void onShow() { list.refresh(); }

    private void fill(List<Object> rows) {
        rows.add(new SearchResult(SearchResult.Kind.NOTE, "+ New note", null, -1, () -> {
            Note n = Note.create();
            List<Note> all = Config.getNotes(ctx);
            all.add(n);
            Config.setNotes(ctx, all);
            open(n.id);
        }));
        for (Note n : Config.getNotes(ctx)) {
            if (n.isBlank()) continue;
            final String id = n.id;
            rows.add(new SearchResult(SearchResult.Kind.NOTE, n.title(), n.preview(), -1,
                    () -> open(id)));
        }
    }

    private void open(String id) {
        Intent i;
        if (Notes.isVaulted(id)) {
            i = new Intent(ctx, VaultTextViewerActivity.class)
                    .putExtra("docId", Notes.docIdOf(id))
                    .putExtra("name", Notes.vaultNoteName(ctx, id));
        } else {
            i = new Intent(ctx, NoteEditActivity.class).putExtra("noteId", id);
        }
        ctx.startActivity(i);
    }
}
