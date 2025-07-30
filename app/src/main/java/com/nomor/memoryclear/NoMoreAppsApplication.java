package com.nomor.memoryclear;

import android.app.Application;
import android.content.Context;

public class NoMoreAppsApplication extends Application {
    
    private static final String TAG = "NoMoreAppsApplication";
    private static Context sContext;
    private ErrorLogger errorLogger;
    
    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        
        // Initialize app preferences
        AppPreferences.init(this);
        
        // Initialize error logger
        errorLogger = ErrorLogger.getInstance(this);
        errorLogger.logInfo(TAG, "No More Apps PRO Application initialized");
        
        // Set up global exception handler
        setupGlobalExceptionHandler();
        
        android.util.Log.i(TAG, "No More Apps PRO Application initialized");
    }
    
    private void setupGlobalExceptionHandler() {
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    // Log the uncaught exception
                    errorLogger.logError("UncaughtException", 
                        "Uncaught exception in thread: " + thread.getName(), throwable);
                    
                    android.util.Log.e(TAG, "Uncaught exception logged to ErrorLogger", throwable);
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Failed to log uncaught exception", e);
                }
                
                // Call the original handler to maintain standard behavior
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            }
        });
        
        errorLogger.logInfo(TAG, "Global exception handler set up");
    }
    
    public static Context getAppContext() {
        return sContext;
    }
}