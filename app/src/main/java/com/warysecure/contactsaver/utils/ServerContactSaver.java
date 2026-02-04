package com.warysecure.contactsaver.utils;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.warysecure.contactsaver.models.ServerContact;

import java.util.ArrayList;

public class ServerContactSaver {
    private Context context;

    public ServerContactSaver(Context context) {
        this.context = context;
    }

    /**
     * Save or update a contact from the server.
     * - Uses the actual applicant name from the server (NOT sequential codes)
     * - If contact exists by phone number, UPDATE the name
     * - If contact doesn't exist, CREATE new contact with the name
     * 
     * @param contact The server contact to save
     * @return Device contact ID if successful, null if failed
     */
    public String saveContact(ServerContact contact) {
        try {
            // Use the actual applicant name from the server
            String contactName = (contact.name != null && !contact.name.isEmpty()) 
                ? contact.name 
                : "Unknown Contact";

            // Check if contact with this phone number already exists
            String existingContactId = findContactByPhone(contact.phone);
            
            if (existingContactId != null) {
                // UPDATE existing contact name
                updateContactName(existingContactId, contactName);
                return existingContactId;
            } else {
                // CREATE new contact
                return createNewContact(contactName, contact.phone, contact.email);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Find a contact by phone number.
     * Normalizes phone numbers to handle different formats.
     * 
     * @param phone The phone number to search for
     * @return Contact ID if found, null otherwise
     */
    private String findContactByPhone(String phone) {
        ContentResolver resolver = context.getContentResolver();
        
        // Normalize phone number (remove spaces, dashes, parentheses, etc.)
        String normalizedPhone = phone.replaceAll("[^+0-9]", "");
        
        Cursor cursor = null;
        try {
            // Query all phone numbers
            cursor = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int contactIdIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                
                do {
                    String existingNumber = cursor.getString(numberIndex);
                    String normalizedExisting = existingNumber.replaceAll("[^+0-9]", "");
                    
                    // Check if numbers match (exact match or last 10 digits match for comparison)
                    if (normalizedExisting.equals(normalizedPhone)) {
                        return cursor.getString(contactIdIndex);
                    }
                    
                    // Compare last 10 digits if both numbers are at least 10 digits
                    // This handles international format differences
                    if (normalizedExisting.length() >= 10 && normalizedPhone.length() >= 10) {
                        String existingLast10 = normalizedExisting.substring(normalizedExisting.length() - 10);
                        String phoneLast10 = normalizedPhone.substring(normalizedPhone.length() - 10);
                        if (existingLast10.equals(phoneLast10)) {
                            return cursor.getString(contactIdIndex);
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

    /**
     * Update the name of an existing contact.
     * 
     * @param contactId The contact ID to update
     * @param newName The new name to set
     */
    private void updateContactName(String contactId, String newName) {
        ContentResolver resolver = context.getContentResolver();
        
        try {
            ContentValues values = new ContentValues();
            values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName);
            
            String where = ContactsContract.Data.CONTACT_ID + " = ? AND " +
                           ContactsContract.Data.MIMETYPE + " = ?";
            String[] args = {contactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};
            
            int updated = resolver.update(ContactsContract.Data.CONTENT_URI, values, where, args);
            
            // If no name record exists for this contact, create one
            if (updated == 0) {
                // Find the raw contact ID for this contact
                String rawContactId = getRawContactId(contactId);
                if (rawContactId != null) {
                    ContentValues nameValues = new ContentValues();
                    nameValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
                    nameValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
                    nameValues.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName);
                    resolver.insert(ContactsContract.Data.CONTENT_URI, nameValues);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the raw contact ID for a contact ID.
     * 
     * @param contactId The contact ID
     * @return Raw contact ID if found, null otherwise
     */
    private String getRawContactId(String contactId) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    new String[]{ContactsContract.RawContacts._ID},
                    ContactsContract.RawContacts.CONTACT_ID + " = ?",
                    new String[]{contactId},
                    null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(ContactsContract.RawContacts._ID);
                return cursor.getString(idIndex);
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

    /**
     * Create a new contact with the given details.
     * 
     * @param name Contact name (actual applicant name)
     * @param phone Contact phone number
     * @param email Contact email (optional)
     * @return Device contact ID if successful, null if failed
     */
    private String createNewContact(String name, String phone, String email) {
        ContentResolver resolver = context.getContentResolver();
        
        try {
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
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build());

            // Insert phone number
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());

            // Insert email if available
            if (email != null && !email.isEmpty()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build());
            }

            // Execute operations
            resolver.applyBatch(ContactsContract.AUTHORITY, ops);

            // Get the newly created contact ID by phone number
            // (We need to query it since we don't get it directly from applyBatch)
            String newContactId = findContactByPhone(phone);
            
            return newContactId != null ? newContactId : "content://contacts/new";

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
