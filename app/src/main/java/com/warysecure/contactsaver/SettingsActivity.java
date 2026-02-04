package com.warysecure.contactsaver;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.warysecure.contactsaver.api.ApiClient;
import com.warysecure.contactsaver.models.ApiResponse;
import com.warysecure.contactsaver.models.ContactStats;
import com.warysecure.contactsaver.models.ServerContact;
import com.warysecure.contactsaver.models.SyncResult;
import com.warysecure.contactsaver.utils.ServerContactSaver;
import com.warysecure.contactsaver.workers.ContactSyncWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SettingsActivity extends Activity {
    private static final String PREFS_NAME = "settings";
    private static final String WORK_NAME = "contact_sync_work";

    private EditText etServerUrl;
    private EditText etApiKey;
    private CheckBox cbAutoSync;
    private CheckBox cbSyncOnStart;
    private Spinner spinnerSyncInterval;
    private TextView tvLastSync;
    private TextView tvPendingCount;
    private TextView tvSyncedCount;
    private TextView tvFailedCount;
    private Button btnTestConnection;
    private Button btnSyncNow;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupSpinner();
        loadSettings();
        setupClickListeners();
        updateSyncStats();
    }

    private void initViews() {
        etServerUrl = findViewById(R.id.etServerUrl);
        etApiKey = findViewById(R.id.etApiKey);
        cbAutoSync = findViewById(R.id.cbAutoSync);
        cbSyncOnStart = findViewById(R.id.cbSyncOnStart);
        spinnerSyncInterval = findViewById(R.id.spinnerSyncInterval);
        tvLastSync = findViewById(R.id.tvLastSync);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvSyncedCount = findViewById(R.id.tvSyncedCount);
        tvFailedCount = findViewById(R.id.tvFailedCount);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnSyncNow = findViewById(R.id.btnSyncNow);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupSpinner() {
        String[] intervals = {"5 minutes", "15 minutes", "30 minutes", "60 minutes"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, intervals);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSyncInterval.setAdapter(adapter);
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        etServerUrl.setText(prefs.getString("server_url", "https://joinus.cx"));
        etApiKey.setText(prefs.getString("api_key", ""));
        cbAutoSync.setChecked(prefs.getBoolean("auto_sync_enabled", false));
        cbSyncOnStart.setChecked(prefs.getBoolean("sync_on_start", false));
        
        int syncInterval = prefs.getInt("sync_interval", 15);
        int spinnerPosition = 1; // Default to 15 minutes
        switch (syncInterval) {
            case 5: spinnerPosition = 0; break;
            case 15: spinnerPosition = 1; break;
            case 30: spinnerPosition = 2; break;
            case 60: spinnerPosition = 3; break;
        }
        spinnerSyncInterval.setSelection(spinnerPosition);

        updateLastSyncText();
    }

    private void setupClickListeners() {
        btnTestConnection.setOnClickListener(v -> testConnection());
        btnSyncNow.setOnClickListener(v -> performManualSync());
        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void testConnection() {
        String serverUrl = etServerUrl.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();

        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "Please enter server URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Please enter API key", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save temporarily for API client to use
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString("server_url", serverUrl)
                .putString("api_key", apiKey)
                .apply();

        ProgressDialog dialog = ProgressDialog.show(this, "Testing Connection", "Please wait...", true);

        new Thread(() -> {
            ApiClient apiClient = new ApiClient(this);
            ApiResponse response = apiClient.verify();

            runOnUiThread(() -> {
                dialog.dismiss();
                if (response.success) {
                    Toast.makeText(this, "✓ Connection successful: " + response.message, Toast.LENGTH_LONG).show();
                } else {
                    String error = response.error != null && !response.error.isEmpty() ? response.error : "Connection failed";
                    Toast.makeText(this, "✗ " + error, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void performManualSync() {
        String apiKey = etApiKey.getText().toString().trim();
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Please enter API key first", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = ProgressDialog.show(this, "Syncing", "Fetching contacts from server...", true);

        new Thread(() -> {
            try {
                ApiClient apiClient = new ApiClient(this);
                ServerContactSaver contactSaver = new ServerContactSaver(this);

                List<ServerContact> pendingContacts = apiClient.getPendingContacts();
                
                if (pendingContacts.isEmpty()) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(this, "No pending contacts to sync", Toast.LENGTH_SHORT).show();
                        updateSyncStats();
                    });
                    return;
                }

                List<SyncResult> syncResults = new ArrayList<>();
                int savedCount = 0;
                int failedCount = 0;

                for (ServerContact contact : pendingContacts) {
                    String deviceContactId = contactSaver.saveContact(contact);
                    
                    if (deviceContactId != null) {
                        syncResults.add(new SyncResult(contact.id, deviceContactId, "synced"));
                        savedCount++;
                    } else {
                        syncResults.add(new SyncResult(contact.id, null, "failed"));
                        failedCount++;
                    }
                }

                if (!syncResults.isEmpty()) {
                    apiClient.bulkSync(syncResults);
                }

                // Update last sync time
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply();

                int finalSavedCount = savedCount;
                int finalFailedCount = failedCount;
                
                runOnUiThread(() -> {
                    dialog.dismiss();
                    String message = finalSavedCount + " contacts saved";
                    if (finalFailedCount > 0) {
                        message += ", " + finalFailedCount + " failed";
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    updateLastSyncText();
                    updateSyncStats();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Sync failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void saveSettings() {
        String serverUrl = etServerUrl.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();

        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "Please enter server URL", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean autoSyncEnabled = cbAutoSync.isChecked();
        boolean syncOnStart = cbSyncOnStart.isChecked();
        
        int syncInterval = 15; // default
        switch (spinnerSyncInterval.getSelectedItemPosition()) {
            case 0: syncInterval = 5; break;
            case 1: syncInterval = 15; break;
            case 2: syncInterval = 30; break;
            case 3: syncInterval = 60; break;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString("server_url", serverUrl)
                .putString("api_key", apiKey)
                .putBoolean("auto_sync_enabled", autoSyncEnabled)
                .putBoolean("sync_on_start", syncOnStart)
                .putInt("sync_interval", syncInterval)
                .apply();

        // Schedule or cancel periodic sync based on settings
        if (autoSyncEnabled && !apiKey.isEmpty()) {
            schedulePeriodicSync(syncInterval);
        } else {
            cancelPeriodicSync();
        }

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void schedulePeriodicSync(int intervalMinutes) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                ContactSyncWorker.class,
                intervalMinutes,
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                syncWorkRequest
        );
    }

    private void cancelPeriodicSync() {
        WorkManager.getInstance(this).cancelUniqueWork(WORK_NAME);
    }

    private void updateLastSyncText() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lastSyncTime = prefs.getLong("last_sync_time", 0);
        
        if (lastSyncTime == 0) {
            tvLastSync.setText("Last sync: Never");
        } else {
            long minutesAgo = (System.currentTimeMillis() - lastSyncTime) / 60000;
            if (minutesAgo < 1) {
                tvLastSync.setText("Last sync: Just now");
            } else if (minutesAgo < 60) {
                tvLastSync.setText("Last sync: " + minutesAgo + " minutes ago");
            } else {
                long hoursAgo = minutesAgo / 60;
                tvLastSync.setText("Last sync: " + hoursAgo + " hours ago");
            }
        }
    }

    private void updateSyncStats() {
        new Thread(() -> {
            try {
                ApiClient apiClient = new ApiClient(this);
                ContactStats stats = apiClient.getContactStats();

                runOnUiThread(() -> {
                    tvPendingCount.setText(String.valueOf(stats.pending));
                    tvSyncedCount.setText(String.valueOf(stats.synced));
                    tvFailedCount.setText(String.valueOf(stats.failed));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
