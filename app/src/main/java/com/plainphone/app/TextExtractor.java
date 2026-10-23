package com.plainphone.app;

import java.io.File;
import java.io.IOException;

/** Reads the text out of one kind of file, a passage at a time, for the content index. */
interface TextExtractor {

    /** Where extracted text goes. */
    interface Sink {
        /**
         * A passage of the file. {@code locator} says where it is ("p. 12", "slide 4", "§ Heading", "L120",
         * or ""). The sink splits long text itself.
         */
        void chunk(String locator, String text);

        /** The document's own title, when it has one. */
        void title(String title);

        /** True when extraction should stop now (enough text, or the job was cancelled). */
        boolean stop();

        /** Only part of the file was read; {@code note} says how much and why. */
        default void partial(String note) {}
    }

    /** The file is of this kind but holds no usable text, or cannot be read (binary, locked ...). */
    class Unreadable extends IOException {
        /** One of the {@code docs.status} values: "empty", "scanned", "locked", "failed". */
        final String status;

        Unreadable(String status, String message) {
            super(message);
            this.status = status;
        }
    }

    boolean supports(String extension);

    void extract(File file, Sink sink) throws IOException;
}
