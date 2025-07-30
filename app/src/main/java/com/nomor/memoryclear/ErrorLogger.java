package com.nomor.memoryclear;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ErrorLogger {
    private static final String TAG = "ErrorLogger";
    private static final String PREFS_NAME = "ErrorLoggerPrefs";
    private static final String KEY_ERROR_LOGS = "error_logs";
    private static final int MAX_LOGS = 100; // Keep last 100 errors
    
    private static ErrorLogger sInstance;
    private SharedPreferences mPrefs;
    private SimpleDateFormat mDateFormat;
    
    private ErrorLogger(Context context) {
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }
    
    public static synchronized ErrorLogger getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ErrorLogger(context.getApplicationContext());
        }
        return sInstance;
    }
    
    /**
     * Log an error with full details
     */
    public void logError(String tag, String message, Throwable throwable) {
        try {
            JSONObject errorObj = new JSONObject();
            errorObj.put("timestamp", mDateFormat.format(new Date()));
            errorObj.put("tag", tag);
            errorObj.put("message", message);
            errorObj.put("level", "ERROR");
            
            // Add device information
            JSONObject deviceInfo = getDeviceInfo();
            errorObj.put("device", deviceInfo);
            
            // Add stack trace if available
            if (throwable != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                errorObj.put("stackTrace", sw.toString());
                errorObj.put("exceptionType", throwable.getClass().getSimpleName());
                errorObj.put("exceptionMessage", throwable.getMessage());
            }
            
            saveErrorLog(errorObj);
            
            // Also log to Android logcat
            if (throwable != null) {
                Log.e(tag, message, throwable);
            } else {
                Log.e(tag, message);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to log error", e);
        }
    }
    
    /**
     * Log a warning
     */
    public void logWarning(String tag, String message) {
        try {
            JSONObject errorObj = new JSONObject();
            errorObj.put("timestamp", mDateFormat.format(new Date()));
            errorObj.put("tag", tag);
            errorObj.put("message", message);
            errorObj.put("level", "WARNING");
            
            JSONObject deviceInfo = getDeviceInfo();
            errorObj.put("device", deviceInfo);
            
            saveErrorLog(errorObj);
            Log.w(tag, message);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to log warning", e);
        }
    }
    
    /**
     * Log an info message
     */
    public void logInfo(String tag, String message) {
        try {
            JSONObject errorObj = new JSONObject();
            errorObj.put("timestamp", mDateFormat.format(new Date()));
            errorObj.put("tag", tag);
            errorObj.put("message", message);
            errorObj.put("level", "INFO");
            
            saveErrorLog(errorObj);
            Log.i(tag, message);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to log info", e);
        }
    }
    
    /**
     * Save error log to SharedPreferences
     */
    private void saveErrorLog(JSONObject errorObj) {
        try {
            String existingLogs = mPrefs.getString(KEY_ERROR_LOGS, "[]");
            JSONArray logsArray = new JSONArray(existingLogs);
            
            // Add new log
            logsArray.put(errorObj);
            
            // Keep only the last MAX_LOGS entries
            if (logsArray.length() > MAX_LOGS) {
                JSONArray trimmedArray = new JSONArray();
                int startIndex = logsArray.length() - MAX_LOGS;
                for (int i = startIndex; i < logsArray.length(); i++) {
                    trimmedArray.put(logsArray.get(i));
                }
                logsArray = trimmedArray;
            }
            
            mPrefs.edit().putString(KEY_ERROR_LOGS, logsArray.toString()).apply();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to save error log", e);
        }
    }
    
    /**
     * Get all error logs
     */
    public List<JSONObject> getAllLogs() {
        List<JSONObject> logs = new ArrayList<>();
        try {
            String existingLogs = mPrefs.getString(KEY_ERROR_LOGS, "[]");
            JSONArray logsArray = new JSONArray(existingLogs);
            
            for (int i = logsArray.length() - 1; i >= 0; i--) { // Most recent first
                logs.add(logsArray.getJSONObject(i));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve logs", e);
        }
        return logs;
    }
    
    /**
     * Get formatted logs as string for copying
     */
    public String getFormattedLogs() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== NO MORE APPS PRO - ERROR LOGS ===\n");
        sb.append("Generated: ").append(mDateFormat.format(new Date())).append("\n\n");
        
        List<JSONObject> logs = getAllLogs();
        
        if (logs.isEmpty()) {
            sb.append("No error logs found.\n");
        } else {
            for (int i = 0; i < logs.size(); i++) {
                try {
                    JSONObject log = logs.get(i);
                    sb.append("--- Log Entry ").append(i + 1).append(" ---\n");
                    sb.append("Timestamp: ").append(log.optString("timestamp", "Unknown")).append("\n");
                    sb.append("Level: ").append(log.optString("level", "Unknown")).append("\n");
                    sb.append("Tag: ").append(log.optString("tag", "Unknown")).append("\n");
                    sb.append("Message: ").append(log.optString("message", "No message")).append("\n");
                    
                    if (log.has("exceptionType")) {
                        sb.append("Exception: ").append(log.optString("exceptionType")).append("\n");
                        sb.append("Exception Message: ").append(log.optString("exceptionMessage", "None")).append("\n");
                    }
                    
                    if (log.has("stackTrace")) {
                        sb.append("Stack Trace:\n").append(log.optString("stackTrace")).append("\n");
                    }
                    
                    if (log.has("device")) {
                        JSONObject device = log.optJSONObject("device");
                        if (device != null) {
                            sb.append("Device Info: ").append(device.toString()).append("\n");
                        }
                    }
                    
                    sb.append("\n");
                    
                } catch (Exception e) {
                    sb.append("Error parsing log entry: ").append(e.getMessage()).append("\n\n");
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Clear all logs
     */
    public void clearLogs() {
        mPrefs.edit().remove(KEY_ERROR_LOGS).apply();
        Log.i(TAG, "All error logs cleared");
    }
    
    /**
     * Get device information
     */
    private JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();
        try {
            deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("model", Build.MODEL);
            deviceInfo.put("device", Build.DEVICE);
            deviceInfo.put("androidVersion", Build.VERSION.RELEASE);
            deviceInfo.put("apiLevel", Build.VERSION.SDK_INT);
            deviceInfo.put("brand", Build.BRAND);
            deviceInfo.put("product", Build.PRODUCT);
            
            // App information
            Context context = NoMoreAppsApplication.getAppContext();
            if (context != null) {
                try {
                    String packageName = context.getPackageName();
                    String versionName = context.getPackageManager()
                            .getPackageInfo(packageName, 0).versionName;
                    int versionCode = context.getPackageManager()
                            .getPackageInfo(packageName, 0).versionCode;
                    
                    deviceInfo.put("appPackage", packageName);
                    deviceInfo.put("appVersion", versionName);
                    deviceInfo.put("appVersionCode", versionCode);
                } catch (Exception e) {
                    deviceInfo.put("appInfoError", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to get device info", e);
        }
        return deviceInfo;
    }
    
    /**
     * Get count of error logs
     */
    public int getLogCount() {
        try {
            String existingLogs = mPrefs.getString(KEY_ERROR_LOGS, "[]");
            JSONArray logsArray = new JSONArray(existingLogs);
            return logsArray.length();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Check if there are any error level logs
     */
    public boolean hasErrors() {
        List<JSONObject> logs = getAllLogs();
        for (JSONObject log : logs) {
            if ("ERROR".equals(log.optString("level"))) {
                return true;
            }
        }
        return false;
    }
}