# Server API Integration Guide

## Overview

The WhatsApp Contact Saver app now supports integration with a remote server to automatically sync contacts. This guide explains how to set up and use the server integration feature.

## Server API Endpoints

### Base URL
```
https://joinus.cx/api/mobile
```

### Authentication
All API requests require an `X-API-Key` header with your 64-character API key.

### Supported Endpoints

1. **Verify API Key** - `GET /verify.php`
   - Verifies that the API key is valid
   - Returns server information and features

2. **Get Pending Contacts** - `GET /contacts.php?action=pending`
   - Fetches all contacts that need to be synced to the device
   - Returns array of contact objects

3. **Get Contact Statistics** - `GET /contacts.php?action=all`
   - Returns statistics about all contacts (pending, synced, failed, etc.)

4. **Sync Single Contact** - `POST /contacts.php?action=sync`
   - Reports sync status for a single contact

5. **Bulk Sync Contacts** - `POST /contacts.php?action=bulk-sync`
   - Reports sync status for multiple contacts (more efficient)

6. **Add Contact** - `POST /contacts.php?action=add`
   - Adds a new contact from the mobile app to the server

## Setup Instructions

### 1. Get Your API Key

1. Contact your server administrator
2. Request access to the admin panel at `https://joinus.cx/admin/api_settings.php`
3. Generate a new API key with a descriptive name
4. Copy the API key (shown only once!)

### 2. Configure the App

1. Open the WhatsApp Contact Saver app
2. Tap the ⚙️ Settings button in the top-right corner
3. Enter your Server URL: `https://joinus.cx`
4. Paste your API Key
5. Tap "Test Connection" to verify
6. Configure auto-sync settings (optional):
   - Enable Auto Sync
   - Select sync interval (5, 15, 30, or 60 minutes)
   - Enable "Sync on App Start" if desired
7. Customize contact prefix (default: JU_)
8. Tap "Save Settings"

### 3. Sync Contacts

**Manual Sync:**
- Tap "Sync from Server" button on the main screen
- Wait for the sync to complete
- Check notification for results

**Automatic Sync:**
- If auto-sync is enabled, the app will automatically fetch and sync contacts at the configured interval
- Sync only runs when network is available
- You'll receive a notification when new contacts are synced

## Features

### Duplicate Detection
- The app checks if a contact already exists before saving
- Phone numbers are normalized and compared
- Supports various phone number formats

### Sequential Numbering
- Contacts are saved with a custom prefix and sequential number
- Format: `{PREFIX}_{NUMBER}` (e.g., JU_001, JU_002)
- Numbering starts from the highest existing number + 1

### Background Sync
- Uses Android WorkManager for reliable background sync
- Respects battery optimization settings
- Only syncs when network is connected
- Configurable sync intervals

### Sync Statistics
- Real-time statistics from server
- Shows pending, synced, and failed counts
- Last sync timestamp
- Connection status indicator

## Troubleshooting

### "Connection failed" error
- Check your internet connection
- Verify the server URL is correct
- Ensure your API key is valid and not expired
- Check if the server is online

### "API key invalid" error
- Verify you copied the entire 64-character key
- Check if the key was deactivated by the administrator
- Request a new API key if needed

### Contacts not syncing
- Ensure auto-sync is enabled
- Check that the app has Contacts permission
- Verify network connectivity
- Check last sync time in Settings

### Duplicate contacts being created
- The app checks for duplicates by phone number
- If duplicates appear, ensure phone numbers are in international format
- Clear app data and reconfigure if issues persist

## API Rate Limits

- **100 requests per minute** per API key
- If you exceed the rate limit, you'll receive a 429 error
- The app will automatically retry with exponential backoff

## Security

### API Key Storage
- API keys are stored in SharedPreferences
- Ready for encryption with Android Security Crypto library
- Never logged or displayed in full

### Network Security
- All API calls use HTTPS
- SSL/TLS encryption for data in transit
- No sensitive data stored in logs

### Privacy
- Contact data is only sent to the configured server
- No third-party services involved
- API key is only sent to the configured server URL

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review the app logs for error messages
3. Contact your server administrator for API-related issues
4. Contact Wary Secure Ltd for app-related issues

---

**Note:** The WhatsApp scanner feature works completely independently from the server sync feature. You can use either or both features as needed.
