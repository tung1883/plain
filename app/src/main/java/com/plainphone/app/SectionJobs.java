package com.plainphone.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Queue helpers for non-import home-section jobs. */
final class SectionJobs {

    private SectionJobs() {}

    static final String TYPE_NOTES_TO_VAULT = "notes.move-to-vault";
    static final String TYPE_NOTES_FROM_VAULT = "notes.move-out-of-vault";
    static final String TYPE_RECORDER_TO_VAULT = "recorder.move-to-vault";
    static final String TYPE_RECORDER_FROM_VAULT = "recorder.move-out-of-vault";
    static final String TYPE_RECORDER_HEAL = "recorder.heal-vault-metadata";

    static final class Result {
        final String message;
        Result(String message) { this.message = message; }
    }

    private static volatile Result lastResult;

    static void startNotesToVault(Context context, List<String> noteIds) {
        enqueue(context, TYPE_NOTES_TO_VAULT, "Moving notes to vault",
                JobQueue.AREA_NOTES, noteIds);
    }

    static void startNotesFromVault(Context context, List<String> noteIds) {
        enqueue(context, TYPE_NOTES_FROM_VAULT, "Moving notes out of vault",
                JobQueue.AREA_NOTES, noteIds);
    }

    static void startRecorderToVault(Context context, List<String> recordingIds) {
        enqueue(context, TYPE_RECORDER_TO_VAULT, "Moving recordings to vault",
                JobQueue.AREA_RECORDER, recordingIds);
    }

    static void startRecorderFromVault(Context context, List<String> recordingIds) {
        enqueue(context, TYPE_RECORDER_FROM_VAULT, "Moving recordings out of vault",
                JobQueue.AREA_RECORDER, recordingIds);
    }

    static void startRecorderHeal(Context context, List<String> docIds) {
        if (docIds == null || docIds.isEmpty()) return;
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_RECORDER_HEAL)
                .label("Updating recording metadata")
                .priority(-10)
                .keep(JobQueue.AREA_RECORDER, JobQueue.AREA_VAULT)
                .require(JobQueue.AREA_VAULT)
                .file("ids", lines(docIds)));
    }

    static boolean isType(String type) {
        return TYPE_NOTES_TO_VAULT.equals(type)
                || TYPE_NOTES_FROM_VAULT.equals(type)
                || TYPE_RECORDER_TO_VAULT.equals(type)
                || TYPE_RECORDER_FROM_VAULT.equals(type)
                || TYPE_RECORDER_HEAL.equals(type);
    }

    static List<String> readIds(Context context, String jobId) {
        List<String> out = new ArrayList<>();
        for (String line : JobQueue.readText(context, jobId, "ids").split("\n")) {
            if (!line.isEmpty()) out.add(line);
        }
        return out;
    }

    static Set<String> readDone(Context context, String jobId) {
        Set<String> out = new LinkedHashSet<>();
        for (String line : JobQueue.readText(context, jobId, "done").split("\n")) {
            if (!line.isEmpty()) out.add(line);
        }
        return out;
    }

    static void writeDone(Context context, String jobId, Set<String> done) {
        StringBuilder sb = new StringBuilder();
        for (String id : done) sb.append(id).append('\n');
        JobQueue.writeText(context, jobId, "done", sb.toString());
    }

    static void finish(Context context, JobQueue.Job job, int moved) {
        if (!TYPE_RECORDER_HEAL.equals(job.type)) {
            lastResult = new Result("Moved " + moved + " item(s)");
        }
        JobQueue.clear(context, job.id);
    }

    static Result takeResult() {
        Result r = lastResult;
        lastResult = null;
        return r;
    }

    static boolean pendingFor(Context context, HomeMode mode) {
        return detailFor(context, mode) != null;
    }

    static String detailFor(Context context, HomeMode mode) {
        String area = areaFor(mode);
        if (area == null) return null;
        for (JobQueue.Job job : JobQueue.pending(context)) {
            if (job.keepUnlockedAreas.contains(area) && isType(job.type)) {
                return job.label != null ? job.label.toLowerCase() : "working";
            }
        }
        return null;
    }

    static String progressLine(Context context, HomeMode mode) {
        ImportJobs.Snapshot s = ImportJobs.snapshot;
        if (s != null && s.plugin == mode && s.total > 1) {
            return s.label + " " + Math.min(s.done + 1, s.total) + " of " + s.total + "...";
        }
        String detail = detailFor(context, mode);
        return detail != null ? detail : "Working...";
    }

    private static void enqueue(Context context, String type, String label, String area,
                                List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        JobQueue.enqueue(context, new JobQueue.Spec(type)
                .label(label)
                .keep(area, JobQueue.AREA_VAULT)
                .require(JobQueue.AREA_VAULT)
                .file("ids", lines(ids)));
    }

    private static String areaFor(HomeMode mode) {
        switch (mode) {
            case NOTES: return JobQueue.AREA_NOTES;
            case RECORDER: return JobQueue.AREA_RECORDER;
            default: return null;
        }
    }

    private static String lines(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) sb.append(id).append('\n');
        return sb.toString();
    }
}
