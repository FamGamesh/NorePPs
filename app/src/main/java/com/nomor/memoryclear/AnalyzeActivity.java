package com.nomor.memoryclear;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AnalyzeActivity extends AppCompatActivity {
    
    private static final String TAG = "AnalyzeActivity";
    
    private RecyclerView recyclerView;
    private Button startButton;
    private ImageButton backButton;
    private TextView titleText;
    private TextView statusText;
    private ProgressBar progressBar;
    
    private AppListAdapter adapter;
    private AppManager appManager;
    private String[] selectedPackages;
    private int totalApps;
    private int processedApps;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze);
        
        appManager = new AppManager(this);
        mainHandler = new Handler(Looper.getMainLooper());
        
        selectedPackages = getIntent().getStringArrayExtra("selected_packages");
        totalApps = getIntent().getIntExtra("selected_apps_count", 0);
        
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadSelectedApps();
    }
    
    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        startButton = findViewById(R.id.btn_start);
        backButton = findViewById(R.id.btn_back);
        titleText = findViewById(R.id.title_text);
        statusText = findViewById(R.id.status_text);
        progressBar = findViewById(R.id.progress_bar);
        
        titleText.setText("Analyze Apps");
        statusText.setText("Review apps before force stopping. Uncheck any app you want to keep running.");
        
        // Style start button
        startButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        startButton.setTextColor(Color.WHITE);
        startButton.setText("START FORCE STOPPING");
    }
    
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppListAdapter(this, AppListAdapter.MODE_ANALYZE);
        adapter.setOnItemClickListener(new AppListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AppInfo appInfo) {
                // Toggle selection
                appInfo.isSelected = !appInfo.isSelected;
                adapter.notifyDataSetChanged();
                updateStatusText();
            }
            
            @Override
            public void onItemLongClick(AppInfo appInfo) {
                showAppInfoDialog(appInfo);
            }
        });
        recyclerView.setAdapter(adapter);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        startButton.setOnClickListener(v -> {
            if (!PermissionHelper.hasAccessibilityPermission(this)) {
                showAccessibilityPermissionDialog();
                return;
            }
            
            List<AppInfo> selectedApps = adapter.getSelectedApps();
            if (selectedApps.isEmpty()) {
                Toast.makeText(this, "No apps selected for force stopping", Toast.LENGTH_SHORT).show();
                return;
            }
            
            startForceStoppingProcess(selectedApps);
        });
    }
    
    private void loadSelectedApps() {
        if (selectedPackages == null || selectedPackages.length == 0) {
            Toast.makeText(this, "No apps to analyze", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        new Thread(() -> {
            List<AppInfo> selectedApps = new ArrayList<>();
            
            for (String packageName : selectedPackages) {
                try {
                    android.content.pm.ApplicationInfo appInfo = getPackageManager().getApplicationInfo(packageName, 0);
                    String appName = getPackageManager().getApplicationLabel(appInfo).toString();
                    
                    AppInfo app = new AppInfo(
                        packageName,
                        appName,
                        getPackageManager().getApplicationIcon(appInfo)
                    );
                    app.isSelected = true; // Default selected
                    selectedApps.add(app);
                } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                    android.util.Log.w(TAG, "App not found: " + packageName);
                }
            }
            
            runOnUiThread(() -> {
                adapter.updateAppList(selectedApps);
                updateStatusText();
            });
        }).start();
    }
    
    private void updateStatusText() {
        List<AppInfo> selectedApps = adapter.getSelectedApps();
        List<AppInfo> allApps = adapter.getAllApps();
        
        statusText.setText(selectedApps.size() + " of " + allApps.size() + 
            " apps will be force stopped. Tap to toggle selection.");
    }
    
    private void startForceStoppingProcess(List<AppInfo> selectedApps) {
        // Hide start button and show progress
        startButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        
        processedApps = 0;
        totalApps = selectedApps.size();
        
        statusText.setText("Starting force stop process...");
        
        // Start accessibility service force stopping
        Intent serviceIntent = new Intent(this, ForceStopAccessibilityService.class);
        serviceIntent.putExtra("action", "force_stop_apps");
        
        String[] packageNames = new String[selectedApps.size()];
        for (int i = 0; i < selectedApps.size(); i++) {
            packageNames[i] = selectedApps.get(i).packageName;
        }
        serviceIntent.putExtra("packages", packageNames);
        
        startService(serviceIntent);
        
        // Simulate the force stopping process with live updates
        simulateForceStoppingProcess(selectedApps);
    }
    
    private void simulateForceStoppingProcess(List<AppInfo> selectedApps) {
        new Thread(() -> {
            for (AppInfo app : selectedApps) {
                try {
                    Thread.sleep(2000); // Simulate time for each app
                    
                    processedApps++;
                    final int progress = (processedApps * 100) / totalApps;
                    
                    mainHandler.post(() -> {
                        progressBar.setProgress(progress);
                        statusText.setText("Force stopping " + app.appName + "... (" + 
                            processedApps + "/" + totalApps + ")");
                    });
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            // Process completed
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                statusText.setText("Force stopping completed! " + processedApps + " apps stopped.");
                
                // Show completion dialog
                showCompletionDialog();
            });
            
        }).start();
    }
    
    private void showCompletionDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Force Stop Completed")
               .setMessage("Successfully force stopped " + processedApps + " apps.\n\n" +
                          "Your device should now have more available memory.")
               .setPositiveButton("Back to Home", (dialog, which) -> {
                   Intent intent = new Intent(this, MainActivity.class);
                   intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                   startActivity(intent);
                   finish();
               })
               .setCancelable(false)
               .show();
    }
    
    private void showAccessibilityPermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Accessibility Permission Required")
               .setMessage("No More Apps PRO needs Accessibility permission to automatically force stop apps.\n\n" +
                          "Please enable 'No More Apps PRO' in Accessibility settings.")
               .setPositiveButton("Open Settings", (dialog, which) -> {
                   PermissionHelper.requestAccessibilityPermission(this);
               })
               .setNegativeButton("Cancel", null)
               .show();
    }
    
    private void showAppInfoDialog(AppInfo appInfo) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("App Information")
               .setMessage("App Name: " + appInfo.appName + "\n" +
                          "Package: " + appInfo.packageName + "\n" +
                          "Selected: " + (appInfo.isSelected ? "Yes" : "No"))
               .setPositiveButton("OK", null)
               .show();
    }
}