package com.plainphone.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.List;

/**
 * Ticks every {@link #TICK_MS} and enqueues a {@link DevSyncJobs} run for any
 * pair whose own {@code scheduleMinutes} interval has elapsed since its last
 * run — same {@code AlarmManager.setInexactRepeating} + one-shot receiver
 * shape as {@link IndexScheduler}, just fanning out to several pairs with
 * different intervals off one alarm instead of one fixed job.
 */
public final class DevSyncScheduler {

    private static final String ACTION_TICK = "com.plainphone.app.dev.SYNC_TICK";
    private static final int REQUEST_CODE = 4108;
    // The shortest interval a pair can pick (see the wizard's schedule step) —
    // ticking any faster wouldn't let a pair actually run more often than that.
    static final long TICK_MS = 15L * 60L * 1000L;

    private DevSyncScheduler() {}

    static void schedule(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + TICK_MS, TICK_MS, pendingIntent(context));
    }

    static void cancel(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) alarms.cancel(pendingIntent(context));
    }

    /** Public so the framework's AppComponentFactory can instantiate it. */
    public static final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<DevSyncPair> pairs = DevSyncPair.all(context);
            if (pairs.isEmpty()) return;
            schedule(context); // re-arm (BOOT_COMPLETED / MY_PACKAGE_REPLACED fire this once, not repeating yet)
            long now = System.currentTimeMillis();
            for (DevSyncPair pair : pairs) {
                if (pair.scheduleMinutes <= DevSyncPair.SCHEDULE_MANUAL) continue;
                boolean due = pair.scheduleMinutes == 24 * 60
                        ? dueForDailyTime(pair, now)
                        : now >= pair.lastRunAt + pair.scheduleMinutes * 60_000L;
                if (!due) continue;
                if (DevSyncJobs.pending(context, pair.id)) continue;
                DevSyncJobs.enqueue(context, pair);
            }
        }

        /** A daily pair is due once we're past today's occurrence of its clock time
         *  and it hasn't already run since then — anchored to the wall-clock time the
         *  user picked rather than "24h after last run", so it doesn't drift a little
         *  later each day depending on exactly when the tick caught it. */
        private boolean dueForDailyTime(DevSyncPair pair, long now) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(now);
            cal.set(java.util.Calendar.HOUR_OF_DAY, pair.dailyMinuteOfDay / 60);
            cal.set(java.util.Calendar.MINUTE, pair.dailyMinuteOfDay % 60);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            long todayOccurrence = cal.getTimeInMillis();
            return now >= todayOccurrence && pair.lastRunAt < todayOccurrence;
        }
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, Receiver.class).setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
