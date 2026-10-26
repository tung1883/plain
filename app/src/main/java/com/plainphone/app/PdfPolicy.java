package com.plainphone.app;

/** Decides whether a PDF has a text layer worth indexing. Scanned (image-only) PDFs are skipped: no OCR. */
final class PdfPolicy {

    private PdfPolicy() {}

    /** A page with fewer characters than this counts as having no text. */
    static final int EMPTY_PAGE_CHARS = 20;

    /** Documents averaging fewer characters per page than this are treated as scanned. */
    static final int MIN_AVERAGE_CHARS = 20;

    /** Documents where more than this share of pages (in percent) have no text are treated as scanned. */
    static final int MAX_EMPTY_PERCENT = 80;

    /** Pages read from one PDF, at most. */
    static final int MAX_PAGES = 3000;

    /** The longest one PDF may take to read, in milliseconds. */
    static final long MAX_MILLIS = 180_000;

    static boolean hasText(int[] charsPerPage) {
        if (charsPerPage == null || charsPerPage.length == 0) return false;
        long total = 0;
        int empty = 0;
        for (int c : charsPerPage) {
            total += c;
            if (c < EMPTY_PAGE_CHARS) empty++;
        }
        if (total / charsPerPage.length < MIN_AVERAGE_CHARS) return false;
        return empty * 100L <= (long) charsPerPage.length * MAX_EMPTY_PERCENT;
    }
}
