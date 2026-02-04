package com.warysecure.contactsaver.models;

public class ContactStats {
    public int pending;
    public int synced;
    public int failed;
    public int deleted;
    public int total;

    public ContactStats() {
    }

    public ContactStats(int pending, int synced, int failed, int deleted, int total) {
        this.pending = pending;
        this.synced = synced;
        this.failed = failed;
        this.deleted = deleted;
        this.total = total;
    }
}
