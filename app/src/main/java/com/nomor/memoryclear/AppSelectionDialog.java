package com.nomor.memoryclear;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AppSelectionDialog extends Dialog {
    
    private List<AppInfo> appList;
    private List<AppInfo> filteredList;
    private AppListAdapter adapter;
    private OnAppSelectedListener listener;
    
    private EditText searchEdit;
    private RecyclerView recyclerView;
    private ImageButton closeButton;
    private TextView titleText;
    
    public interface OnAppSelectedListener {
        void onAppSelected(AppInfo appInfo);
    }
    
    public AppSelectionDialog(@NonNull Context context, List<AppInfo> appList) {
        super(context);
        this.appList = new ArrayList<>(appList);
        this.filteredList = new ArrayList<>(appList);
    }
    
    public void setOnAppSelectedListener(OnAppSelectedListener listener) {
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_app_selection);
        
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();
    }
    
    private void initializeViews() {
        searchEdit = findViewById(R.id.search_edit);
        recyclerView = findViewById(R.id.recycler_view);
        closeButton = findViewById(R.id.btn_close);
        titleText = findViewById(R.id.title_text);
        
        titleText.setText("Select App to Whitelist");
    }
    
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppListAdapter(getContext(), AppListAdapter.MODE_WHITELIST);
        adapter.setOnItemClickListener(new AppListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AppInfo appInfo) {
                if (listener != null) {
                    listener.onAppSelected(appInfo);
                }
                dismiss();
            }
            
            @Override
            public void onItemLongClick(AppInfo appInfo) {
                // Not used in selection dialog
            }
        });
        recyclerView.setAdapter(adapter);
        adapter.updateAppList(filteredList);
    }
    
    private void setupClickListeners() {
        closeButton.setOnClickListener(v -> dismiss());
    }
    
    private void setupSearch() {
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void filterApps(String query) {
        filteredList.clear();
        
        if (query.isEmpty()) {
            filteredList.addAll(appList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (AppInfo app : appList) {
                if (app.appName.toLowerCase().contains(lowerQuery) ||
                    app.packageName.toLowerCase().contains(lowerQuery)) {
                    filteredList.add(app);
                }
            }
        }
        
        adapter.updateAppList(filteredList);
    }
}