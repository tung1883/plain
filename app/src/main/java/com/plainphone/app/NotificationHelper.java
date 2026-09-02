package com.plainphone.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;

class NotificationHelper {

    private static final String CHANNEL_ID = "flagged_app_timeout";

    static void notifyClosingSoon(Context context, String packageName) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        ensureChannel(manager);

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setContentTitle("Closing soon")
                .setContentText(appLabel(context, packageName) + " will close in 10 seconds")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .setOngoing(false)
                .setTimeoutAfter(10_000)   // gone by the time the app actually closes
                .build();
        manager.notify(packageName.hashCode(), notification);
    }

    /** Pull the "closing soon" warning — the app closed, or the user left / earned more time. */
    static void cancelClosingSoon(Context context, String packageName) {
        if (packageName == null) return;
        context.getSystemService(NotificationManager.class).cancel(packageName.hashCode());
    }

    private static void ensureChannel(NotificationManager manager) {
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "App timeout warnings", NotificationManager.IMPORTANCE_HIGH));
        }
    }

    private static String appLabel(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }
}

