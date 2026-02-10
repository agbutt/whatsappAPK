package com.warysecure.contactsaver;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ViewNumbersActivity extends Activity {

    private Button btnTabAll;
    private Button btnTabSaved;
    private Button btnTabUnsaved;
    private Button btnCloseView;
    
    private TextView statsTotal;
    private TextView statsSaved;
    private TextView statsUnsaved;
    
    private LinearLayout numberListContainer;
    
    private String currentTab = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_numbers);

        initViews();
        setupClickListeners();
        updateStats();
        displayNumbers("all");
    }

    private void initViews() {
        btnTabAll = findViewById(R.id.btnTabAll);
        btnTabSaved = findViewById(R.id.btnTabSaved);
        btnTabUnsaved = findViewById(R.id.btnTabUnsaved);
        btnCloseView = findViewById(R.id.btnCloseView);
        
        statsTotal = findViewById(R.id.statsTotal);
        statsSaved = findViewById(R.id.statsSaved);
        statsUnsaved = findViewById(R.id.statsUnsaved);
        
        numberListContainer = findViewById(R.id.numberListContainer);
    }

    private void setupClickListeners() {
        btnTabAll.setOnClickListener(v -> {
            currentTab = "all";
            updateTabStyle();
            displayNumbers("all");
        });

        btnTabSaved.setOnClickListener(v -> {
            currentTab = "saved";
            updateTabStyle();
            displayNumbers("saved");
        });

        btnTabUnsaved.setOnClickListener(v -> {
            currentTab = "unsaved";
            updateTabStyle();
            displayNumbers("unsaved");
        });

        btnCloseView.setOnClickListener(v -> finish());
    }

    private void updateTabStyle() {
        // Reset all tabs to default style
        btnTabAll.setTextColor(Color.parseColor("#666666"));
        btnTabAll.setBackgroundColor(Color.WHITE);
        
        btnTabSaved.setTextColor(Color.parseColor("#666666"));
        btnTabSaved.setBackgroundColor(Color.WHITE);
        
        btnTabUnsaved.setTextColor(Color.parseColor("#666666"));
        btnTabUnsaved.setBackgroundColor(Color.WHITE);
        
        // Highlight active tab
        if (currentTab.equals("all")) {
            btnTabAll.setTextColor(Color.WHITE);
            btnTabAll.setBackgroundColor(Color.parseColor("#075E54"));
        } else if (currentTab.equals("saved")) {
            btnTabSaved.setTextColor(Color.WHITE);
            btnTabSaved.setBackgroundColor(Color.parseColor("#25D366"));
        } else if (currentTab.equals("unsaved")) {
            btnTabUnsaved.setTextColor(Color.WHITE);
            btnTabUnsaved.setBackgroundColor(Color.parseColor("#FF5252"));
        }
    }

    private void updateStats() {
        int total = WhatsAppScannerService.detectedNumbers.size();
        int saved = WhatsAppScannerService.savedCount;
        int unsaved = WhatsAppScannerService.unsavedNumbers.size();
        
        statsTotal.setText(String.valueOf(total));
        statsSaved.setText(String.valueOf(saved));
        statsUnsaved.setText(String.valueOf(unsaved));
    }

    private void displayNumbers(String filter) {
        numberListContainer.removeAllViews();
        
        List<String> numbersToDisplay = new ArrayList<>();
        
        if (filter.equals("all")) {
            numbersToDisplay.addAll(WhatsAppScannerService.detectedNumbers);
        } else if (filter.equals("saved")) {
            // Show only saved numbers (those not in unsaved list)
            for (String number : WhatsAppScannerService.detectedNumbers) {
                if (!WhatsAppScannerService.unsavedNumbers.contains(number)) {
                    numbersToDisplay.add(number);
                }
            }
        } else if (filter.equals("unsaved")) {
            numbersToDisplay.addAll(WhatsAppScannerService.unsavedNumbers);
        }
        
        if (numbersToDisplay.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No numbers to display");
            emptyView.setTextSize(16);
            emptyView.setTextColor(Color.parseColor("#888888"));
            emptyView.setPadding(20, 40, 20, 40);
            emptyView.setGravity(android.view.Gravity.CENTER);
            numberListContainer.addView(emptyView);
            return;
        }
        
        int index = 0;
        for (String number : numbersToDisplay) {
            index++;
            boolean isSaved = !WhatsAppScannerService.unsavedNumbers.contains(number);
            String contactName = isSaved ? getContactNameByPhone(number) : null;
            View numberCard = createNumberCard(number, contactName, index, isSaved);
            numberListContainer.addView(numberCard);
        }
    }

    /**
     * Get contact name from device contacts by phone number.
     * Returns null if contact not found or no permission.
     */
    private String getContactNameByPhone(String phoneNumber) {
        // Check permission
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        
        // Normalize phone number
        String normalizedPhone = phoneNumber.replaceAll("[^+0-9]", "");
        
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                
                do {
                    String existingNumber = cursor.getString(numberIndex);
                    String normalizedExisting = existingNumber.replaceAll("[^+0-9]", "");
                    
                    // Check if numbers match
                    if (normalizedExisting.equals(normalizedPhone)) {
                        return cursor.getString(nameIndex);
                    }
                    
                    // Compare last 10 digits for international format differences
                    if (normalizedExisting.length() >= 10 && normalizedPhone.length() >= 10) {
                        String existingLast10 = normalizedExisting.substring(normalizedExisting.length() - 10);
                        String phoneLast10 = normalizedPhone.substring(normalizedPhone.length() - 10);
                        if (existingLast10.equals(phoneLast10)) {
                            return cursor.getString(nameIndex);
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return null;
    }

    private View createNumberCard(String phoneNumber, String contactName, int index, boolean isSaved) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(15, 12, 15, 12);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        card.setLayoutParams(params);
        card.setElevation(2);
        
        // Index
        TextView indexView = new TextView(this);
        indexView.setText(String.valueOf(index));
        indexView.setTextSize(14);
        indexView.setTextColor(Color.parseColor("#888888"));
        indexView.setWidth(60);
        card.addView(indexView);
        
        // Contact info container (name + number)
        LinearLayout infoContainer = new LinearLayout(this);
        infoContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1
        );
        infoContainer.setLayoutParams(infoParams);
        
        // Contact name (if available)
        if (contactName != null && !contactName.isEmpty()) {
            TextView nameView = new TextView(this);
            nameView.setText(contactName);
            nameView.setTextSize(16);
            nameView.setTextColor(Color.parseColor("#333333"));
            nameView.setTypeface(null, android.graphics.Typeface.BOLD);
            infoContainer.addView(nameView);
            
            // Phone number (smaller, below name)
            TextView numberView = new TextView(this);
            numberView.setText(phoneNumber);
            numberView.setTextSize(14);
            numberView.setTextColor(Color.parseColor("#666666"));
            infoContainer.addView(numberView);
        } else {
            // No name - just show phone number
            TextView numberView = new TextView(this);
            numberView.setText(phoneNumber);
            numberView.setTextSize(16);
            numberView.setTextColor(Color.parseColor("#333333"));
            infoContainer.addView(numberView);
        }
        
        card.addView(infoContainer);
        
        // Status badge
        TextView statusView = new TextView(this);
        if (isSaved) {
            statusView.setText("✓ SAVED");
            statusView.setTextColor(Color.WHITE);
            statusView.setBackgroundColor(Color.parseColor("#25D366"));
        } else {
            statusView.setText("UNSAVED");
            statusView.setTextColor(Color.WHITE);
            statusView.setBackgroundColor(Color.parseColor("#FF5252"));
        }
        statusView.setTextSize(11);
        statusView.setPadding(12, 6, 12, 6);
        card.addView(statusView);
        
        return card;
    }
}
