package com.nomor.memoryclear;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import java.util.Calendar;
import java.util.List;

public class ScheduleService extends Service {
    
    private static final String TAG = "ScheduleService";
    private static final String CHANNEL_ID = "ScheduleServiceChannel";
    private static final int NOTIFICATION_ID = 1002;
    private static final int ALARM_REQUEST_CODE = 2001;
    private static final int WARNING_REQUEST_CODE = 2002;
    
    private AlarmManager alarmManager;
    private NotificationManager notificationManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_SCHEDULE".equals(action)) {
                startScheduledForceStop();
            } else if ("STOP_SCHEDULE".equals(action)) {
                stopScheduledForceStop();
            } else if ("EXECUTE_FORCE_STOP".equals(action)) {
                executeScheduledForceStop();
            } else if ("SHOW_WARNING".equals(action)) {
                showWarningNotification();
            }
        }
        
        return START_STICKY;
    }
    
    private void startScheduledForceStop() {
        if (!AppPreferences.isScheduleEnabled()) {
            return;
        }
        
        String scheduleTime = AppPreferences.getScheduleTime();
        String[] timeParts = scheduleTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        // If the time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Schedule warning notification 5 minutes before
        Calendar warningCalendar = (Calendar) calendar.clone();
        warningCalendar.add(Calendar.MINUTE, -5);
        
        // Create pending intents
        Intent executeIntent = new Intent(this, ScheduleService.class);
        executeIntent.setAction("EXECUTE_FORCE_STOP");
        PendingIntent executePendingIntent = PendingIntent.getService(
            this, ALARM_REQUEST_CODE, executeIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        Intent warningIntent = new Intent(this, ScheduleService.class);
        warningIntent.setAction("SHOW_WARNING");
        PendingIntent warningPendingIntent = PendingIntent.getService(
            this, WARNING_REQUEST_CODE, warningIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        // Set alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 
                calendar.getTimeInMillis(), executePendingIntent);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 
                warningCalendar.getTimeInMillis(), warningPendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, 
                calendar.getTimeInMillis(), executePendingIntent);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, 
                warningCalendar.getTimeInMillis(), warningPendingIntent);
        }
        
        startForeground(NOTIFICATION_ID, createNotification("Schedule active", 
            "Next force stop at " + scheduleTime));
        
        android.util.Log.d(TAG, "Schedule set for " + scheduleTime);
    }
    
    private void stopScheduledForceStop() {
        Intent executeIntent = new Intent(this, ScheduleService.class);
        executeIntent.setAction("EXECUTE_FORCE_STOP");
        PendingIntent executePendingIntent = PendingIntent.getService(
            this, ALARM_REQUEST_CODE, executeIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_NO_CREATE
        );
        
        Intent warningIntent = new Intent(this, ScheduleService.class);
        warningIntent.setAction("SHOW_WARNING");
        PendingIntent warningPendingIntent = PendingIntent.getService(
            this, WARNING_REQUEST_CODE, warningIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_NO_CREATE
        );
        
        if (executePendingIntent != null) {
            alarmManager.cancel(executePendingIntent);
        }
        if (warningPendingIntent != null) {
            alarmManager.cancel(warningPendingIntent);
        }
        
        stopForeground(true);
        android.util.Log.d(TAG, "Schedule cancelled");
    }
    
    private void executeScheduledForceStop() {
        android.util.Log.d(TAG, "Executing scheduled force stop");
        
        if (!PermissionHelper.hasAccessibilityPermission(this)) {
            showNotification("Schedule Failed", 
                "Accessibility permission is required for scheduled force stopping");
            return;
        }
        
        new Thread(() -> {
            try {
                AppManager appManager = new AppManager(this);
                List<AppInfo> runningApps = appManager.getRunningApps();
                
                if (!runningApps.isEmpty()) {
                    // Check if premium speed is active for scheduled tasks
                    boolean isPremiumActive = AppPreferences.isPremiumActive();
                    
                    // Start accessibility service to force stop apps
                    Intent serviceIntent = new Intent(this, ForceStopAccessibilityService.class);
                    serviceIntent.putExtra("action", "force_stop_apps");
                    serviceIntent.putExtra("premium_speed", isPremiumActive);
                    
                    String[] packageNames = new String[runningApps.size()];
                    for (int i = 0; i < runningApps.size(); i++) {
                        packageNames[i] = runningApps.get(i).packageName;
                    }
                    serviceIntent.putExtra("packages", packageNames);
                    
                    startService(serviceIntent);
                    
                    // Show completion notification after delay (adjust for premium speed)
                    int completionDelay = isPremiumActive ? 8000 : 30000; // Much faster for premium
                    android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                    handler.postDelayed(() -> {
                        String speedNote = isPremiumActive ? " (Premium Speed)" : "";
                        showNotification("Scheduled Force Stop Completed" + speedNote, 
                            "Successfully force stopped " + runningApps.size() + " apps" + speedNote);
                    }, completionDelay);
                    
                } else {
                    showNotification("No Apps to Stop", 
                        "No running apps found during scheduled force stop");
                }
                
                // Reschedule for next day if still enabled
                if (AppPreferences.isScheduleEnabled()) {
                    startScheduledForceStop();
                }
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error during scheduled force stop", e);
                showNotification("Schedule Error", 
                    "Error occurred during scheduled force stop: " + e.getMessage());
            }
        }).start();
    }
    
    private void showWarningNotification() {
        showNotification("Scheduled Force Stop in 5 Minutes", 
            "Apps will be force stopped automatically in 5 minutes");
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Schedule Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background service for scheduled force stopping");
            channel.setLightColor(Color.GREEN);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
    
    private void showNotification(String title, String content) {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build();
        
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notification);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}