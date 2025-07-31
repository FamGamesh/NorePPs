package com.nomor.memoryclear;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class AppManager {
    private static final String TAG = "AppManager";
    private Context context;
    private PackageManager packageManager;
    private ActivityManager activityManager;
    private UsageStatsManager usageStatsManager;
    
    // Cache management for immediate refresh after force stop
    private List<AppInfo> cachedRunningApps = null;
    private long lastCacheTime = 0;
    private static final long CACHE_VALIDITY_MS = 3000; // 3 seconds cache validity
    private static final long FORCE_STOP_DETECTION_WINDOW_MS = 2 * 60 * 1000; // 2 minutes after force stop
    
    // System apps to exclude from force stopping
    private static final String[] SYSTEM_EXCLUSIONS = {
        "android",
        "com.android.systemui",
        "com.android.launcher",
        "com.google.android.gms",
        "com.nomor.memoryclear" // Our own app
    };
    
    public AppManager(Context context) {
        this.context = context;
        this.packageManager = context.getPackageManager();
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    }
    
    public List<AppInfo> getAllInstalledApps() {
        List<AppInfo> appsList = new ArrayList<>();
        List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        
        for (ApplicationInfo appInfo : installedApps) {
            // Skip system apps that shouldn't be shown
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }
            
            try {
                String appName = packageManager.getApplicationLabel(appInfo).toString();
                AppInfo app = new AppInfo(
                    appInfo.packageName,
                    appName,
                    packageManager.getApplicationIcon(appInfo)
                );
                app.isWhitelisted = AppPreferences.isWhitelisted(appInfo.packageName);
                appsList.add(app);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error getting app info for " + appInfo.packageName, e);
            }
        }
        
        // Sort by app name
        Collections.sort(appsList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a1, AppInfo a2) {
                return a1.appName.compareToIgnoreCase(a2.appName);
            }
        });
        
        return appsList;
    }
    
    public List<AppInfo> getRunningApps() {
        // Check if we have valid cached results
        long currentTime = System.currentTimeMillis();
        if (cachedRunningApps != null && (currentTime - lastCacheTime) < CACHE_VALIDITY_MS) {
            android.util.Log.d(TAG, "Returning cached running apps: " + cachedRunningApps.size());
            return new ArrayList<>(cachedRunningApps);
        }
        
        return getRunningAppsInternal(false);
    }
    
    /**
     * Force refresh of running apps - ignores cache and uses optimized detection after force stop
     */
    public List<AppInfo> getRunningAppsForceRefresh() {
        android.util.Log.d(TAG, "Force refresh requested - clearing cache and using optimized detection");
        clearCache();
        return getRunningAppsInternal(true);
    }
    
    /**
     * Clear the cache to force fresh detection on next call
     */
    public void clearCache() {
        cachedRunningApps = null;
        lastCacheTime = 0;
        android.util.Log.d(TAG, "App detection cache cleared");
    }
    
    private List<AppInfo> getRunningAppsInternal(boolean postForceStopRefresh) {
        List<AppInfo> runningApps = new ArrayList<>();
        Set<String> whitelistedApps = AppPreferences.getWhitelistedApps();
        
        try {
            // Use multiple detection strategies for comprehensive app detection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Strategy 1: UsageStatsManager - with optimized time window after force stop
                addRunningAppsFromUsageStats(runningApps, whitelistedApps, postForceStopRefresh);
                
                // Strategy 2: ActivityManager - Process based detection (still works on newer Android)
                addRunningAppsFromProcesses(runningApps, whitelistedApps);
                
                // Strategy 3: Extended recent usage detection for Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    addExtendedRunningAppsDetection(runningApps, whitelistedApps, postForceStopRefresh);
                }
                
                // Strategy 4: Running services detection
                addRunningServicesDetection(runningApps, whitelistedApps);
                
            } else {
                // Fallback for older Android versions (Pre-Lollipop)
                addRunningAppsFromProcesses(runningApps, whitelistedApps);
            }
            
            // Remove duplicates and sort
            removeDuplicates(runningApps);
            
            // Cache the results
            cachedRunningApps = new ArrayList<>(runningApps);
            lastCacheTime = System.currentTimeMillis();
            
            String refreshType = postForceStopRefresh ? " [POST-FORCE-STOP OPTIMIZED]" : "";
            android.util.Log.d(TAG, "Detected " + runningApps.size() + " running apps using enhanced detection" + refreshType);
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in comprehensive app detection", e);
        }
        
        return runningApps;
    }
    
    private void addRunningAppsFromUsageStats(List<AppInfo> runningApps, Set<String> whitelistedApps, boolean postForceStopRefresh) {
        try {
            // Optimized time window - shorter after force stop for immediate detection
            Calendar calendar = Calendar.getInstance();
            if (postForceStopRefresh) {
                // Use much shorter window after force stop for accurate count
                calendar.add(Calendar.MINUTE, -2); // Only last 2 minutes for post-force-stop detection
                android.util.Log.d(TAG, "Using optimized 2-minute detection window after force stop");
            } else {
                // Normal detection window
                calendar.add(Calendar.MINUTE, -10); // Reduced from 30 to 10 minutes for better accuracy
            }
            
            long startTime = calendar.getTimeInMillis();
            long endTime = System.currentTimeMillis();
            
            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, startTime, endTime);
            
            for (UsageStats usageStats : usageStatsList) {
                // More strict criteria for post-force-stop detection
                boolean isActive = false;
                if (postForceStopRefresh) {
                    // Very recent activity required after force stop
                    isActive = usageStats.getLastTimeUsed() > (endTime - FORCE_STOP_DETECTION_WINDOW_MS) ||
                              usageStats.getLastTimeVisible() > (endTime - FORCE_STOP_DETECTION_WINDOW_MS);
                } else {
                    // Normal detection criteria
                    isActive = usageStats.getLastTimeUsed() > startTime || 
                              usageStats.getTotalTimeInForeground() > 0;
                }
                
                if (isActive) {
                    String packageName = usageStats.getPackageName();
                    
                    // Skip critical system exclusions and whitelisted apps
                    if (isCriticalSystemApp(packageName) || whitelistedApps.contains(packageName)) {
                        continue;
                    }
                    
                    try {
                        ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                        String appName = packageManager.getApplicationLabel(appInfo).toString();
                        
                        AppInfo app = new AppInfo(
                            packageName,
                            appName,
                            packageManager.getApplicationIcon(appInfo)
                        );
                        
                        if (!containsApp(runningApps, app)) {
                            runningApps.add(app);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        // App not found, skip
                    }
                }
            }
            
            String detectionType = postForceStopRefresh ? " [OPTIMIZED POST-FORCE-STOP]" : "";
            android.util.Log.d(TAG, "UsageStats detection added " + runningApps.size() + " apps" + detectionType);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in UsageStats detection", e);
        }
    }
    
    private void addRunningAppsFromProcesses(List<AppInfo> runningApps, Set<String> whitelistedApps) {
        try {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = 
                activityManager.getRunningAppProcesses();
            
            if (runningProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                    // Only consider foreground and visible processes for newer Android
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (processInfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                            continue;
                        }
                    }
                    
                    for (String packageName : processInfo.pkgList) {
                        if (isCriticalSystemApp(packageName) || whitelistedApps.contains(packageName)) {
                            continue;
                        }
                        
                        try {
                            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                            String appName = packageManager.getApplicationLabel(appInfo).toString();
                            
                            AppInfo app = new AppInfo(
                                packageName,
                                appName,
                                packageManager.getApplicationIcon(appInfo)
                            );
                            
                            if (!containsApp(runningApps, app)) {
                                runningApps.add(app);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            // App not found, skip
                        }
                    }
                }
                
                android.util.Log.d(TAG, "Process detection added additional apps, total now: " + runningApps.size());
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in process detection", e);
        }
    }
    
    private void addExtendedRunningAppsDetection(List<AppInfo> runningApps, Set<String> whitelistedApps, boolean postForceStopRefresh) {
        try {
            // Get all installed apps and check recent activity
            List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            
            // Optimized time window for different scenarios
            Calendar recentCalendar = Calendar.getInstance();
            if (postForceStopRefresh) {
                recentCalendar.add(Calendar.MINUTE, -1); // Very recent - 1 minute for post-force-stop
            } else {
                recentCalendar.add(Calendar.MINUTE, -5); // Normal - 5 minutes for regular detection
            }
            long recentTime = recentCalendar.getTimeInMillis();
            
            List<UsageStats> recentStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, recentTime, System.currentTimeMillis());
            
            for (ApplicationInfo appInfo : installedApps) {
                String packageName = appInfo.packageName;
                
                if (isCriticalSystemApp(packageName) || whitelistedApps.contains(packageName)) {
                    continue;
                }
                
                // Check if app has recent activity
                for (UsageStats stats : recentStats) {
                    if (stats.getPackageName().equals(packageName)) {
                        boolean hasRecentActivity = false;
                        
                        if (postForceStopRefresh) {
                            // Stricter criteria after force stop
                            hasRecentActivity = stats.getLastTimeUsed() > recentTime && 
                                              stats.getLastTimeVisible() > recentTime;
                        } else {
                            // Normal criteria
                            hasRecentActivity = stats.getLastTimeUsed() > recentTime || 
                                              stats.getLastTimeVisible() > recentTime;
                        }
                        
                        if (hasRecentActivity) {
                            try {
                                String appName = packageManager.getApplicationLabel(appInfo).toString();
                                
                                AppInfo app = new AppInfo(
                                    packageName,
                                    appName,
                                    packageManager.getApplicationIcon(appInfo)
                                );
                                
                                if (!containsApp(runningApps, app)) {
                                    runningApps.add(app);
                                }
                                break;
                            } catch (Exception e) {
                                // Skip this app
                            }
                        }
                    }
                }
            }
            
            String detectionType = postForceStopRefresh ? " [POST-FORCE-STOP OPTIMIZED]" : "";
            android.util.Log.d(TAG, "Extended detection added more apps, total now: " + runningApps.size() + detectionType);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in extended detection", e);
        }
    }
    
    private void addRunningServicesDetection(List<AppInfo> runningApps, Set<String> whitelistedApps) {
        try {
            List<ActivityManager.RunningServiceInfo> runningServices = 
                activityManager.getRunningServices(Integer.MAX_VALUE);
                
            if (runningServices != null) {
                for (ActivityManager.RunningServiceInfo serviceInfo : runningServices) {
                    String packageName = serviceInfo.service.getPackageName();
                    
                    if (isCriticalSystemApp(packageName) || whitelistedApps.contains(packageName)) {
                        continue;
                    }
                    
                    try {
                        ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                        String appName = packageManager.getApplicationLabel(appInfo).toString();
                        
                        AppInfo app = new AppInfo(
                            packageName,
                            appName,
                            packageManager.getApplicationIcon(appInfo)
                        );
                        
                        if (!containsApp(runningApps, app)) {
                            runningApps.add(app);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        // App not found, skip
                    }
                }
                
                android.util.Log.d(TAG, "Service detection completed, total apps: " + runningApps.size());
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in service detection", e);
        }
    }
    
    private boolean containsApp(List<AppInfo> appList, AppInfo app) {
        for (AppInfo existingApp : appList) {
            if (existingApp.packageName.equals(app.packageName)) {
                return true;
            }
        }
        return false;
    }
    
    private void removeDuplicates(List<AppInfo> runningApps) {
        // Remove duplicates while preserving order
        List<AppInfo> uniqueApps = new ArrayList<>();
        Set<String> seenPackages = new java.util.HashSet<>();
        
        for (AppInfo app : runningApps) {
            if (!seenPackages.contains(app.packageName)) {
                uniqueApps.add(app);
                seenPackages.add(app.packageName);
            }
        }
        
        runningApps.clear();
        runningApps.addAll(uniqueApps);
        
        // Sort by app name for better user experience
        Collections.sort(runningApps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a1, AppInfo a2) {
                return a1.appName.compareToIgnoreCase(a2.appName);
            }
        });
    }
    
    private boolean isCriticalSystemApp(String packageName) {
        // More restrictive system app filtering - only exclude truly critical system apps
        String[] criticalSystemApps = {
            "android",
            "com.android.systemui",
            "com.android.launcher3",
            "com.google.android.gms",
            "com.nomor.memoryclear", // Our own app
            "com.android.phone",
            "com.android.settings",
            "com.android.inputmethod"
        };
        
        for (String systemPackage : criticalSystemApps) {
            if (packageName.equals(systemPackage) || packageName.startsWith(systemPackage + ".")) {
                return true;
            }
        }
        return false;
    }

    
    public List<AppInfo> getExcludedRunningApps() {
        List<AppInfo> excludedApps = new ArrayList<>();
        Set<String> whitelistedApps = AppPreferences.getWhitelistedApps();
        
        for (String packageName : whitelistedApps) {
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                String appName = packageManager.getApplicationLabel(appInfo).toString();
                
                AppInfo app = new AppInfo(
                    packageName,
                    appName,
                    packageManager.getApplicationIcon(appInfo)
                );
                app.isWhitelisted = true;
                excludedApps.add(app);
            } catch (PackageManager.NameNotFoundException e) {
                // App not found, remove from whitelist
                AppPreferences.removeFromWhitelist(packageName);
            }
        }
        
        return excludedApps;
    }
    
    private boolean isSystemApp(String packageName) {
        // Delegate to the more comprehensive method
        return isCriticalSystemApp(packageName);
    }
    
    public int getRunningAppsCount() {
        return getRunningApps().size();
    }
    
    /**
     * Get running apps count with force refresh for immediate update after force stop
     */
    public int getRunningAppsCountForceRefresh() {
        return getRunningAppsForceRefresh().size();
    }
}