package com.warysecure.contactsaver;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.warysecure.contactsaver.utils.ServerContactSaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Activity to view contacts saved through the API sync.
 * Only displays contacts that were saved via the server API, NOT all phone contacts.
 */
public class SavedContactsActivity extends Activity {

    private TextView tvTitle;
    private TextView tvContactCount;
    private Button btnClose;
    private LinearLayout contactListContainer;

    private List<ContactInfo> savedContacts = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_contacts);

        initViews();
        setupClickListeners();

        // Show loading state then load on background thread
        tvContactCount.setText("Loading...");
        new Thread(() -> {
            loadApiSavedContacts();
            mainHandler.post(() -> displayContacts());
        }).start();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvContactCount = findViewById(R.id.tvContactCount);
        btnClose = findViewById(R.id.btnClose);
        contactListContainer = findViewById(R.id.contactListContainer);
    }

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> finish());
    }

    /**
     * Load only contacts that were saved through the API.
     * Reads from the tracked set in SharedPreferences instead of querying all phone contacts.
     */
    private void loadApiSavedContacts() {
        savedContacts.clear();

        try {
            Set<String> apiPhones = ServerContactSaver.getApiSavedPhones(this);

            for (String entry : apiPhones) {
                try {
                    String phone;
                    String name;
                    if (entry.contains("||")) {
                        String[] parts = entry.split("\\|\\|", 2);
                        phone = parts[0];
                        name = parts.length > 1 ? parts[1] : phone;
                    } else {
                        phone = entry;
                        name = entry;
                    }
                    savedContacts.add(new ContactInfo(name, phone));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Sort by name (safe since list is only accessed by main thread after post)
            savedContacts.sort((a, b) -> {
                if (a.name == null) return 1;
                if (b.name == null) return -1;
                return a.name.compareToIgnoreCase(b.name);
            });
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            mainHandler.post(() ->
                Toast.makeText(this, "Error loading contacts: " + errorMsg, Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void displayContacts() {
        try {
            contactListContainer.removeAllViews();

            tvContactCount.setText(savedContacts.size() + " API contacts");

            if (savedContacts.isEmpty()) {
                TextView emptyView = new TextView(this);
                emptyView.setText("No API-saved contacts found\n\nSync contacts from the server to see them here");
                emptyView.setTextSize(16);
                emptyView.setTextColor(Color.parseColor("#888888"));
                emptyView.setPadding(20, 40, 20, 40);
                emptyView.setGravity(android.view.Gravity.CENTER);
                contactListContainer.addView(emptyView);
                return;
            }

            int index = 0;
            for (ContactInfo contact : savedContacts) {
                index++;
                View contactCard = createContactCard(contact, index);
                contactListContainer.addView(contactCard);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error displaying contacts", Toast.LENGTH_SHORT).show();
        }
    }

    private View createContactCard(ContactInfo contact, int index) {
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
        
        // Contact name
        TextView nameView = new TextView(this);
        nameView.setText(contact.name);
        nameView.setTextSize(16);
        nameView.setTextColor(Color.parseColor("#333333"));
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        infoContainer.addView(nameView);
        
        // Phone number (smaller, below name)
        TextView numberView = new TextView(this);
        numberView.setText(contact.phoneNumber);
        numberView.setTextSize(14);
        numberView.setTextColor(Color.parseColor("#666666"));
        infoContainer.addView(numberView);
        
        card.addView(infoContainer);
        
        return card;
    }

    /**
     * Simple class to hold contact information
     */
    private static class ContactInfo {
        String name;
        String phoneNumber;

        ContactInfo(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }
    }
}
