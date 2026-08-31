package com.plainphone.app;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One line of a todo.txt file. No id — identity is the list index, per the format.
 * Round-trips in canonical order; the free-text description is stored verbatim so
 * unknown {@code key:value} tags and their ordering survive a rewrite.
 */
class Todo {

    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern PROJECT = Pattern.compile("(?:^|\\s)\\+(\\S+)");
    private static final Pattern CONTEXT = Pattern.compile("(?:^|\\s)@(\\S+)");

    boolean done;
    char priority;          // 0 = none, else 'A'..'Z'
    String creationDate;    // "yyyy-MM-dd" or null
    String completionDate;  // "yyyy-MM-dd" or null (only meaningful when done)
    String description;     // verbatim remainder

    private Todo() {
        this.priority = 0;
        this.description = "";
    }

    static Todo parse(String line) {
        Todo todo = new Todo();
        String rest = line.trim();

        if (rest.startsWith("x ") || rest.equals("x")) {
            todo.done = true;
            rest = rest.length() > 1 ? rest.substring(2).trim() : "";
        }

        Matcher pri = Pattern.compile("^\\(([A-Z])\\)\\s+").matcher(rest);
        if (pri.find()) {
            todo.priority = pri.group(1).charAt(0);
            rest = rest.substring(pri.end());
        }

        // Up to two leading bare dates. On a done line the first is completion,
        // the second creation; otherwise the single date is creation.
        List<String> dates = new ArrayList<>();
        while (dates.size() < 2) {
            Matcher m = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})\\s+").matcher(rest);
            if (!m.find()) break;
            dates.add(m.group(1));
            rest = rest.substring(m.end());
        }
        if (todo.done && dates.size() == 2) {
            todo.completionDate = dates.get(0);
            todo.creationDate = dates.get(1);
        } else if (todo.done && dates.size() == 1) {
            todo.completionDate = dates.get(0);
        } else if (!dates.isEmpty()) {
            todo.creationDate = dates.get(0);
        }

        todo.description = rest;

        // todo.sh stashes the pre-completion priority as pri:X; surface it.
        if (todo.priority == 0) {
            Matcher stashed = Pattern.compile("(?:^|\\s)pri:([A-Z])(?:\\s|$)").matcher(todo.description);
            if (stashed.find()) todo.priority = stashed.group(1).charAt(0);
        }
        return todo;
    }

    String toLine() {
        StringBuilder sb = new StringBuilder();
        if (done) {
            sb.append("x ");
            if (completionDate != null) sb.append(completionDate).append(' ');
            if (creationDate != null) sb.append(creationDate).append(' ');
        } else {
            if (priority != 0) sb.append('(').append(priority).append(") ");
            if (creationDate != null) sb.append(creationDate).append(' ');
        }
        sb.append(description);
        return sb.toString().trim();
    }

    Set<String> projects() {
        return collect(PROJECT);
    }

    Set<String> contexts() {
        return collect(CONTEXT);
    }

    private Set<String> collect(Pattern pattern) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = pattern.matcher(description);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    String tag(String key) {
        Matcher m = Pattern.compile("(?:^|\\s)" + Pattern.quote(key) + ":(\\S+)").matcher(description);
        return m.find() ? m.group(1) : null;
    }

    String dueDate() {
        return tag("due");
    }

    /** Text with +project / @context / key:value tokens stripped, for display. */
    String displayText() {
        String t = description
                .replaceAll("(?:^|\\s)[+@]\\S+", " ")
                .replaceAll("(?:^|\\s)\\S+:\\S+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return t.isEmpty() ? description.trim() : t;
    }

    Todo markDone(boolean value) {
        Todo copy = copy();
        if (value && !copy.done) {
            copy.done = true;
            copy.completionDate = today();
            if (copy.creationDate == null) copy.creationDate = copy.completionDate;
            if (copy.priority != 0 && copy.tag("pri") == null) {
                copy.description = ("pri:" + copy.priority + " " + copy.description).trim();
            }
        } else if (!value && copy.done) {
            copy.done = false;
            copy.completionDate = null;
            String stashed = copy.tag("pri");
            if (stashed != null) {
                copy.priority = stashed.charAt(0);
                copy.description = copy.description
                        .replaceAll("(?:^|\\s)pri:[A-Z](?=\\s|$)", " ")
                        .replaceAll("\\s+", " ").trim();
            }
        }
        return copy;
    }

    Todo withPriority(char value) {
        Todo copy = copy();
        copy.priority = value;
        return copy;
    }

    private Todo copy() {
        Todo c = new Todo();
        c.done = done;
        c.priority = priority;
        c.creationDate = creationDate;
        c.completionDate = completionDate;
        c.description = description;
        return c;
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
