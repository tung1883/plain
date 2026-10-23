package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import org.junit.Test;

public class PdfTest {

    private static PdfTextSource pages(String... texts) {
        return (file, sink) -> {
            for (int i = 0; i < texts.length; i++) if (!sink.page(i + 1, texts[i])) break;
        };
    }

    private static String filler(int n) {
        return "text ".repeat(n);
    }

    private static TestFiles.Recorder run(PdfTextSource source) throws IOException {
        TestFiles.Recorder r = new TestFiles.Recorder();
        new PdfExtractor(source).extract(new File("x.pdf"), r);
        return r;
    }

    @Test
    public void policyTreatsTextPagesAsIndexable() {
        assertTrue(PdfPolicy.hasText(new int[]{500, 800, 300}));
        assertTrue(PdfPolicy.hasText(new int[]{500, 0, 700, 600, 650}));          // one empty page in five
        assertFalse(PdfPolicy.hasText(new int[]{}));
        assertFalse(PdfPolicy.hasText(null));
    }

    @Test
    public void policyRejectsScans() {
        assertFalse(PdfPolicy.hasText(new int[]{0, 0, 0, 0}));
        assertFalse(PdfPolicy.hasText(new int[]{5, 12, 3, 0, 8}));                // stray characters only
        // mostly empty pages, a few with text: over 80% empty
        assertFalse(PdfPolicy.hasText(new int[]{900, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        // exactly 80% empty still passes
        assertTrue(PdfPolicy.hasText(new int[]{900, 900, 0, 0, 0, 0, 0, 0, 0, 0}));
    }

    @Test
    public void textPdfGivesOnePassagePerPage() throws IOException {
        TestFiles.Recorder r = run(pages(filler(40), filler(50), filler(60)));
        assertEquals(3, r.chunks.size());
        assertEquals("p. 1", r.chunks.get(0)[0]);
        assertEquals("p. 3", r.chunks.get(2)[0]);
    }

    @Test
    public void emptyPagesInATextPdfAreLeftOut() throws IOException {
        TestFiles.Recorder r = run(pages(filler(40), "", filler(50), filler(50), filler(50)));
        assertEquals(4, r.chunks.size());
        assertEquals("p. 3", r.chunks.get(1)[0]);
    }

    @Test
    public void scannedPdfIsReportedAndNothingIsStored() throws IOException {
        try {
            run(pages("", "", "  ", ""));
            fail("a scan should be refused");
        } catch (TextExtractor.Unreadable e) {
            assertEquals("scanned", e.status);
        }
    }

    @Test
    public void pdfWithNoPagesIsEmpty() throws IOException {
        try {
            run(pages());
            fail();
        } catch (TextExtractor.Unreadable e) {
            assertEquals("empty", e.status);
        }
    }

    @Test
    public void lockedAndBrokenFilesKeepTheirStatus() {
        try {
            run((file, sink) -> {
                throw new TextExtractor.Unreadable("locked", "password protected");
            });
            fail();
        } catch (TextExtractor.Unreadable e) {
            assertEquals("locked", e.status);
        } catch (IOException e) {
            fail("wrong exception");
        }
        ExtractRunner.Result r = ExtractRunner.run(new PdfExtractor((file, sink) -> {
            throw new IOException("corrupt");
        }), new File("x.pdf"), 1000, () -> false);
        assertEquals("failed", r.status);
    }

    @Test
    public void readingStopsAtThePageLimit() throws IOException {
        PdfTextSource many = (file, sink) -> {
            for (int i = 1; i <= PdfPolicy.MAX_PAGES + 50; i++) if (!sink.page(i, filler(5))) break;
        };
        TestFiles.Recorder r = run(many);
        assertEquals(PdfPolicy.MAX_PAGES, r.chunks.size());
    }

    @Test
    public void readingStopsWhenCancelled() throws IOException {
        TestFiles.Recorder r = new TestFiles.Recorder();
        r.stop = true;
        try {
            new PdfExtractor(pages(filler(40), filler(40))).extract(new File("x.pdf"), r);
            fail("nothing was read, so there is nothing to index");
        } catch (TextExtractor.Unreadable e) {
            assertEquals("empty", e.status);
        }
    }

    @Test
    public void runnerMapsPdfStatuses() {
        ExtractRunner.Result scan = ExtractRunner.run(new PdfExtractor(pages("", "")), new File("x.pdf"), 1000, () -> false);
        assertEquals("scanned", scan.status);
        assertTrue(scan.passages.isEmpty());
        ExtractRunner.Result ok = ExtractRunner.run(new PdfExtractor(pages(filler(60), filler(60))), new File("x.pdf"), 100000, () -> false);
        assertEquals("ok", ok.status);
        assertEquals(2, ok.passages.size());
        assertEquals("p. 2", ok.passages.get(1).locator);
    }
}
