package com.warysecure.contactsaver;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to view all contacts saved through the API sync.
 * Displays contacts with their names and phone numbers.
 */
public class SavedContactsActivity extends Activity {

    private static final String PREFS_NAME = "settings";
    
    private TextView tvTitle;
    private TextView tvContactCount;
    private Button btnClose;
    private LinearLayout contactListContainer;
    
    private List<ContactInfo> savedContacts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_contacts);

        initViews();
        setupClickListeners();
        loadSavedContacts();
        displayContacts();
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

    private void loadSavedContacts() {
        // Check permission
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Contacts permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the contact prefix from settings
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String prefix = prefs.getString("contact_prefix", "JU_");

        savedContacts.clear();

        Cursor cursor = null;
        try {
            // Query all contacts
            cursor = getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI,
                    new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.HAS_PHONE_NUMBER
                    },
                    null,
                    null,
                    ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);

                do {
                    String contactId = cursor.getString(idIndex);
                    String name = cursor.getString(nameIndex);
                    boolean hasPhone = cursor.getInt(hasPhoneIndex) > 0;

                    // Filter contacts saved from API (those without the old prefix or with actual names)
                    // We want to show all contacts, but especially those saved through API
                    if (hasPhone && name != null) {
                        // Get phone number for this contact
                        String phoneNumber = getPhoneNumber(contactId);
                        if (phoneNumber != null) {
                            savedContacts.add(new ContactInfo(name, phoneNumber));
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading contacts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String getPhoneNumber(String contactId) {
        Cursor phoneCursor = null;
        try {
            phoneCursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{contactId},
                    null
            );

            if (phoneCursor != null && phoneCursor.moveToFirst()) {
                int numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                return phoneCursor.getString(numberIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (phoneCursor != null) {
                phoneCursor.close();
            }
        }
        return null;
    }

    private void displayContacts() {
        contactListContainer.removeAllViews();

        tvContactCount.setText(savedContacts.size() + " contacts");

        if (savedContacts.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No contacts found\n\nSync contacts from the server to see them here");
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
