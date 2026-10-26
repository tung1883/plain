package com.plainphone.app;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF files with a text layer, one passage group per page. A PDF whose pages hold (almost) no text
 * is a scan: it is reported as "scanned" and not indexed.
 */
final class PdfExtractor implements TextExtractor {

    private final PdfTextSource source;

    PdfExtractor(PdfTextSource source) {
        this.source = source;
    }

    /** Make PDFs indexable with the PdfBox reader (does nothing when the library is missing). */
    static void install(Context context) {
        if (Extractors.pdf != null) return;
        try {
            PdfTextSource box = new PdfBoxSource(context);
            PdfTextSource source = box;
            try {
                source = new PdfFallbackSource(new PdfiumSource(context), box);   // pdfium first, PdfBox for what it cannot read
            } catch (Throwable e) {
                android.util.Log.w("PlainContentIndex", "pdfium unavailable, using PdfBox", e);
            }
            Extractors.pdf = new PdfExtractor(source);
        } catch (Throwable e) {
            android.util.Log.w("PlainContentIndex", "PDF support unavailable", e);
        }
    }

    @Override
    public boolean supports(String ext) {
        return ext.equals("pdf");
    }

    @Override
    public void extract(File file, Sink sink) throws IOException {
        final List<Integer> numbers = new ArrayList<>();
        final List<String> texts = new ArrayList<>();
        final long start = System.currentTimeMillis();
        final boolean[] timedOut = {false};
        final int[] chars = {0};

        source.read(file, (number, text) -> {
            if (sink.stop() || numbers.size() >= PdfPolicy.MAX_PAGES) return false;
            if (System.currentTimeMillis() - start > PdfPolicy.MAX_MILLIS) {
                timedOut[0] = true;
                return false;
            }
            String t = text == null ? "" : text.trim();
            numbers.add(number);
            texts.add(t);
            chars[0] += t.length();
            return chars[0] < ExtractRunner.MAX_CHARS;
        });

        if (texts.isEmpty()) throw new Unreadable(timedOut[0] ? "failed" : "empty", timedOut[0] ? "took too long" : "no pages");
        int[] perPage = new int[texts.size()];
        for (int i = 0; i < perPage.length; i++) perPage[i] = texts.get(i).length();
        if (!PdfPolicy.hasText(perPage)) throw new Unreadable("scanned", "no text layer");

        if (timedOut[0]) sink.partial("Only the first " + texts.size() + " pages were read, it took too long");
        for (int i = 0; i < texts.size(); i++) {
            if (perPage[i] > 0) sink.chunk("p. " + numbers.get(i), texts.get(i));
        }
    }
}
