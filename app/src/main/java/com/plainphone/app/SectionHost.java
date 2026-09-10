package com.plainphone.app;

import android.app.Activity;

/**
 * What a home-plugin section ({@link TodoSection}, {@link NotesSection},
 * {@link RecorderSection}) needs from whatever is showing it — the launcher home
 * screen ({@link MainActivity}) or a workspace {@link PluginPanel}. The section
 * builds the exact same rows either way.
 */
interface SectionHost {

    Activity activity();

    /** Re-render this section (the home {@code filter()} or a panel {@code list.refresh()}). */
    void refresh();

    boolean settingsOpen(HomeMode section);

    void setSettingsOpen(HomeMode section, boolean open);

    /** Open a multi-select document picker; the host owns the request code + result. */
    void pickImport(HomeMode section, String mimeType);

    /** The "Todo file: …" row — host runs {@link Todos#showFileOptions} with its own code. */
    void pickTodoFile();

    /** The "Export folder: …" row — host runs {@link Notes#showFolderOptions} with its own code. */
    void pickNotesFolder();

    /** Open the vault unlock screen (unlock-only). The section re-renders on return. */
    void unlockVault();

    static SearchResult inert(SearchResult.Kind kind, String title) {
        return new SearchResult(kind, title, null, -1, () -> {});
    }
}
