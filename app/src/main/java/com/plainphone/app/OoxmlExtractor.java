package com.plainphone.app;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/** Word (.docx), Excel (.xlsx) and PowerPoint (.pptx) documents. */
final class OoxmlExtractor implements TextExtractor {

    private static final int MAX_CELLS = 200_000;

    @Override
    public boolean supports(String ext) {
        return ext.equals("docx") || ext.equals("xlsx") || ext.equals("pptx");
    }

    @Override
    public void extract(File file, Sink sink) throws IOException {
        String ext = PlainTextExtractor.extensionOf(file.getName());
        try (ZipFile zip = new ZipFile(file)) {
            switch (ext) {
                case "docx": docx(zip, sink); break;
                case "xlsx": xlsx(zip, sink); break;
                default: pptx(zip, sink);
            }
        } catch (ZipException e) {
            throw new Unreadable("failed", "not a valid document");
        }
    }

    // ---------------------------------------------------------------------- docx

    private void docx(ZipFile zip, Sink sink) throws IOException {
        InputStream in = ZipUtil.open(zip, "word/document.xml");
        if (in == null) throw new Unreadable("failed", "no document part");
        try (InputStream s = in) {
            XmlPullParser p = ZipUtil.xml(s);
            StringBuilder acc = new StringBuilder();
            String heading = null;
            StringBuilder para = null;
            boolean isHeading = false, inText = false, first = true;
            int ev;
            while ((ev = ZipUtil.next(p)) != XmlPullParser.END_DOCUMENT) {
                if (sink.stop()) break;
                if (ev == XmlPullParser.START_TAG) {
                    switch (p.getName()) {
                        case "w:p": para = new StringBuilder(); isHeading = false; break;
                        case "w:pStyle": {
                            String v = p.getAttributeValue(null, "w:val");
                            if (v != null && (v.startsWith("Heading") || v.startsWith("heading") || v.equals("Title"))) isHeading = true;
                            break;
                        }
                        case "w:t": inText = true; break;
                        case "w:tab": case "w:br": case "w:cr": if (para != null) para.append(' '); break;
                        default:
                    }
                } else if (ev == XmlPullParser.TEXT) {
                    if (inText && para != null) para.append(p.getText());
                } else if (ev == XmlPullParser.END_TAG) {
                    String n = p.getName();
                    if (n.equals("w:t")) {
                        inText = false;
                    } else if (n.equals("w:p") && para != null) {
                        String text = para.toString().trim();
                        para = null;
                        if (text.isEmpty()) continue;
                        if (isHeading) {
                            flush(sink, acc, heading);
                            heading = text;
                            if (first) sink.title(text);
                        }
                        first = false;
                        acc.append(text).append('\n');
                        if (acc.length() >= Chunker.TARGET) flush(sink, acc, heading);
                    }
                }
            }
            flush(sink, acc, heading);
        }
    }

    private static void flush(Sink sink, StringBuilder acc, String heading) {
        if (acc.toString().trim().isEmpty()) {
            acc.setLength(0);
            return;
        }
        sink.chunk(heading == null ? "" : "§ " + heading, acc.toString());
        acc.setLength(0);
    }

    // ---------------------------------------------------------------------- xlsx

    private void xlsx(ZipFile zip, Sink sink) throws IOException {
        List<String> shared = sharedStrings(zip);
        int cells = 0;
        for (String[] sheet : sheets(zip)) {
            InputStream in = ZipUtil.open(zip, sheet[1]);
            if (in == null) continue;
            try (InputStream s = in) {
                XmlPullParser p = ZipUtil.xml(s);
                StringBuilder acc = new StringBuilder();
                String type = "";
                StringBuilder val = new StringBuilder();
                boolean inV = false, inT = false;
                int ev;
                while ((ev = ZipUtil.next(p)) != XmlPullParser.END_DOCUMENT) {
                    if (sink.stop() || cells > MAX_CELLS) return;
                    if (ev == XmlPullParser.START_TAG) {
                        switch (p.getName()) {
                            case "c": type = p.getAttributeValue(null, "t"); type = type == null ? "" : type; val.setLength(0); break;
                            case "v": inV = true; break;
                            case "t": inT = true; break;
                            default:
                        }
                    } else if (ev == XmlPullParser.TEXT) {
                        if (inV || inT) val.append(p.getText());
                    } else if (ev == XmlPullParser.END_TAG) {
                        switch (p.getName()) {
                            case "v": inV = false; break;
                            case "t": inT = false; break;
                            case "c": {
                                String cell = null;
                                if (type.equals("s")) {
                                    try {
                                        int idx = Integer.parseInt(val.toString().trim());
                                        if (idx >= 0 && idx < shared.size()) cell = shared.get(idx);
                                    } catch (NumberFormatException ignored) {
                                        // not an index: skip the cell
                                    }
                                } else if (type.equals("inlineStr") || type.equals("str")) {
                                    cell = val.toString();
                                }
                                if (cell != null && !cell.trim().isEmpty()) {
                                    if (acc.length() > 0 && acc.charAt(acc.length() - 1) != '\n') acc.append(" | ");
                                    acc.append(cell.trim());
                                    cells++;
                                }
                                break;
                            }
                            case "row":
                                if (acc.length() > 0) acc.append('\n');
                                if (acc.length() >= Chunker.TARGET) {
                                    sink.chunk(sheet[0], acc.toString());
                                    acc.setLength(0);
                                }
                                break;
                            default:
                        }
                    }
                }
                if (acc.toString().trim().length() > 0) sink.chunk(sheet[0], acc.toString());
            }
        }
    }

