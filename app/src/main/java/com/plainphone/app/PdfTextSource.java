package com.plainphone.app;

import java.io.File;
import java.io.IOException;

/** Reads the text layer of a PDF a page at a time. Behind an interface so the rules can be tested. */
interface PdfTextSource {

    interface PageSink {
        /** The text of page {@code number} (1-based). Return false to stop reading. */
        boolean page(int number, String text);
    }

    /**
     * Read the pages in order. Throws {@link TextExtractor.Unreadable} with status "locked" for a PDF
     * that needs a password and "failed" for one that cannot be read.
     */
    void read(File file, PageSink sink) throws IOException;
}
