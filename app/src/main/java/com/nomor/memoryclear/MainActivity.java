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

// AdMob imports
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.LoadAdError;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    
    private View animatedCircle;
    private TextView runningAppsCount;
    private Button whitelistButton;
    private Button runningAppsButton;
    private Button excludedAppsButton;
    private Button premiumSpeedButton;
    private ImageButton settingsButton;
    private ImageButton infoButton;
    private LinearLayout bottomNavigation;
    private Button homeButton;
    private Button moreButton;
    
    // AdMob components
    private AdView topBannerAd;
    private AdView bottomBannerAd;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    
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
            
            // Initialize AdMob
            initializeAdMob();
            
            appManager = new AppManager(this);
            mainHandler = new Handler(Looper.getMainLooper());
            
            initializeViews();
            setupAnimatedCircle();
            setupClickListeners();
            
            // Apply intelligent UI adaptation
            adaptUIForDevice();
            
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
    
    private void initializeAdMob() {
        try {
            // Initialize AdMob
            MobileAds.initialize(this, initializationStatus -> {
                errorLogger.logInfo(TAG, "AdMob initialized successfully");
            });
            
            // Load banner ads
            loadBannerAds();
            
            // Load interstitial ad
            loadInterstitialAd();
            
            // Load rewarded ad
            loadRewardedAd();
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error initializing AdMob", e);
        }
    }
    
    private void loadBannerAds() {
        try {
            // Top banner ad
            topBannerAd = findViewById(R.id.top_banner_ad);
            if (topBannerAd != null) {
                AdRequest adRequest = new AdRequest.Builder().build();
                topBannerAd.loadAd(adRequest);
            }
            
            // Bottom banner ad
            bottomBannerAd = findViewById(R.id.bottom_banner_ad);
            if (bottomBannerAd != null) {
                AdRequest adRequest = new AdRequest.Builder().build();
                bottomBannerAd.loadAd(adRequest);
            }
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error loading banner ads", e);
        }
    }
    
    private void loadInterstitialAd() {
        try {
            AdRequest adRequest = new AdRequest.Builder().build();
            
            InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd interstitialAd) {
                        MainActivity.this.interstitialAd = interstitialAd;
                        errorLogger.logInfo(TAG, "Interstitial ad loaded");
                    }
                    
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        interstitialAd = null;
                        errorLogger.logWarning(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                    }
                });
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error loading interstitial ad", e);
        }
    }
    
    private void loadRewardedAd() {
        try {
            AdRequest adRequest = new AdRequest.Builder().build();
            
            RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd rewardedAd) {
                        MainActivity.this.rewardedAd = rewardedAd;
                        errorLogger.logInfo(TAG, "Rewarded ad loaded");
                    }
                    
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        rewardedAd = null;
                        errorLogger.logWarning(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                    }
                });
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error loading rewarded ad", e);
        }
    }
    
    private void showInterstitialAd() {
        try {
            if (interstitialAd != null) {
                interstitialAd.show(this);
                loadInterstitialAd(); // Load next ad
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing interstitial ad", e);
        }
    }
    
    /**
     * Shows interstitial ad immediately when clicked, then executes navigation after delay
     */
    private void showInterstitialAdThenNavigate(Runnable navigationAction) {
        try {
            if (interstitialAd != null) {
                interstitialAd.show(this);
                
                // Set up callback for when ad is dismissed
                interstitialAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        // Execute navigation after ad is dismissed
                        navigationAction.run();
                        // Load next ad
                        loadInterstitialAd();
                    }
                    
                    @Override
                    public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                        // If ad fails to show, still proceed with navigation
                        navigationAction.run();
                        loadInterstitialAd();
                    }
                    
                    @Override
                    public void onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent();
                        errorLogger.logInfo(TAG, "Interstitial ad shown successfully");
                    }
                });
            } else {
                // No ad available, proceed with navigation immediately
                navigationAction.run();
                errorLogger.logInfo(TAG, "No interstitial ad available, proceeding with navigation");
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing interstitial ad", e);
            // If there's an error, still proceed with navigation
            navigationAction.run();
        }
    }
    
    /**
     * Intelligent UI adaptation based on device screen size and Android version
     */
    private void adaptUIForDevice() {
        try {
            // Get screen dimensions
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            
            int screenHeight = displayMetrics.heightPixels;
            int screenWidth = displayMetrics.widthPixels;
            float density = displayMetrics.density;
            
            // Calculate available space considering system UI
            int statusBarHeight = getStatusBarHeight();
            int navigationBarHeight = getNavigationBarHeight();
            int availableHeight = screenHeight - statusBarHeight - navigationBarHeight;
            
            // Calculate ad banner heights
            int topBannerHeight = getBannerAdHeight();
            int bottomBannerHeight = getBannerAdHeight();
            
            // Available content area
            int contentHeight = availableHeight - topBannerHeight - bottomBannerHeight;
            
            android.util.Log.d(TAG, "Screen adaptation - Height: " + screenHeight + 
                ", Available: " + availableHeight + ", Content: " + contentHeight + 
                ", Android: " + Build.VERSION.SDK_INT);
            
            // Apply intelligent adaptations
            adaptLayoutForScreenSize(contentHeight, screenHeight, density);
            adaptForAndroidVersion();
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error adapting UI for device", e);
        }
    }
    
    private void adaptLayoutForScreenSize(int contentHeight, int screenHeight, float density) {
        try {
            // Convert dp to pixels
            int dp400 = (int) (400 * density);
            int dp350 = (int) (350 * density);
            
            if (contentHeight < dp400) {
                // Small screen adaptations
                makeLayoutCompact();
                enableScrollingForButtons();
                adjustBannerAdSizes(true);
                
                android.util.Log.d(TAG, "Applied small screen adaptations");
            } else if (contentHeight < dp350 * 2) {
                // Medium screen adaptations
                adjustSpacingForMediumScreen();
                enableScrollingForButtons();
                
                android.util.Log.d(TAG, "Applied medium screen adaptations");
            } else {
                // Large screen - standard layout
                adjustBannerAdSizes(false);
                android.util.Log.d(TAG, "Using standard layout for large screen");
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error adapting layout for screen size", e);
        }
    }
    
    private void makeLayoutCompact() {
        try {
            // Make buttons smaller and reduce spacing
            if (whitelistButton != null) {
                android.view.ViewGroup.LayoutParams params = whitelistButton.getLayoutParams();
                if (params != null) {
                    params.height = (int) (40 * getResources().getDisplayMetrics().density);
                    whitelistButton.setLayoutParams(params);
                }
            }
            
            if (runningAppsButton != null) {
                android.view.ViewGroup.LayoutParams params = runningAppsButton.getLayoutParams();
                if (params != null) {
                    params.height = (int) (40 * getResources().getDisplayMetrics().density);
                    runningAppsButton.setLayoutParams(params);
                }
            }
            
            if (excludedAppsButton != null) {
                android.view.ViewGroup.LayoutParams params = excludedAppsButton.getLayoutParams();
                if (params != null) {
                    params.height = (int) (40 * getResources().getDisplayMetrics().density);
                    excludedAppsButton.setLayoutParams(params);
                }
            }
            
            if (premiumSpeedButton != null) {
                android.view.ViewGroup.LayoutParams params = premiumSpeedButton.getLayoutParams();
                if (params != null) {
                    params.height = (int) (35 * getResources().getDisplayMetrics().density);
                    premiumSpeedButton.setLayoutParams(params);
                }
            }
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error making layout compact", e);
        }
    }
    
    private void enableScrollingForButtons() {
        try {
            // Find the main content container and wrap it in a ScrollView if not already
            View mainContent = findViewById(android.R.id.content);
            if (mainContent != null) {
                // Add scrolling capability to ensure all buttons are accessible
                if (!(mainContent.getParent() instanceof android.widget.ScrollView)) {
                    // The layout should already have proper scrolling structure
                    // Ensure scroll indicators are visible
                    android.widget.ScrollView scrollView = findViewById(android.R.id.content);
                    if (scrollView != null) {
                        scrollView.setScrollbarFadingEnabled(false);
                        scrollView.setVerticalScrollBarEnabled(true);
                    }
                }
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error enabling scrolling", e);
        }
    }
    
    private void adjustSpacingForMediumScreen() {
        try {
            // Reduce margins and padding for medium screens
            int reducedMargin = (int) (8 * getResources().getDisplayMetrics().density);
            
            if (bottomNavigation != null) {
                android.view.ViewGroup.MarginLayoutParams params = 
                    (android.view.ViewGroup.MarginLayoutParams) bottomNavigation.getLayoutParams();
                if (params != null) {
                    params.bottomMargin = reducedMargin;
                    bottomNavigation.setLayoutParams(params);
                }
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error adjusting spacing for medium screen", e);
        }
    }
    
    private void adjustBannerAdSizes(boolean useSmallBanners) {
        try {
            if (topBannerAd != null) {
                android.view.ViewGroup.LayoutParams params = topBannerAd.getLayoutParams();
                if (params != null && useSmallBanners) {
                    // Use smaller banner ad size for small screens
                    params.height = (int) (32 * getResources().getDisplayMetrics().density);
                    topBannerAd.setLayoutParams(params);
                }
            }
            
            if (bottomBannerAd != null) {
                android.view.ViewGroup.LayoutParams params = bottomBannerAd.getLayoutParams();
                if (params != null && useSmallBanners) {
                    params.height = (int) (32 * getResources().getDisplayMetrics().density);
                    bottomBannerAd.setLayoutParams(params);
                }
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error adjusting banner ad sizes", e);
        }
    }
    
    private void adaptForAndroidVersion() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
                // Handle display cutouts and edge-to-edge display
                getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                );
            }
            
            if (Build.VERSION.SDK_INT >= 35) { // Android 15 (API 35)
                // Special adaptations for Android 15
                adjustForAndroid15();
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error adapting for Android version", e);
        }
    }
    
    private void adjustForAndroid15() {
        try {
            // Android 15 specific adaptations
            // Ensure proper padding for gesture navigation
            View mainContent = findViewById(android.R.id.content);
            if (mainContent != null) {
                int gestureNavPadding = (int) (16 * getResources().getDisplayMetrics().density);
                mainContent.setPadding(
                    mainContent.getPaddingLeft(),
                    mainContent.getPaddingTop(),
                    mainContent.getPaddingRight(),
                    gestureNavPadding
                );
            }
            
            // Ensure bottom navigation is properly positioned above system UI
            if (bottomNavigation != null) {
                android.view.ViewGroup.MarginLayoutParams params = 
                    (android.view.ViewGroup.MarginLayoutParams) bottomNavigation.getLayoutParams();
                if (params != null) {
                    params.bottomMargin = getNavigationBarHeight() + 
                        (int) (8 * getResources().getDisplayMetrics().density);
                    bottomNavigation.setLayoutParams(params);
                }
            }
            
            android.util.Log.d(TAG, "Applied Android 15 specific adaptations");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error adjusting for Android 15", e);
        }
    }
    
    private int getStatusBarHeight() {
        try {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                return getResources().getDimensionPixelSize(resourceId);
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error getting status bar height", e);
        }
        return (int) (24 * getResources().getDisplayMetrics().density); // Default 24dp
    }
    
    private int getNavigationBarHeight() {
        try {
            int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                return getResources().getDimensionPixelSize(resourceId);
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error getting navigation bar height", e);
        }
        return (int) (48 * getResources().getDisplayMetrics().density); // Default 48dp
    }
    
    private int getBannerAdHeight() {
        // Standard banner ad height
        return (int) (50 * getResources().getDisplayMetrics().density);
    }
    private void initializeViews() {
        try {
            animatedCircle = findViewById(R.id.animated_circle);
            runningAppsCount = findViewById(R.id.running_apps_count);
            whitelistButton = findViewById(R.id.btn_whitelist);
            runningAppsButton = findViewById(R.id.btn_running_apps);
            excludedAppsButton = findViewById(R.id.btn_excluded_apps);
            premiumSpeedButton = findViewById(R.id.btn_premium_speed);
            settingsButton = findViewById(R.id.btn_settings);
            infoButton = findViewById(R.id.btn_info);
            bottomNavigation = findViewById(R.id.bottom_navigation);
            homeButton = findViewById(R.id.btn_home);
            moreButton = findViewById(R.id.btn_more);
            
            // Fix home button text
            if (homeButton != null) {
                homeButton.setText("Home");
            }
            
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
            if (premiumSpeedButton == null) {
                errorLogger.logWarning(TAG, "Premium speed button not found");
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
                        // Show interstitial ad first, then navigate
                        showInterstitialAdThenNavigate(() -> {
                            Intent intent = new Intent(this, WhitelistActivity.class);
                            startActivity(intent);
                            errorLogger.logInfo(TAG, "Whitelist activity launched");
                        });
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error launching whitelist activity", e);
                        Toast.makeText(this, "Error opening whitelist", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (runningAppsButton != null) {
                runningAppsButton.setOnClickListener(v -> {
                    try {
                        if (!hasRequiredPermissions()) {
                            return; // Permission check will handle the error
                        }
                        
                        // Show interstitial ad first, then navigate
                        showInterstitialAdThenNavigate(() -> {
                            Intent intent = new Intent(this, RunningAppsActivity.class);
                            startActivity(intent);
                            errorLogger.logInfo(TAG, "Running apps activity launched");
                        });
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error launching running apps activity", e);
                        Toast.makeText(this, "Error opening running apps", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (excludedAppsButton != null) {
                excludedAppsButton.setOnClickListener(v -> {
                    try {
                        // Show interstitial ad first, then navigate
                        showInterstitialAdThenNavigate(() -> {
                            Intent intent = new Intent(this, RunningAppsActivity.class);
                            intent.putExtra("show_excluded", true);
                            startActivity(intent);
                            errorLogger.logInfo(TAG, "Excluded apps activity launched");
                        });
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error launching excluded apps activity", e);
                        Toast.makeText(this, "Error opening excluded apps", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (premiumSpeedButton != null) {
                premiumSpeedButton.setOnClickListener(v -> {
                    try {
                        showRewardedAdForPremium();
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error showing rewarded ad for premium", e);
                        Toast.makeText(this, "Error opening premium feature", Toast.LENGTH_SHORT).show();
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
    
    private boolean hasRequiredPermissions() {
        try {
            if (!PermissionHelper.hasUsageStatsPermission(this)) {
                showIntelligentPermissionDialog("Usage Stats Permission Required", 
                    "Enable Usage Stats permission to see running apps and improve performance.", 
                    () -> PermissionHelper.requestUsageStatsPermission(this));
                return false;
            }
            return true;
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error checking permissions", e);
            return false;
        }
    }
    
    private void showRewardedAdForPremium() {
        try {
            if (rewardedAd != null) {
                rewardedAd.show(this, rewardItem -> {
                    try {
                        // User earned reward, activate premium for 1 day
                        AppPreferences.setPremiumActive(true);
                        AppPreferences.setPremiumExpiryTime(System.currentTimeMillis() + (24 * 60 * 60 * 1000)); // 1 day
                        
                        Toast.makeText(this, "Premium Speed activated for 24 hours!", Toast.LENGTH_LONG).show();
                        updatePremiumButtonUI();
                        errorLogger.logInfo(TAG, "Premium activated via rewarded ad");
                        
                        // Load next rewarded ad
                        loadRewardedAd();
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error activating premium", e);
                    }
                });
            } else {
                // No ad available, show message
                showPremiumUnavailableDialog();
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing rewarded ad", e);
            showPremiumUnavailableDialog();
        }
    }
    
    private void showPremiumUnavailableDialog() {
        try {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Premium Speed")
                   .setMessage("Watch a rewarded ad to unlock Premium Speed for 24 hours!\n\n" +
                              "Premium Features:\n" +
                              "• 3-5x faster force stopping\n" +
                              "• Enhanced performance mode\n" +
                              "• Priority processing\n\n" +
                              "Ad not available right now. Please try again later.")
                   .setPositiveButton("Try Again", (dialog, which) -> {
                       loadRewardedAd();
                       Toast.makeText(this, "Loading ad...", Toast.LENGTH_SHORT).show();
                   })
                   .setNegativeButton("Cancel", null)
                   .show();
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing premium unavailable dialog", e);
        }
    }
    
    private void updatePremiumButtonUI() {
        try {
            if (premiumSpeedButton != null) {
                boolean isPremiumActive = AppPreferences.isPremiumActive();
                if (isPremiumActive) {
                    premiumSpeedButton.setText("Premium Active ⚡");
                    premiumSpeedButton.setEnabled(false);
                } else {
                    premiumSpeedButton.setText("Premium Speed");
                    premiumSpeedButton.setEnabled(true);
                }
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error updating premium button UI", e);
        }
    }
    
    private void checkPermissionsAndSetup() {
        try {
            // Only show setup dialog if no usage stats permission at all
            // Remove duplicate dialog issue
            updateRunningAppsCount(); // Always try to update, permission check is in hasRequiredPermissions()
            updatePremiumButtonUI(); // Update premium button state
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
    
    /**
     * Enhanced intelligent permission dialog that checks permission status before showing dialog
     * Only shows dialog if permission is not already granted
     * Provides better user experience and avoids duplicate dialogs
     */
    private void showIntelligentPermissionDialog(String title, String message, Runnable onPositive) {
        try {
            // Intelligent logic: Check permission type based on title and only show if needed
            boolean shouldShowDialog = false;
            String enhancedMessage = message;
            
            if (title.toLowerCase().contains("usage stats")) {
                // Check if Usage Stats permission is already granted
                if (!PermissionHelper.hasUsageStatsPermission(this)) {
                    shouldShowDialog = true;
                    enhancedMessage = message + "\n\n⚠️ This permission is required to detect running apps and improve the app's performance.";
                } else {
                    // Permission already granted, show success message instead
                    Toast.makeText(this, "Usage Stats permission is already granted! ✅", Toast.LENGTH_SHORT).show();
                    errorLogger.logInfo(TAG, "Usage Stats permission already granted - dialog not shown");
                    return;
                }
            } else if (title.toLowerCase().contains("accessibility")) {
                // Check if Accessibility permission is already granted
                if (!PermissionHelper.hasAccessibilityPermission(this)) {
                    shouldShowDialog = true;
                    enhancedMessage = message + "\n\n⚠️ This permission allows the app to automatically force stop apps for you.";
                } else {
                    Toast.makeText(this, "Accessibility permission is already granted! ✅", Toast.LENGTH_SHORT).show();
                    errorLogger.logInfo(TAG, "Accessibility permission already granted - dialog not shown");
                    return;
                }
            } else if (title.toLowerCase().contains("overlay") || title.toLowerCase().contains("display over")) {
                // Check if Overlay permission is already granted
                if (!PermissionHelper.hasOverlayPermission(this)) {
                    shouldShowDialog = true;
                    enhancedMessage = message + "\n\n⚠️ This permission is needed for floating dock and overlays.";
                } else {
                    Toast.makeText(this, "Display over apps permission is already granted! ✅", Toast.LENGTH_SHORT).show();
                    errorLogger.logInfo(TAG, "Overlay permission already granted - dialog not shown");
                    return;
                }
            } else {
                // Generic permission - always show dialog
                shouldShowDialog = true;
            }
            
            if (shouldShowDialog) {
                // Create final copies for lambda expressions
                final String finalTitle = title;
                final String finalEnhancedMessage = enhancedMessage;
                
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                builder.setTitle("🔐 " + title)
                       .setMessage(enhancedMessage)
                       .setPositiveButton("Grant Permission", (dialog, which) -> {
                           try {
                               onPositive.run();
                               errorLogger.logInfo(TAG, "Permission request initiated: " + finalTitle);
                           } catch (Exception e) {
                               errorLogger.logError(TAG, "Error executing permission callback", e);
                               Toast.makeText(this, "Error granting permission", Toast.LENGTH_SHORT).show();
                           }
                       })
                       .setNegativeButton("Not Now", (dialog, which) -> {
                           dialog.dismiss();
                           Toast.makeText(this, "You can grant this permission later from Settings", Toast.LENGTH_LONG).show();
                           errorLogger.logInfo(TAG, "Permission request declined: " + finalTitle);
                       })
                       .setNeutralButton("Learn More", (dialog, which) -> {
                           showPermissionEducationDialog(finalTitle, finalEnhancedMessage);
                       })
                       .setCancelable(false)
                       .show();
                
                errorLogger.logInfo(TAG, "Intelligent permission dialog shown: " + title);
            }
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error in showIntelligentPermissionDialog", e);
            // Fallback to simple permission dialog
            showPermissionDialog(title, message, onPositive);
        }
    }
    
    /**
     * Shows educational dialog about why permissions are needed
     */
    private void showPermissionEducationDialog(String permissionTitle, String message) {
        try {
            String educationalContent = getPermissionEducationContent(permissionTitle);
            
            // Create final copies for lambda expressions
            final String finalPermissionTitle = permissionTitle;
            final String finalMessage = message;
            
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Why " + permissionTitle + "?")
                   .setMessage(educationalContent)
                   .setPositiveButton("Grant Now", (dialog, which) -> {
                       // Close education dialog and show permission request again
                       dialog.dismiss();
                       showIntelligentPermissionDialog(finalPermissionTitle, finalMessage, () -> {
                           // This will be the original callback passed to intelligent dialog
                       });
                   })
                   .setNegativeButton("Maybe Later", (dialog, which) -> dialog.dismiss())
                   .show();
                   
            errorLogger.logInfo(TAG, "Permission education dialog shown: " + permissionTitle);
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing permission education dialog", e);
        }
    }
    
    /**
     * Get educational content for specific permissions
     */
    private String getPermissionEducationContent(String permissionTitle) {
        try {
            if (permissionTitle.toLowerCase().contains("usage stats")) {
                return "📊 Usage Stats Permission allows No More Apps PRO to:\n\n" +
                       "✅ Detect which apps are currently running\n" +
                       "✅ Monitor memory usage of apps\n" +
                       "✅ Provide accurate app performance data\n" +
                       "✅ Help you identify apps consuming resources\n\n" +
                       "🔒 This permission is safe and doesn't access personal data.\n" +
                       "🚀 It helps the app work 3-5x faster in detecting running apps!";
            } else if (permissionTitle.toLowerCase().contains("accessibility")) {
                return "♿ Accessibility Permission allows No More Apps PRO to:\n\n" +
                       "✅ Automatically force stop apps in background\n" +
                       "✅ Work even when your device screen is off\n" +
                       "✅ Perform scheduled cleanups automatically\n" +
                       "✅ Provide Premium Speed features\n\n" +
                       "🔒 This permission is used only for app management.\n" +
                       "⚡ Essential for automated force stopping!";
            } else if (permissionTitle.toLowerCase().contains("overlay") || permissionTitle.toLowerCase().contains("display over")) {
                return "📱 Display Over Apps Permission allows No More Apps PRO to:\n\n" +
                       "✅ Show floating dock for quick access\n" +
                       "✅ Display cleanup notifications\n" +
                       "✅ Provide quick force stop shortcuts\n" +
                       "✅ Work seamlessly with other apps\n\n" +
                       "🔒 Only used for app's own interface elements.\n" +
                       "🎯 Makes the app more convenient to use!";
            } else {
                return "🔐 This permission helps No More Apps PRO work better:\n\n" +
                       "✅ Provides enhanced functionality\n" +
                       "✅ Improves user experience\n" +
                       "✅ Ensures reliable performance\n\n" +
                       "🔒 Your privacy and security are always protected.";
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error getting permission education content", e);
            return "This permission helps the app provide better functionality and user experience.";
        }
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
        try {
            updateRunningAppsCount();
            updatePremiumButtonUI(); // Check premium status
            
            // Update home button state
            if (homeButton != null) {
                homeButton.setTextColor(Color.parseColor("#4CAF50"));
            }
            if (moreButton != null) {
                moreButton.setTextColor(Color.parseColor("#757575"));
            }
            
            errorLogger.logInfo(TAG, "MainActivity resumed successfully");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error in onResume", e);
        }
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