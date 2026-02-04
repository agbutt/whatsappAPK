package com.warysecure.contactsaver.models;

public class ServerContact {
    public int id;
    public int applicationId;
    public String phone;
    public String name;
    public String email;
    public String source;
    public String createdAt;

    public ServerContact() {
    }

    public ServerContact(int id, int applicationId, String phone, String name, String email, String source, String createdAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.phone = phone;
        this.name = name;
        this.email = email;
        this.source = source;
        this.createdAt = createdAt;
    }
}
