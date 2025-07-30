package com.nomor.memoryclear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
            
            android.util.Log.d(TAG, "Boot completed or app updated - starting services");
            
            // Restart schedule service if enabled
            if (AppPreferences.isScheduleEnabled()) {
                Intent scheduleIntent = new Intent(context, ScheduleService.class);
                scheduleIntent.setAction("START_SCHEDULE");
                context.startService(scheduleIntent);
            }
            
            // Restart dock service if enabled
            if (AppPreferences.isDockEnabled()) {
                Intent dockIntent = new Intent(context, FloatingDockService.class);
                dockIntent.setAction("START_DOCK");
                context.startService(dockIntent);
            }
        }
    }
}