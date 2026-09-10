package com.plainphone.app;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Base for the home-plugin windows (Notes / To-do / Recorder). Renders the whole
 * home section via {@code *Section.render}, covers it with a {@link LockGate}
 * when the plugin's PIN lock is active, and supports the same long-press
 * multi-select as the home screen (its own {@link SelectionBar}).
 */
abstract class PluginPanel implements PanelContent, SelectionHost {

    Context ctx;
    FrameLayout root;
    SectionListView list;
    private LockGate gate;
    private SelectionBar bar;
    private boolean settingsOpen;

    private final LinkedHashSet<String> sel = new LinkedHashSet<>();
    private boolean selecting;

    abstract HomeMode section();

    abstract void renderNormal(List<Object> rows);

    abstract void renderSelectionRows(List<Object> rows);

    /** The selection id for a non-selection row's payload (or null if not selectable). */
    abstract String selectionIdOf(Object payload);

    void afterRoot() {}

    private Lock lock() {
        HomeMode s = section();
        if (s == null) return null;
        switch (s) {
            case NOTES: return Lock.NOTES;
            case TODOS: return Lock.TODOS;
            case RECORDER: return Lock.RECORDER;
            default: return null;
        }
    }

    @Override
    public View onCreate(Context ctx) {
        this.ctx = ctx;
        root = new FrameLayout(ctx);

        list = new SectionListView(ctx);
        list.setLongPress(row -> {
            String id = selectionIdOf(row.payload);
            if (id == null) return false;
            selecting = true;
            sel.add(id);
            list.refresh();
            return true;
        });
        root.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        bar = new SelectionBar(ctx);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        root.addView(bar, bp);

        Lock lk = lock();
        gate = new LockGate(ctx, "Tap to unlock " + title(), () -> {
            if (lk != null && ctx instanceof android.app.Activity) ctx.startActivity(lk.pinGate(ctx));
        });
        gate.setVisibility(View.GONE);
        root.addView(gate, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        afterRoot();
        list.setProvider(this::fill); // triggers the first fill() — bar/gate must exist by now
        refreshGate();
        return root;
    }

    private void fill(List<Object> rows) {
        if (selecting) {
            bar.setVisibility(View.VISIBLE);
            renderSelectionRows(rows);
        } else {
            bar.setVisibility(View.GONE);
            renderNormal(rows);
        }
    }

    @Override public void onShow() { refreshGate(); }
    @Override public void onLeave() { exitSelection(); }
    @Override public void onClose() { exitSelection(); }

    void refreshGate() {
        Lock lk = lock();
        if (lk != null && lk.gateActive(ctx)) {
            if (selecting) exitSelection();
            gate.setVisibility(View.VISIBLE);
        } else {
            if (lk != null) lk.keepUnlocked(ctx);
            gate.setVisibility(View.GONE);
            list.refresh();
        }
    }

    // --- SectionHost / SelectionHost -------------------------------

    @Override public android.app.Activity activity() { return (android.app.Activity) ctx; }
    @Override public void refresh() { if (list != null) list.refresh(); }
    @Override public boolean settingsOpen(HomeMode s) { return settingsOpen; }
    @Override public void setSettingsOpen(HomeMode s, boolean open) { settingsOpen = open; }
    @Override public void pickImport(HomeMode s, String mime) {
        SectionImports.pickImport(activity(), s, mime);
    }
    @Override public void pickTodoFile() {
        Todos.showFileOptions(activity(), SectionImports.TODO_FILE, this::refresh);
    }
    @Override public void pickNotesFolder() {
        Notes.showFolderOptions(activity(), SectionImports.NOTES_FOLDER, this::refresh);
    }
    @Override public void unlockVault() {
        activity().startActivity(new android.content.Intent(activity(), VaultActivity.class)
                .putExtra(VaultActivity.EXTRA_UNLOCK_ONLY, true));
    }

    @Override public Set<String> selection() { return sel; }
    @Override public SelectionBar selectionBar() { return bar; }
    @Override public void toggle(String id) {
        if (!sel.remove(id)) sel.add(id);
        list.refresh();
    }
    @Override public void setSelected(Collection<String> ids) {
        sel.clear();
        sel.addAll(ids);
        list.refresh();
    }
    @Override public void exitSelection() {
        selecting = false;
        sel.clear();
        if (bar != null) bar.setVisibility(View.GONE);
        if (list != null) list.refresh();
    }
}
