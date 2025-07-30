package com.nomor.memoryclear;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public String packageName;
    public String appName;
    public Drawable icon;
    public boolean isSelected;
    public boolean isSystem;
    public boolean isWhitelisted;
    
    public AppInfo(String packageName, String appName, Drawable icon) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.isSelected = true; // Default selected for force stopping
        this.isSystem = false;
        this.isWhitelisted = false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AppInfo appInfo = (AppInfo) obj;
        return packageName.equals(appInfo.packageName);
    }
    
    @Override
    public int hashCode() {
        return packageName.hashCode();
    }
}