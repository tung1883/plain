package com.plainphone.app;

class SearchResult {

    enum Kind {
        BEST("Best match"),
        APP("Apps"),
        ACTION("Actions"),
        NOTE("Notes"),
        TODO("To-do"),
        RECORDING("Recordings"),
        PLAIN("Plain"),
        SYSTEM("Phone settings"),
        WEB("Web"),
        FILE("Files"),
        CONTENT("Inside files"),
        CONTACT("Contacts"),
        VAULT("Vault"),
        DEV("Dev"),
        WORKSPACE("Workspace");

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

    /** Lower is better. Search history may lower it a little after the result is built. */
    int score;

    /** The score the source gave; {@link #score} may be lowered from it by what the user picked before. */
    final int baseScore;

    final Object payload;

    /** How a row is drawn in search results; other screens leave it PLAIN. */
    enum Style { PLAIN, BEST, CONTENT, CALC }

    Style style = Style.PLAIN;

    /** Render the title struck through and greyed — used for completed to-dos. */
    boolean strike;

    /** Show a leading [ ] / [x] checkbox marker — used in multi-select mode. */
    boolean showCheck;
    boolean checked;

    /** Never fire this from the keyboard's Go/Enter key — a stray match must not lock the phone. */
    boolean guarded;

    /** Show a leading live-activity dot — used for the "recording in progress" card. */
    boolean liveDot;

    /** Labels for up to two trailing inline buttons (e.g. "Pause", "Stop") that fire
     *  instead of the row's own action — null means no button in that slot. */
    String actionLabel;
    String actionLabel2;

    private final Action action;
    private Action secondaryAction;
    private Action secondaryAction2;

    SearchResult(Kind kind, String title, String subtitle, int score, Action action) {
        this(kind, title, subtitle, score, action, null);
    }

    SearchResult(Kind kind, String title, String subtitle, int score, Action action, Object payload) {
        this.kind = kind;
        this.title = title;
        this.subtitle = subtitle;
        this.score = score;
        this.baseScore = score;
        this.action = action;
        this.payload = payload;
    }

    SearchResult withStrike(boolean value) {
        this.strike = value;
        return this;
    }

    SearchResult check(boolean on) {
        this.showCheck = true;
        this.checked = on;
        return this;
    }

    SearchResult styled(Style s) {
        this.style = s;
        return this;
    }

    SearchResult guarded() {
        this.guarded = true;
        return this;
    }

    SearchResult live() {
        this.liveDot = true;
        return this;
    }

    SearchResult withAction(String label, Action run) {
        if (this.actionLabel == null) {
            this.actionLabel = label;
            this.secondaryAction = run;
        } else {
            this.actionLabel2 = label;
            this.secondaryAction2 = run;
        }
        return this;
    }

    /** Set the score used for ordering (from {@link #baseScore}, never compounding). */
    void rank(int score) {
        this.score = score;
    }

    void activate() {
        action.run();
    }

    void runAction() {
        if (secondaryAction != null) secondaryAction.run();
    }

    void runAction2() {
        if (secondaryAction2 != null) secondaryAction2.run();
    }
}

