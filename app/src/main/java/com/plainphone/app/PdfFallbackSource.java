package com.plainphone.app;

import java.io.File;
import java.io.IOException;

/**
 * Reads with the fast reader first; when it cannot open or read a file (it is stricter than PdfBox with
 * some damaged PDFs), tries the second one before giving up. A locked file stays locked.
 */
final class PdfFallbackSource implements PdfTextSource {

    private final PdfTextSource primary, fallback;

    PdfFallbackSource(PdfTextSource primary, PdfTextSource fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public void read(File file, PageSink sink) throws IOException {
        final int[] delivered = {0};
        try {
            primary.read(file, (number, text) -> {
                delivered[0]++;
                return sink.page(number, text);
            });
        } catch (TextExtractor.Unreadable e) {
            throw e;                               // locked: no other reader will open it either
        } catch (IOException | RuntimeException e) {
            if (delivered[0] > 0) throw e;         // pages were already handed over: do not read twice
            android.util.Log.d("PlainContentIndex", "pdfium failed (" + e + "), trying PdfBox: " + file.getName());
            fallback.read(file, sink);
        }
    }
}
