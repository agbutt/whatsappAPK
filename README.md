# WhatsApp Contact Saver
## By Wary Secure Ltd

An Android app that automatically detects and saves unsaved phone numbers from WhatsApp to your contacts.

## Features
- 📱 Scans WhatsApp chat list for phone numbers
- 🔄 Auto-scrolls through your entire chat list
- ✅ Automatically saves new contacts
- 📊 Shows summary of detected and saved numbers
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

1. **Install the APK** on your Android device
2. **Enable Accessibility Service**: Settings > Accessibility > WA Contact Saver > ON
3. **Grant Overlay Permission**: Allow the app to display over other apps
4. **Grant Contacts Permission**: Allow the app to manage contacts
5. **Tap "Start Floating Button"** in the app
6. **Open WhatsApp** and navigate to your chat list
7. **Tap START** on the floating button
8. Watch as the app auto-scrolls and saves contacts!

## Permissions Required
- **Accessibility Service**: To read WhatsApp screen content
- **Display Over Other Apps**: For the floating control button
- **Read/Write Contacts**: To check existing and save new contacts

## Notes
- Works with WhatsApp and WhatsApp Business
- Contacts are saved with prefix "WA" + last 4 digits
- Already saved numbers are skipped
- Maximum 100 scrolls per scan session

## Technical Details
- Min SDK: Android 7.0 (API 24)
- Target SDK: Android 14 (API 34)
- Package: com.warysecure.contactsaver

## Disclaimer
This app uses Android Accessibility Services which require special permissions. 
Use responsibly and in compliance with local laws and WhatsApp's terms of service.

---
Developed for Wary Secure Ltd
