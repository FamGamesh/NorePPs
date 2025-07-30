package com.nomor.memoryclear;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class WhitelistActivity extends AppCompatActivity {
    
    private static final String TAG = "WhitelistActivity";
    
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private ImageButton backButton;
    private TextView titleText;
    private TextView emptyText;
    
    private AppListAdapter adapter;
    private AppManager appManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist);
        
        appManager = new AppManager(this);
        
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadWhitelistedApps();
    }
    
    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        fabAdd = findViewById(R.id.fab_add);
        backButton = findViewById(R.id.btn_back);
        titleText = findViewById(R.id.title_text);
        emptyText = findViewById(R.id.empty_text);
        
        titleText.setText("Whitelist");
        emptyText.setText("No apps in whitelist.\nTap + to add apps that you don't want to force stop.");
    }
    
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppListAdapter(this, AppListAdapter.MODE_WHITELIST);
        adapter.setOnItemClickListener(new AppListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AppInfo appInfo) {
                // Remove from whitelist
                AppPreferences.removeFromWhitelist(appInfo.packageName);
                Toast.makeText(WhitelistActivity.this, 
                    appInfo.appName + " removed from whitelist", Toast.LENGTH_SHORT).show();
                loadWhitelistedApps();
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
        
        fabAdd.setOnClickListener(v -> {
            showAddToWhitelistDialog();
        });
    }
    
    private void loadWhitelistedApps() {
        new Thread(() -> {
            List<AppInfo> whitelistedApps = appManager.getExcludedRunningApps();
            runOnUiThread(() -> {
                adapter.updateAppList(whitelistedApps);
                
                if (whitelistedApps.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }
    
    private void showAddToWhitelistDialog() {
        new Thread(() -> {
            List<AppInfo> allApps = appManager.getAllInstalledApps();
            
            // Filter out already whitelisted apps
            allApps.removeIf(app -> app.isWhitelisted);
            
            runOnUiThread(() -> {
                if (allApps.isEmpty()) {
                    Toast.makeText(this, "All apps are already whitelisted!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                AppSelectionDialog dialog = new AppSelectionDialog(this, allApps);
                dialog.setOnAppSelectedListener(new AppSelectionDialog.OnAppSelectedListener() {
                    @Override
                    public void onAppSelected(AppInfo appInfo) {
                        AppPreferences.addToWhitelist(appInfo.packageName);
                        Toast.makeText(WhitelistActivity.this, 
                            appInfo.appName + " added to whitelist", Toast.LENGTH_SHORT).show();
                        loadWhitelistedApps();
                    }
                });
                dialog.show();
            });
        }).start();
    }
    
    private void showAppOptions(AppInfo appInfo) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(appInfo.appName)
               .setItems(new String[]{"Remove from Whitelist", "App Info"}, (dialog, which) -> {
                   switch (which) {
                       case 0:
                           AppPreferences.removeFromWhitelist(appInfo.packageName);
                           Toast.makeText(this, appInfo.appName + " removed from whitelist", 
                               Toast.LENGTH_SHORT).show();
                           loadWhitelistedApps();
                           break;
                       case 1:
                           showAppInfoDialog(appInfo);
                           break;
                   }
               })
               .show();
    }
    
    private void showAppInfoDialog(AppInfo appInfo) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("App Information")
               .setMessage("App Name: " + appInfo.appName + "\n" +
                          "Package: " + appInfo.packageName + "\n" +
                          "Status: Whitelisted")
               .setPositiveButton("OK", null)
               .show();
    }
}