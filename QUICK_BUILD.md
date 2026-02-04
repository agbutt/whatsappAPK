# Quick Build Guide - WhatsApp Contact Saver APK

## EASIEST METHOD: Use GitHub (Free, No Installation Required)

1. **Create a GitHub account** at https://github.com if you don't have one

2. **Create a new repository:**
   - Click "New repository"
   - Name it "whatsapp-contact-saver"
   - Make it Public (or Private)
   - Click "Create repository"

3. **Upload this project:**
   - Extract the ZIP file
   - Drag and drop all files to the GitHub repository
   - Click "Commit changes"

4. **Build automatically:**
   - Go to "Actions" tab
   - Click "Build Android APK"
   - Click "Run workflow"
   - Wait 5-10 minutes

5. **Download your APK:**
   - Click on the completed workflow
   - Scroll to "Artifacts"
   - Download "app-debug" 
   - Extract and install the APK!

---

## ALTERNATIVE: Build on Your Computer

### Windows:
1. Download Android Studio: https://developer.android.com/studio
2. Install and open Android Studio
3. Click "Open" and select this project folder
4. Wait for Gradle sync
5. Click Build > Build Bundle(s) / APK(s) > Build APK(s)
6. Find APK at: app/build/outputs/apk/debug/app-debug.apk

### Mac/Linux:
```bash
# Install Android Studio or just Android SDK
brew install android-sdk  # Mac with Homebrew

# Navigate to project
cd whatsapp-contact-saver

# Create gradle wrapper
gradle wrapper --gradle-version 8.4

# Build APK
./gradlew assembleDebug

# APK at: app/build/outputs/apk/debug/app-debug.apk
```

---

## ONLINE BUILD SERVICES (No Installation)

### Option 1: Appetize.io Build
1. Go to https://appetize.io/upload
2. Upload the ZIP file
3. They build and provide download

### Option 2: AppCenter
1. Go to https://appcenter.ms
2. Create free account
3. Create new app (Android)
4. Connect to GitHub or upload directly
5. Build and download

---

## After Building - Installation

1. Transfer APK to your Android phone
2. Enable "Install from Unknown Sources" in Settings
3. Tap the APK file to install
4. Follow the in-app setup guide:
   - Enable Accessibility Service
   - Grant Overlay Permission
   - Grant Contacts Permission
5. Open WhatsApp and start scanning!

---

For questions: abdul@warysecure.com
