package com.plainphone.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

/**
 * Routes hardware media-button presses (wired remote, Bluetooth headset / car)
 * to {@link RecorderService}, which dispatches them to its {@link android.media.session.MediaSession}.
 * Registered as the session's media-button receiver in {@link RecorderService#setUpSession()}.
 */
public class RecorderMediaButtonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) return;
        KeyEvent key = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        try {
            context.startService(new Intent(context, RecorderService.class)
                    .setAction(Intent.ACTION_MEDIA_BUTTON)
                    .putExtra(Intent.EXTRA_KEY_EVENT, key));
        } catch (Exception ignored) {
            // service not running -> nothing is playing, nothing to control
        }
    }
}
