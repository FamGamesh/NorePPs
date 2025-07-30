package com.nomor.memoryclear;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONObject;

import java.util.List;

public class ErrorLogActivity extends AppCompatActivity {
    
    private static final String TAG = "ErrorLogActivity";
    
    private ImageButton backButton;
    private TextView titleText;
    private TextView logCountText;
    private Button copyAllButton;
    private Button clearLogsButton;
    private ScrollView scrollView;
    private LinearLayout logsContainer;
    private TextView noLogsText;
    
    private ErrorLogger errorLogger;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_log);
        
        errorLogger = ErrorLogger.getInstance(this);
        
        initializeViews();
        setupClickListeners();
        loadErrorLogs();
        
        // Log that error log activity was opened
        errorLogger.logInfo(TAG, "Error log activity opened");
    }
    
    private void initializeViews() {
        try {
            backButton = findViewById(R.id.btn_back);
            titleText = findViewById(R.id.title_text);
            logCountText = findViewById(R.id.log_count_text);
            copyAllButton = findViewById(R.id.btn_copy_all);
            clearLogsButton = findViewById(R.id.btn_clear_logs);
            scrollView = findViewById(R.id.scroll_view);
            logsContainer = findViewById(R.id.logs_container);
            noLogsText = findViewById(R.id.no_logs_text);
            
            if (titleText != null) {
                titleText.setText("Error Logs");
            }
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
                    } catch (Exception e) {
                        errorLogger.logError(TAG, "Error finishing activity", e);
                    }
                });
            }
            
            if (copyAllButton != null) {
                copyAllButton.setOnClickListener(v -> copyAllLogs());
            }
            
            if (clearLogsButton != null) {
                clearLogsButton.setOnClickListener(v -> showClearLogsDialog());
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error setting up click listeners", e);
        }
    }
    
    private void loadErrorLogs() {
        try {
            List<JSONObject> logs = errorLogger.getAllLogs();
            
            if (logCountText != null) {
                logCountText.setText("Total Logs: " + logs.size());
            }
            
            if (logs.isEmpty()) {
                showNoLogsMessage();
            } else {
                showLogEntries(logs);
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error loading error logs", e);
            showNoLogsMessage();
        }
    }
    
    private void showNoLogsMessage() {
        try {
            if (scrollView != null) {
                scrollView.setVisibility(View.GONE);
            }
            if (noLogsText != null) {
                noLogsText.setVisibility(View.VISIBLE);
                noLogsText.setText("No error logs found.\nThis is good news! 🎉");
            }
            if (copyAllButton != null) {
                copyAllButton.setEnabled(false);
            }
            if (clearLogsButton != null) {
                clearLogsButton.setEnabled(false);
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing no logs message", e);
        }
    }
    
    private void showLogEntries(List<JSONObject> logs) {
        try {
            if (scrollView != null) {
                scrollView.setVisibility(View.VISIBLE);
            }
            if (noLogsText != null) {
                noLogsText.setVisibility(View.GONE);
            }
            if (copyAllButton != null) {
                copyAllButton.setEnabled(true);
            }
            if (clearLogsButton != null) {
                clearLogsButton.setEnabled(true);
            }
            
            if (logsContainer != null) {
                logsContainer.removeAllViews();
                
                for (int i = 0; i < logs.size(); i++) {
                    JSONObject log = logs.get(i);
                    createLogEntryCard(log, i + 1);
                }
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing log entries", e);
        }
    }
    
    private void createLogEntryCard(JSONObject log, int entryNumber) {
        try {
            CardView card = new CardView(this);
            card.setCardElevation(4);
            card.setRadius(8);
            card.setUseCompatPadding(true);
            
            LinearLayout cardLayout = new LinearLayout(this);
            cardLayout.setOrientation(LinearLayout.VERTICAL);
            cardLayout.setPadding(16, 16, 16, 16);
            
            // Header with entry number and timestamp
            LinearLayout headerLayout = new LinearLayout(this);
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
            headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            
            TextView entryNumberText = new TextView(this);
            entryNumberText.setText("Entry #" + entryNumber);
            entryNumberText.setTextSize(14);
            entryNumberText.setTypeface(entryNumberText.getTypeface(), android.graphics.Typeface.BOLD);
            entryNumberText.setTextColor(Color.parseColor("#2C2C2C"));
            
            TextView timestampText = new TextView(this);
            timestampText.setText(log.optString("timestamp", "Unknown time"));
            timestampText.setTextSize(12);
            timestampText.setTextColor(Color.parseColor("#666666"));
            
            LinearLayout.LayoutParams timestampParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            timestampParams.leftMargin = 32;
            
            headerLayout.addView(entryNumberText);
            headerLayout.addView(timestampText, timestampParams);
            
            // Level indicator
            TextView levelText = new TextView(this);
            String level = log.optString("level", "UNKNOWN");
            levelText.setText("Level: " + level);
            levelText.setTextSize(12);
            levelText.setTypeface(levelText.getTypeface(), android.graphics.Typeface.BOLD);
            
            // Set color based on level
            switch (level) {
                case "ERROR":
                    levelText.setTextColor(Color.parseColor("#F44336"));
                    break;
                case "WARNING":
                    levelText.setTextColor(Color.parseColor("#FF9800"));
                    break;
                case "INFO":
                    levelText.setTextColor(Color.parseColor("#4CAF50"));
                    break;
                default:
                    levelText.setTextColor(Color.parseColor("#666666"));
                    break;
            }
            
            // Tag and Message
            TextView tagText = new TextView(this);
            tagText.setText("Tag: " + log.optString("tag", "Unknown"));
            tagText.setTextSize(12);
            tagText.setTextColor(Color.parseColor("#666666"));
            
            TextView messageText = new TextView(this);
            messageText.setText("Message: " + log.optString("message", "No message"));
            messageText.setTextSize(13);
            messageText.setTextColor(Color.parseColor("#2C2C2C"));
            
            // Exception details if available
            TextView exceptionText = null;
            if (log.has("exceptionType")) {
                exceptionText = new TextView(this);
                String exceptionInfo = "Exception: " + log.optString("exceptionType", "Unknown") + 
                        "\n" + log.optString("exceptionMessage", "No details");
                exceptionText.setText(exceptionInfo);
                exceptionText.setTextSize(12);
                exceptionText.setTextColor(Color.parseColor("#F44336"));
                exceptionText.setTypeface(exceptionText.getTypeface(), android.graphics.Typeface.ITALIC);
            }
            
            // Copy button for individual log
            Button copyButton = new Button(this);
            copyButton.setText("Copy This Log");
            copyButton.setTextSize(11);
            copyButton.setPadding(16, 8, 16, 8);
            copyButton.setOnClickListener(v -> copyIndividualLog(log, entryNumber));
            
            // Add all views to card
            cardLayout.addView(headerLayout);
            cardLayout.addView(levelText);
            cardLayout.addView(tagText);
            cardLayout.addView(messageText);
            if (exceptionText != null) {
                cardLayout.addView(exceptionText);
            }
            cardLayout.addView(copyButton);
            
            card.addView(cardLayout);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 8);
            
            if (logsContainer != null) {
                logsContainer.addView(card, params);
            }
            
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error creating log entry card", e);
        }
    }
    
    private void copyAllLogs() {
        try {
            String allLogs = errorLogger.getFormattedLogs();
            copyToClipboard("All Error Logs", allLogs);
            Toast.makeText(this, "All logs copied to clipboard", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error copying all logs", e);
            Toast.makeText(this, "Failed to copy logs", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void copyIndividualLog(JSONObject log, int entryNumber) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Error Log Entry #").append(entryNumber).append(" ===\n");
            sb.append("Timestamp: ").append(log.optString("timestamp", "Unknown")).append("\n");
            sb.append("Level: ").append(log.optString("level", "Unknown")).append("\n");
            sb.append("Tag: ").append(log.optString("tag", "Unknown")).append("\n");
            sb.append("Message: ").append(log.optString("message", "No message")).append("\n");
            
            if (log.has("exceptionType")) {
                sb.append("Exception: ").append(log.optString("exceptionType")).append("\n");
                sb.append("Exception Message: ").append(log.optString("exceptionMessage", "None")).append("\n");
            }
            
            if (log.has("stackTrace")) {
                sb.append("Stack Trace:\n").append(log.optString("stackTrace")).append("\n");
            }
            
            copyToClipboard("Error Log Entry #" + entryNumber, sb.toString());
            Toast.makeText(this, "Log entry copied to clipboard", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error copying individual log", e);
            Toast.makeText(this, "Failed to copy log entry", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void copyToClipboard(String label, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(label, text);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error copying to clipboard", e);
        }
    }
    
    private void showClearLogsDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Clear All Logs")
                   .setMessage("Are you sure you want to clear all error logs? This action cannot be undone.")
                   .setPositiveButton("Clear All", (dialog, which) -> {
                       try {
                           errorLogger.clearLogs();
                           loadErrorLogs(); // Refresh the display
                           Toast.makeText(this, "All logs cleared", Toast.LENGTH_SHORT).show();
                       } catch (Exception e) {
                           errorLogger.logError(TAG, "Error clearing logs", e);
                           Toast.makeText(this, "Failed to clear logs", Toast.LENGTH_SHORT).show();
                       }
                   })
                   .setNegativeButton("Cancel", null)
                   .show();
        } catch (Exception e) {
            errorLogger.logError(TAG, "Error showing clear logs dialog", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        errorLogger.logInfo(TAG, "Error log activity closed");
    }
}