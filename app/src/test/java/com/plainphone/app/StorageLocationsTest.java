package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class StorageLocationsTest {

    private static final File INTERNAL = new File("/storage/emulated/0");
    private static final File CARD = new File("/storage/1234-ABCD");

    private static List<StorageLocations.Volume> volumes(boolean cardMounted) {
        return Arrays.asList(
                new StorageLocations.Volume(StorageLocations.PRIMARY, "Internal only", INTERNAL, true),
                new StorageLocations.Volume("1234-ABCD", "SD card", cardMounted ? CARD : null, false));
    }

    private static Set<String> set(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    @Test
    public void oldStoredValuesStillMeanTheMainStorage() {
        StorageLocations.Location docs = StorageLocations.parse("Documents");
        assertEquals(StorageLocations.PRIMARY, docs.volumeId);
        assertEquals("Documents", docs.path);
        assertFalse(docs.whole());
        StorageLocations.Location all = StorageLocations.parse("*");
        assertEquals(StorageLocations.PRIMARY, all.volumeId);
        assertTrue(all.whole());
    }

    @Test
    public void otherVolumesCarryTheirId() {
        StorageLocations.Location books = StorageLocations.parse("1234-ABCD:Books/Sub");
        assertEquals("1234-ABCD", books.volumeId);
        assertEquals("Books/Sub", books.path);
        assertTrue(StorageLocations.parse("1234-ABCD:*").whole());
        assertTrue(StorageLocations.parse("1234-ABCD:").whole());      // an empty path is the whole volume
    }

    @Test
    public void encodeRoundTrips() {
        assertEquals("Documents", StorageLocations.encode(StorageLocations.PRIMARY, "Documents"));
        assertEquals("*", StorageLocations.encode(StorageLocations.PRIMARY, ""));
        assertEquals("1234-ABCD:Books/Sub", StorageLocations.encode("1234-ABCD", "/Books/Sub/"));
        for (String s : new String[]{"Documents", "*", "1234-ABCD:Books/Sub", "1234-ABCD:*"}) {
            StorageLocations.Location l = StorageLocations.parse(s);
            assertEquals(s, StorageLocations.encode(l.volumeId, l.path));
        }
    }

    @Test
    public void resolvesToFolders() {
        List<StorageLocations.Volume> v = volumes(true);
        assertEquals(new File(INTERNAL, "Documents"), StorageLocations.resolve(StorageLocations.parse("Documents"), v));
        assertEquals(INTERNAL, StorageLocations.resolve(StorageLocations.parse("*"), v));
        assertEquals(new File(CARD, "Books/Sub"), StorageLocations.resolve(StorageLocations.parse("1234-ABCD:Books/Sub"), v));
        assertEquals(CARD, StorageLocations.resolve(StorageLocations.parse("1234-ABCD:*"), v));
    }

    @Test
    public void missingVolumesAndBadPathsDoNotResolve() {
        assertNull(StorageLocations.resolve(StorageLocations.parse("1234-ABCD:Books"), volumes(false)));   // card not mounted
        assertNull(StorageLocations.resolve(StorageLocations.parse("FFFF-0000:Books"), volumes(true)));    // unknown card
        assertNull(StorageLocations.resolve(StorageLocations.parse("../etc"), volumes(true)));             // no way out of the volume
        assertNull(StorageLocations.resolve(StorageLocations.parse("1234-ABCD:Books/../../x"), volumes(true)));
    }

    @Test
    public void entryNames() {
        List<StorageLocations.Volume> v = volumes(true);
        assertEquals("Books", StorageLocations.entryName(StorageLocations.parse("Documents/Books"), v));
        assertEquals("Internal: all", StorageLocations.entryName(StorageLocations.parse("*"), v));
        assertEquals("SD card: all", StorageLocations.entryName(StorageLocations.parse("1234-ABCD:*"), v));
    }

    @Test
    public void treeDocumentIdsBecomeLocations() {
        assertEquals("Books/Sub", StorageLocations.fromTreeDocumentId("primary:Books/Sub"));
        assertEquals("*", StorageLocations.fromTreeDocumentId("primary:"));
        assertEquals("1234-ABCD:Backup", StorageLocations.fromTreeDocumentId("1234-ABCD:Backup"));
        assertEquals("1234-ABCD:*", StorageLocations.fromTreeDocumentId("1234-ABCD:"));
        assertNull(StorageLocations.fromTreeDocumentId("raw:/storage/emulated/0/x"));
        assertNull(StorageLocations.fromTreeDocumentId("home:Documents"));
        assertNull(StorageLocations.fromTreeDocumentId("garbage"));
        assertNull(StorageLocations.fromTreeDocumentId(null));
    }

    @Test
    public void choosingAFolderCoversWhatIsInsideIt() {
        Set<String> s = StorageLocations.choose(set("Documents/Books", "Documents/Notes", "Download"), StorageLocations.PRIMARY, "Documents");
        assertEquals(set("Download", "Documents"), s);
        assertTrue(StorageLocations.isChosen(s, StorageLocations.PRIMARY, "Documents"));
        assertTrue(StorageLocations.coveredByParent(s, StorageLocations.PRIMARY, "Documents/Books/Sub"));
        assertFalse(StorageLocations.coveredByParent(s, StorageLocations.PRIMARY, "Documents"));
        assertFalse(StorageLocations.coveredByParent(s, StorageLocations.PRIMARY, "Music"));
    }

    @Test
    public void wholeVolumeCoversEverythingAndOtherVolumesAreUntouched() {
        Set<String> s = StorageLocations.choose(set("Documents", "1234-ABCD:Books"), StorageLocations.PRIMARY, "*");
        assertEquals(set("1234-ABCD:Books", "*"), s);
        assertTrue(StorageLocations.coveredByParent(s, StorageLocations.PRIMARY, "Documents/x"));
        assertFalse(StorageLocations.coveredByParent(s, "1234-ABCD", "Music"));
        assertFalse(StorageLocations.coveredByParent(s, StorageLocations.PRIMARY, "*"));
    }

    @Test
    public void unchoose() {
        assertEquals(set("Download"), StorageLocations.unchoose(set("Documents", "Download"), StorageLocations.PRIMARY, "Documents"));
        assertEquals(set("Documents"), StorageLocations.unchoose(set("Documents", "1234-ABCD:Books"), "1234-ABCD", "Books"));
    }
}
