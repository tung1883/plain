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

    static List<SearchResult> results(Activity host, TextMatch.Query query) {
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
                host.startActivity(intent);
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
