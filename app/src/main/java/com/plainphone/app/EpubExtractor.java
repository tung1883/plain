package com.plainphone.app;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/** EPUB books: the chapters in reading order, one passage group per chapter. */
final class EpubExtractor implements TextExtractor {

    private static final Pattern HEADING = Pattern.compile("(?is)<h[1-3][^>]*>(.*?)</h[1-3]>");
    private static final Pattern TITLE = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");

    @Override
    public boolean supports(String ext) {
        return ext.equals("epub");
    }

    @Override
    public void extract(File file, Sink sink) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            String opfPath = rootFile(zip);
            if (opfPath == null) throw new Unreadable("failed", "no package file");

            Map<String, String> manifest = new HashMap<>();
            List<String> spine = new ArrayList<>();
            readPackage(zip, opfPath, manifest, spine, sink);

            String base = opfPath.contains("/") ? opfPath.substring(0, opfPath.lastIndexOf('/') + 1) : "";
            int chapter = 0;
            for (String idref : spine) {
                if (sink.stop()) return;
                String href = manifest.get(idref);
                if (href == null) continue;
                chapter++;
                String path = base + URLDecoder.decode(href.replaceAll("#.*$", ""), "UTF-8");
                InputStream in = ZipUtil.open(zip, path);
                if (in == null) continue;
                String html;
                try (InputStream s = in) {
                    html = new String(readAll(s), StandardCharsets.UTF_8);
                }
                String locator = "ch. " + chapter;
                Matcher h = HEADING.matcher(html);
                Matcher t = TITLE.matcher(html);
                if (h.find()) locator = HtmlText.strip(h.group(1));
                else if (t.find()) locator = HtmlText.strip(t.group(1));
                if (locator.isEmpty()) locator = "ch. " + chapter;
                String text = HtmlText.strip(html);
                if (!text.isEmpty()) sink.chunk(locator, text);
            }
        } catch (ZipException e) {
            throw new Unreadable("failed", "not a valid book");
        }
    }

    private static String rootFile(ZipFile zip) throws IOException {
        InputStream in = ZipUtil.open(zip, "META-INF/container.xml");
        if (in == null) return null;
        try (InputStream s = in) {
            XmlPullParser p = ZipUtil.xml(s);
            int ev;
            while ((ev = ZipUtil.next(p)) != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && p.getName().equals("rootfile")) {
                    return p.getAttributeValue(null, "full-path");
                }
            }
        }
        return null;
    }

    private static void readPackage(ZipFile zip, String opfPath, Map<String, String> manifest,
                                    List<String> spine, Sink sink) throws IOException {
        InputStream in = ZipUtil.open(zip, opfPath);
        if (in == null) throw new Unreadable("failed", "no package file");
        try (InputStream s = in) {
            XmlPullParser p = ZipUtil.xml(s);
            boolean inTitle = false;
            StringBuilder title = new StringBuilder();
            int ev;
            while ((ev = ZipUtil.next(p)) != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    switch (p.getName()) {
                        case "item": {
                            String id = p.getAttributeValue(null, "id"), href = p.getAttributeValue(null, "href");
                            if (id != null && href != null) manifest.put(id, href);
                            break;
                        }
                        case "itemref": {
                            String idref = p.getAttributeValue(null, "idref");
                            if (idref != null) spine.add(idref);
                            break;
                        }
                        case "dc:title": inTitle = true; break;
                        default:
                    }
                } else if (ev == XmlPullParser.TEXT) {
                    if (inTitle) title.append(p.getText());
                } else if (ev == XmlPullParser.END_TAG && p.getName().equals("dc:title")) {
                    inTitle = false;
                }
            }
            if (title.toString().trim().length() > 0) sink.title(title.toString().trim());
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }
}
