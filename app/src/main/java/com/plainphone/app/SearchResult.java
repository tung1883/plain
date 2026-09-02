package com.plainphone.app;

class SearchResult {

    enum Kind {
        APP("Apps"),
        NOTE("Notes"),
        TODO("To-do"),
        RECORDING("Recordings"),
        PLAIN("Plain"),
        SYSTEM("Phone settings"),
        WEB("Web"),
        FILE("Files"),
        CONTACT("Contacts"),
        VAULT("Vault");

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

    /** Never fire this from the keyboard's Go/Enter key — a stray match must not lock the phone. */
    boolean guarded;

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

    SearchResult guarded() {
        this.guarded = true;
        return this;
    }

    void activate() {
        action.run();
    }
}

