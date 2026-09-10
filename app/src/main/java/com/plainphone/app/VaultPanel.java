package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.List;

/**
 * A vault window: vault notes + recordings, rendered like the launcher. Covered
 * by a {@link LockGate} whenever the vault is locked; re-gates live when the
 * vault auto-locks (a {@link VaultSession.Listener}).
 */
final class VaultPanel implements PanelContent, VaultSession.Listener {

    private Context ctx;
    private FrameLayout root;
    private SectionListView list;
    private LockGate gate;
    private boolean registered;

    @Override public String kind() { return "vault"; }
    @Override public String title() { return "Vault"; }

    @Override
    public View onCreate(Context ctx) {
        this.ctx = ctx;
        root = new FrameLayout(ctx);
        list = new SectionListView(ctx);
        list.setProvider(this::fill);
        root.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        gate = new LockGate(ctx, "Tap to unlock Vault", () ->
                ctx.startActivity(new Intent(ctx, VaultActivity.class)
                        .putExtra(VaultActivity.EXTRA_UNLOCK_ONLY, true)));
        gate.setVisibility(View.GONE);
        root.addView(gate, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        refreshGate();
        return root;
    }

    @Override
    public void onShow() {
        if (!registered) { VaultSession.get().addListener(this); registered = true; }
        refreshGate();
    }

    @Override public void onHide() { unregister(); }
    @Override public void onLeave() { unregister(); }
    @Override public void onClose() { unregister(); }

    private void unregister() {
        if (registered) { VaultSession.get().removeListener(this); registered = false; }
    }

    @Override
    public void onVaultLockStateChanged() {
        if (root != null) root.post(this::refreshGate);
    }

    private void refreshGate() {
        boolean open = VaultSession.get().isUnlocked();
        gate.setVisibility(open ? View.GONE : View.VISIBLE);
        if (open) list.refresh();
    }

    private void fill(List<Object> rows) {
        rows.add(new SearchResult(SearchResult.Kind.VAULT, "Open vault browser", null, -1,
                () -> ctx.startActivity(new Intent(ctx, VaultActivity.class))));
        for (Note n : Notes.vaultNotes(ctx)) {
            final String id = n.id;
            rows.add(new SearchResult(SearchResult.Kind.VAULT, n.title(), n.preview(), -1,
                    () -> ctx.startActivity(new Intent(ctx, VaultTextViewerActivity.class)
                            .putExtra("docId", Notes.docIdOf(id))
                            .putExtra("name", Notes.vaultNoteName(ctx, id)))));
        }
        for (Recording r : Recorder.vaultRecordings(ctx)) {
            final String id = r.id;
            rows.add(new SearchResult(SearchResult.Kind.VAULT, r.displayName(), r.subtitle(), -1,
                    () -> {
                        String name = Recorder.vaultRecordingName(ctx, id);
                        int dot = name.lastIndexOf('.');
                        ctx.startActivity(new Intent(ctx, RecordingPlayerActivity.class)
                                .putExtra("docId", Recorder.docIdOf(id))
                                .putExtra("name", dot > 0 ? name.substring(0, dot) : name)
                                .putExtra("format", dot >= 0 ? name.substring(dot + 1).toLowerCase() : "m4a"));
                    }));
        }
    }
}
