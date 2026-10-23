package com.plainphone.app;

import android.content.Context;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

/** Reads PDF text with PdfBox for Android. The file is opened from disk, never held in memory whole. */
final class PdfBoxSource implements PdfTextSource {

    private static boolean ready;

    PdfBoxSource(Context context) {
        synchronized (PdfBoxSource.class) {
            if (!ready) {
                PDFBoxResourceLoader.init(context.getApplicationContext());
                ready = true;
            }
        }
    }

    /** Thrown from the stripper to stop reading once the sink has had enough. */
    private static final class Stop extends RuntimeException {
        Stop() {
            super(null, null, false, false);
        }
    }

    @Override
    public void read(File file, PageSink sink) throws IOException {
        PDDocument doc;
        try {
            // a little memory before PdfBox spills to a scratch file: on an SD card every spill is slow
            doc = PDDocument.load(file, MemoryUsageSetting.setupMixed(24L * 1024 * 1024));
        } catch (InvalidPasswordException e) {
            throw new TextExtractor.Unreadable("locked", "password protected");
        }
        try {
            final int pages = Math.min(doc.getNumberOfPages(), PdfPolicy.MAX_PAGES);
            final java.io.StringWriter out = new java.io.StringWriter();
            // one pass over the document, the text of each page handed over as soon as it is read
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void endPage(com.tom_roush.pdfbox.pdmodel.PDPage page) throws IOException {
                    super.endPage(page);
                    String text = out.toString();
                    out.getBuffer().setLength(0);
                    int number = getCurrentPageNo();
                    if (!sink.page(number, text) || number >= pages) throw new Stop();
                }
            };
            stripper.setSortByPosition(false);
            stripper.setEndPage(pages);
            try {
                stripper.writeText(doc, out);
            } catch (Stop done) {
                // the sink had enough
            }
        } finally {
            doc.close();
        }
    }
}
