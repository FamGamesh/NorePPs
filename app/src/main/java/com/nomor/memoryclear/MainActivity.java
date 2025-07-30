package com.nomor.memoryclear;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    
    private View animatedCircle;
    private TextView runningAppsCount;
    private Button whitelistButton;
    private Button runningAppsButton;
    private Button excludedAppsButton;
    private ImageButton settingsButton;
    private ImageButton infoButton;
    private LinearLayout bottomNavigation;
    private Button homeButton;
    private Button moreButton;
    private LinearLayout brandingLayout;
    
    private AppManager appManager;
    private Handler mainHandler;
    private Runnable updateRunnable;
    private boolean isAnimating = false;
    private ErrorLogger errorLogger;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_main);
            
            // Initialize error logger first
            errorLogger = ErrorLogger.getInstance(this);
            errorLogger.logInfo(TAG, "MainActivity onCreate started");
            
            appManager = new AppManager(this);
            mainHandler = new Handler(Looper.getMainLooper());
            
            initializeViews();
            setupAnimatedCircle();
            setupClickListeners();
            checkPermissionsAndSetup();
            startPeriodicUpdate();
            
            // Show first launch tutorial if needed
            if (AppPreferences.isFirstLaunch()) {
                showFirstLaunchDialog();
                AppPreferences.setFirstLaunch(false);
            }
            
            errorLogger.logInfo(TAG, "MainActivity onCreate completed successfully");
            
        } catch (Exception e) {
            if (errorLogger != null) {
                errorLogger.logError(TAG, "Critical error in MainActivity onCreate", e);
            }
            
            // Try to show a basic error message
            try {
                Toast.makeText(this, "Error initializing app. Check error logs.", Toast.LENGTH_LONG).show();
            } catch (Exception toastError) {
                // If even toast fails, log it
                android.util.Log.e(TAG, "Failed to show error toast", toastError);
            }
        }
    }
    
    private void initializeViews() {
        try {
            animatedCircle = findViewById(R.id.animated_circle);
            runningAppsCount = findViewById(R.id.running_apps_count);
            whitelistButton = findViewById(R.id.btn_whitelist);
            runningAppsButton = findViewById(R.id.btn_running_apps);
            excludedAppsButton = findViewById(R.id.btn_excluded_apps);
            settingsButton = findViewById(R.id.btn_settings);
            infoButton = findViewById(R.id.btn_info);
            bottomNavigation = findViewById(R.id.bottom_navigation);
            homeButton = findViewById(R.id.btn_home);
            moreButton = findViewById(R.id.btn_more);
            brandingLayout = findViewById(R.id.branding_layout);
            
            // Null checks for critical views
            if (animatedCircle == null) {
                errorLogger.logWarning(TAG, "Animated circle view not found");
            }
            if (runningAppsCount == null) {
                errorLogger.logWarning(TAG, "Running apps count view not found");
            }
            if (settingsButton == null) {
                errorLogger.logWarning(TAG, "Settings button not found");
            }
            if (infoButton == null) {
                errorLogger.logWarning(TAG, "Info button not found");
            }
            if (moreButton == null) {
                errorLogger.logWarning(TAG, "More button not found");
            }
            
            errorLogger.logInfo(TAG, "Views initialized successfully");
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error initializing views", e);
        }
    }
    
    private void setupAnimatedCircle() {
        try {
            if (animatedCircle == null) {
                errorLogger.logWarning(TAG, "Cannot setup animated circle - view is null");
                return;
            }
            
            // Create animated gradient circle
            GradientDrawable circleDrawable = new GradientDrawable();
            circleDrawable.setShape(GradientDrawable.OVAL);
            circleDrawable.setStroke(8, Color.parseColor("#4CAF50"));
            circleDrawable.setColor(Color.parseColor("#E8F5E8"));
            
            animatedCircle.setBackground(circleDrawable);
            
            // Start rotating animation
            startCircleAnimation();
            
            errorLogger.logInfo(TAG, "Animated circle setup completed");
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error setting up animated circle", e);
        }
    }
    
    private void startCircleAnimation() {
        try {
            if (isAnimating || animatedCircle == null) return;
            isAnimating = true;
            
            // Rotation animation
            ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(animatedCircle, "rotation", 0f, 360f);
            rotationAnimator.setDuration(4000);
            rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
            rotationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            
            // Scale animation for pulsing effect
            ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(animatedCircle, "scaleX", 1f, 1.1f, 1f);
            scaleXAnimator.setDuration(2000);
            scaleXAnimator.setRepeatCount(ValueAnimator.INFINITE);
            
            ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(animatedCircle, "scaleY", 1f, 1.1f, 1f);
            scaleYAnimator.setDuration(2000);
            scaleYAnimator.setRepeatCount(ValueAnimator.INFINITE);
            
            // Color changing animation for the border
            ValueAnimator colorAnimator = ValueAnimator.ofArgb(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#E91E63"),
                Color.parseColor("#4CAF50")
            );
            colorAnimator.setDuration(5000);
            colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
            colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    try {
                        int color = (int) animator.getAnimatedValue();
                        if (animatedCircle != null && animatedCircle.getBackground() instanceof GradientDrawable) {
                            GradientDrawable drawable = (GradientDrawable) animatedCircle.getBackground();
                            drawable.setStroke(8, color);
                        }
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error updating animation color", e);
                    }
                }
            });
            
            // Combine all animations
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(rotationAnimator, scaleXAnimator, scaleYAnimator, colorAnimator);
            animatorSet.start();
            
            errorLogger.logInfo(TAG, "Circle animation started successfully");
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error starting circle animation", e);
            isAnimating = false;
        }
    }
    
    private void setupClickListeners() {
        try {
            if (whitelistButton != null) {
                whitelistButton.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, WhitelistActivity.class);
                        startActivity(intent);
                        errorLogger.logInfo(TAG, "Whitelist activity launched");
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error launching whitelist activity", e);
                        Toast.makeText(this, "Error opening whitelist", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (runningAppsButton != null) {
                runningAppsButton.setOnClickListener(v -> {
                    try {
                        if (!PermissionHelper.hasUsageStatsPermission(this)) {
                            showPermissionDialog("Usage Stats Permission Required", 
                                "This app needs Usage Stats permission to see running apps.", 
                                () -> PermissionHelper.requestUsageStatsPermission(this));
                            return;
                        }
                        
                        Intent intent = new Intent(this, RunningAppsActivity.class);
                        startActivity(intent);
                        errorLogger.logInfo(TAG, "Running apps activity launched");
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error launching running apps activity", e);
                        Toast.makeText(this, "Error opening running apps", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (excludedAppsButton != null) {
                excludedAppsButton.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, RunningAppsActivity.class);
                        intent.putExtra("show_excluded", true);
                        startActivity(intent);
                        errorLogger.logInfo(TAG, "Excluded apps activity launched");
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error launching excluded apps activity", e);
                        Toast.makeText(this, "Error opening excluded apps", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (settingsButton != null) {
                settingsButton.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, SettingsActivity.class);
                        startActivity(intent);
                        errorLogger.logInfo(TAG, "Settings activity launched successfully");
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error launching settings activity", e);
                        Toast.makeText(this, "Error opening settings. Check error logs.", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                errorLogger.logWarning(TAG, "Settings button is null - cannot set click listener");
            }
            
            if (infoButton != null) {
                infoButton.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, ErrorLogActivity.class);
                        startActivity(intent);
                        errorLogger.logInfo(TAG, "Error log activity launched");
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error launching error log activity", e);
                        Toast.makeText(this, "Error opening error logs", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (homeButton != null) {
                homeButton.setOnClickListener(v -> {
                    try {
                        // Already on home, just refresh
                        updateRunningAppsCount();
                        Toast.makeText(this, "Refreshed!", Toast.LENGTH_SHORT).show();
                        errorLogger.logInfo(TAG, "Home refresh completed");
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error refreshing home", e);
                        Toast.makeText(this, "Error refreshing", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (moreButton != null) {
                moreButton.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, SettingsActivity.class);
                        intent.putExtra("show_more", true);
                        startActivity(intent);
                        errorLogger.logInfo(TAG, "More section in settings launched successfully");
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error launching more section", e);
                        Toast.makeText(this, "Error opening more options. Check error logs.", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                errorLogger.logWarning(TAG, "More button is null - cannot set click listener");
            }
            
            errorLogger.logInfo(TAG, "Click listeners setup completed");
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error setting up click listeners", e);
        }
    }
    
    private void checkPermissionsAndSetup() {
        try {
            if (!PermissionHelper.hasUsageStatsPermission(this)) {
                showPermissionDialog("Setup Required", 
                    "No More Apps PRO needs Usage Stats permission to detect running apps. Grant permission to continue.", 
                    () -> PermissionHelper.requestUsageStatsPermission(this));
            } else {
                updateRunningAppsCount();
            }
            errorLogger.logInfo(TAG, "Permission check completed");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error checking permissions", e);
        }
    }
    
    private void updateRunningAppsCount() {
        new Thread(() -> {
            try {
                final int count = appManager.getRunningAppsCount();
                runOnUiThread(() -> {
                    try {
                        if (runningAppsCount != null) {
                            runningAppsCount.setText(String.valueOf(count));
                            animateCountChange();
                            errorLogger.logInfo(TAG, "Running apps count updated: " + count);
                        } else {
                            errorLogger.logWarning(TAG, "Running apps count view is null");
                        }
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error updating UI with running apps count", e);
                    }
                });
            } catch (Exception e) {
                errorLogger.logError(TAG, "Error getting running apps count", e);
                runOnUiThread(() -> {
                    try {
                        if (runningAppsCount != null) {
                            runningAppsCount.setText("0");
                        }
                    } catch (Exception uiError) {
                        errorLogger.logError(TAG, "Error setting fallback count", uiError);
                    }
                });
            }
        }).start();
    }
    
    private void animateCountChange() {
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(runningAppsCount, "scaleX", 1f, 1.3f, 1f);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(runningAppsCount, "scaleY", 1f, 1.3f, 1f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator);
        animatorSet.setDuration(500);
        animatorSet.start();
    }
    
    private void startPeriodicUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateRunningAppsCount();
                mainHandler.postDelayed(this, 5000); // Update every 5 seconds
            }
        };
        mainHandler.post(updateRunnable);
    }
    
    private void showPermissionDialog(String title, String message, Runnable onPositive) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(title)
               .setMessage(message)
               .setPositiveButton("Grant Permission", (dialog, which) -> onPositive.run())
               .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
               .setCancelable(false)
               .show();
    }
    
    private void showFirstLaunchDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Welcome to No More Apps PRO!")
               .setMessage("This app helps you force stop running apps to free up memory and improve performance.\n\n" +
                          "Features:\n" +
                          "• Force stop running apps\n" +
                          "• Whitelist important apps\n" +
                          "• Schedule automatic cleanup\n" +
                          "• Floating dock for quick access\n\n" +
                          "Grant the required permissions to get started.")
               .setPositiveButton("Get Started", (dialog, which) -> {
                   dialog.dismiss();
                   checkPermissionsAndSetup();
               })
               .setCancelable(false)
               .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateRunningAppsCount();
        
        // Update home button state
        homeButton.setTextColor(Color.parseColor("#4CAF50"));
        moreButton.setTextColor(Color.parseColor("#757575"));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mainHandler != null && updateRunnable != null) {
                mainHandler.removeCallbacks(updateRunnable);
            }
            isAnimating = false;
            errorLogger.logInfo(TAG, "MainActivity destroyed successfully");
        } catch (Exception e) {
            if (errorLogger != null) {
                errorLogger.logError(TAG, "Error in onDestroy", e);
            }
        }
    }
}