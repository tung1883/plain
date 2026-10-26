package com.plainphone.app;

import android.content.Context;

/** Queue helpers for global/search maintenance work. */
final class SearchJobs {

    private SearchJobs() {}

    static final String TYPE_FILE_INDEX = "search.file-index";
    static final String TYPE_CONTENT_INDEX = "search.content-index";

    static void startFileIndex(Context context) {
        if (JobQueue.anyOfType(context, TYPE_FILE_INDEX)) {
            JobQueue.resumeIfPending(context);
            return;
        }
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_FILE_INDEX)
                .label("Indexing files")
                .priority(-20));
    }

    /** Read new and changed documents into the content index (does nothing when the feature is off). */
    static void startContentIndex(Context context) {
        if (!Config.isContentSearchEnabled(context) || !FileIndex.canWalk(context)) return;
        if (JobQueue.anyOfType(context, TYPE_CONTENT_INDEX)) {
            JobQueue.resumeIfPending(context);
            return;
        }
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_CONTENT_INDEX)
                .label("Indexing documents")
                .priority(-30));
    }

    static boolean isType(String type) {
        return TYPE_FILE_INDEX.equals(type) || TYPE_CONTENT_INDEX.equals(type);
    }
}
