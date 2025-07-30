package com.nomor.memoryclear;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

public class FloatingDockService extends Service {
    
    private static final String TAG = "FloatingDockService";
    private static final String CHANNEL_ID = "FloatingDockChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private WindowManager windowManager;
    private View floatingView;
    private boolean isServiceRunning = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_DOCK".equals(action)) {
                startFloatingDock();
            } else if ("STOP_DOCK".equals(action)) {
                stopFloatingDock();
            }
        }
        
        return START_STICKY;
    }
    
    private void startFloatingDock() {
        if (isServiceRunning) {
            return;
        }
        
        if (!PermissionHelper.hasOverlayPermission(this)) {
            Toast.makeText(this, "Display over other apps permission required", Toast.LENGTH_LONG).show();
            return;
        }
        
        createFloatingView();
        startForeground(NOTIFICATION_ID, createNotification());
        isServiceRunning = true;
        
        android.util.Log.d(TAG, "Floating dock started");
    }
    
    private void stopFloatingDock() {
        if (!isServiceRunning) {
            return;
        }
        
        removeFloatingView();
        stopForeground(true);
        isServiceRunning = false;
        
        android.util.Log.d(TAG, "Floating dock stopped");
    }
    
    private void createFloatingView() {
        try {
            LayoutInflater inflater = LayoutInflater.from(this);
            floatingView = inflater.inflate(R.layout.floating_dock, null);
            
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 100;
            
            ImageButton dockButton = floatingView.findViewById(R.id.dock_button);
            dockButton.setOnClickListener(v -> {
                // Open main app
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                
                Toast.makeText(this, "Opening No More Apps PRO", Toast.LENGTH_SHORT).show();
            });
            
            // Make the dock draggable
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                            int xDiff = (int) (event.getRawX() - initialTouchX);
                            int yDiff = (int) (event.getRawY() - initialTouchY);
                            
                            // If it's a small movement, consider it a click
                            if (Math.abs(xDiff) < 10 && Math.abs(yDiff) < 10) {
                                v.performClick();
                            }
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            
                            try {
                                windowManager.updateViewLayout(floatingView, params);
                            } catch (Exception e) {
                                android.util.Log.e(TAG, "Error updating floating view position", e);
                            }
                            return true;
                    }
                    return false;
                }
            });
            
            windowManager.addView(floatingView, params);
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error creating floating view", e);
            Toast.makeText(this, "Error creating floating dock", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void removeFloatingView() {
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
                floatingView = null;
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error removing floating view", e);
            }
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Floating Dock Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Floating dock for quick access to force stop functionality");
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("No More Apps PRO")
            .setContentText("Floating dock is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        removeFloatingView();
        isServiceRunning = false;
    }
}