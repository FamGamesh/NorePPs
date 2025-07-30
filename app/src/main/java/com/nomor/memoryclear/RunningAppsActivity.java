package com.nomor.memoryclear;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RunningAppsActivity extends AppCompatActivity {
    
    private static final String TAG = "RunningAppsActivity";
    
    private RecyclerView recyclerView;
    private Button stopButton;
    private ImageButton backButton;
    private TextView titleText;
    private TextView emptyText;
    private TextView countText;
    
    private AppListAdapter adapter;
    private AppManager appManager;
    private boolean showExcluded = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running_apps);
        
        appManager = new AppManager(this);
        showExcluded = getIntent().getBooleanExtra("show_excluded", false);
        
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadApps();
    }
    
    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        stopButton = findViewById(R.id.btn_stop);
        backButton = findViewById(R.id.btn_back);
        titleText = findViewById(R.id.title_text);
        emptyText = findViewById(R.id.empty_text);
        countText = findViewById(R.id.count_text);
        
        if (showExcluded) {
            titleText.setText("Excluded Running Apps");
            emptyText.setText("No excluded apps.\nAdd apps to whitelist to exclude them from force stopping.");
            stopButton.setVisibility(View.GONE);
        } else {
            titleText.setText("Running Apps");
            emptyText.setText("No running apps detected.\nMake sure you have granted Usage Stats permission.");
            
            // Style the stop button with red background
            stopButton.setBackgroundColor(Color.parseColor("#F44336"));
            stopButton.setTextColor(Color.WHITE);
            stopButton.setText("STOP");
        }
    }
    
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppListAdapter(this, AppListAdapter.MODE_RUNNING);
        adapter.setOnItemClickListener(new AppListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AppInfo appInfo) {
                // Toggle selection for force stopping
                appInfo.isSelected = !appInfo.isSelected;
                adapter.notifyDataSetChanged();
                updateCountText();
            }
            
            @Override
            public void onItemLongClick(AppInfo appInfo) {
                showAppOptions(appInfo);
            }
        });
        recyclerView.setAdapter(adapter);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        stopButton.setOnClickListener(v -> {
            List<AppInfo> selectedApps = adapter.getSelectedApps();
            if (selectedApps.isEmpty()) {
                Toast.makeText(this, "No apps selected for force stopping", Toast.LENGTH_SHORT).show();
                return;
            }
            
            showAnalyzeScreen(selectedApps);
        });
    }
    
    private void loadApps() {
        new Thread(() -> {
            List<AppInfo> apps;
            if (showExcluded) {
                apps = appManager.getExcludedRunningApps();
            } else {
                apps = appManager.getRunningApps();
            }
            
            runOnUiThread(() -> {
                adapter.updateAppList(apps);
                updateCountText();
                
                if (apps.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    countText.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    countText.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }
    
    private void updateCountText() {
        List<AppInfo> allApps = adapter.getAllApps();
        List<AppInfo> selectedApps = adapter.getSelectedApps();
        
        if (showExcluded) {
            countText.setText(allApps.size() + " excluded apps");
        } else {
            countText.setText(selectedApps.size() + " of " + allApps.size() + " apps selected");
        }
    }
    
    private void showAnalyzeScreen(List<AppInfo> selectedApps) {
        Intent intent = new Intent(this, AnalyzeActivity.class);
        intent.putExtra("selected_apps_count", selectedApps.size());
        
        // Pass app package names
        String[] packageNames = new String[selectedApps.size()];
        for (int i = 0; i < selectedApps.size(); i++) {
            packageNames[i] = selectedApps.get(i).packageName;
        }
        intent.putExtra("selected_packages", packageNames);
        
        startActivity(intent);
    }
    
    private void showAppOptions(AppInfo appInfo) {
        String[] options;
        if (showExcluded) {
            options = new String[]{"Remove from Whitelist", "App Info"};
        } else {
            if (appInfo.isWhitelisted) {
                options = new String[]{"Remove from Whitelist", "App Info"};
            } else {
                options = new String[]{"Add to Whitelist", "App Info"};
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(appInfo.appName)
               .setItems(options, (dialog, which) -> {
                   switch (which) {
                       case 0:
                           if (appInfo.isWhitelisted || showExcluded) {
                               AppPreferences.removeFromWhitelist(appInfo.packageName);
                               Toast.makeText(this, appInfo.appName + " removed from whitelist", 
                                   Toast.LENGTH_SHORT).show();
                           } else {
                               AppPreferences.addToWhitelist(appInfo.packageName);
                               Toast.makeText(this, appInfo.appName + " added to whitelist", 
                                   Toast.LENGTH_SHORT).show();
                           }
                           loadApps();
                           break;
                       case 1:
                           showAppInfoDialog(appInfo);
                           break;
                   }
               })
               .show();
    }
    
    private void showAppInfoDialog(AppInfo appInfo) {
        String status = appInfo.isWhitelisted ? "Whitelisted" : "Not Whitelisted";
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("App Information")
               .setMessage("App Name: " + appInfo.appName + "\n" +
                          "Package: " + appInfo.packageName + "\n" +
                          "Status: " + status)
               .setPositiveButton("OK", null)
               .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
    }
}