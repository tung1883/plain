package com.plainphone.app;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import io.legere.pdfiumandroid.PdfDocument;
import io.legere.pdfiumandroid.PdfPage;
import io.legere.pdfiumandroid.PdfTextPage;
import io.legere.pdfiumandroid.PdfiumCore;

import io.legere.pdfiumandroid.api.PdfPasswordException;

import java.io.File;
import java.io.IOException;

/** Reads PDF text with pdfium (Chrome's PDF engine, native code): many times faster than PdfBox. */
final class PdfiumSource implements PdfTextSource {

    /** A one-page file kept open for the life of the process: see {@link #keepAlive}. */
    private static final byte[] BLANK = ("%PDF-1.0\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
            + "3 0 obj<</Type/Page/MediaBox[0 0 3 3]/Parent 2 0 R>>endobj\ntrailer<</Root 1 0 R>>\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    private final PdfiumCore core;
    @SuppressWarnings("unused")
    private PdfDocument keepAlive;

    /** Throws when the native library cannot be loaded; the caller then falls back to another reader. */
    PdfiumSource(Context context) {
        core = new PdfiumCore();
        // The library tears itself down when the last document closes and starts again on the next file;
        // that restart crashed the app (stack corruption). One open document keeps it loaded.
        try {
            keepAlive = core.newDocument(BLANK);
        } catch (Throwable e) {
            android.util.Log.w("PlainContentIndex", "pdfium keep-alive document failed", e);
        }
    }

    @Override
    public void read(File file, PageSink sink) throws IOException {
        final long t0 = android.os.SystemClock.elapsedRealtime();
        try (ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
            PdfDocument doc;
            try {
                doc = core.newDocument(fd);
            } catch (PdfPasswordException e) {
                throw new TextExtractor.Unreadable("locked", "password protected");
            }
            final long opened = android.os.SystemClock.elapsedRealtime();
            long inText = 0;
            long chars = 0;
            int read = 0;
            try {
                int pages = Math.min(doc.getPageCount(), PdfPolicy.MAX_PAGES);
                for (int i = 0; i < pages; i++) {
                    long a = android.os.SystemClock.elapsedRealtime();
                    String text = pageText(doc, i);
                    inText += android.os.SystemClock.elapsedRealtime() - a;
                    chars += text.length();
                    read++;
                    if (!sink.page(i + 1, text)) break;
                }
            } finally {
                android.util.Log.d("PlainContentIndex", "pdfium " + file.getName() + ": " + read + " pages, open " + (opened - t0)
                        + " ms, text " + inText + " ms, total " + (android.os.SystemClock.elapsedRealtime() - t0) + " ms, " + chars + " chars");
                doc.close();
            }
        }
    }

    private static String pageText(PdfDocument doc, int index) {
        // the library hands back null for a page it cannot open; skip that page, not the whole file
        PdfPage page = doc.openPage(index);
        if (page == null) return "";
        try (PdfPage p = page; PdfTextPage text = p.openTextPage()) {
            if (text == null) return "";
            int count = text.textPageCountChars();
            if (count <= 0) return "";
            // not textPageGetText: its native byte-array path overflows a stack buffer on some pages (SIGABRT, stack protector)
            String s = text.textPageGetTextLegacy(0, count);
            return s == null ? "" : s;
        }
    }
}
