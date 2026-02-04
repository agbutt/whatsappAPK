package com.warysecure.contactsaver.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.warysecure.contactsaver.models.ApiResponse;
import com.warysecure.contactsaver.models.ContactStats;
import com.warysecure.contactsaver.models.ServerContact;
import com.warysecure.contactsaver.models.SyncResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static final String PREFS_NAME = "settings";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private String serverUrl;
    private String apiKey;
    private OkHttpClient client;
    private Gson gson;

    public ApiClient(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.serverUrl = prefs.getString("server_url", "https://joinus.cx");
        this.apiKey = prefs.getString("api_key", "");
        
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        this.gson = new Gson();
    }

    private String getApiUrl(String endpoint) {
        String baseUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        return baseUrl + "api/mobile/" + endpoint;
    }

    private Request.Builder getRequestBuilder(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("X-API-Key", apiKey);
    }

    public ApiResponse verify() {
        try {
            Request request = getRequestBuilder(getApiUrl("verify.php")).build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject obj = gson.fromJson(json, JsonObject.class);
                    
                    boolean success = obj.has("success") && obj.get("success").getAsBoolean();
                    String message = obj.has("message") ? obj.get("message").getAsString() : "";
                    String error = obj.has("error") ? obj.get("error").getAsString() : "";
                    
                    return new ApiResponse(success, message, error);
                } else {
                    return new ApiResponse(false, "", "HTTP " + response.code());
                }
            }
        } catch (IOException e) {
            return new ApiResponse(false, "", e.getMessage());
        }
    }

    public List<ServerContact> getPendingContacts() {
        List<ServerContact> contacts = new ArrayList<>();
        
        try {
            Request request = getRequestBuilder(getApiUrl("contacts.php?action=pending")).build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject obj = gson.fromJson(json, JsonObject.class);
                    
                    if (obj.has("success") && obj.get("success").getAsBoolean()) {
                        if (obj.has("contacts")) {
                            JsonArray contactsArray = obj.getAsJsonArray("contacts");
                            for (int i = 0; i < contactsArray.size(); i++) {
                                JsonObject contactObj = contactsArray.get(i).getAsJsonObject();
                                ServerContact contact = new ServerContact();
                                contact.id = contactObj.get("id").getAsInt();
                                contact.applicationId = contactObj.has("application_id") ? contactObj.get("application_id").getAsInt() : 0;
                                contact.phone = contactObj.get("phone").getAsString();
                                contact.name = contactObj.has("name") && !contactObj.get("name").isJsonNull() ? contactObj.get("name").getAsString() : "";
                                contact.email = contactObj.has("email") && !contactObj.get("email").isJsonNull() ? contactObj.get("email").getAsString() : "";
                                contact.source = contactObj.has("source") ? contactObj.get("source").getAsString() : "";
                                contact.createdAt = contactObj.has("created_at") ? contactObj.get("created_at").getAsString() : "";
                                contacts.add(contact);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return contacts;
    }

    public ContactStats getContactStats() {
        ContactStats stats = new ContactStats();
        
        try {
            Request request = getRequestBuilder(getApiUrl("contacts.php?action=all")).build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject obj = gson.fromJson(json, JsonObject.class);
                    
                    if (obj.has("success") && obj.get("success").getAsBoolean()) {
                        if (obj.has("stats")) {
                            JsonObject statsObj = obj.getAsJsonObject("stats");
                            stats.pending = statsObj.has("pending") ? statsObj.get("pending").getAsInt() : 0;
                            stats.synced = statsObj.has("synced") ? statsObj.get("synced").getAsInt() : 0;
                            stats.failed = statsObj.has("failed") ? statsObj.get("failed").getAsInt() : 0;
                            stats.deleted = statsObj.has("deleted") ? statsObj.get("deleted").getAsInt() : 0;
                            stats.total = statsObj.has("total") ? statsObj.get("total").getAsInt() : 0;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return stats;
    }

    public ApiResponse syncContact(int contactId, String deviceContactId, String status) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("contact_id", contactId);
            payload.addProperty("device_contact_id", deviceContactId);
            payload.addProperty("status", status);
            
            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = getRequestBuilder(getApiUrl("contacts.php?action=sync"))
                    .post(body)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject obj = gson.fromJson(json, JsonObject.class);
                    
                    boolean success = obj.has("success") && obj.get("success").getAsBoolean();
                    String message = obj.has("message") ? obj.get("message").getAsString() : "";
                    String error = obj.has("error") ? obj.get("error").getAsString() : "";
                    
                    return new ApiResponse(success, message, error);
                } else {
                    return new ApiResponse(false, "", "HTTP " + response.code());
                }
            }
        } catch (IOException e) {
            return new ApiResponse(false, "", e.getMessage());
        }
    }

    public ApiResponse bulkSync(List<SyncResult> results) {
        try {
            JsonObject payload = new JsonObject();
            JsonArray contactsArray = new JsonArray();
            
            for (SyncResult result : results) {
                JsonObject contactObj = new JsonObject();
                contactObj.addProperty("contact_id", result.contactId);
                if (result.deviceContactId != null) {
                    contactObj.addProperty("device_contact_id", result.deviceContactId);
                } else {
                    contactObj.add("device_contact_id", null);
                }
                contactObj.addProperty("status", result.status);
                contactsArray.add(contactObj);
            }
            
            payload.add("contacts", contactsArray);
            
            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = getRequestBuilder(getApiUrl("contacts.php?action=bulk-sync"))
                    .post(body)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject obj = gson.fromJson(json, JsonObject.class);
                    
                    boolean success = obj.has("success") && obj.get("success").getAsBoolean();
                    String message = obj.has("message") ? obj.get("message").getAsString() : "";
                    String error = obj.has("error") ? obj.get("error").getAsString() : "";
                    
                    return new ApiResponse(success, message, error);
                } else {
                    return new ApiResponse(false, "", "HTTP " + response.code());
                }
            }
        } catch (IOException e) {
            return new ApiResponse(false, "", e.getMessage());
        }
    }

    public ApiResponse addContact(String phone, String name) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("phone", phone);
            payload.addProperty("name", name);
            
            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = getRequestBuilder(getApiUrl("contacts.php?action=add"))
                    .post(body)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject obj = gson.fromJson(json, JsonObject.class);
                    
                    boolean success = obj.has("success") && obj.get("success").getAsBoolean();
                    String message = obj.has("message") ? obj.get("message").getAsString() : "";
                    String error = obj.has("error") ? obj.get("error").getAsString() : "";
                    
                    return new ApiResponse(success, message, error);
                } else {
                    return new ApiResponse(false, "", "HTTP " + response.code());
                }
            }
        } catch (IOException e) {
            return new ApiResponse(false, "", e.getMessage());
        }
    }
}
