package com.plainphone.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The extractors the content index knows, and the groups of file types shown in Search settings. */
final class Extractors {

    private Extractors() {}

    /** Type groups, as stored in {@code Config.getContentTypes}. */
    static final String TEXT = "text", OFFICE = "office", EPUB = "epub", PDF = "pdf";

    private static final TextExtractor PLAIN = new PlainTextExtractor();
    private static final TextExtractor OOXML = new OoxmlExtractor();
    private static final TextExtractor ODF = new OdfExtractor();
    private static final TextExtractor BOOK = new EpubExtractor();
    private static final TextExtractor RTF = new RtfExtractor();

    /** Set by the app before indexing, so tests can run without the PDF library. */
    static volatile TextExtractor pdf;

    /** The extractor for files ending in {@code ext} (lower case, no dot), or null. */
    static TextExtractor forExt(String ext) {
        if (PLAIN.supports(ext)) return PLAIN;
        if (OOXML.supports(ext)) return OOXML;
        if (ODF.supports(ext)) return ODF;
        if (BOOK.supports(ext)) return BOOK;
        if (RTF.supports(ext)) return RTF;
        TextExtractor p = pdf;
        if (p != null && p.supports(ext)) return p;
        return null;
    }

    /** Which settings group a file extension belongs to, or null when it is not indexed at all. */
    static String groupOf(String ext) {
        if (PlainTextExtractor.EXTENSIONS.contains(ext)) return TEXT;
        if (ext.equals("pdf")) return PDF;
        if (ext.equals("epub")) return EPUB;
        if (Arrays.asList("docx", "xlsx", "pptx", "odt", "ods", "odp", "rtf").contains(ext)) return OFFICE;
        return null;
    }

    static Set<String> allGroups() {
        return new HashSet<>(Arrays.asList(TEXT, OFFICE, EPUB, PDF));
    }

    /** Largest file read for each group, in bytes. */
    static long sizeLimit(String group) {
        switch (group) {
            case TEXT: return 20L * 1024 * 1024;
            case OFFICE: case EPUB: return 50L * 1024 * 1024;
            default: return 100L * 1024 * 1024;
        }
    }

    static List<String> groupNames() {
        return new ArrayList<>(Arrays.asList(TEXT, OFFICE, EPUB, PDF));
    }
}
