package com.plainphone.app;

import android.content.Context;
import android.view.View;

/**
 * The body of a {@link Panel} in the {@link WorkspaceActivity} — a terminal, a
 * screen mirror, a web view. The {@link Panel} owns the frame (title bar, drag,
 * resize, close); the content just supplies a {@link View} and reacts to the
 * shared {@code plaind} link coming and going.
 */
interface PanelContent {

    /** Build the body view once. Called before the panel is shown. */
    View onCreate(Context ctx);

    /** Title-bar text; may change over the panel's life. */
    String title();

    /** Extra buttons for the panel's title bar (e.g. a ⌨ toggle). */
    default View[] titleButtons(Context ctx) { return null; }

    /** The host calls {@code onChanged} when {@link #title()} may have changed. */
    default void setTitleListener(Runnable onChanged) {}

    /** Whether this content needs the {@code plaind} connection at all. */
    default boolean needsConnection() { return false; }

    /** Which paired device this panel talks to (null for content that needs none). */
    default String hostId() { return null; }

    // --- persistence -------------------------------------------------

    /** Stable kind tag for saving/restoring the workspace: "shell" / "screen" / "proc" / "web". */
    default String kind() { return "?"; }

    /** One opaque token to restore this panel (a shell session id, a web URL, …). */
    default String saveExtra() { return ""; }

    // --- lifecycle --------------------------------------------------

    /** The shared link came up ({@code conn != null}) or dropped ({@code null}). Dev panels only. */
    default void onConnection(DevConnection conn) {}

    /** The panel was brought to the front / given focus. */
    default void onFocus() {}

    /** The panel became visible (opened or restored from the taskbar). */
    default void onShow() {}

    /** The panel was minimised to the taskbar. */
    default void onHide() {}

    /** {@code false} = there is unsaved work; the host confirms before closing. */
    default boolean confirmClose() { return true; }

    /**
     * The workspace went to the background (or is being torn down). Stop streams
     * but keep any daemon session alive. Defaults to {@link #onClose()}.
     */
    default void onLeave() { onClose(); }

    /** The panel's × was pressed — close for good: kill sessions, free views. */
    default void onClose() {}
}
