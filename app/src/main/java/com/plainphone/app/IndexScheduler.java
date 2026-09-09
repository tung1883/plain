package com.plainphone.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/** Schedules low-priority file index refreshes without bypassing JobQueue. */
public final class IndexScheduler {

    private static final String ACTION_INDEX = "com.plainphone.app.INDEX_REFRESH";
    private static final int REQUEST_CODE = 4107;
    static final long INTERVAL_MS = 6L * 60L * 60L * 1000L;

    private IndexScheduler() {}

    static void schedule(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;

        PendingIntent operation = pendingIntent(context);
        alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + INTERVAL_MS,
                INTERVAL_MS,
                operation);
    }

    /** Public so the framework's AppComponentFactory can instantiate it. */
    public static final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            schedule(context);
            if (FileIndex.canWalk(context)) {
                SearchJobs.startFileIndex(context.getApplicationContext());
            }
        }
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, Receiver.class).setAction(ACTION_INDEX);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
