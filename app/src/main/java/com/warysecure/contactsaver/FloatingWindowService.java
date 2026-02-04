package com.warysecure.contactsaver;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingWindowService extends Service {

    private static final String CHANNEL_ID = "FloatingWindowChannel";
    private static final int NOTIFICATION_ID = 1;

    private WindowManager windowManager;
    private View floatingView;
    private TextView statusText;
    private Button btnStart;
    private Button btnStop;
    private Button btnClose;

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    private Handler handler;
    private static FloatingWindowService instance;

    public static FloatingWindowService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        handler = new Handler(Looper.getMainLooper());
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        createFloatingWindow();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "WhatsApp Contact Saver",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Running in background to scan WhatsApp");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("WA Contact Saver")
                .setContentText("Ready to scan WhatsApp")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 200;

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.floating_window, null);

        statusText = floatingView.findViewById(R.id.floatingStatus);
        btnStart = floatingView.findViewById(R.id.btnFloatStart);
        btnStop = floatingView.findViewById(R.id.btnFloatStop);
        btnClose = floatingView.findViewById(R.id.btnFloatClose);

        btnStart.setOnClickListener(v -> startScanning());
        btnStop.setOnClickListener(v -> stopScanning());
        btnClose.setOnClickListener(v -> {
            stopSelf();
        });

        // Make floating window draggable
        floatingView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY + (int) (event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
            }
            return false;
        });

        windowManager.addView(floatingView, params);
    }

    private void startScanning() {
        WhatsAppScannerService scanner = WhatsAppScannerService.getInstance();
        if (scanner != null) {
            scanner.startScanning();
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            statusText.setText("Scanning...");
        } else {
            Toast.makeText(this, "Please enable Accessibility Service first!", Toast.LENGTH_LONG).show();
        }
    }

    private void stopScanning() {
        WhatsAppScannerService scanner = WhatsAppScannerService.getInstance();
        if (scanner != null) {
            scanner.stopScanning();
        }
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        statusText.setText("Stopped");
    }

    public void updateStatus(String status) {
        handler.post(() -> {
            if (statusText != null) {
                statusText.setText(status);
            }
        });
    }

    public void showSummaryDialog(int detected, int saved, int skipped) {
        handler.post(() -> {
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
            statusText.setText("Done: " + saved + " saved");

            // Show toast summary
            String summary = "✅ Scan Complete!\n" +
                    "📱 Detected: " + detected + "\n" +
                    "💾 Saved: " + saved + "\n" +
                    "⏭️ Skipped: " + skipped;
            Toast.makeText(this, summary, Toast.LENGTH_LONG).show();
        });
    }
}
