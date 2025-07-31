package com.nomor.memoryclear;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ForceStopAccessibilityService extends AccessibilityService {
    
    private static final String TAG = "ForceStopService";
    private Handler mainHandler;
    private List<String> appsToStop;
    private int currentAppIndex = 0;
    private boolean isProcessing = false;
    private boolean isPremiumSpeedActive = false;
    
    // Premium speed settings - 3-4x faster
    private static final int NORMAL_SETTINGS_DELAY = 1500;
    private static final int NORMAL_PROCESS_DELAY = 2000;
    private static final int NORMAL_CONFIRMATION_DELAY = 500;
    
    private static final int PREMIUM_SETTINGS_DELAY = 400;   // 3.75x faster
    private static final int PREMIUM_PROCESS_DELAY = 500;    // 4x faster  
    private static final int PREMIUM_CONFIRMATION_DELAY = 150; // 3.3x faster
    
    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        android.util.Log.d(TAG, "ForceStopAccessibilityService created");
    }
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
                          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.packageNames = new String[]{"com.android.settings"};
        
        setServiceInfo(info);
        android.util.Log.d(TAG, "ForceStopAccessibilityService connected");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if ("force_stop_apps".equals(action)) {
                String[] packages = intent.getStringArrayExtra("packages");
                boolean premiumSpeed = intent.getBooleanExtra("premium_speed", false);
                
                if (packages != null) {
                    startForceStoppingApps(Arrays.asList(packages), premiumSpeed);
                }
            }
        }
        
        return START_NOT_STICKY;
    }
    
    private void startForceStoppingApps(List<String> packages, boolean premiumSpeed) {
        if (isProcessing) {
            android.util.Log.w(TAG, "Already processing apps, ignoring new request");
            return;
        }
        
        appsToStop = new ArrayList<>(packages);
        currentAppIndex = 0;
        isProcessing = true;
        isPremiumSpeedActive = premiumSpeed;
        
        android.util.Log.d(TAG, "Starting to force stop " + appsToStop.size() + " apps" + 
            (premiumSpeed ? " with PREMIUM SPEED (3-4x faster)" : " at normal speed"));
        
        if (!appsToStop.isEmpty()) {
            processNextApp();
        } else {
            completeProcessing();
        }
    }
    
    private void processNextApp() {
        if (currentAppIndex >= appsToStop.size()) {
            completeProcessing();
            return;
        }
        
        String packageName = appsToStop.get(currentAppIndex);
        android.util.Log.d(TAG, "Processing app: " + packageName + " (" + (currentAppIndex + 1) + "/" + appsToStop.size() + ")" +
            (isPremiumSpeedActive ? " [PREMIUM SPEED]" : ""));
        
        // Open app info settings for the package
        openAppInfoSettings(packageName);
        
        // Use premium speed delays if active
        int settingsDelay = isPremiumSpeedActive ? PREMIUM_SETTINGS_DELAY : NORMAL_SETTINGS_DELAY;
        int processDelay = isPremiumSpeedActive ? PREMIUM_PROCESS_DELAY : NORMAL_PROCESS_DELAY;
        
        // Wait for settings to open and then try to click force stop
        mainHandler.postDelayed(() -> {
            clickForceStopButton();
            
            // Move to next app after delay
            mainHandler.postDelayed(() -> {
                currentAppIndex++;
                processNextApp();
            }, processDelay);
        }, settingsDelay);
    }
    
    private void openAppInfoSettings(String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error opening app info for " + packageName, e);
        }
    }
    
    private void clickForceStopButton() {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // Try to find and click Force Stop button
                List<AccessibilityNodeInfo> forceStopNodes = rootNode.findAccessibilityNodeInfosByText("Force stop");
                if (forceStopNodes.isEmpty()) {
                    forceStopNodes = rootNode.findAccessibilityNodeInfosByText("FORCE STOP");
                }
                if (forceStopNodes.isEmpty()) {
                    forceStopNodes = rootNode.findAccessibilityNodeInfosByText("강제 종료"); // Korean
                }
                if (forceStopNodes.isEmpty()) {
                    forceStopNodes = rootNode.findAccessibilityNodeInfosByText("强制停止"); // Chinese
                }
                
                for (AccessibilityNodeInfo node : forceStopNodes) {
                    if (node.isClickable() && node.isEnabled()) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        android.util.Log.d(TAG, "Clicked Force Stop button" + 
                            (isPremiumSpeedActive ? " [PREMIUM SPEED]" : ""));
                        
                        // Use premium speed delay for confirmation
                        int confirmationDelay = isPremiumSpeedActive ? PREMIUM_CONFIRMATION_DELAY : NORMAL_CONFIRMATION_DELAY;
                        mainHandler.postDelayed(this::clickConfirmationButton, confirmationDelay);
                        break;
                    }
                }
                
                rootNode.recycle();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error clicking force stop button", e);
        }
    }
    
    private void clickConfirmationButton() {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // Try to find and click OK or Force Stop confirmation button
                List<AccessibilityNodeInfo> confirmNodes = rootNode.findAccessibilityNodeInfosByText("OK");
                if (confirmNodes.isEmpty()) {
                    confirmNodes = rootNode.findAccessibilityNodeInfosByText("Force stop");
                }
                if (confirmNodes.isEmpty()) {
                    confirmNodes = rootNode.findAccessibilityNodeInfosByText("FORCE STOP");
                }
                if (confirmNodes.isEmpty()) {
                    confirmNodes = rootNode.findAccessibilityNodeInfosByText("확인"); // Korean OK
                }
                if (confirmNodes.isEmpty()) {
                    confirmNodes = rootNode.findAccessibilityNodeInfosByText("确定"); // Chinese OK
                }
                
                for (AccessibilityNodeInfo node : confirmNodes) {
                    if (node.isClickable() && node.isEnabled()) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        android.util.Log.d(TAG, "Clicked confirmation button" + 
                            (isPremiumSpeedActive ? " [PREMIUM SPEED]" : ""));
                        break;
                    }
                }
                
                rootNode.recycle();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error clicking confirmation button", e);
        }
    }
    
    private void completeProcessing() {
        android.util.Log.d(TAG, "Force stopping process completed");
        isProcessing = false;
        currentAppIndex = 0;
        
        if (appsToStop != null) {
            appsToStop.clear();
        }
        
        // Go back to home screen
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handle accessibility events if needed
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && isProcessing) {
            android.util.Log.d(TAG, "Window state changed: " + event.getPackageName());
        }
    }
    
    @Override
    public void onInterrupt() {
        android.util.Log.d(TAG, "ForceStopAccessibilityService interrupted");
        isProcessing = false;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        android.util.Log.d(TAG, "ForceStopAccessibilityService destroyed");
        isProcessing = false;
    }
}