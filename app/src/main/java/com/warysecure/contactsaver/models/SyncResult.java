package com.warysecure.contactsaver.models;

public class SyncResult {
    public int contactId;
    public String deviceContactId;
    public String status; // "synced", "failed", "deleted"

    public SyncResult() {
    }

    public SyncResult(int contactId, String deviceContactId, String status) {
        this.contactId = contactId;
        this.deviceContactId = deviceContactId;
        this.status = status;
    }
}
