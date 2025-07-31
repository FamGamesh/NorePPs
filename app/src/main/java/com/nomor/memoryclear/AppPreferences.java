package com.nomor.memoryclear;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class AppPreferences {
    private static final String PREFS_NAME = "NoMoreAppsPrefs";
    private static final String KEY_WHITELIST = "whitelist_apps";
    private static final String KEY_SCHEDULE_ENABLED = "schedule_enabled";
    private static final String KEY_SCHEDULE_TIME = "schedule_time";
    private static final String KEY_DOCK_ENABLED = "dock_enabled";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_PREMIUM_ACTIVE = "premium_active";
    private static final String KEY_PREMIUM_EXPIRY = "premium_expiry_time";
    
    // Permission tracking keys
    private static final String KEY_USAGE_STATS_GRANTED = "usage_stats_granted";
    private static final String KEY_ACCESSIBILITY_GRANTED = "accessibility_granted";  
    private static final String KEY_OVERLAY_GRANTED = "overlay_granted";
    private static final String KEY_PERMISSIONS_SETUP_COMPLETED = "permissions_setup_completed";
    
    private static SharedPreferences sPrefs;
    
    public static void init(Context context) {
        sPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static Set<String> getWhitelistedApps() {
        return sPrefs.getStringSet(KEY_WHITELIST, new HashSet<>());
    }
    
    public static void addToWhitelist(String packageName) {
        Set<String> whitelist = new HashSet<>(getWhitelistedApps());
        whitelist.add(packageName);
        sPrefs.edit().putStringSet(KEY_WHITELIST, whitelist).apply();
    }
    
    public static void removeFromWhitelist(String packageName) {
        Set<String> whitelist = new HashSet<>(getWhitelistedApps());
        whitelist.remove(packageName);
        sPrefs.edit().putStringSet(KEY_WHITELIST, whitelist).apply();
    }
    
    public static boolean isWhitelisted(String packageName) {
        return getWhitelistedApps().contains(packageName);
    }
    
    public static boolean isScheduleEnabled() {
        return sPrefs.getBoolean(KEY_SCHEDULE_ENABLED, false);
    }
    
    public static void setScheduleEnabled(boolean enabled) {
        sPrefs.edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply();
    }
    
    public static String getScheduleTime() {
        return sPrefs.getString(KEY_SCHEDULE_TIME, "02:00");
    }
    
    public static void setScheduleTime(String time) {
        sPrefs.edit().putString(KEY_SCHEDULE_TIME, time).apply();
    }
    
    public static boolean isDockEnabled() {
        return sPrefs.getBoolean(KEY_DOCK_ENABLED, false);
    }
    
    public static void setDockEnabled(boolean enabled) {
        sPrefs.edit().putBoolean(KEY_DOCK_ENABLED, enabled).apply();
    }
    
    public static boolean isFirstLaunch() {
        return sPrefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }
    
    public static void setFirstLaunch(boolean firstLaunch) {
        sPrefs.edit().putBoolean(KEY_FIRST_LAUNCH, firstLaunch).apply();
    }
    
    // Premium functionality
    public static boolean isPremiumActive() {
        boolean isActive = sPrefs.getBoolean(KEY_PREMIUM_ACTIVE, false);
        if (isActive) {
            // Check if premium has expired
            long expiryTime = sPrefs.getLong(KEY_PREMIUM_EXPIRY, 0);
            if (System.currentTimeMillis() > expiryTime) {
                // Premium expired, deactivate
                setPremiumActive(false);
                return false;
            }
        }
        return isActive;
    }
    
    public static void setPremiumActive(boolean active) {
        sPrefs.edit().putBoolean(KEY_PREMIUM_ACTIVE, active).apply();
        if (!active) {
            // Clear expiry time when deactivating
            sPrefs.edit().remove(KEY_PREMIUM_EXPIRY).apply();
        }
    }
    
    public static void setPremiumExpiryTime(long expiryTime) {
        sPrefs.edit().putLong(KEY_PREMIUM_EXPIRY, expiryTime).apply();
    }
    
    public static long getPremiumExpiryTime() {
        return sPrefs.getLong(KEY_PREMIUM_EXPIRY, 0);
    }
    
    // Permission tracking methods
    public static boolean isUsageStatsGranted() {
        return sPrefs.getBoolean(KEY_USAGE_STATS_GRANTED, false);
    }
    
    public static void setUsageStatsGranted(boolean granted) {
        sPrefs.edit().putBoolean(KEY_USAGE_STATS_GRANTED, granted).apply();
    }
    
    public static boolean isAccessibilityGranted() {
        return sPrefs.getBoolean(KEY_ACCESSIBILITY_GRANTED, false);
    }
    
    public static void setAccessibilityGranted(boolean granted) {
        sPrefs.edit().putBoolean(KEY_ACCESSIBILITY_GRANTED, granted).apply();
    }
    
    public static boolean isOverlayGranted() {
        return sPrefs.getBoolean(KEY_OVERLAY_GRANTED, false);
    }
    
    public static void setOverlayGranted(boolean granted) {
        sPrefs.edit().putBoolean(KEY_OVERLAY_GRANTED, granted).apply();
    }
    
    public static boolean isPermissionsSetupCompleted() {
        return sPrefs.getBoolean(KEY_PERMISSIONS_SETUP_COMPLETED, false);
    }
    
    public static void setPermissionsSetupCompleted(boolean completed) {
        sPrefs.edit().putBoolean(KEY_PERMISSIONS_SETUP_COMPLETED, completed).apply();
    }
    
    /**
     * Check if all critical permissions are granted
     */
    public static boolean areAllCriticalPermissionsGranted() {
        return isUsageStatsGranted() && isAccessibilityGranted() && isOverlayGranted();
    }
    
    /**
     * Get count of granted permissions
     */
    public static int getGrantedPermissionsCount() {
        int count = 0;
        if (isUsageStatsGranted()) count++;
        if (isAccessibilityGranted()) count++;
        if (isOverlayGranted()) count++;
        return count;
    }
}