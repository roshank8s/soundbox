package com.soundbox.app.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Ensures the notification listener service starts on boot.
 * The actual service is managed by Android's notification listener system,
 * but this receiver helps ensure our app is active after a restart.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed - SoundBox notification listener will be active");
            // NotificationListenerService is automatically restarted by the system
            // if the user has granted permission. No manual start needed.
        }
    }
}