    private List<String> sharedStrings(ZipFile zip) throws IOException {
        List<String> out = new ArrayList<>();
        InputStream in = ZipUtil.open(zip, "xl/sharedStrings.xml");
        if (in == null) return out;
        try (InputStream s = in) {
            XmlPullParser p = ZipUtil.xml(s);
            StringBuilder sb = new StringBuilder();
            boolean inSi = false, inT = false, inPhonetic = false;
            int ev;
            while ((ev = ZipUtil.next(p)) != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    switch (p.getName()) {
                        case "si": inSi = true; sb.setLength(0); break;
                        case "t": inT = true; break;
                        case "rPh": inPhonetic = true; break;
                        default:
                    }
                } else if (ev == XmlPullParser.TEXT) {
                    if (inSi && inT && !inPhonetic) sb.append(p.getText());
                } else if (ev == XmlPullParser.END_TAG) {
                    switch (p.getName()) {
                        case "si": inSi = false; out.add(sb.toString()); break;
                        case "t": inT = false; break;
                        case "rPh": inPhonetic = false; break;
                        default:
                    }
                }
            }
        }
        return out;
    }

    /** {name, part path} of every sheet, in workbook order. */
    private List<String[]> sheets(ZipFile zip) throws IOException {
        Map<String, String> targets = new HashMap<>();
        InputStream rels = ZipUtil.open(zip, "xl/_rels/workbook.xml.rels");
        if (rels != null) {
            try (InputStream s = rels) {
                XmlPullParser p = ZipUtil.xml(s);
                int ev;
                while ((ev = ZipUtil.next(p)) != XmlPullParser.END_DOCUMENT) {
                    if (ev == XmlPullParser.START_TAG && p.getName().equals("Relationship")) {
                        String id = p.getAttributeValue(null, "Id"), t = p.getAttributeValue(null, "Target");
                        if (id != null && t != null) targets.put(id, t.startsWith("/") ? t.substring(1) : "xl/" + t);
                    }
                }
            }
        }
        List<String[]> out = new ArrayList<>();
        InputStream wb = ZipUtil.open(zip, "xl/workbook.xml");
        if (wb != null) {
            try (InputStream s = wb) {
                XmlPullParser p = ZipUtil.xml(s);
                int ev;
                while ((ev = ZipUtil.next(p)) != XmlPullParser.END_DOCUMENT) {
                    if (ev == XmlPullParser.START_TAG && p.getName().equals("sheet")) {
                        String name = p.getAttributeValue(null, "name"), rid = p.getAttributeValue(null, "r:id");
                        String path = rid == null ? null : targets.get(rid);
                        if (name != null && path != null) out.add(new String[]{name, path});
                    }
                }
            }
        }
        if (out.isEmpty()) {
            int i = 1;
            for (String n : ZipUtil.names(zip, "xl/worksheets/sheet", ".xml")) out.add(new String[]{"sheet " + i++, n});
        }
        return out;
    }

    // ---------------------------------------------------------------------- pptx

    private void pptx(ZipFile zip, Sink sink) throws IOException {
        int n = 0;
        for (String name : ZipUtil.names(zip, "ppt/slides/slide", ".xml")) {
            n++;
            if (sink.stop()) return;
            InputStream in = ZipUtil.open(zip, name);
            if (in == null) continue;
            try (InputStream s = in) {
                XmlPullParser p = ZipUtil.xml(s);
                StringBuilder text = new StringBuilder();
                boolean inT = false;
                int ev;
                while ((ev = ZipUtil.next(p)) != XmlPullParser.END_DOCUMENT) {
                    if (ev == XmlPullParser.START_TAG) {
                        if (p.getName().equals("a:t")) inT = true;
                    } else if (ev == XmlPullParser.TEXT) {
                        if (inT) text.append(p.getText());
                    } else if (ev == XmlPullParser.END_TAG) {
                        if (p.getName().equals("a:t")) inT = false;
                        else if (p.getName().equals("a:p")) text.append('\n');
                    }
                }
                if (text.toString().trim().length() > 0) sink.chunk("slide " + n, text.toString());
            }
        }
    }
}
