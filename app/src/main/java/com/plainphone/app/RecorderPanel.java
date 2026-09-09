package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import java.util.List;

/** A recorder window: the recordings list, rendered like the home Recorder section. */
final class RecorderPanel implements PanelContent {

    private Context ctx;
    private SectionListView list;

    @Override public String kind() { return "recorder"; }

    @Override public String title() { return "Recorder"; }

    @Override
    public View onCreate(Context ctx) {
        this.ctx = ctx;
        list = new SectionListView(ctx);
        list.setProvider(this::fill);
        return list;
    }

    @Override public void onShow() { list.refresh(); }

    private void fill(List<Object> rows) {
        rows.add(new SearchResult(SearchResult.Kind.RECORDING, "+ New recording", null, -1,
                () -> ctx.startActivity(new Intent(ctx, RecordActivity.class))));
        for (Recording r : Recorder.orderedAll(ctx)) {
            final String id = r.id;
            rows.add(new SearchResult(SearchResult.Kind.RECORDING, r.displayName(), r.subtitle(), -1,
                    () -> open(id)));
        }
    }

    private void open(String id) {
        Intent i = new Intent(ctx, RecordingPlayerActivity.class);
        if (Recorder.isVaulted(id)) {
            String name = Recorder.vaultRecordingName(ctx, id);
            int dot = name.lastIndexOf('.');
            i.putExtra("docId", Recorder.docIdOf(id));
            i.putExtra("name", dot > 0 ? name.substring(0, dot) : name);
            i.putExtra("format", dot >= 0 ? name.substring(dot + 1).toLowerCase() : "m4a");
        } else {
            i.putExtra("recId", id);
        }
        ctx.startActivity(i);
    }
}
