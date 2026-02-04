package com.warysecure.contactsaver.utils;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.warysecure.contactsaver.models.ServerContact;

import java.util.ArrayList;

public class ServerContactSaver {
    private static final String PREFS_NAME = "settings";
    private static final String DEFAULT_PREFIX = "JU_";
    
    private Context context;
    private String contactPrefix;

    public ServerContactSaver(Context context) {
        this.context = context;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.contactPrefix = prefs.getString("contact_prefix", DEFAULT_PREFIX);
    }

    public String saveContact(ServerContact contact) {
        try {
            // Check if contact already exists
            if (contactExists(contact.phone)) {
                return null;
            }

            // Get next sequence number
            int sequenceNumber = getNextSequenceNumber();
            String contactName = contactPrefix + String.format("%03d", sequenceNumber);

            // Prepare operations to insert contact
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            
            int rawContactIndex = ops.size();
            
            // Insert raw contact
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, (String) null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, (String) null)
                    .build());

            // Insert name
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contactName)
                    .build());

            // Insert phone number
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phone)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());

            // Insert email if available
            if (contact.email != null && !contact.email.isEmpty()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, contact.email)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build());
            }

            // Execute operations
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

            // Save sequence number
            saveSequenceNumber(sequenceNumber);

            // Return the contact URI (we'll use a simple format)
            return "content://contacts/" + sequenceNumber;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean contactExists(String phoneNumber) {
        ContentResolver resolver = context.getContentResolver();
        
        // Normalize phone number for comparison
        String normalizedPhone = phoneNumber.replaceAll("[^0-9+]", "");
        
        Cursor cursor = null;
        try {
            cursor = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                do {
                    String existingNumber = cursor.getString(numberIndex);
                    String normalizedExisting = existingNumber.replaceAll("[^0-9+]", "");
                    
                    // Check if numbers match (exact match or last 10 digits match for both numbers >= 10 digits)
                    if (normalizedExisting.equals(normalizedPhone)) {
                        return true;
                    }
                    
                    // Compare last 10 digits only if both numbers are at least 10 digits
                    if (normalizedExisting.length() >= 10 && normalizedPhone.length() >= 10) {
                        String existingLast10 = normalizedExisting.substring(normalizedExisting.length() - 10);
                        String phoneLast10 = normalizedPhone.substring(normalizedPhone.length() - 10);
                        if (existingLast10.equals(phoneLast10)) {
                            return true;
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
        
        return false;
    }

    private int getNextSequenceNumber() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentSequence = prefs.getInt("contact_sequence", 0);
        
        // Also check existing contacts to find highest number
        int highestExisting = findHighestSequenceNumber();
        
        return Math.max(currentSequence, highestExisting) + 1;
    }

    private int findHighestSequenceNumber() {
        ContentResolver resolver = context.getContentResolver();
        int highest = 0;
        
        Cursor cursor = null;
        try {
            cursor = resolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                    ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?",
                    new String[]{contactPrefix + "%"},
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                do {
                    String name = cursor.getString(nameIndex);
                    if (name != null && name.startsWith(contactPrefix)) {
                        String numberPart = name.substring(contactPrefix.length());
                        try {
                            int number = Integer.parseInt(numberPart);
                            if (number > highest) {
                                highest = number;
                            }
                        } catch (NumberFormatException e) {
                            // Ignore non-numeric suffixes
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
        
        return highest;
    }

    private void saveSequenceNumber(int sequence) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt("contact_sequence", sequence).apply();
    }
}
