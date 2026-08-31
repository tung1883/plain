package com.plainphone.app;

class SearchResult {

    enum Kind {
        APP("Apps"),
        NOTE("Notes"),
        TODO("To-do"),
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

    final String subtitle;

    final int score;

    final Object payload;

    /** Render the title struck through and greyed — used for completed to-dos. */
    boolean strike;

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

    SearchResult withStrike(boolean value) {
        this.strike = value;
        return this;
    }

    void activate() {
        action.run();
    }
}

