package com.plainphone.app;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/** OpenDocument text, spreadsheets and presentations (.odt .ods .odp). */
final class OdfExtractor implements TextExtractor {

    @Override
    public boolean supports(String ext) {
        return ext.equals("odt") || ext.equals("ods") || ext.equals("odp");
    }

    @Override
    public void extract(File file, Sink sink) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            InputStream in = ZipUtil.open(zip, "content.xml");
            if (in == null) throw new Unreadable("failed", "no content part");
            try (InputStream s = in) {
                parse(ZipUtil.xml(s), sink);
            }
        } catch (ZipException e) {
            throw new Unreadable("failed", "not a valid document");
        }
    }

    private void parse(XmlPullParser p, Sink sink) throws IOException {
        StringBuilder acc = new StringBuilder();
        StringBuilder para = null;
        String locator = "";           // sheet name, "slide N" or "§ heading"
        String structural = "";        // sheet / slide locator, which headings must not override
        boolean heading = false, firstHeading = true;
        int slide = 0;
        int ev;
        while ((ev = ZipUtil.next(p)) != XmlPullParser.END_DOCUMENT) {
            if (sink.stop()) break;
            if (ev == XmlPullParser.START_TAG) {
                switch (p.getName()) {
                    case "table:table": {
                        flush(sink, acc, locator);
                        String name = p.getAttributeValue(null, "table:name");
                        structural = locator = name == null ? "" : name;
                        break;
                    }
                    case "draw:page":
                        flush(sink, acc, locator);
                        slide++;
                        structural = locator = "slide " + slide;
                        break;
                    case "text:h": para = new StringBuilder(); heading = true; break;
                    case "text:p": para = new StringBuilder(); heading = false; break;
                    case "text:s": case "text:tab": case "text:line-break": if (para != null) para.append(' '); break;
                    default:
                }
            } else if (ev == XmlPullParser.TEXT) {
                if (para != null) para.append(p.getText());
            } else if (ev == XmlPullParser.END_TAG) {
                String n = p.getName();
                if ((n.equals("text:p") || n.equals("text:h")) && para != null) {
                    String text = para.toString().trim();
                    para = null;
                    if (text.isEmpty()) continue;
                    if (heading && structural.isEmpty()) {
                        flush(sink, acc, locator);
                        locator = "§ " + text;
                        if (firstHeading) sink.title(text);
                        firstHeading = false;
                    }
                    acc.append(text).append('\n');
                    if (acc.length() >= Chunker.TARGET) flush(sink, acc, locator);
                }
            }
        }
        flush(sink, acc, locator);
    }

    private static void flush(Sink sink, StringBuilder acc, String locator) {
        if (acc.toString().trim().isEmpty()) {
            acc.setLength(0);
            return;
        }
        sink.chunk(locator, acc.toString());
        acc.setLength(0);
    }
}
