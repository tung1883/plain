package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class IndexedTreeTest {

    private static ContentDb.Entry e(String path, String status) {
        return new ContentDb.Entry(path, "pdf", status, null, 10);
    }

    private static List<String[]> roots() {
        return Arrays.asList(new String[]{"/storage/emulated/0", "Internal only"}, new String[]{"/storage/1234-ABCD", "SD card"});
    }

    private static List<IndexedTree.Node> sample() {
        return IndexedTree.build(Arrays.asList(
                e("/storage/emulated/0/Documents/Books/a.pdf", "ok"),
                e("/storage/emulated/0/Documents/Books/b.pdf", "scanned"),
                e("/storage/emulated/0/Documents/notes.txt", "ok"),
                e("/storage/emulated/0/Download/x.pdf", "ok"),
                e("/storage/1234-ABCD/Backup/y.pdf", "ok"),
                e("/data/other/z.txt", "ok")), roots());
    }

    @Test
    public void filesGoUnderTheirStorageAndFolders() {
        List<IndexedTree.Node> top = sample();
        assertEquals(Arrays.asList("Internal only", "Other", "SD card"), names(top));
        IndexedTree.Node internal = top.get(0);
        assertEquals(4, internal.total);
        assertEquals(1, internal.bad);
        IndexedTree.Node docs = internal.dirs.get("Documents");
        assertNotNull(docs);
        assertEquals(3, docs.total);
        assertEquals(1, docs.files.size());                       // notes.txt directly in Documents
        assertEquals(2, docs.dirs.get("Books").files.size());
    }

    @Test
    public void notReadFilesComeFirstInAFolder() {
        IndexedTree.Node books = sample().get(0).dirs.get("Documents").dirs.get("Books");
        assertEquals("scanned", books.files.get(0).status);
        assertEquals("ok", books.files.get(1).status);
    }

    @Test
    public void storagesAndFoldersWithProblemsStartOpen() {
        List<IndexedTree.Node> top = sample();
        List<IndexedTree.Row> rows = IndexedTree.rows(top, new HashSet<String>());
        List<String> shown = new ArrayList<>();
        for (IndexedTree.Row r : rows) shown.add(r.folder != null ? "d:" + r.folder.name : "f:" + r.file.path.substring(r.file.path.lastIndexOf('/') + 1));
        // Documents and Books have a problem below them, so they are open; Download has none, so it is closed
        assertEquals(Arrays.asList("d:Internal only", "d:Documents", "d:Books", "f:b.pdf", "f:a.pdf", "f:notes.txt", "d:Download",
                "d:Other", "d:data", "d:SD card", "d:Backup"), shown);
    }

    @Test
    public void flippingAFolderOpensOrClosesIt() {
        List<IndexedTree.Node> top = sample();
        Set<String> flipped = new HashSet<>();
        flipped.add(top.get(0).dirs.get("Download").key);          // closed by default, now open
        flipped.add(top.get(0).dirs.get("Documents").key);         // open by default, now closed
        List<IndexedTree.Row> rows = IndexedTree.rows(top, flipped);
        boolean sawXpdf = false, sawBooks = false;
        for (IndexedTree.Row r : rows) {
            if (r.file != null && r.file.path.endsWith("x.pdf")) sawXpdf = true;
            if (r.folder != null && r.folder.name.equals("Books")) sawBooks = true;
        }
        assertTrue(sawXpdf);
        assertFalse(sawBooks);
    }

    private static List<String> names(List<IndexedTree.Node> nodes) {
        List<String> out = new ArrayList<>();
        for (IndexedTree.Node n : nodes) out.add(n.name);
        return out;
    }
}
