package com.plainphone.app;

import android.app.admin.DeviceAdminReceiver;

/** Device admin with a single policy, force-lock, so Plain can turn the screen off without accessibility. */
public class LockAdminReceiver extends DeviceAdminReceiver {
}
