package com.nomor.memoryclear;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {
    
    public static final int MODE_WHITELIST = 1;
    public static final int MODE_RUNNING = 2;
    public static final int MODE_ANALYZE = 3;
    
    private Context context;
    private List<AppInfo> appList;
    private int mode;
    private OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onItemClick(AppInfo appInfo);
        void onItemLongClick(AppInfo appInfo);
    }
    
    public AppListAdapter(Context context, int mode) {
        this.context = context;
        this.mode = mode;
        this.appList = new ArrayList<>();
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void updateAppList(List<AppInfo> newAppList) {
        this.appList.clear();
        this.appList.addAll(newAppList);
        notifyDataSetChanged();
    }
    
    public List<AppInfo> getAllApps() {
        return new ArrayList<>(appList);
    }
    
    public List<AppInfo> getSelectedApps() {
        List<AppInfo> selectedApps = new ArrayList<>();
        for (AppInfo app : appList) {
            if (app.isSelected) {
                selectedApps.add(app);
            }
        }
        return selectedApps;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo appInfo = appList.get(position);
        
        holder.appIcon.setImageDrawable(appInfo.icon);
        holder.appName.setText(appInfo.appName);
        holder.packageName.setText(appInfo.packageName);
        
        // Configure based on mode
        switch (mode) {
            case MODE_WHITELIST:
                holder.checkBox.setVisibility(View.GONE);
                holder.statusIndicator.setVisibility(View.VISIBLE);
                holder.statusIndicator.setText("WHITELISTED");
                holder.statusIndicator.setTextColor(Color.parseColor("#4CAF50"));
                break;
                
            case MODE_RUNNING:
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(appInfo.isSelected);
                holder.statusIndicator.setVisibility(View.VISIBLE);
                holder.statusIndicator.setText("RUNNING");
                holder.statusIndicator.setTextColor(Color.parseColor("#FF5722"));
                break;
                
            case MODE_ANALYZE:
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(appInfo.isSelected);
                holder.statusIndicator.setVisibility(View.VISIBLE);
                if (appInfo.isSelected) {
                    holder.statusIndicator.setText("WILL STOP");
                    holder.statusIndicator.setTextColor(Color.parseColor("#F44336"));
                } else {
                    holder.statusIndicator.setText("KEEP RUNNING");
                    holder.statusIndicator.setTextColor(Color.parseColor("#4CAF50"));
                }
                break;
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(appInfo);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(appInfo);
            }
            return true;
        });
        
        holder.checkBox.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(appInfo);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return appList.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView packageName;
        TextView statusIndicator;
        CheckBox checkBox;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            packageName = itemView.findViewById(R.id.package_name);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }
}