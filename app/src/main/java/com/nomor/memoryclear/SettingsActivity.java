package com.nomor.memoryclear;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SettingsActivity extends AppCompatActivity {
    
    private static final String TAG = "SettingsActivity";
    
    private ImageButton backButton;
    private TextView titleText;
    private LinearLayout settingsContainer;
    private LinearLayout moreContainer;
    private CardView brandingLayout;  // Changed from LinearLayout to CardView
    
    private Switch scheduleSwitch;
    private TextView scheduleTimeText;
    private Button scheduleTimeButton;
    private Switch dockSwitch;
    private TextView dockStatusText;
    
    private boolean showMoreSection = false;
    private ErrorLogger errorLogger;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_settings);
            
            // Initialize error logger first
            errorLogger = ErrorLogger.getInstance(this);
            errorLogger.logInfo(TAG, "SettingsActivity onCreate started");
            
            showMoreSection = getIntent().getBooleanExtra("show_more", false);
            
            initializeViews();
            setupClickListeners();
            loadSettings();
            
            if (showMoreSection) {
                showMoreSection();
            } else {
                showSettingsSection();
            }
            
            errorLogger.logInfo(TAG, "SettingsActivity onCreate completed successfully");
            
        } catch (Exception e) {
            if (errorLogger != null) {
                errorLogger.logError(TAG, "Critical error in SettingsActivity onCreate", e);
            }
            
            try {
                Toast.makeText(this, "Error initializing settings. Check error logs.", Toast.LENGTH_LONG).show();
            } catch (Exception toastError) {
                android.util.Log.e(TAG, "Failed to show error toast", toastError);
            }
            
            // Try to finish gracefully
            try {
                finish();
            } catch (Exception finishError) {
                android.util.Log.e(TAG, "Failed to finish activity", finishError);
            }
        }
    }
    
    private void initializeViews() {
        try {
            backButton = findViewById(R.id.btn_back);
            titleText = findViewById(R.id.title_text);
            settingsContainer = findViewById(R.id.settings_container);
            moreContainer = findViewById(R.id.more_container);
            brandingLayout = findViewById(R.id.branding_layout);
            
            scheduleSwitch = findViewById(R.id.switch_schedule);
            scheduleTimeText = findViewById(R.id.schedule_time_text);
            scheduleTimeButton = findViewById(R.id.btn_schedule_time);
            dockSwitch = findViewById(R.id.switch_dock);
            dockStatusText = findViewById(R.id.dock_status_text);
            
            // Null checks for critical views
            if (backButton == null) {
                errorLogger.logWarning(TAG, "Back button not found");
            }
            if (titleText == null) {
                errorLogger.logWarning(TAG, "Title text not found");
            }
            if (settingsContainer == null) {
                errorLogger.logWarning(TAG, "Settings container not found");
            }
            if (moreContainer == null) {
                errorLogger.logWarning(TAG, "More container not found");
            }
            if (scheduleSwitch == null) {
                errorLogger.logWarning(TAG, "Schedule switch not found");
            }
            if (dockSwitch == null) {
                errorLogger.logWarning(TAG, "Dock switch not found");
            }
            
            // Setup branding safely
            if (brandingLayout != null) {
                TextView brandingText = brandingLayout.findViewById(R.id.branding_text);
                if (brandingText != null) {
                    brandingText.setText("Made By HEMANT SINGH");
                    brandingText.setTextColor(Color.parseColor("#FFD700"));
                    brandingText.setTextSize(16);
                    brandingText.setTypeface(brandingText.getTypeface(), android.graphics.Typeface.BOLD);
                } else {
                    errorLogger.logWarning(TAG, "Branding text not found within branding layout");
                }
            } else {
                errorLogger.logWarning(TAG, "Branding layout not found");
            }
            
            errorLogger.logInfo(TAG, "Views initialized successfully");
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error initializing views", e);
        }
    }
    
    private void setupClickListeners() {
        try {
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    try {
                        finish();
                        errorLogger.logInfo(TAG, "Settings activity finished by back button");
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error finishing activity", e);
                    }
                });
            }
            
            if (scheduleSwitch != null) {
                scheduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    try {
                        AppPreferences.setScheduleEnabled(isChecked);
                        updateScheduleUI();
                        
                        if (isChecked) {
                            requestSchedulePermissions();
                        } else {
                            stopScheduleService();
                        }
                        errorLogger.logInfo(TAG, "Schedule switch toggled: " + isChecked);
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error handling schedule switch", e);
                        Toast.makeText(this, "Error updating schedule setting", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (scheduleTimeButton != null) {
                scheduleTimeButton.setOnClickListener(v -> {
                    try {
                        showTimePickerDialog();
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error showing time picker", e);
                        Toast.makeText(this, "Error opening time picker", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (dockSwitch != null) {
                dockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    try {
                        AppPreferences.setDockEnabled(isChecked);
                        updateDockUI();
                        
                        if (isChecked) {
                            requestDockPermissions();
                        } else {
                            stopDockService();
                        }
                        errorLogger.logInfo(TAG, "Dock switch toggled: " + isChecked);
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error handling dock switch", e);
                        Toast.makeText(this, "Error updating dock setting", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            errorLogger.logInfo(TAG, "Click listeners setup completed");
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error setting up click listeners", e);
        }
    }
    
    private void showSettingsSection() {
        try {
            if (titleText != null) {
                titleText.setText("Settings");
            }
            if (settingsContainer != null) {
                settingsContainer.setVisibility(View.VISIBLE);
            }
            if (moreContainer != null) {
                moreContainer.setVisibility(View.GONE);
            }
            errorLogger.logInfo(TAG, "Settings section displayed");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing settings section", e);
        }
    }
    
    private void showMoreSection() {
        try {
            if (titleText != null) {
                titleText.setText("More");
            }
            if (settingsContainer != null) {
                settingsContainer.setVisibility(View.GONE);
            }
            if (moreContainer != null) {
                moreContainer.setVisibility(View.VISIBLE);
            }
            setupMoreSection();
            errorLogger.logInfo(TAG, "More section displayed");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing more section", e);
        }
    }
    
    private void setupMoreSection() {
        try {
            // Add Schedule Force Closing card
            addMoreCard("Schedule Force Closing", 
                "Automatically force close apps at scheduled time, even when device is in rest mode with screen off.",
                v -> {
                    try {
                        // Switch to settings section and enable schedule
                        showMoreSection = false;
                        showSettingsSection();
                        if (scheduleSwitch != null) {
                            scheduleSwitch.setChecked(true);
                        }
                        errorLogger.logInfo(TAG, "Switched to schedule settings from more section");
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error switching to schedule settings", e);
                    }
                });
            
            // Add Dock card  
            addMoreCard("Dock",
                "This is a floating dock that will resolve automatic disabling of accessibility service by the device.",
                v -> {
                    try {
                        showDockInfoDialog();
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error showing dock info dialog", e);
                    }
                });
                
            errorLogger.logInfo(TAG, "More section setup completed");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error setting up more section", e);
        }
    }
    
    private void addMoreCard(String title, String description, View.OnClickListener clickListener) {
        try {
            CardView card = new CardView(this);
            card.setCardElevation(4);
            card.setRadius(8);
            card.setUseCompatPadding(true);
            card.setClickable(true);
            
            // Use a safe background instead of getDrawable which might crash
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    card.setForeground(getDrawable(android.R.drawable.list_selector_background));
                }
            } catch (Exception e) {
                errorLogger.logWarning(TAG, "Could not set card foreground: " + e.getMessage());
            }
            
            LinearLayout cardLayout = new LinearLayout(this);
            cardLayout.setOrientation(LinearLayout.VERTICAL);
            cardLayout.setPadding(24, 24, 24, 24);
            
            TextView titleView = new TextView(this);
            titleView.setText(title);
            titleView.setTextSize(18);
            titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
            titleView.setTextColor(Color.parseColor("#2C2C2C"));
            
            TextView descriptionView = new TextView(this);
            descriptionView.setText(description);
            descriptionView.setTextSize(14);
            descriptionView.setTextColor(Color.parseColor("#666666"));
            descriptionView.setPadding(0, 8, 0, 0);
            
            cardLayout.addView(titleView);
            cardLayout.addView(descriptionView);
            card.addView(cardLayout);
            
            card.setOnClickListener(clickListener);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            
            if (moreContainer != null) {
                moreContainer.addView(card, params);
                errorLogger.logInfo(TAG, "Added more card: " + title);
            } else {
                errorLogger.logWarning(TAG, "More container is null, cannot add card: " + title);
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error adding more card: " + title, e);
        }
    }
    
    private void loadSettings() {
        try {
            if (scheduleSwitch != null) {
                scheduleSwitch.setChecked(AppPreferences.isScheduleEnabled());
            } else {
                errorLogger.logWarning(TAG, "Schedule switch is null in loadSettings");
            }
            
            if (dockSwitch != null) {
                dockSwitch.setChecked(AppPreferences.isDockEnabled());
            } else {
                errorLogger.logWarning(TAG, "Dock switch is null in loadSettings");
            }
            
            updateScheduleUI();
            updateDockUI();
            
            errorLogger.logInfo(TAG, "Settings loaded successfully");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error loading settings", e);
        }
    }
    
    private void updateScheduleUI() {
        try {
            boolean isEnabled = AppPreferences.isScheduleEnabled();
            
            if (scheduleTimeText != null) {
                scheduleTimeText.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
            } else {
                errorLogger.logWarning(TAG, "Schedule time text is null in updateScheduleUI");
            }
            
            if (scheduleTimeButton != null) {
                scheduleTimeButton.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
            } else {
                errorLogger.logWarning(TAG, "Schedule time button is null in updateScheduleUI");
            }
            
            if (isEnabled && scheduleTimeText != null) {
                String time = AppPreferences.getScheduleTime();
                scheduleTimeText.setText("Scheduled time: " + time);
            }
            
            errorLogger.logInfo(TAG, "Schedule UI updated successfully");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error updating schedule UI", e);
        }
    }
    
    private void updateDockUI() {
        try {
            boolean isEnabled = AppPreferences.isDockEnabled();
            
            if (dockStatusText != null) {
                if (isEnabled) {
                    if (PermissionHelper.hasOverlayPermission(this)) {
                        dockStatusText.setText("Floating dock is enabled and ready");
                        dockStatusText.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        dockStatusText.setText("Display over other apps permission required");
                        dockStatusText.setTextColor(Color.parseColor("#FF5722"));
                    }
                } else {
                    dockStatusText.setText("Floating dock is disabled");
                    dockStatusText.setTextColor(Color.parseColor("#666666"));
                }
            } else {
                errorLogger.logWarning(TAG, "Dock status text is null in updateDockUI");
            }
            
            errorLogger.logInfo(TAG, "Dock UI updated successfully");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error updating dock UI", e);
        }
    }
    
    private void showTimePickerDialog() {
        try {
            String currentTime = AppPreferences.getScheduleTime();
            String[] timeParts = currentTime.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            
            TimePickerDialog dialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    try {
                        String formattedTime = String.format("%02d:%02d", hourOfDay, minute);
                        AppPreferences.setScheduleTime(formattedTime);
                        updateScheduleUI();
                        
                        // Restart schedule service with new time
                        if (AppPreferences.isScheduleEnabled()) {
                            startScheduleService();
                        }
                        
                        Toast.makeText(SettingsActivity.this, 
                            "Schedule time updated to " + formattedTime, Toast.LENGTH_SHORT).show();
                        
                        errorLogger.logInfo(TAG, "Schedule time updated to " + formattedTime);
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error setting schedule time", e);
                        Toast.makeText(SettingsActivity.this, "Error updating schedule time", Toast.LENGTH_SHORT).show();
                    }
                }
            }, hour, minute, true);
            
            dialog.setTitle("Select Schedule Time");
            dialog.show();
            
            errorLogger.logInfo(TAG, "Time picker dialog shown");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing time picker dialog", e);
            Toast.makeText(this, "Error opening time picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void requestSchedulePermissions() {
        try {
            if (!PermissionHelper.hasAccessibilityPermission(this)) {
                showPermissionDialog("Accessibility Permission Required",
                    "Schedule force closing needs Accessibility permission to automatically force stop apps in background.",
                    () -> PermissionHelper.requestAccessibilityPermission(this));
            } else {
                startScheduleService();
            }
            errorLogger.logInfo(TAG, "Schedule permission check completed");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error requesting schedule permissions", e);
        }
    }
    
    private void requestDockPermissions() {
        try {
            if (!PermissionHelper.hasOverlayPermission(this)) {
                showPermissionDialog("Display Over Other Apps Permission Required",
                    "Floating dock needs permission to display over other apps to work properly.",
                    () -> PermissionHelper.requestOverlayPermission(this));
            } else {
                startDockService();
            }
            errorLogger.logInfo(TAG, "Dock permission check completed");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error requesting dock permissions", e);
        }
    }
    
    private void startScheduleService() {
        try {
            Intent intent = new Intent(this, ScheduleService.class);
            intent.setAction("START_SCHEDULE");
            startService(intent);
            Toast.makeText(this, "Schedule service started", Toast.LENGTH_SHORT).show();
            errorLogger.logInfo(TAG, "Schedule service started successfully");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error starting schedule service", e);
            Toast.makeText(this, "Error starting schedule service", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopScheduleService() {
        try {
            Intent intent = new Intent(this, ScheduleService.class);
            intent.setAction("STOP_SCHEDULE");
            startService(intent);
            Toast.makeText(this, "Schedule service stopped", Toast.LENGTH_SHORT).show();
            errorLogger.logInfo(TAG, "Schedule service stopped successfully");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error stopping schedule service", e);
            Toast.makeText(this, "Error stopping schedule service", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startDockService() {
        try {
            Intent intent = new Intent(this, FloatingDockService.class);
            intent.setAction("START_DOCK");
            startService(intent);
            Toast.makeText(this, "Floating dock started", Toast.LENGTH_SHORT).show();
            errorLogger.logInfo(TAG, "Floating dock service started successfully");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error starting dock service", e);
            Toast.makeText(this, "Error starting floating dock", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopDockService() {
        try {
            Intent intent = new Intent(this, FloatingDockService.class);
            intent.setAction("STOP_DOCK");
            startService(intent);
            Toast.makeText(this, "Floating dock stopped", Toast.LENGTH_SHORT).show();
            errorLogger.logInfo(TAG, "Floating dock service stopped successfully");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error stopping dock service", e);
            Toast.makeText(this, "Error stopping floating dock", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showDockInfoDialog() {
        try {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Floating Dock")
                   .setMessage("This is a floating dock that will resolve automatic disabling of accessibility service by the device.")
                   .setPositiveButton("Enable Floating Dock", (dialog, which) -> {
                       try {
                           if (dockSwitch != null) {
                               dockSwitch.setChecked(true);
                           }
                           errorLogger.logInfo(TAG, "Dock enabled from info dialog");
                       } catch (Exception e) {
                           errorLogger.logError(TAG, "Error enabling dock from dialog", e);
                       }
                   })
                   .setNegativeButton("Cancel", null)
                   .show();
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing dock info dialog", e);
        }
    }
    
    private void showPermissionDialog(String title, String message, Runnable onPositive) {
        try {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle(title)
                   .setMessage(message)
                   .setPositiveButton("Grant Permission", (dialog, which) -> {
                       try {
                           onPositive.run();
                       } catch (Exception e) {
                           errorLogger.logError(TAG, "Error running permission callback", e);
                       }
                   })
                   .setNegativeButton("Cancel", null)
                   .show();
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing permission dialog", e);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        try {
            updateScheduleUI();
            updateDockUI();
            errorLogger.logInfo(TAG, "SettingsActivity resumed successfully");
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error in onResume", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            errorLogger.logInfo(TAG, "SettingsActivity destroyed successfully");
        } catch (Exception e) {
            if (errorLogger != null) {
                errorLogger.logError(TAG, "Error in onDestroy", e);
            }
        }
    }
}