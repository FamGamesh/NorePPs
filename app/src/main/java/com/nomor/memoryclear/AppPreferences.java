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
}