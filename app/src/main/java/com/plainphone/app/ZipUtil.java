package com.plainphone.app;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Safe reading of zip-based documents (Office, OpenDocument, EPUB) and their XML. */
final class ZipUtil {

    /** No single part of a document is read past this many bytes (a guard against zip bombs). */
    static final int MAX_ENTRY = 20 * 1024 * 1024;

    private ZipUtil() {}

    private static final class Limited extends InputStream {
        private final InputStream in;
        private long left;

        Limited(InputStream in, long limit) {
            this.in = in;
            this.left = limit;
        }

        @Override
        public int read() throws IOException {
            if (left <= 0) throw new IOException("part too large");
            int b = in.read();
            if (b >= 0) left--;
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            if (left <= 0) throw new IOException("part too large");
            int n = in.read(buf, off, (int) Math.min(len, left));
            if (n > 0) left -= n;
            return n;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    /** The stream of part {@code name}, or null when the document has no such part. */
    static InputStream open(ZipFile zip, String name) throws IOException {
        ZipEntry e = zip.getEntry(name);
        if (e == null || e.isDirectory()) return null;
        return new Limited(zip.getInputStream(e), MAX_ENTRY);
    }

    /** Names of the parts that start with {@code prefix} and end with {@code suffix}, in natural order (slide2 before slide10). */
    static List<String> names(ZipFile zip, String prefix, String suffix) {
        List<String> out = new ArrayList<>();
        Enumeration<? extends ZipEntry> all = zip.entries();
        while (all.hasMoreElements()) {
            ZipEntry e = all.nextElement();
            String n = e.getName();
            if (!e.isDirectory() && n.startsWith(prefix) && n.endsWith(suffix) && n.indexOf('/', prefix.length()) < 0) {
                out.add(n);
            }
        }
        Collections.sort(out, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return natural(a, b);
            }
        });
        return out;
    }

    static int natural(String a, String b) {
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char x = a.charAt(i), y = b.charAt(j);
            if (Character.isDigit(x) && Character.isDigit(y)) {
                int si = i, sj = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) i++;
                while (j < b.length() && Character.isDigit(b.charAt(j))) j++;
                long nx = Long.parseLong(a.substring(si, Math.min(i, si + 18)));
                long ny = Long.parseLong(b.substring(sj, Math.min(j, sj + 18)));
                if (nx != ny) return Long.compare(nx, ny);
            } else {
                if (x != y) return Character.compare(x, y);
                i++;
                j++;
            }
        }
        return Integer.compare(a.length() - i, b.length() - j);
    }

    /** An XML pull parser over {@code in}. Namespaces are not resolved: element names keep their prefix ("w:p"). */
    static XmlPullParser xml(InputStream in) throws IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser p = factory.newPullParser();
            p.setInput(in, null);
            return p;
        } catch (XmlPullParserException e) {
            throw new IOException("bad xml", e);
        }
    }

    /** Wrap the checked parser exception so callers only deal with IOException. */
    static int next(XmlPullParser p) throws IOException {
        try {
            return p.next();
        } catch (XmlPullParserException e) {
            throw new IOException("bad xml", e);
        }
    }
}
