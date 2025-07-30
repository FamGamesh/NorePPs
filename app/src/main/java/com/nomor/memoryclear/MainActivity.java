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
    private LinearLayout bottomNavigation;
    private Button homeButton;
    private Button moreButton;
    private LinearLayout brandingLayout;
    
    private AppManager appManager;
    private Handler mainHandler;
    private Runnable updateRunnable;
    private boolean isAnimating = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
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
    }
    
    private void initializeViews() {
        animatedCircle = findViewById(R.id.animated_circle);
        runningAppsCount = findViewById(R.id.running_apps_count);
        whitelistButton = findViewById(R.id.btn_whitelist);
        runningAppsButton = findViewById(R.id.btn_running_apps);
        excludedAppsButton = findViewById(R.id.btn_excluded_apps);
        settingsButton = findViewById(R.id.btn_settings);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        homeButton = findViewById(R.id.btn_home);
        moreButton = findViewById(R.id.btn_more);
        brandingLayout = findViewById(R.id.branding_layout);
    }
    
    private void setupAnimatedCircle() {
        // Create animated gradient circle
        GradientDrawable circleDrawable = new GradientDrawable();
        circleDrawable.setShape(GradientDrawable.OVAL);
        circleDrawable.setStroke(8, Color.parseColor("#4CAF50"));
        circleDrawable.setColor(Color.parseColor("#E8F5E8"));
        
        animatedCircle.setBackground(circleDrawable);
        
        // Start rotating animation
        startCircleAnimation();
    }
    
    private void startCircleAnimation() {
        if (isAnimating) return;
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
                int color = (int) animator.getAnimatedValue();
                GradientDrawable drawable = (GradientDrawable) animatedCircle.getBackground();
                drawable.setStroke(8, color);
            }
        });
        
        // Combine all animations
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(rotationAnimator, scaleXAnimator, scaleYAnimator, colorAnimator);
        animatorSet.start();
    }
    
    private void setupClickListeners() {
        whitelistButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, WhitelistActivity.class);
            startActivity(intent);
        });
        
        runningAppsButton.setOnClickListener(v -> {
            if (!PermissionHelper.hasUsageStatsPermission(this)) {
                showPermissionDialog("Usage Stats Permission Required", 
                    "This app needs Usage Stats permission to see running apps.", 
                    () -> PermissionHelper.requestUsageStatsPermission(this));
                return;
            }
            
            Intent intent = new Intent(this, RunningAppsActivity.class);
            startActivity(intent);
        });
        
        excludedAppsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RunningAppsActivity.class);
            intent.putExtra("show_excluded", true);
            startActivity(intent);
        });
        
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        
        homeButton.setOnClickListener(v -> {
            // Already on home, just refresh
            updateRunningAppsCount();
            Toast.makeText(this, "Refreshed!", Toast.LENGTH_SHORT).show();
        });
        
        moreButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("show_more", true);
            startActivity(intent);
        });
    }
    
    private void checkPermissionsAndSetup() {
        if (!PermissionHelper.hasUsageStatsPermission(this)) {
            showPermissionDialog("Setup Required", 
                "No More Apps PRO needs Usage Stats permission to detect running apps. Grant permission to continue.", 
                () -> PermissionHelper.requestUsageStatsPermission(this));
        } else {
            updateRunningAppsCount();
        }
    }
    
    private void updateRunningAppsCount() {
        new Thread(() -> {
            try {
                final int count = appManager.getRunningAppsCount();
                runOnUiThread(() -> {
                    runningAppsCount.setText(String.valueOf(count));
                    animateCountChange();
                });
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error updating running apps count", e);
                runOnUiThread(() -> runningAppsCount.setText("0"));
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
        if (mainHandler != null && updateRunnable != null) {
            mainHandler.removeCallbacks(updateRunnable);
        }
        isAnimating = false;
    }
}