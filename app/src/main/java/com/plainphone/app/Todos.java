package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Store and mutations for the todo.txt list. Line order is canonical and is never
 * reordered — mutations edit a line in place, the view sorts a copy.
 *
 * <p>Two backends, picked by {@link #usingFile}:
 * <ul>
 *   <li><b>internal</b> (default): canonical text in {@code Config} key "todos".
 *   <li><b>file</b>: a linked external {@code todo.txt} via a persisted SAF document
 *       URI ({@code Config.getTodoFileUri}). Re-read on every {@link #load};
 *       mutations reload → mutate → write (last-write-wins).
 * </ul>
 * The completed archive stays an internal blob ({@code Config} key "todos_done") in
 * both modes — a sibling {@code done.txt} needs folder-tree access we don't hold.
 */
class Todos {

    private Todos() {}

    /** A todo paired with its canonical line index, so row actions address the right line. */
    static class Item {
        final Todo todo;
        final int index;

        Item(Todo todo, int index) {
            this.todo = todo;
            this.index = index;
        }
    }

    // --- backend ----------------------------------------------------------

    static boolean usingFile(Context context) {
        return Config.getTodoFileUri(context) != null;
    }

    static List<Todo> load(Context context) {
        String uri = Config.getTodoFileUri(context);
        if (uri != null) {
            String text = readFile(context, Uri.parse(uri));
            if (text != null) return fromText(text);
            // File vanished or lost permission — drop back to the internal blob.
            Config.setTodoFileUri(context, null);
        }
        return fromText(Config.getTodosText(context));
    }

    static void save(Context context, List<Todo> todos) {
        String text = toText(todos);
        String uri = Config.getTodoFileUri(context);
        if (uri != null && writeFile(context, Uri.parse(uri), text)) return;
        if (uri != null) Config.setTodoFileUri(context, null); // write failed — fall back
        Config.setTodosText(context, text);
    }

    /** Raw canonical text through the active backend — for the text editor. */
    static String rawText(Context context) {
        String uri = Config.getTodoFileUri(context);
        if (uri != null) {
            String text = readFile(context, Uri.parse(uri));
            if (text != null) return text.trim();
            Config.setTodoFileUri(context, null);
        }
        return Config.getTodosText(context);
    }

    static void saveRawText(Context context, String text) {
        String trimmed = text.trim();
        String uri = Config.getTodoFileUri(context);
        if (uri != null && writeFile(context, Uri.parse(uri), trimmed)) return;
        if (uri != null) Config.setTodoFileUri(context, null);
        Config.setTodosText(context, trimmed);
    }

    // --- mutations (reload → mutate → write) -----------------------------

    static void quickAdd(Context context, String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;
        Todo todo = Todo.parse(trimmed);
        if (todo.creationDate == null && !todo.done) todo.creationDate = Todo.today();
        List<Todo> todos = load(context);
        todos.add(todo);
        save(context, todos);
    }

    static void toggleDone(Context context, int index) {
        List<Todo> todos = load(context);
        if (index < 0 || index >= todos.size()) return;
        todos.set(index, todos.get(index).markDone(!todos.get(index).done));
        save(context, todos);
    }

    static void setPriority(Context context, int index, char priority) {
        List<Todo> todos = load(context);
        if (index < 0 || index >= todos.size()) return;
        todos.set(index, todos.get(index).withPriority(priority));
        save(context, todos);
    }

    static void delete(Context context, int index) {
        List<Todo> todos = load(context);
        if (index < 0 || index >= todos.size()) return;
        todos.remove(index);
        save(context, todos);
    }

    /** Batch mark-done/undone across canonical indices, one save. */
    static void setDone(Context context, java.util.Collection<Integer> indices, boolean done) {
        List<Todo> todos = load(context);
        for (int i : indices) {
            if (i >= 0 && i < todos.size()) todos.set(i, todos.get(i).markDone(done));
        }
        save(context, todos);
    }

    /** Batch delete across canonical indices, one save. */
    static void deleteAll(Context context, java.util.Collection<Integer> indices) {
        List<Integer> sorted = new ArrayList<>(indices);
        java.util.Collections.sort(sorted, java.util.Collections.reverseOrder());
        List<Todo> todos = load(context);
        for (int i : sorted) {
            if (i >= 0 && i < todos.size()) todos.remove(i);
        }
        save(context, todos);
    }

    static int completedCount(Context context) {
        int n = 0;
        for (Todo todo : load(context)) {
            if (todo.done) n++;
        }
        return n;
    }

    /** Move completed lines out of the active list into the internal done blob. */
    static void archiveCompleted(Context context) {
        List<Todo> todos = load(context);
        List<Todo> remaining = new ArrayList<>();
        List<Todo> archived = fromText(Config.getTodosDoneText(context));
        for (Todo todo : todos) {
            if (todo.done) {
                archived.add(todo);
            } else {
                remaining.add(todo);
            }
        }
        Config.setTodosDoneText(context, toText(archived));
        save(context, remaining);
    }

    // --- view sort ------------------------------------------------------

    /**
     * Incomplete first (priority A..Z, then none, then creation date), completed
     * appended — or dropped when {@code !showCompleted}. Each result keeps its
     * original canonical index.
     */
    static List<Item> sortedForView(List<Todo> todos, boolean showCompleted) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < todos.size(); i++) {
            Todo todo = todos.get(i);
            if (todo.done && !showCompleted) continue;
            items.add(new Item(todo, i));
        }
        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item a, Item b) {
                if (a.todo.done != b.todo.done) return a.todo.done ? 1 : -1;
                int pa = rank(a.todo.priority);
                int pb = rank(b.todo.priority);
                if (pa != pb) return Integer.compare(pa, pb);
                String ca = a.todo.creationDate == null ? "" : a.todo.creationDate;
                String cb = b.todo.creationDate == null ? "" : b.todo.creationDate;
                int byDate = ca.compareTo(cb);
                if (byDate != 0) return byDate;
                return Integer.compare(a.index, b.index);
            }
        });
        return items;
    }

    private static int rank(char priority) {
        return priority == 0 ? 26 : (priority - 'A');
    }

    // --- external file linking ----------------------------------------

    static String fileLabel(Context context) {
        String uriString = Config.getTodoFileUri(context);
        if (uriString == null) return "Internal";
        String name = displayName(context, Uri.parse(uriString));
        return name != null ? name : "Linked file";
    }

    static void pickFile(Activity host, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        host.startActivityForResult(intent, requestCode);
    }

    /**
     * Link the file the picker returned. A non-empty file wins and becomes
     * canonical (the internal list is left in place, unused); an empty file
     * receives the current internal list. Returns a status message, or null.
     */
    static String handleFilePick(Context context, Intent data) {
        if (data == null || data.getData() == null) return null;
        Uri uri = data.getData();
        try {
            context.getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }

        String fileText = readFile(context, uri);
        if (fileText == null) return "Couldn't read that file";

        boolean fileHasTasks = !fileText.trim().isEmpty();
        boolean internalHasTasks = !Config.getTodosText(context).trim().isEmpty();
        Config.setTodoFileUri(context, uri.toString());

        if (fileHasTasks) {
            return internalHasTasks
                    ? "Linked — the file's tasks are now shown; your internal list is set aside"
                    : "Linked";
        }
        writeFile(context, uri, Config.getTodosText(context));
        return "Linked — your tasks were written to the file";
    }

    static void unlink(Context context) {
        String uriString = Config.getTodoFileUri(context);
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            String text = readFile(context, uri);
            if (text != null) Config.setTodosText(context, text.trim());
            try {
                context.getContentResolver().releasePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {
            }
        }
        Config.setTodoFileUri(context, null);
    }

    static void showFileOptions(Activity host, int pickRequestCode, Runnable after) {
        Typeface font = Fonts.current(host);
        boolean linked = usingFile(host);

        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 32, 0, 8);

        TextView title = new TextView(host);
        title.setText(linked ? fileLabel(host) : "Todo file");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(font);
        title.setPadding(48, 0, 48, 16);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        root.addView(title);

        AlertDialog dialog = new AlertDialog.Builder(host).setView(root).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        root.addView(optionRow(host, font, linked ? "Change file" : "Link file", v -> {
            dialog.dismiss();
            pickFile(host, pickRequestCode);
        }));
        if (linked) {
            root.addView(optionRow(host, font, "Unlink", v -> {
                dialog.dismiss();
                unlink(host);
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

    // --- io ------------------------------------------------------------

    private static String readFile(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean writeFile(Context context, Uri uri, String text) {
        try (OutputStream os = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (os == null) return false;
            os.write(text.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String displayName(Context context, Uri uri) {
        try (Cursor c = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
        } catch (Exception ignored) {
        }
        return null;
    }

    // --- text <-> model ----------------------------------------------

    private static List<Todo> fromText(String text) {
        List<Todo> todos = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return todos;
        for (String line : text.split("\n", -1)) {
            if (line.trim().isEmpty()) continue;
            todos.add(Todo.parse(line));
        }
        return todos;
    }

    private static String toText(List<Todo> todos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < todos.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(todos.get(i).toLine());
        }
        return sb.toString();
    }

    // --- popup chrome (mirrors Notes) --------------------------------

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
