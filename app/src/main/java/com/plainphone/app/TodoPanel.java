package com.plainphone.app;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** A to-do window: the todo.txt list, rendered like the home To-do section. */
final class TodoPanel implements PanelContent {

    private Context ctx;
    private SectionListView list;

    @Override public String kind() { return "todo"; }

    @Override public String title() { return "To-do"; }

    @Override
    public View onCreate(Context ctx) {
        this.ctx = ctx;
        list = new SectionListView(ctx);
        list.setProvider(this::fill);
        return list;
    }

    @Override public void onShow() { list.refresh(); }

    private void fill(List<Object> rows) {
        rows.add(new SearchResult(SearchResult.Kind.TODO, "+ New task", null, -1, this::promptNew));
        List<Todos.Item> items = Todos.sortedForView(Todos.load(ctx),
                Config.isTodosShowCompleted(ctx));
        for (int i = 0; i < items.size(); i++) {
            Todos.Item it = items.get(i);
            final int index = it.index;
            rows.add(new SearchResult(SearchResult.Kind.TODO, title(it.todo), subtitle(it.todo), i,
                    () -> { Todos.toggleDone(ctx, index); list.refresh(); })
                    .withStrike(it.todo.done));
        }
    }

    private void promptNew() {
        if (ctx instanceof Activity) Todos.promptAdd((Activity) ctx, list::refresh);
    }

    private String title(Todo todo) {
        String text = todo.displayText();
        if (todo.priority != 0 && !todo.done) text = "(" + todo.priority + ") " + text;
        return text;
    }

    private String subtitle(Todo todo) {
        List<String> tags = new ArrayList<>();
        for (String pj : todo.projects()) tags.add("+" + pj);
        for (String cx : todo.contexts()) tags.add("@" + cx);
        return tags.isEmpty() ? null : android.text.TextUtils.join("  ", tags);
    }
}
