package com.plainphone.app;

import java.io.File;

/** One file that matched a content search, with the best passage found in it. */
final class ContentHit {

    final File file;
    final String locator;     // "p. 12", "slide 4", "§ Heading" ... or ""
    final String snippet;     // the passage, plain text
    final int matches;        // how many passages of the file matched
    final String folder;      // where the file is, for people ("Internal/Documents")

    ContentHit(File file, String locator, String snippet, int matches, String folder) {
        this.file = file;
        this.locator = locator;
        this.snippet = snippet;
        this.matches = matches;
        this.folder = folder;
    }
}
