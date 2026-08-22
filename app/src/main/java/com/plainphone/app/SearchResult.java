package com.plainphone.app;

/**
 * One row in the home screen's search results, whatever it came from — an installed app,
 * one of Plain's own screens, a phone settings page, a file, or a contact. Everything the
 * list needs to draw and act on a row lives here, so MainActivity's list doesn't have to
 * know which source produced any given result.
 */
class SearchResult {

    /** Result category, in the order its group is shown under its header. */
    enum Kind {
        APP("Apps"),
        PLAIN("Plain"),
        SYSTEM("Phone settings"),
        WEB("Web"),
        FILE("Files"),
        CONTACT("Contacts");

        final String header;

        Kind(String header) {
            this.header = header;
        }
    }

    interface Action {
        void run();
    }

    final Kind kind;
    final String title;
    /** Dim second line (file location, contact number, what a settings row does); null for none. */
    final String subtitle;
    /** Lower sorts first within a group; see {@link TextMatch}. */
    final int score;
    /**
     * Source object for results that need more than activation — currently the ResolveInfo
     * behind an app result, so a long press can still open that app's options.
     */
    final Object payload;

    private final Action action;

    SearchResult(Kind kind, String title, String subtitle, int score, Action action) {
        this(kind, title, subtitle, score, action, null);
    }

    SearchResult(Kind kind, String title, String subtitle, int score, Action action, Object payload) {
        this.kind = kind;
        this.title = title;
        this.subtitle = subtitle;
        this.score = score;
        this.action = action;
        this.payload = payload;
    }

    void activate() {
        action.run();
    }
}
