package com.warysecure.contactsaver;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhatsAppScannerService extends AccessibilityService {

    private static final String TAG = "WhatsAppScanner";
    private static final int SCROLL_DELAY_MS = 800;
    private static final int RETRY_DELAY_MS = 1500;
    
    // Phone number patterns (international formats)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\+?[0-9]{1,4}[\\s\\-]?[0-9]{2,4}[\\s\\-]?[0-9]{3,4}[\\s\\-]?[0-9]{3,4}"
    );

    public static Set<String> detectedNumbers = new HashSet<>();
    public static Set<String> existingContacts = new HashSet<>();
    public static Set<String> unsavedNumbers = new HashSet<>();
    public static int savedCount = 0;
    // Sequential counter for CLAUD_XXX contact naming (resets to 1 at each scan session)
    public static int contactSequenceNumber = 1;
    public static boolean isScanning = false;

    private Handler handler;
    private int screenHeight;
    private int screenWidth;
    private int scrollCount = 0;
    private int maxScrolls = 100;
    private int minScrolls = 50; // Minimum scrolls before allowing early stop
    private int maxNumbersToSave = 2000;
    private int noNewNumbersCount = 0;

    private static WhatsAppScannerService instance;

    public static WhatsAppScannerService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        handler = new Handler(Looper.getMainLooper());
        
        // Get screen dimensions
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;

        // Load existing contacts
        loadExistingContacts();
        
        Log.d(TAG, "WhatsApp Scanner Service Created");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isScanning) return;

        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        
        if (packageName.equals("com.whatsapp") || packageName.equals("com.whatsapp.w4b")) {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                scanForPhoneNumbers();
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    private void loadExistingContacts() {
        existingContacts.clear();
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
            null, null, null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String number = cursor.getString(0);
                if (number != null) {
                    existingContacts.add(normalizeNumber(number));
                }
            }
            cursor.close();
        }
        Log.d(TAG, "Loaded " + existingContacts.size() + " existing contacts");
    }

    private String normalizeNumber(String number) {
        return number.replaceAll("[^0-9+]", "");
    }

    public void startScanning() {
        isScanning = true;
        scrollCount = 0;
        noNewNumbersCount = 0;
        detectedNumbers.clear();
        unsavedNumbers.clear();
        savedCount = 0;
        contactSequenceNumber = 1;
        loadExistingContacts();
        
        // Initial scan
        scanForPhoneNumbers();
        
        // Start auto-scroll
        handler.postDelayed(this::performAutoScroll, 1000);
        
        showToast("Scanning started...");
        Log.d(TAG, "Scanning started");
    }

    public void stopScanning() {
        isScanning = false;
        handler.removeCallbacksAndMessages(null);
        
        // Show summary
        showSummary();
        
        Log.d(TAG, "Scanning stopped. Detected: " + detectedNumbers.size() + ", Saved: " + savedCount);
    }

    private void scanForPhoneNumbers() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        int previousCount = detectedNumbers.size();
        traverseNode(rootNode);
        
        if (detectedNumbers.size() == previousCount) {
            noNewNumbersCount++;
        } else {
            noNewNumbersCount = 0;
        }

        rootNode.recycle();
        
        // Update floating window
        FloatingWindowService floatingService = FloatingWindowService.getInstance();
        if (floatingService != null) {
            floatingService.updateStatus("Found: " + detectedNumbers.size());
        }
    }

    private void traverseNode(AccessibilityNodeInfo node) {
        if (node == null) return;

        // Get text content
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();

        if (text != null) {
            extractPhoneNumbers(text.toString());
        }
        if (desc != null) {
            extractPhoneNumbers(desc.toString());
        }

        // Traverse children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                traverseNode(child);
                child.recycle();
            }
        }
    }

    private void extractPhoneNumbers(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        while (matcher.find()) {
            String number = matcher.group();
            String normalized = normalizeNumber(number);
            
            // Validate: at least 10 digits
            if (normalized.replaceAll("\\+", "").length() >= 10) {
                if (!detectedNumbers.contains(normalized)) {
                    detectedNumbers.add(normalized);
                    Log.d(TAG, "New number detected: " + normalized);
                    
                    // Check if number already exists or if we've reached the limit
                    if (existingContacts.contains(normalized)) {
                        unsavedNumbers.add(normalized);
                        Log.d(TAG, "Number already exists in contacts: " + normalized);
                    } else if (savedCount < maxNumbersToSave) {
                        // Save to contacts immediately
                        saveContact(normalized);
                    } else {
                        unsavedNumbers.add(normalized);
                        Log.d(TAG, "Max save limit reached: " + normalized);
                    }
                }
            }
        }
    }

    private void saveContact(String phoneNumber) {
        try {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            
            int rawContactInsertIndex = ops.size();
            
            // Create new raw contact
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            // Add display name with CLAUD_ prefix
            String displayName = String.format("CLAUD_%03d", contactSequenceNumber);
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .build());

            // Add phone number
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());

            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            savedCount++;
            contactSequenceNumber++;
            existingContacts.add(phoneNumber);
            
            Log.d(TAG, "Saved contact: " + displayName + " - " + phoneNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving contact: " + e.getMessage());
        }
    }

    private void performAutoScroll() {
        if (!isScanning) return;

        scrollCount++;
        
        // Stop conditions - only stop early if we've done at least minScrolls
        if (scrollCount >= maxScrolls) {
            stopScanning();
            return;
        }
        
        // Allow early stop only after minimum scrolls and if no new numbers found
        if (scrollCount >= minScrolls && noNewNumbersCount >= 10) {
            stopScanning();
            return;
        }

        // Perform swipe gesture to scroll
        Path swipePath = new Path();
        float startY = screenHeight * 0.7f;
        float endY = screenHeight * 0.3f;
        float x = screenWidth / 2f;

        swipePath.moveTo(x, startY);
        swipePath.lineTo(x, endY);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 300));

        dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                // Continue scrolling after a delay
                handler.postDelayed(() -> {
                    if (isScanning) {
                        scanForPhoneNumbers();
                        performAutoScroll();
                    }
                }, SCROLL_DELAY_MS);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.d(TAG, "Scroll gesture cancelled, retrying...");
                // Retry after a longer delay if gesture was cancelled
                handler.postDelayed(() -> {
                    if (isScanning) {
                        scanForPhoneNumbers();
                        performAutoScroll();
                    }
                }, RETRY_DELAY_MS);
            }
        }, null);
    }

    private void showSummary() {
        String message = "Scan Complete!\n\n" +
                "📱 Numbers Detected: " + detectedNumbers.size() + "\n" +
                "✅ Contacts Saved: " + savedCount + "\n" +
                "⏭️ Unsaved Numbers: " + unsavedNumbers.size();
        
        showToast(message);
        
        // Update floating window
        FloatingWindowService floatingService = FloatingWindowService.getInstance();
        if (floatingService != null) {
            floatingService.showSummaryDialog(detectedNumbers.size(), savedCount, unsavedNumbers.size());
        }
    }

    private void showToast(String message) {
        handler.post(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }
}
