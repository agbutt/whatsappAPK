# WhatsApp Contact Saver
## By Wary Secure Ltd

An Android app that automatically detects and saves unsaved phone numbers from WhatsApp to your contacts.

## Features

### WhatsApp Scanner
- 📱 Scans WhatsApp chat list for phone numbers
- 🔄 Auto-scrolls through your entire chat list
- ✅ Automatically saves new contacts with CLAUD_XXX prefix
- 📊 Shows summary of detected and saved numbers
- 📋 View detailed list of detected numbers with tabs (All/Saved/Unsaved)
- 🔢 Supports up to 2000 numbers per scan

### Server Integration (NEW!)
- 🌐 Connect to remote server via API
- 🔑 Secure API key authentication
- 📥 Automatically sync contacts from server
- ⏰ Background auto-sync with configurable intervals (5, 15, 30, 60 minutes)
- 📊 Real-time sync statistics (pending, synced, failed)
- 🔄 Manual sync on demand
- 📱 Customizable contact prefix (default: JU_)
- 🔔 Notifications for sync completion
- 🎨 WhatsApp-themed UI

## How to Build

### Option 1: Using Android Studio (Recommended)
1. Install Android Studio from https://developer.android.com/studio
2. Open this project folder in Android Studio
3. Wait for Gradle sync to complete
4. Click Build > Build Bundle(s) / APK(s) > Build APK(s)
5. Find the APK in `app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Using Command Line
```bash
# Install Android SDK and set ANDROID_HOME
export ANDROID_HOME=/path/to/android-sdk

# Build debug APK
./gradlew assembleDebug

# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

## How to Use

### WhatsApp Scanner

1. **Install the APK** on your Android device
2. **Enable Accessibility Service**: Settings > Accessibility > WA Contact Saver > ON
3. **Grant Overlay Permission**: Allow the app to display over other apps
4. **Grant Contacts Permission**: Allow the app to manage contacts
5. **Tap "Start Floating Button"** in the app
6. **Open WhatsApp** and navigate to your chat list
7. **Tap START** on the floating button
8. Watch as the app auto-scrolls and saves contacts!

### Server Sync (NEW!)

1. **Open Settings** by tapping the ⚙️ button in the app
2. **Enter Server URL** (e.g., https://joinus.cx)
3. **Enter API Key** (get this from your server admin panel)
4. **Test Connection** to verify your credentials
5. **Configure Auto Sync** (optional):
   - Enable Auto Sync toggle
   - Select sync interval (5, 15, 30, or 60 minutes)
   - Enable "Sync on App Start" if desired
6. **Customize Contact Prefix** (default: JU_)
   - Contacts will be saved as JU_001, JU_002, etc.
7. **Save Settings**
8. **Tap "Sync from Server"** on the main screen to manually sync anytime

The app will automatically fetch pending contacts from your server and save them to your device in the background.

## Permissions Required
- **Accessibility Service**: To read WhatsApp screen content
- **Display Over Other Apps**: For the floating control button
- **Read/Write Contacts**: To check existing and save new contacts
- **Internet**: For server API communication (NEW!)
- **Access Network State**: To check network connectivity (NEW!)
- **Receive Boot Completed**: For background sync scheduling (NEW!)

## Notes

### WhatsApp Scanner
- Works with WhatsApp and WhatsApp Business
- Contacts are saved with prefix "CLAUD_001", "CLAUD_002", etc.
- Already saved numbers are skipped and shown in the unsaved list
- Maximum 2000 numbers can be saved per scan session
- Maximum 100 scrolls per scan session
- Use "View Detected Numbers" button to see detailed list with tabs

### Server Sync
- Requires valid API key from server administrator
- Contacts saved with customizable prefix (default: JU_001, JU_002, etc.)
- Duplicate phone numbers are automatically skipped
- Background sync only runs when network is available
- Sync statistics are fetched in real-time from server
- Works completely independently from WhatsApp scanner feature

## Technical Details
- Min SDK: Android 7.0 (API 24)
- Target SDK: Android 14 (API 34)
- Package: com.warysecure.contactsaver
- Dependencies:
  - OkHttp 4.12.0 (HTTP client)
  - Gson 2.10.1 (JSON parsing)
  - WorkManager 2.9.0 (background sync)
  - Security Crypto 1.1.0-alpha06 (encrypted storage)

## Disclaimer
This app uses Android Accessibility Services which require special permissions. 
Use responsibly and in compliance with local laws and WhatsApp's terms of service.

---
Developed for Wary Secure Ltd
