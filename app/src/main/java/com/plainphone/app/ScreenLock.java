package com.plainphone.app;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/** "Screen off": the accessibility service in Nerd mode, otherwise the force-lock device admin. */
final class ScreenLock {

    private ScreenLock() {}

    private static ComponentName admin(Context context) {
        return new ComponentName(context, LockAdminReceiver.class);
    }

    private static DevicePolicyManager dpm(Context context) {
        return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    static boolean adminActive(Context context) {
        return dpm(context).isAdminActive(admin(context));
    }

    /** Lock now, or ask for the device admin the first time it is needed. */
    static void lockOrRequest(Activity activity) {
        if (Config.isNerdMode(activity) && AppMonitorService.isEnabled(activity)) {
            AppMonitorService.lockScreen();
        } else if (adminActive(activity)) {
            dpm(activity).lockNow();
        } else {
            requestAdmin(activity);
        }
    }

    static void requestAdmin(Activity activity) {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin(activity));
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Lets Plain turn the screen off. It cannot do anything else.");
        activity.startActivity(intent);
    }

    static void removeAdmin(Context context) {
        dpm(context).removeActiveAdmin(admin(context));
    }
}
