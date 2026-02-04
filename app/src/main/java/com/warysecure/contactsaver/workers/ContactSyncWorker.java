package com.warysecure.contactsaver.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.warysecure.contactsaver.api.ApiClient;
import com.warysecure.contactsaver.models.ApiResponse;
import com.warysecure.contactsaver.models.ServerContact;
import com.warysecure.contactsaver.models.SyncResult;
import com.warysecure.contactsaver.utils.ServerContactSaver;

import java.util.ArrayList;
import java.util.List;

public class ContactSyncWorker extends Worker {
    private static final String PREFS_NAME = "settings";
    private static final String CHANNEL_ID = "contact_sync_channel";
    private static final int NOTIFICATION_ID = 1001;

    public ContactSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        
        // Check if auto sync is enabled
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean autoSyncEnabled = prefs.getBoolean("auto_sync_enabled", false);
        
        if (!autoSyncEnabled) {
            return Result.success();
        }

        // Check if API key is configured
        String apiKey = prefs.getString("api_key", "");
        if (apiKey.isEmpty()) {
            return Result.success();
        }

        try {
            // Initialize API client and contact saver
            ApiClient apiClient = new ApiClient(context);
            ServerContactSaver contactSaver = new ServerContactSaver(context);

            // Fetch pending contacts
            List<ServerContact> pendingContacts = apiClient.getPendingContacts();
            
            if (pendingContacts.isEmpty()) {
                updateLastSyncTime();
                return Result.success();
            }

            // Process contacts
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

            // Report sync results back to server
            if (!syncResults.isEmpty()) {
                ApiResponse bulkSyncResponse = apiClient.bulkSync(syncResults);
                if (!bulkSyncResponse.success) {
                    // Log but don't fail the work
                    android.util.Log.e("ContactSyncWorker", "Bulk sync failed: " + bulkSyncResponse.error);
                }
            }

            // Update last sync time
            updateLastSyncTime();

            // Show notification if contacts were synced
            if (savedCount > 0 || failedCount > 0) {
                showNotification(savedCount, failedCount);
            }

            return Result.success();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

    private void updateLastSyncTime() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply();
    }

    private void showNotification(int savedCount, int failedCount) {
        Context context = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Contact Sync",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for contact synchronization");
            notificationManager.createNotificationChannel(channel);
        }

        // Build notification
        String title = "Contact Sync Complete";
        String message;
        if (failedCount > 0) {
            message = savedCount + " contacts saved, " + failedCount + " failed";
        } else {
            message = savedCount + " new contacts saved";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
