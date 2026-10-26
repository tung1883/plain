package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ExtractorsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private TestFiles.Recorder run(TextExtractor ex, File f) throws IOException {
        TestFiles.Recorder r = new TestFiles.Recorder();
        ex.extract(f, r);
        return r;
    }

    // ---------------------------------------------------------------- plain text

    @Test
    public void markdownChunksFollowHeadings() throws IOException {
        File f = TestFiles.text(tmp.getRoot(), "notes.md",
                "# Plan\nWe ship in June.\n\n## Risks\nBudget is tight.\nStaffing too.\n");
        TestFiles.Recorder r = run(new PlainTextExtractor(), f);
        assertEquals("Plan", r.title);
        assertEquals(2, r.chunks.size());
        assertEquals("§ Plan", r.chunks.get(0)[0]);
        assertTrue(r.chunks.get(0)[1].contains("ship in June"));
        assertEquals("§ Risks", r.chunks.get(1)[0]);
        assertTrue(r.chunks.get(1)[1].contains("Staffing"));
    }

    @Test
    public void plainTextGetsLineNumbers() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 200; i++) sb.append("line number ").append(i).append(" with some filler text here\n");
        TestFiles.Recorder r = run(new PlainTextExtractor(), TestFiles.text(tmp.getRoot(), "a.txt", sb.toString()));
        assertTrue(r.chunks.size() > 3);
        assertEquals("L1", r.chunks.get(0)[0]);
        assertTrue(r.chunks.get(1)[0].startsWith("L"));
        assertFalse(r.chunks.get(1)[0].equals("L1"));
    }

    @Test
    public void latin1AndUtf16FilesAreDecoded() throws IOException {
        File latin = TestFiles.bytes(tmp.getRoot(), "l.txt", "café crème".getBytes(StandardCharsets.ISO_8859_1));
        assertTrue(run(new PlainTextExtractor(), latin).all().contains("café crème"));

        byte[] body = "Tài liệu".getBytes(StandardCharsets.UTF_16LE);
        byte[] withBom = new byte[body.length + 2];
        withBom[0] = (byte) 0xFF;
        withBom[1] = (byte) 0xFE;
        System.arraycopy(body, 0, withBom, 2, body.length);
        assertTrue(run(new PlainTextExtractor(), TestFiles.bytes(tmp.getRoot(), "u.txt", withBom)).all().contains("Tài liệu"));
    }

    @Test
    public void binaryAndEmptyFilesAreUnreadable() throws IOException {
        try {
            run(new PlainTextExtractor(), TestFiles.bytes(tmp.getRoot(), "b.txt", new byte[]{1, 2, 0, 3, 0, 0, 9, 9}));
            fail("binary should be rejected");
        } catch (TextExtractor.Unreadable e) {
            assertEquals("failed", e.status);
        }
        try {
            run(new PlainTextExtractor(), TestFiles.bytes(tmp.getRoot(), "e.txt", new byte[0]));
            fail("empty should be rejected");
        } catch (TextExtractor.Unreadable e) {
            assertEquals("empty", e.status);
        }
    }

    @Test
    public void htmlIsStrippedToText() throws IOException {
        File f = TestFiles.text(tmp.getRoot(), "p.html",
                "<html><head><title>My &amp; Page</title><style>p{}</style></head><body><h1>Hello</h1>"
                        + "<script>var x=1;</script><p>First &lt;para&gt;</p><p>Second</p></body></html>");
        TestFiles.Recorder r = run(new PlainTextExtractor(), f);
        assertEquals("My & Page", r.title);
        String all = r.all();
        assertTrue(all.contains("Hello"));
        assertTrue(all.contains("First <para>"));
        assertFalse(all.contains("var x"));
        assertFalse(all.contains("p{}"));
    }

    @Test
    public void textCapReachedStopsEarly() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) sb.append("word").append(i).append(' ');
        File f = TestFiles.text(tmp.getRoot(), "big.txt", sb.toString());
        ExtractRunner.Result r = ExtractRunner.run(new PlainTextExtractor(), f, 3000, () -> false);
        assertEquals("ok", r.status);
        int total = 0;
        for (ExtractRunner.Passage p : r.passages) total += p.body.length();
        assertTrue(total >= 3000 && total < 6000);
    }

    // ---------------------------------------------------------------------- docx

    @Test
    public void docxParagraphsGroupedUnderHeadings() throws IOException {
        String doc = "<?xml version=\"1.0\"?><w:document><w:body>"
                + "<w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr><w:r><w:t>Budget Report</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t xml:space=\"preserve\">Revenue grew by </w:t></w:r><w:r><w:t>twelve percent.</w:t></w:r></w:p>"
                + "<w:p><w:pPr><w:pStyle w:val=\"Heading2\"/></w:pPr><w:r><w:t>Risks</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t>Currency swings.</w:t></w:r></w:p>"
                + "</w:body></w:document>";
        File f = TestFiles.zip(tmp.getRoot(), "r.docx", new String[]{"word/document.xml", doc});
        TestFiles.Recorder r = run(new OoxmlExtractor(), f);
        assertEquals("Budget Report", r.title);
        assertEquals(2, r.chunks.size());
        assertEquals("§ Budget Report", r.chunks.get(0)[0]);
        assertTrue(r.chunks.get(0)[1].contains("Revenue grew by twelve percent."));
        assertEquals("§ Risks", r.chunks.get(1)[0]);
        assertTrue(r.chunks.get(1)[1].contains("Currency swings."));
    }

    @Test
    public void brokenZipIsUnreadable() throws IOException {
        try {
            run(new OoxmlExtractor(), TestFiles.text(tmp.getRoot(), "x.docx", "not a zip"));
            fail();
        } catch (TextExtractor.Unreadable e) {
            assertEquals("failed", e.status);
        }
    }

    // ---------------------------------------------------------------------- xlsx

    @Test
    public void xlsxReadsSharedAndInlineStringsPerSheet() throws IOException {
        File f = TestFiles.zip(tmp.getRoot(), "b.xlsx",
                new String[]{"xl/workbook.xml", "<workbook><sheets><sheet name=\"Sales\" sheetId=\"1\" r:id=\"rId1\"/>"
                        + "<sheet name=\"Costs\" sheetId=\"2\" r:id=\"rId2\"/></sheets></workbook>"},
                new String[]{"xl/_rels/workbook.xml.rels", "<Relationships>"
                        + "<Relationship Id=\"rId1\" Target=\"worksheets/sheet1.xml\"/>"
                        + "<Relationship Id=\"rId2\" Target=\"worksheets/sheet2.xml\"/></Relationships>"},
                new String[]{"xl/sharedStrings.xml", "<sst><si><t>Apples</t></si><si><r><t>Ban</t></r><r><t>anas</t></r></si>"
                        + "<si><t>Hidden</t><rPh><t>PHON</t></rPh></si></sst>"},
                new String[]{"xl/worksheets/sheet1.xml", "<worksheet><sheetData>"
                        + "<row><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\"><v>42</v></c><c r=\"C1\" t=\"s\"><v>2</v></c></row>"
                        + "<row><c r=\"A2\" t=\"s\"><v>1</v></c></row></sheetData></worksheet>"},
                new String[]{"xl/worksheets/sheet2.xml", "<worksheet><sheetData><row>"
                        + "<c t=\"inlineStr\"><is><t>Rent</t></is></c></row></sheetData></worksheet>"});
        TestFiles.Recorder r = run(new OoxmlExtractor(), f);
        assertEquals(2, r.chunks.size());
        assertEquals("Sales", r.chunks.get(0)[0]);
        assertEquals("Apples | Hidden\nBananas\n", r.chunks.get(0)[1]);
        assertEquals("Costs", r.chunks.get(1)[0]);
        assertTrue(r.chunks.get(1)[1].contains("Rent"));
        assertFalse(r.all().contains("42"));
        assertFalse(r.all().contains("PHON"));
    }

    // ---------------------------------------------------------------------- pptx

    @Test
    public void pptxSlidesInNumericOrder() throws IOException {
        String slide = "<p:sld><p:cSld><p:spTree><p:sp><p:txBody><a:p><a:r><a:t>%s</a:t></a:r></a:p>"
                + "<a:p><a:r><a:t>%s</a:t></a:r></a:p></p:txBody></p:sp></p:spTree></p:cSld></p:sld>";
        File f = TestFiles.zip(tmp.getRoot(), "d.pptx",
                new String[]{"ppt/slides/slide10.xml", String.format(slide, "Tenth", "last")},
                new String[]{"ppt/slides/slide2.xml", String.format(slide, "Second", "middle")},
                new String[]{"ppt/slides/slide1.xml", String.format(slide, "Launch plan", "Ship in Q3")});
        TestFiles.Recorder r = run(new OoxmlExtractor(), f);
        assertEquals(3, r.chunks.size());
        assertEquals("slide 1", r.chunks.get(0)[0]);
        assertTrue(r.chunks.get(0)[1].contains("Launch plan"));
        assertTrue(r.chunks.get(0)[1].contains("Ship in Q3"));
        assertTrue(r.chunks.get(1)[1].contains("Second"));
        assertTrue(r.chunks.get(2)[1].contains("Tenth"));
    }

    // ----------------------------------------------------------------------- odf

    @Test
    public void odtHeadingsParagraphsAndSpaces() throws IOException {
        File f = TestFiles.zip(tmp.getRoot(), "n.odt", new String[]{"content.xml",
                "<office:document-content><office:body><office:text>"
                        + "<text:h text:outline-level=\"1\">Meeting Notes</text:h>"
                        + "<text:p>We agreed on <text:span>the plan</text:span>.</text:p>"
                        + "<text:p>Next<text:s/>steps</text:p></office:text></office:body></office:document-content>"});
        TestFiles.Recorder r = run(new OdfExtractor(), f);
        assertEquals("Meeting Notes", r.title);
        assertEquals(1, r.chunks.size());
        assertEquals("§ Meeting Notes", r.chunks.get(0)[0]);
        assertTrue(r.chunks.get(0)[1].contains("We agreed on the plan."));
        assertTrue(r.chunks.get(0)[1].contains("Next steps"));
    }

    @Test
    public void odsAndOdpUseSheetAndSlideLocators() throws IOException {
        File ods = TestFiles.zip(tmp.getRoot(), "s.ods", new String[]{"content.xml",
                "<office:document-content><office:body><office:spreadsheet>"
                        + "<table:table table:name=\"Budget\"><table:table-row><table:table-cell><text:p>Rent</text:p></table:table-cell>"
                        + "</table:table-row></table:table></office:spreadsheet></office:body></office:document-content>"});
        TestFiles.Recorder a = run(new OdfExtractor(), ods);
        assertEquals("Budget", a.chunks.get(0)[0]);

        File odp = TestFiles.zip(tmp.getRoot(), "p.odp", new String[]{"content.xml",
                "<office:document-content><office:body><office:presentation>"
                        + "<draw:page draw:name=\"page1\"><draw:frame><draw:text-box><text:p>Hello slide</text:p></draw:text-box></draw:frame></draw:page>"
                        + "<draw:page draw:name=\"page2\"><draw:frame><draw:text-box><text:p>Second slide</text:p></draw:text-box></draw:frame></draw:page>"
                        + "</office:presentation></office:body></office:document-content>"});
        TestFiles.Recorder b = run(new OdfExtractor(), odp);
        assertEquals(2, b.chunks.size());
        assertEquals("slide 1", b.chunks.get(0)[0]);
        assertEquals("slide 2", b.chunks.get(1)[0]);
    }

    // ---------------------------------------------------------------------- epub

    @Test
    public void epubChaptersInSpineOrder() throws IOException {
        File f = TestFiles.zip(tmp.getRoot(), "book.epub",
                new String[]{"META-INF/container.xml", "<container><rootfiles><rootfile full-path=\"OEBPS/content.opf\"/></rootfiles></container>"},
                new String[]{"OEBPS/content.opf", "<package><metadata><dc:title>My Book</dc:title></metadata>"
                        + "<manifest><item id=\"c1\" href=\"ch1.xhtml\"/><item id=\"c2\" href=\"text/ch%202.xhtml\"/></manifest>"
                        + "<spine><itemref idref=\"c2\"/><itemref idref=\"c1\"/></spine></package>"},
                new String[]{"OEBPS/ch1.xhtml", "<html><head><title>T</title></head><body><h1>Chapter One</h1><p>It was a dark night.</p></body></html>"},
                new String[]{"OEBPS/text/ch 2.xhtml", "<html><body><p>The morning came.</p></body></html>"});
        TestFiles.Recorder r = run(new EpubExtractor(), f);
        assertEquals("My Book", r.title);
        assertEquals(2, r.chunks.size());
        assertEquals("ch. 1", r.chunks.get(0)[0]);                 // spine order: c2 first, no heading
        assertTrue(r.chunks.get(0)[1].contains("The morning came."));
        assertEquals("Chapter One", r.chunks.get(1)[0]);
        assertTrue(r.chunks.get(1)[1].contains("dark night"));
    }

    // ------------------------------------------------------------------------ rtf

    @Test
    public void rtfControlWordsAndEscapes() {
        String rtf = "{\\rtf1\\ansi{\\fonttbl{\\f0 Arial;}}\\f0 Hello \\b world\\b0\\par Second line caf\\'e9 \\u8364?{\\*\\generator x}}";
        assertEquals("Hello world\nSecond line café €", RtfExtractor.strip(rtf));
    }

    // -------------------------------------------------------------------- registry

    @Test
    public void registryPicksByExtensionAndGroup() {
        assertTrue(Extractors.forExt("md") instanceof PlainTextExtractor);
        assertTrue(Extractors.forExt("docx") instanceof OoxmlExtractor);
        assertTrue(Extractors.forExt("odt") instanceof OdfExtractor);
        assertTrue(Extractors.forExt("epub") instanceof EpubExtractor);
        assertTrue(Extractors.forExt("rtf") instanceof RtfExtractor);
        assertNull(Extractors.forExt("exe"));
        assertEquals(Extractors.TEXT, Extractors.groupOf("kt"));
        assertEquals(Extractors.OFFICE, Extractors.groupOf("pptx"));
        assertEquals(Extractors.PDF, Extractors.groupOf("pdf"));
        assertNull(Extractors.groupOf("png"));
        assertTrue(Arrays.asList(Extractors.TEXT, Extractors.OFFICE, Extractors.EPUB, Extractors.PDF)
                .containsAll(Extractors.allGroups()));
    }

    @Test
    public void runnerTurnsFailuresIntoStatuses() throws IOException {
        ExtractRunner.Result empty = ExtractRunner.run(new PlainTextExtractor(),
                TestFiles.bytes(tmp.getRoot(), "z.txt", new byte[0]), 1000, () -> false);
        assertEquals("empty", empty.status);
        ExtractRunner.Result bad = ExtractRunner.run(new OoxmlExtractor(),
                TestFiles.text(tmp.getRoot(), "z.docx", "nope"), 1000, () -> false);
        assertEquals("failed", bad.status);
        ExtractRunner.Result blank = ExtractRunner.run(new PlainTextExtractor(),
                TestFiles.text(tmp.getRoot(), "w.txt", "   \n\n  "), 1000, () -> false);
        assertEquals("empty", blank.status);
        ExtractRunner.Result ok = ExtractRunner.run(new PlainTextExtractor(),
                TestFiles.text(tmp.getRoot(), "ok.txt", "hello world"), 1000, () -> false);
        assertEquals("ok", ok.status);
        assertEquals(1, ok.passages.size());
    }

    @Test
    public void cancelStopsExtraction() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) sb.append("line ").append(i).append('\n');
        ExtractRunner.Result r = ExtractRunner.run(new PlainTextExtractor(),
                TestFiles.text(tmp.getRoot(), "c.txt", sb.toString()), 100000, () -> true);
        assertTrue(r.passages.size() <= 1);
    }
}
