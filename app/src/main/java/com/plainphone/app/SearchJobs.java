package com.plainphone.app;

import android.content.Context;

/** Queue helpers for global/search maintenance work. */
final class SearchJobs {

    private SearchJobs() {}

    static final String TYPE_FILE_INDEX = "search.file-index";

    static void startFileIndex(Context context) {
        if (JobQueue.anyOfType(context, TYPE_FILE_INDEX)) {
            JobQueue.resumeIfPending(context);
            return;
        }
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_FILE_INDEX)
                .label("Indexing files")
                .priority(-20));
    }

    static boolean isType(String type) {
        return TYPE_FILE_INDEX.equals(type);
    }
}
