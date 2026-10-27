package com.plainphone.app;

import android.content.Intent;

import java.io.File;

/** One row of keyboard search: what it shows, what a tap inserts, and what its arrow opens. */
final class KbHit {

    enum Kind { FILE, CONTENT, CONTACT, NOTE, TODO, WEB, RECENT, LOCKED, PERMISSION, HINT }

    final Kind kind;
    final String title, subtitle;
    /** Text a plain tap inserts; null = a tap opens instead (WEB, LOCKED, PERMISSION). */
    final String insert;
    /** FILE / CONTENT only; enables the "/" and paperclip buttons. */
    final File file;
    /** What the arrow (and WEB / LOCKED tap) launches, or null. */
    final Intent open;
    final String section;
    /** PERMISSION only: "files" or "contacts". */
    final String access;

    private KbHit(Kind kind, String title, String subtitle, String insert, File file, Intent open,
                  String section, String access) {
        this.kind = kind;
        this.title = title;
        this.subtitle = subtitle;
        this.insert = insert;
        this.file = file;
        this.open = open;
        this.section = section;
        this.access = access;
    }

    static KbHit file(File file, String subtitle) {
        return new KbHit(Kind.FILE, file.getName(), subtitle, file.getName(), file, null, "FILES", null);
    }

    static KbHit content(File file, String subtitle, String snippet) {
        return new KbHit(Kind.CONTENT, file.getName(), subtitle, snippet, file, null, "INSIDE DOCUMENTS", null);
    }

    static KbHit contact(String name, String subtitle, String insert, Intent open) {
        return new KbHit(Kind.CONTACT, name, subtitle, insert, null, open, "CONTACTS", null);
    }

    static KbHit note(String title, String preview, String text, Intent open) {
        return new KbHit(Kind.NOTE, title, preview, text, null, open, "NOTES", null);
    }

    static KbHit todo(String text, String subtitle, Intent open) {
        return new KbHit(Kind.TODO, text, subtitle, text, null, open, "TO-DO", null);
    }

    static KbHit web(String title, String subtitle, Intent open) {
        return new KbHit(Kind.WEB, title, subtitle, null, null, open, "SEARCH THE WEB", null);
    }

    static KbHit recent(String query, Intent open) {
        return new KbHit(Kind.RECENT, query, "Recent", null, null, open, "RECENT", null);
    }

    static KbHit locked(String title, Intent open) {
        return new KbHit(Kind.LOCKED, title, "Open Plain to unlock", null, null, open, "NOTES", null);
    }

    static KbHit permission(String title, String access) {
        return new KbHit(Kind.PERMISSION, title, "Tap to allow", null, null, null, "DEVICE", access);
    }

    static KbHit hint(String text) {
        return new KbHit(Kind.HINT, text, null, null, null, null, null, null);
    }
}
