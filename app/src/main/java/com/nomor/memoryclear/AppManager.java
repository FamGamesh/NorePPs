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
        
        try {
            // Use multiple detection strategies for comprehensive app detection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Strategy 1: UsageStatsManager - Recent usage based detection
                addRunningAppsFromUsageStats(runningApps, whitelistedApps);
                
                // Strategy 2: ActivityManager - Process based detection (still works on newer Android)
                addRunningAppsFromProcesses(runningApps, whitelistedApps);
                
                // Strategy 3: Extended recent usage detection for Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    addExtendedRunningAppsDetection(runningApps, whitelistedApps);
                }
                
                // Strategy 4: Running services detection
                addRunningServicesDetection(runningApps, whitelistedApps);
                
            } else {
                // Fallback for older Android versions (Pre-Lollipop)
                addRunningAppsFromProcesses(runningApps, whitelistedApps);
            }
            
            // Remove duplicates and sort
            removeDuplicates(runningApps);
            
            android.util.Log.d(TAG, "Detected " + runningApps.size() + " running apps using enhanced detection");
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in comprehensive app detection", e);
        }
        
        return runningApps;
    }
    
    private void addRunningAppsFromUsageStats(List<AppInfo> runningApps, Set<String> whitelistedApps) {
        try {
            // Extended time window for better detection
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -30); // Last 30 minutes for comprehensive detection
            long startTime = calendar.getTimeInMillis();
            long endTime = System.currentTimeMillis();
            
            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, startTime, endTime);
            
            for (UsageStats usageStats : usageStatsList) {
                // Check if app was active recently
                if (usageStats.getLastTimeUsed() > startTime || 
                    usageStats.getTotalTimeInForeground() > 0) {
                    
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
            
            android.util.Log.d(TAG, "UsageStats detection added " + runningApps.size() + " apps");
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
    
    private void addExtendedRunningAppsDetection(List<AppInfo> runningApps, Set<String> whitelistedApps) {
        try {
            // Get all installed apps and check recent activity
            List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            
            // Check recent activity (last 5 minutes for immediate detection)
            Calendar recentCalendar = Calendar.getInstance();
            recentCalendar.add(Calendar.MINUTE, -5);
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
                    if (stats.getPackageName().equals(packageName) && 
                        (stats.getLastTimeUsed() > recentTime || 
                         stats.getLastTimeVisible() > recentTime)) {
                        
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
            
            android.util.Log.d(TAG, "Extended detection added more apps, total now: " + runningApps.size());
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
}