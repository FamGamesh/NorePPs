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
    private LinearLayout brandingLayout;
    
    private Switch scheduleSwitch;
    private TextView scheduleTimeText;
    private Button scheduleTimeButton;
    private Switch dockSwitch;
    private TextView dockStatusText;
    
    private boolean showMoreSection = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        showMoreSection = getIntent().getBooleanExtra("show_more", false);
        
        initializeViews();
        setupClickListeners();
        loadSettings();
        
        if (showMoreSection) {
            showMoreSection();
        } else {
            showSettingsSection();
        }
    }
    
    private void initializeViews() {
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
        
        // Setup branding safely
        if (brandingLayout != null) {
            TextView brandingText = brandingLayout.findViewById(R.id.branding_text);
            if (brandingText != null) {
                brandingText.setText("Made By HEMANT SINGH");
                brandingText.setTextColor(Color.parseColor("#FFD700"));
                brandingText.setTextSize(16);
                brandingText.setTypeface(brandingText.getTypeface(), android.graphics.Typeface.BOLD);
            }
        }
    }
    
    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
        
        if (scheduleSwitch != null) {
            scheduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                AppPreferences.setScheduleEnabled(isChecked);
                updateScheduleUI();
                
                if (isChecked) {
                    requestSchedulePermissions();
                } else {
                    stopScheduleService();
                }
            });
        }
        
        if (scheduleTimeButton != null) {
            scheduleTimeButton.setOnClickListener(v -> showTimePickerDialog());
        }
        
        if (dockSwitch != null) {
            dockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                AppPreferences.setDockEnabled(isChecked);
                updateDockUI();
                
                if (isChecked) {
                    requestDockPermissions();
                } else {
                    stopDockService();
                }
            });
        }
    }
    
    private void showSettingsSection() {
        titleText.setText("Settings");
        settingsContainer.setVisibility(View.VISIBLE);
        moreContainer.setVisibility(View.GONE);
    }
    
    private void showMoreSection() {
        titleText.setText("More");
        settingsContainer.setVisibility(View.GONE);
        moreContainer.setVisibility(View.VISIBLE);
        setupMoreSection();
    }
    
    private void setupMoreSection() {
        // Add Schedule Force Closing card
        addMoreCard("Schedule Force Closing", 
            "Automatically force close apps at scheduled time, even when device is in rest mode with screen off.",
            v -> {
                // Switch to settings section and enable schedule
                showMoreSection = false;
                showSettingsSection();
                scheduleSwitch.setChecked(true);
            });
        
        // Add Dock card  
        addMoreCard("Dock",
            "This is a floating dock that will resolve automatic disabling of accessibility service by the device.",
            v -> showDockInfoDialog());
    }
    
    private void addMoreCard(String title, String description, View.OnClickListener clickListener) {
        CardView card = new CardView(this);
        card.setCardElevation(4);
        card.setRadius(8);
        card.setUseCompatPadding(true);
        card.setClickable(true);
        
        // Use a safe background instead of getDrawable which might crash
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            card.setForeground(getDrawable(android.R.drawable.list_selector_background));
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
        }
    }
    
    private void loadSettings() {
        scheduleSwitch.setChecked(AppPreferences.isScheduleEnabled());
        dockSwitch.setChecked(AppPreferences.isDockEnabled());
        
        updateScheduleUI();
        updateDockUI();
    }
    
    private void updateScheduleUI() {
        boolean isEnabled = AppPreferences.isScheduleEnabled();
        scheduleTimeText.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        scheduleTimeButton.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        
        if (isEnabled) {
            String time = AppPreferences.getScheduleTime();
            scheduleTimeText.setText("Scheduled time: " + time);
        }
    }
    
    private void updateDockUI() {
        boolean isEnabled = AppPreferences.isDockEnabled();
        
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
    }
    
    private void showTimePickerDialog() {
        String currentTime = AppPreferences.getScheduleTime();
        String[] timeParts = currentTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        TimePickerDialog dialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                String formattedTime = String.format("%02d:%02d", hourOfDay, minute);
                AppPreferences.setScheduleTime(formattedTime);
                updateScheduleUI();
                
                // Restart schedule service with new time
                if (AppPreferences.isScheduleEnabled()) {
                    startScheduleService();
                }
                
                Toast.makeText(SettingsActivity.this, 
                    "Schedule time updated to " + formattedTime, Toast.LENGTH_SHORT).show();
            }
        }, hour, minute, true);
        
        dialog.setTitle("Select Schedule Time");
        dialog.show();
    }
    
    private void requestSchedulePermissions() {
        if (!PermissionHelper.hasAccessibilityPermission(this)) {
            showPermissionDialog("Accessibility Permission Required",
                "Schedule force closing needs Accessibility permission to automatically force stop apps in background.",
                () -> PermissionHelper.requestAccessibilityPermission(this));
        } else {
            startScheduleService();
        }
    }
    
    private void requestDockPermissions() {
        if (!PermissionHelper.hasOverlayPermission(this)) {
            showPermissionDialog("Display Over Other Apps Permission Required",
                "Floating dock needs permission to display over other apps to work properly.",
                () -> PermissionHelper.requestOverlayPermission(this));
        } else {
            startDockService();
        }
    }
    
    private void startScheduleService() {
        Intent intent = new Intent(this, ScheduleService.class);
        intent.setAction("START_SCHEDULE");
        startService(intent);
        Toast.makeText(this, "Schedule service started", Toast.LENGTH_SHORT).show();
    }
    
    private void stopScheduleService() {
        Intent intent = new Intent(this, ScheduleService.class);
        intent.setAction("STOP_SCHEDULE");
        startService(intent);
        Toast.makeText(this, "Schedule service stopped", Toast.LENGTH_SHORT).show();
    }
    
    private void startDockService() {
        Intent intent = new Intent(this, FloatingDockService.class);
        intent.setAction("START_DOCK");
        startService(intent);
        Toast.makeText(this, "Floating dock started", Toast.LENGTH_SHORT).show();
    }
    
    private void stopDockService() {
        Intent intent = new Intent(this, FloatingDockService.class);
        intent.setAction("STOP_DOCK");
        startService(intent);
        Toast.makeText(this, "Floating dock stopped", Toast.LENGTH_SHORT).show();
    }
    
    private void showDockInfoDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Floating Dock")
               .setMessage("This is a floating dock that will resolve automatic disabling of accessibility service by the device.")
               .setPositiveButton("Enable Floating Dock", (dialog, which) -> {
                   dockSwitch.setChecked(true);
               })
               .setNegativeButton("Cancel", null)
               .show();
    }
    
    private void showPermissionDialog(String title, String message, Runnable onPositive) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(title)
               .setMessage(message)
               .setPositiveButton("Grant Permission", (dialog, which) -> onPositive.run())
               .setNegativeButton("Cancel", null)
               .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateScheduleUI();
        updateDockUI();
    }
}