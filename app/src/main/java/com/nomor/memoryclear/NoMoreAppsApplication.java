package com.nomor.memoryclear;

import android.app.Application;
import android.content.Context;

public class NoMoreAppsApplication extends Application {
    
    private static final String TAG = "NoMoreAppsApplication";
    private static Context sContext;
    
    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        
        // Initialize app preferences
        AppPreferences.init(this);
        
        android.util.Log.i(TAG, "No More Apps PRO Application initialized");
    }
    
    public static Context getAppContext() {
        return sContext;
    }
}