package com.warysecure.contactsaver;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

import com.warysecure.contactsaver.api.ApiClient;
import com.warysecure.contactsaver.models.ContactStats;
import com.warysecure.contactsaver.models.ServerContact;
import com.warysecure.contactsaver.models.SyncResult;
import com.warysecure.contactsaver.utils.ServerContactSaver;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_CONTACTS_PERMISSION = 1001;
    private static final int REQUEST_OVERLAY_PERMISSION = 1002;
    private static final String PREFS_NAME = "settings";

    private TextView statusText;
    private TextView detectedCount;
    private TextView savedCount;
    private TextView unsavedCount;
    private Button btnAccessibility;
    private Button btnOverlay;
    private Button btnContacts;
    private Button btnStartService;
    private Button btnViewDetails;
    private Button btnSettings;
    private Button btnSyncFromServer;
    private TextView tvServerStatus;
    private TextView tvServerUrl;
    private TextView tvLastSync;
    private TextView serverPendingCount;
    private TextView serverSyncedCount;
    private TextView serverFailedCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
        
        // Check if sync on start is enabled
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean("sync_on_start", false)) {
            performServerSync();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        updateServerSection();
    }

    private void initViews() {
        statusText = findViewById(R.id.statusText);
        detectedCount = findViewById(R.id.detectedCount);
        savedCount = findViewById(R.id.savedCount);
        unsavedCount = findViewById(R.id.unsavedCount);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnOverlay = findViewById(R.id.btnOverlay);
        btnContacts = findViewById(R.id.btnContacts);
        btnStartService = findViewById(R.id.btnStartService);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        btnSettings = findViewById(R.id.btnSettings);
        btnSyncFromServer = findViewById(R.id.btnSyncFromServer);
        tvServerStatus = findViewById(R.id.tvServerStatus);
        tvServerUrl = findViewById(R.id.tvServerUrl);
        tvLastSync = findViewById(R.id.tvLastSync);
        serverPendingCount = findViewById(R.id.serverPendingCount);
        serverSyncedCount = findViewById(R.id.serverSyncedCount);
        serverFailedCount = findViewById(R.id.serverFailedCount);
    }

    private void setupClickListeners() {
        btnAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        btnOverlay.setOnClickListener(v -> requestOverlayPermission());
        btnContacts.setOnClickListener(v -> requestContactsPermission());
        btnStartService.setOnClickListener(v -> startFloatingService());
        btnViewDetails.setOnClickListener(v -> openViewNumbers());
        btnSettings.setOnClickListener(v -> openSettings());
        btnSyncFromServer.setOnClickListener(v -> performServerSync());
    }

    private void updateUI() {
        boolean accessibilityEnabled = isAccessibilityServiceEnabled();
        boolean overlayEnabled = Settings.canDrawOverlays(this);
        boolean contactsEnabled = checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED;

        // Update status
        if (accessibilityEnabled && overlayEnabled && contactsEnabled) {
            statusText.setText("● Ready to Scan");
            statusText.setTextColor(0xFF25D366);
        } else {
            statusText.setText("● Setup Required");
            statusText.setTextColor(0xFFFF5252);
        }

        // Update button states
        btnAccessibility.setText(accessibilityEnabled ? "✓ Accessibility Enabled" : "Enable Accessibility Service");
        btnAccessibility.setEnabled(!accessibilityEnabled);

        btnOverlay.setText(overlayEnabled ? "✓ Overlay Enabled" : "Grant Overlay Permission");
        btnOverlay.setEnabled(!overlayEnabled);

        btnContacts.setText(contactsEnabled ? "✓ Contacts Enabled" : "Grant Contacts Permission");
        btnContacts.setEnabled(!contactsEnabled);

        btnStartService.setEnabled(accessibilityEnabled && overlayEnabled && contactsEnabled);

        // Update counts from service
        detectedCount.setText(String.valueOf(WhatsAppScannerService.detectedNumbers.size()));
        savedCount.setText(String.valueOf(WhatsAppScannerService.savedCount));
        unsavedCount.setText(String.valueOf(WhatsAppScannerService.unsavedNumbers.size()));
        
        // Enable view details button only if there are detected numbers
        btnViewDetails.setEnabled(WhatsAppScannerService.detectedNumbers.size() > 0);
    }

    private void updateServerSection() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String apiKey = prefs.getString("api_key", "");
        String serverUrl = prefs.getString("server_url", "");
        
        if (apiKey.isEmpty()) {
            tvServerStatus.setText("Status: ● Not Configured");
            tvServerStatus.setTextColor(0xFF888888);
            tvServerUrl.setText("Server: Not set");
            tvLastSync.setText("Last sync: Never");
            btnSyncFromServer.setEnabled(false);
        } else {
            tvServerStatus.setText("Status: ● Connected");
            tvServerStatus.setTextColor(0xFF25D366);
            
            String displayUrl = serverUrl.replace("https://", "").replace("http://", "");
            tvServerUrl.setText("Server: " + displayUrl);
            
            updateLastSyncText();
            btnSyncFromServer.setEnabled(true);
            
            // Update server stats
            updateServerStats();
        }
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
                tvLastSync.setText("Last sync: " + minutesAgo + " min ago");
            } else {
                long hoursAgo = minutesAgo / 60;
                tvLastSync.setText("Last sync: " + hoursAgo + " hours ago");
            }
        }
    }

    private void updateServerStats() {
        new Thread(() -> {
            try {
                ApiClient apiClient = new ApiClient(this);
                ContactStats stats = apiClient.getContactStats();

                runOnUiThread(() -> {
                    serverPendingCount.setText(String.valueOf(stats.pending));
                    serverSyncedCount.setText(String.valueOf(stats.synced));
                    serverFailedCount.setText(String.valueOf(stats.failed));
                });
            } catch (Exception e) {
                // Silent fail - stats will remain at 0
            }
        }).start();
    }

    private void performServerSync() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String apiKey = prefs.getString("api_key", "");
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Please configure API key in Settings first", Toast.LENGTH_SHORT).show();
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
                        updateServerSection();
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
                    updateServerSection();
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

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);

        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void openAccessibilitySettings() {
        new AlertDialog.Builder(this)
            .setTitle("Enable Accessibility Service")
            .setMessage("1. Find 'WA Contact Saver' in the list\n2. Tap on it\n3. Toggle ON the switch\n4. Confirm the permission")
            .setPositiveButton("Open Settings", (d, w) -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
    }

    private void requestContactsPermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
            }, REQUEST_CONTACTS_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CONTACTS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Contacts permission granted!", Toast.LENGTH_SHORT).show();
            }
            updateUI();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            updateUI();
        }
    }

    private void startFloatingService() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Please enable Accessibility Service first", Toast.LENGTH_LONG).show();
            return;
        }

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant Overlay permission first", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        Toast.makeText(this, "Floating button started! Open WhatsApp and tap START", Toast.LENGTH_LONG).show();

        // Minimize app
        moveTaskToBack(true);
    }

    private void openViewNumbers() {
        Intent intent = new Intent(this, ViewNumbersActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
