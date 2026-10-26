package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * When the typed query itself is a phone number, an email address or a link,
 * offer a direct action (call / message / email / open) above the other groups.
 */
class QuickActions {

    private QuickActions() {}

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern URL = Pattern.compile(
            "^(https?://\\S+|[\\w-]+(\\.[\\w-]+)*\\.[a-z]{2,}(/\\S*)?)$", Pattern.CASE_INSENSITIVE);

    /** Capture rows ("todo buy milk"), then a calculation or a link / number / address action for the typed text. */
    static List<SearchResult> results(Activity host, ParsedQuery parsed) {
        List<SearchResult> out = new ArrayList<>();
        if (parsed.command != null && !parsed.commandText.isEmpty()) out.addAll(capture(host, parsed));
        if (parsed.core.empty) return out;
        List<SearchResult> typed = typedActions(host, parsed.core);
        out.addAll(typed);
        if (typed.isEmpty()) {
            Calc.Answer answer = Calc.evaluate(parsed.raw);
            if (answer != null) {
                out.add(new SearchResult(SearchResult.Kind.ACTION, "= " + answer.value, answer.expression, 0,
                        () -> DeviceSearch.copyText(host, "Result", answer.value.replaceAll(" .*$", "")))
                        .styled(SearchResult.Style.CALC));
            }
        }
        return out;
    }

    /** "Add to-do: …" / "Add note: …" for a query that starts with todo / note. Never fired by the Go key. */
    private static List<SearchResult> capture(Activity host, ParsedQuery parsed) {
        List<SearchResult> out = new ArrayList<>();
        String text = parsed.commandText;
        if (parsed.command.equals("todo") && !Lock.TODOS.gateActive(host)) {
            out.add(new SearchResult(SearchResult.Kind.ACTION, "Add to-do: " + text, "To-do", 0, () -> {
                Todos.quickAdd(host.getApplicationContext(), text);
                android.widget.Toast.makeText(host, "Added to your to-dos", android.widget.Toast.LENGTH_SHORT).show();
            }).guarded());
        } else if (parsed.command.equals("note") && !Lock.NOTES.gateActive(host)) {
            out.add(new SearchResult(SearchResult.Kind.ACTION, "Add note: " + text, "Notes", 0, () -> {
                Note note = Note.create();
                note.text = text;
                List<Note> all = Config.getNotes(host);
                all.add(note);
                Config.setNotes(host, all);
                android.widget.Toast.makeText(host, "Note added", android.widget.Toast.LENGTH_SHORT).show();
            }).guarded());
        }
        return out;
    }

    private static List<SearchResult> typedActions(Activity host, TextMatch.Query query) {
        List<SearchResult> out = new ArrayList<>();
        if (query.empty) return out;
        String typed = query.raw.trim();

        String phone = asPhoneNumber(typed);
        if (phone != null) {
            out.add(action(host, "Call " + phone, "Phone", 0,
                    new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone)))));
            out.add(action(host, "Message " + phone, "SMS", 1,
                    new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(phone)))));
            return out;
        }

        if (EMAIL.matcher(typed).matches()) {
            out.add(action(host, "Email " + typed, "Email", 0,
                    new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + typed))));
            return out;
        }

        if (URL.matcher(typed).matches()) {
            String url = typed.matches("(?i)^https?://.*") ? typed : "https://" + typed;
            out.add(action(host, "Open " + typed, "Link", 0,
                    new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
        }
        return out;
    }

    private static SearchResult action(Activity host, String title, String subtitle,
                                       int score, Intent intent) {
        return new SearchResult(SearchResult.Kind.ACTION, title, subtitle, score, () -> {
            try {
                GatedLaunch.start(host, intent);
            } catch (Exception e) {
                android.widget.Toast.makeText(host, "No app can handle that",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }).guarded();
    }

    /** Digits (optional leading +), 5–15 long, once spacing / dashes / parens are stripped. */
    private static String asPhoneNumber(String typed) {
        String stripped = typed.replaceAll("[\\s().\\-]", "");
        return stripped.matches("\\+?[0-9]{5,15}") ? stripped : null;
    }
}
