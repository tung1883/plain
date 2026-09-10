package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * The home To-do section, as a reusable block of rows. {@link MainActivity} and
 * a workspace {@link TodoPanel} both call {@link #render} so the two are the
 * same UI. Lock gating and multi-select stay with the caller.
 */
final class TodoSection {

    private TodoSection() {}

    static void render(SectionHost host, List<Object> rows) {
        Activity a = host.activity();
        SearchResult.Kind K = SearchResult.Kind.TODO;

        boolean open = host.settingsOpen(HomeMode.TODOS);
        rows.add(new SearchResult(K, (open ? "▾  " : "▸  ") + "To-do settings", null, -1,
                () -> { host.setSettingsOpen(HomeMode.TODOS, !open); host.refresh(); }));
        if (open) {
            rows.add(new SearchResult(K, "Locked: " + (Lock.TODOS.isLocked(a) ? "On" : "Off"), null, -1,
                    () -> Lock.TODOS.toggleLock(a, host::refresh)));
            rows.add(new SearchResult(K,
                    "Show completed: " + (Config.isTodosShowCompleted(a) ? "On" : "Off"), null, -1,
                    () -> { Config.setTodosShowCompleted(a, !Config.isTodosShowCompleted(a)); host.refresh(); }));
            int done = Todos.completedCount(a);
            rows.add(new SearchResult(K, "Archive completed (" + done + ")", null, -1, () -> {
                if (done == 0) {
                    Toast.makeText(a, "Nothing to archive", Toast.LENGTH_SHORT).show();
                    return;
                }
                VaultUi.confirm(a, "Archive " + done + " completed task" + (done == 1 ? "" : "s") + "?",
                        "They move out of the list into the done archive.",
                        "Archive", () -> { Todos.archiveCompleted(a); host.refresh(); }, "Cancel", null);
            }));
            rows.add(new SearchResult(K, "Edit as text", null, -1,
                    () -> a.startActivity(new Intent(a, TodoListEditActivity.class))));
            rows.add(new SearchResult(K, "Todo file: " + Todos.fileLabel(a), null, -1,
                    host::pickTodoFile));
            rows.add(new SearchResult(K, "Guide", null, -1,
                    () -> a.startActivity(new Intent(a, TodoGuideActivity.class))));
        }

        if (ImportJobs.pendingForPlugin(a, HomeMode.TODOS)) {
            rows.add(SectionHost.inert(K, ImportJobs.progressLine(a, HomeMode.TODOS)));
        } else {
            rows.add(new SearchResult(K, "+ Import files", null, -1,
                    () -> host.pickImport(HomeMode.TODOS, "text/*")));
        }
        rows.add(new SearchResult(K, "+ New task", null, -1, () -> Todos.promptAdd(a, host::refresh)));

        List<Todos.Item> items = Todos.sortedForView(Todos.load(a), Config.isTodosShowCompleted(a));
        for (int i = 0; i < items.size(); i++) {
            Todos.Item it = items.get(i);
            final int index = it.index;
            rows.add(new SearchResult(K, title(it.todo), subtitle(it.todo), i,
                    () -> { Todos.toggleDone(a, index); host.refresh(); }, it)
                    .withStrike(it.todo.done));
        }
    }

    static String selectionId(Object payload) {
        return payload instanceof Todos.Item ? "todo:" + ((Todos.Item) payload).index : null;
    }

    static void renderSelection(SelectionHost host, List<Object> rows) {
        Activity a = host.activity();
        SearchResult.Kind K = SearchResult.Kind.TODO;
        List<Todos.Item> items = Todos.sortedForView(Todos.load(a), Config.isTodosShowCompleted(a));

        List<String> ids = new ArrayList<>();
        for (Todos.Item it : items) ids.add("todo:" + it.index);
        List<Integer> selIdx = new ArrayList<>();
        int done = 0, open = 0;
        for (Todos.Item it : items) {
            if (host.selection().contains("todo:" + it.index)) {
                selIdx.add(it.index);
                if (it.todo.done) done++; else open++;
            }
        }

        List<BarAction> actions = new ArrayList<>();
        if (open > 0) actions.add(new BarAction("Complete",
                () -> { Todos.setDone(a, selIdx, true); host.exitSelection(); }));
        if (done > 0) actions.add(new BarAction("Reopen",
                () -> { Todos.setDone(a, selIdx, false); host.exitSelection(); }));
        if (selIdx.size() == 1) {
            char cur = 0;
            for (Todos.Item it : items) if (it.index == selIdx.get(0)) cur = it.todo.priority;
            char nextP = nextPriority(cur);
            int only = selIdx.get(0);
            actions.add(new BarAction("Priority " + priorityLabel(cur),
                    () -> { Todos.setPriority(a, only, nextP); host.exitSelection(); }));
        }
        actions.add(new BarAction("Delete", () -> VaultUi.confirm(a,
                "Delete " + selIdx.size() + " task" + (selIdx.size() == 1 ? "" : "s") + "?",
                null, "Delete", () -> { Todos.deleteAll(a, selIdx); host.exitSelection(); }, "Cancel", null)));
        host.selectionBar().bind(host, ids, actions);

        for (Todos.Item it : items) {
            String id = "todo:" + it.index;
            rows.add(new SearchResult(K, title(it.todo), subtitle(it.todo), -1,
                    () -> host.toggle(id), it)
                    .withStrike(it.todo.done).check(host.selection().contains(id)));
        }
    }

    static char nextPriority(char current) {
        switch (current) {
            case 0: return 'A';
            case 'A': return 'B';
            case 'B': return 'C';
            default: return 0;
        }
    }

    static String priorityLabel(char current) {
        char next = nextPriority(current);
        return next == 0 ? "none" : String.valueOf(next);
    }

    static String title(Todo todo) {
        String text = todo.displayText();
        if (todo.priority != 0 && !todo.done) text = "(" + todo.priority + ") " + text;
        return text;
    }

    static String subtitle(Todo todo) {
        List<String> tags = new ArrayList<>();
        for (String p : todo.projects()) tags.add("+" + p);
        for (String c : todo.contexts()) tags.add("@" + c);
        if (!tags.isEmpty()) return android.text.TextUtils.join(" ", tags);
        String due = todo.dueDate();
        return due != null ? "due " + due : null;
    }
}
