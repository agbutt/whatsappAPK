# Quick Start Guide - Server Integration

## What's New?

The WhatsApp Contact Saver now includes **Server Integration** - automatically sync contacts from a remote server to your device!

## 5-Minute Setup

### Step 1: Get Your API Key
Contact your administrator to get:
- Server URL (e.g., https://joinus.cx)
- 64-character API Key

### Step 2: Open Settings
1. Launch WhatsApp Contact Saver
2. Tap the **⚙️ Settings** button (top-right)

### Step 3: Configure Server
1. Enter **Server URL**: `https://joinus.cx`
2. Paste your **API Key**
3. Tap **Test Connection** ✓
4. You should see: "Connection successful"

### Step 4: Enable Auto-Sync (Optional)
1. Check **Enable Auto Sync**
2. Select interval: **15 minutes** (recommended)
3. Check **Sync on App Start** (optional)

### Step 5: Customize (Optional)
1. Change **Contact Prefix** if desired (default: `JU_`)
2. Contacts will be saved as: JU_001, JU_002, etc.

### Step 6: Save
1. Tap **Save Settings**
2. Return to main screen

### Step 7: First Sync
1. Tap **🔄 Sync from Server** button
2. Wait for sync to complete
3. Check notification for results

## That's It!

Your contacts will now automatically sync from the server based on your settings.

## Check Sync Status

On the main screen, you'll see:
- **Status**: ● Connected / Not Configured
- **Server**: Your server URL
- **Last sync**: Time since last sync
- **Pending**: Contacts waiting on server
- **Synced**: Successfully saved contacts
- **Failed**: Any sync failures

## Manual Sync Anytime

Just tap the **🔄 Sync from Server** button on the main screen.

## Troubleshooting

### Connection Failed?
- Check your internet connection
- Verify Server URL is correct
- Ensure API Key is complete (64 characters)
- Test Connection in Settings

### No Contacts Syncing?
- Check "Pending" count on main screen
- If 0, there are no contacts to sync
- Contact your administrator to add contacts

### Duplicates Created?
- The app automatically skips duplicates
- If you see duplicates, clear app data and reconfigure

## Need Help?

1. Check **SERVER_API_GUIDE.md** for detailed documentation
2. Review the main **README.md** for general info
3. Contact your administrator for API issues
4. Contact Wary Secure Ltd for app support

---

**Pro Tip**: Enable auto-sync and forget about it - the app will automatically fetch and save new contacts in the background!
