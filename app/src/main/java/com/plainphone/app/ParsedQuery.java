package com.plainphone.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** A typed search with its operators taken apart (see {@link QueryParser}). */
final class ParsedQuery {

    /** The whole input, trimmed (arithmetic like "2 -1" must not lose its minus sign). */
    final String raw;

    /** What is left to match against names and text, as typed. */
    final String text;

    /** "Quoted phrases", folded. Their words are also part of {@link #text}. */
    final List<String> phrases;

    /** Words after a minus sign, folded: results containing them are left out. */
    final List<String> excluded;

    /** Result groups asked for with {@code app:} {@code note:} ...; empty = all of them. */
    final Set<SearchResult.Kind> kinds;

    /** File extensions asked for with {@code pdf:} / {@code ext:pdf} (lower case, no dot); empty = any. */
    final Set<String> exts;

    /** Only things changed after this time (epoch millis) with {@code in:today|week|month|year}; 0 = any. */
    final long since;

    /** {@code "todo"} or {@code "note"} when the query starts with that word and more follows, else null. */
    final String command;

    /** The text after the command word. */
    final String commandText;

    /** {@link #text} prepared for matching. */
    final TextMatch.Query core;

    ParsedQuery(String raw, String text, List<String> phrases, List<String> excluded, Set<SearchResult.Kind> kinds,
                Set<String> exts, long since, String command, String commandText) {
        this.raw = raw;
        this.text = text;
        this.phrases = Collections.unmodifiableList(new ArrayList<>(phrases));
        this.excluded = Collections.unmodifiableList(new ArrayList<>(excluded));
        this.kinds = kinds.isEmpty() ? EnumSet.noneOf(SearchResult.Kind.class) : EnumSet.copyOf(kinds);
        this.exts = Collections.unmodifiableSet(new HashSet<>(exts));
        this.since = since;
        this.command = command;
        this.commandText = commandText;
        this.core = TextMatch.prepare(text);
    }

    /** True when the query used any operator, so the plain word match alone is not the whole story. */
    boolean hasOperators() {
        return !phrases.isEmpty() || !excluded.isEmpty() || !kinds.isEmpty() || !exts.isEmpty() || since > 0;
    }

    /** Whether the result group {@code kind} should be searched at all. */
    boolean wants(SearchResult.Kind kind) {
        // extension and date filters only make sense for things that are files (or notes, for dates)
        if (!exts.isEmpty() && kind != SearchResult.Kind.FILE && kind != SearchResult.Kind.CONTENT) return false;
        if (since > 0 && kind != SearchResult.Kind.FILE && kind != SearchResult.Kind.CONTENT
                && kind != SearchResult.Kind.NOTE) return false;
        return kinds.isEmpty() || kinds.contains(kind);
    }

    /** True when {@code foldedText} contains a word that was excluded with a minus sign. */
    boolean rejects(String foldedText) {
        for (String ex : excluded) if (foldedText.contains(ex)) return true;
        return false;
    }

    /** True when {@code fileName} has one of the asked-for extensions (or none were asked for). */
    boolean acceptsExt(String fileName) {
        if (exts.isEmpty()) return true;
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && exts.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** True when something last changed at {@code millis} is recent enough. */
    boolean acceptsTime(long millis) {
        return since <= 0 || millis >= since;
    }
}
