package com.plainphone.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Test helpers: files and zip documents made on the fly, and a sink that records what an extractor sends. */
final class TestFiles {

    private TestFiles() {}

    /** A zip with the given {name, content} parts. */
    static File zip(File dir, String name, String[]... parts) throws IOException {
        File f = new File(dir, name);
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(f))) {
            for (String[] p : parts) {
                z.putNextEntry(new ZipEntry(p[0]));
                z.write(p[1].getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return f;
    }

    static File text(File dir, String name, String content) throws IOException {
        return bytes(dir, name, content.getBytes(StandardCharsets.UTF_8));
    }

    static File bytes(File dir, String name, byte[] data) throws IOException {
        File f = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }
        return f;
    }

    /** Records every passage and the title. */
    static final class Recorder implements TextExtractor.Sink {
        final List<String[]> chunks = new ArrayList<>();
        String title;
        boolean stop;

        @Override
        public void chunk(String locator, String text) {
            chunks.add(new String[]{locator, text});
        }

        @Override
        public void title(String t) {
            if (title == null) title = t;
        }

        @Override
        public boolean stop() {
            return stop;
        }

        String all() {
            StringBuilder sb = new StringBuilder();
            for (String[] c : chunks) sb.append(c[1]).append('\n');
            return sb.toString();
        }
    }
}
