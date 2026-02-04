package com.warysecure.contactsaver.models;

public class ApiResponse {
    public boolean success;
    public String message;
    public String error;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, String error) {
        this.success = success;
        this.message = message;
        this.error = error;
    }
}
