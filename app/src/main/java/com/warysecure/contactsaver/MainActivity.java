package com.warysecure.contactsaver;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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

import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_CONTACTS_PERMISSION = 1001;
    private static final int REQUEST_OVERLAY_PERMISSION = 1002;

    private TextView statusText;
    private TextView detectedCount;
    private TextView savedCount;
    private Button btnAccessibility;
    private Button btnOverlay;
    private Button btnContacts;
    private Button btnStartService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void initViews() {
        statusText = findViewById(R.id.statusText);
        detectedCount = findViewById(R.id.detectedCount);
        savedCount = findViewById(R.id.savedCount);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnOverlay = findViewById(R.id.btnOverlay);
        btnContacts = findViewById(R.id.btnContacts);
        btnStartService = findViewById(R.id.btnStartService);
    }

    private void setupClickListeners() {
        btnAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        btnOverlay.setOnClickListener(v -> requestOverlayPermission());
        btnContacts.setOnClickListener(v -> requestContactsPermission());
        btnStartService.setOnClickListener(v -> startFloatingService());
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
}
