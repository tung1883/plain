package com.plainphone.app;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JobQueueTest {

    private File root;

    @Before
    public void setUp() throws Exception {
        root = Files.createTempDirectory("plain-jobs").toFile();
    }

    @After
    public void tearDown() {
        JobQueue.clearAllForTest(root);
        root.delete();
    }

    @Test
    public void ordersByPriorityBeforeAge() throws Exception {
        JobQueue.Job older = JobQueue.enqueueForTest(root, new JobQueue.Spec("notes.import")
                .label("old")
                .keep(JobQueue.AREA_NOTES));
        Thread.sleep(2);
        JobQueue.Job newerHighPriority = JobQueue.enqueueForTest(root, new JobQueue.Spec("vault.reset")
                .label("reset")
                .priority(100)
                .keep(JobQueue.AREA_VAULT));

        List<JobQueue.Job> jobs = JobQueue.pendingForTest(root);

        assertEquals(newerHighPriority.id, jobs.get(0).id);
        assertEquals(older.id, jobs.get(1).id);
    }

    @Test
    public void persistsAreasDataAndSidecarFiles() {
        JobQueue.Job enqueued = JobQueue.enqueueForTest(root, new JobQueue.Spec("vault.import.files")
                .label("2 files")
                .keep(JobQueue.AREA_VAULT)
                .require(JobQueue.AREA_VAULT)
                .put("dest", "root")
                .file("files", "content://one\tone.txt\n"));

        List<JobQueue.Job> jobs = JobQueue.pendingForTest(root);
        JobQueue.Job read = jobs.get(0);

        assertEquals(enqueued.id, read.id);
        assertEquals("vault.import.files", read.type);
        assertTrue(read.keepUnlockedAreas.contains(JobQueue.AREA_VAULT));
        assertTrue(read.requiredUnlockedAreas.contains(JobQueue.AREA_VAULT));
        assertEquals("root", read.data.get("dest"));
        assertEquals("content://one\tone.txt\n",
                JobQueue.readAll(new File(new File(root, read.id), "files")));
    }

    @Test
    public void statusUpdatesAndClearRemoveJobs() {
        JobQueue.Job enqueued = JobQueue.enqueueForTest(root, new JobQueue.Spec("recorder.import")
                .keep(JobQueue.AREA_RECORDER));

        JobQueue.setStatusForTest(root, enqueued.id, JobQueue.STATUS_RUNNING);
        assertEquals(JobQueue.STATUS_RUNNING, JobQueue.pendingForTest(root).get(0).status);

        JobQueue.clearForTest(root, enqueued.id);
        assertFalse(JobQueue.pendingForTest(root).iterator().hasNext());
    }
}
