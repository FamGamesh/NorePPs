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
        List<AppInfo> runningApps = new ArrayList<>();
        Set<String> whitelistedApps = AppPreferences.getWhitelistedApps();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Use UsageStatsManager for Android 5.0+
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -10); // Last 10 minutes for better detection
            long startTime = calendar.getTimeInMillis();
            long endTime = System.currentTimeMillis();
            
            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, startTime, endTime);
            
            // For higher Android versions, also try recent tasks
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - use multiple strategies
                addRecentlyUsedApps(runningApps, whitelistedApps, startTime);
            }
            
            for (UsageStats usageStats : usageStatsList) {
                if (usageStats.getLastTimeUsed() > startTime) {
                    
                    String packageName = usageStats.getPackageName();
                    
                    // Skip system exclusions and whitelisted apps
                    if (isSystemApp(packageName) || whitelistedApps.contains(packageName)) {
                        continue;
                    }
                    
                    try {
                        ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                        
                        // Focus on user apps for higher Android versions
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Skip system apps more aggressively on newer Android
                            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                                continue;
                            }
                        }
                        
                        String appName = packageManager.getApplicationLabel(appInfo).toString();
                        
                        AppInfo app = new AppInfo(
                            packageName,
                            appName,
                            packageManager.getApplicationIcon(appInfo)
                        );
                        
                        if (!runningApps.contains(app)) {
                            runningApps.add(app);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        // App not found, skip
                    }
                }
            }
        } else {
            // Fallback for older Android versions
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = 
                activityManager.getRunningAppProcesses();
            
            if (runningProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                    for (String packageName : processInfo.pkgList) {
                        if (isSystemApp(packageName) || whitelistedApps.contains(packageName)) {
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
                            
                            if (!runningApps.contains(app)) {
                                runningApps.add(app);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            // App not found, skip
                        }
                    }
                }
            }
        }
        
        return runningApps;
    }
    
    private void addRecentlyUsedApps(List<AppInfo> runningApps, Set<String> whitelistedApps, long startTime) {
        try {
            // Get all installed user apps and check their last used time
            List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            
            for (ApplicationInfo appInfo : installedApps) {
                // Focus on user apps only
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    continue;
                }
                
                String packageName = appInfo.packageName;
                
                if (isSystemApp(packageName) || whitelistedApps.contains(packageName)) {
                    continue;
                }
                
                // Check if app was used recently
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, -5); // Last 5 minutes
                long recentTime = calendar.getTimeInMillis();
                
                List<UsageStats> recentStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST, recentTime, System.currentTimeMillis());
                
                for (UsageStats stats : recentStats) {
                    if (stats.getPackageName().equals(packageName) && 
                        stats.getLastTimeUsed() > recentTime) {
                        
                        try {
                            String appName = packageManager.getApplicationLabel(appInfo).toString();
                            
                            AppInfo app = new AppInfo(
                                packageName,
                                appName,
                                packageManager.getApplicationIcon(appInfo)
                            );
                            
                            if (!runningApps.contains(app)) {
                                runningApps.add(app);
                            }
                            break;
                        } catch (Exception e) {
                            // Skip this app
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error adding recently used apps", e);
        }
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
        for (String systemPackage : SYSTEM_EXCLUSIONS) {
            if (packageName.startsWith(systemPackage)) {
                return true;
            }
        }
        return false;
    }
    
    public int getRunningAppsCount() {
        return getRunningApps().size();
    }
}