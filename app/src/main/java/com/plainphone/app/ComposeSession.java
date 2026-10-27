package com.plainphone.app;

import android.view.View;

/** Who receives the text typed in the keyboard's own compose bar (Translate, Search, Emoji, Clipboard). */
interface ComposeSession {

    /** Placeholder shown while nothing is typed yet. */
    String hint();

    /** Called on every change, with the whole text so far. */
    void onChange(String text);

    /** Called when typing ends (Done, Enter or Back). */
    void onDone(String text);

    /** A view shown between the compose bar and the keys while typing (live results), or null. */
    default View strip() {
        return null;
    }

    /** Starts a {@link ComposeSession}; panels get one of these from {@link KeyboardRoot}. */
    interface Starter {
        void start(ComposeSession session, String initial);
    }
}
