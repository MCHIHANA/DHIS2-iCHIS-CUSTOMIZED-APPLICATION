# Diagnostic for Your FORA02 Device

## Your Device Details (Confirmed via nRF Connect)
- **Device Name:** FORA02
- **MAC Address:** C0:26:DA:17:D5:7D  (Matches app configuration)
- **Connection:** Works in nRF Connect 
- **Bonding:** BONDED 

## App Configuration Status
 MAC address matches: `C0:26:DA:17:D5:7D` is in `KnownDevices.kt`
 Device name "FORA02" contains "O2" - should be detected
 Device can connect (proven by nRF Connect)

## Why It's Not Working in the App

Since your device works perfectly in nRF Connect but not in the DHIS2 app, the issue is **NOT** the sensor. The problem is one of these:

### 1. Permissions Issue (Most Likely)
The app doesn't have the required permissions to scan for BLE devices.

**Check Now:**
1. Go to: **Settings → Apps → DHIS2 → Permissions**
2. Verify these are **ALLOWED**:
   - **Nearby devices** (or Bluetooth on older Android)
   - **Location** (CRITICAL - required for BLE scanning)

3. Also check: **Settings → Location**
   - Location services must be **ON**

**Why Location?** Android requires Location permission for BLE scanning because BLE can be used to determine location (beacon tracking). Even though we're not using it for location, Android enforces this.

### 2. Bluetooth State
**Check:**
- Bluetooth is ON in system settings
- No other app is connected to the FORA02
- Device is NOT paired in Bluetooth settings (should be unpaired)

### 3. App Not Requesting Permissions
The app should request permissions when you first tap "Connect Sensor". If it doesn't:
- Uninstall the app
- Reinstall
- When you tap "Connect Sensor", grant ALL permissions

## What the New Code Does

I've added extensive improvements:

### Better Error Messages
The dialog will now show:
- "Scanning for sensor..." - Scan started
- "Found X device(s), searching..." - Devices detected
- "Connecting to sensor..." - Your FORA02 found
- "Connected - waiting for data..." - Place finger now
- "Error: [message]" - If something fails

### Better Logging
The app now logs every step:
- `BLE_SCAN: Starting continuous unfiltered scan`
- `BLE_DEVICE: Found: name='FORA02' mac=C0:26:DA:17:D5:7D`
- `BLE_MATCH: FORA/O2 name matched: 'FORA02'`
- `BLE_CONNECT: Connecting to C0:26:DA:17:D5:7D`

### Safety Checks
- Checks if Bluetooth adapter exists
- Checks if Bluetooth is enabled
- Checks if scanner is available
- Catches permission errors
- Shows error messages in dialog

## Testing Steps

### Step 1: Rebuild and Install
```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/dhis2/debug/app-dhis2-debug.apk
```

### Step 2: Grant Permissions
1. Open DHIS2 app
2. Navigate to form with SpO2/Pulse fields
3. Tap "Connect Sensor"
4. **IMPORTANT:** When permission dialog appears, tap "Allow"
5. If no permission dialog appears, go to Settings → Apps → DHIS2 → Permissions and grant manually

### Step 3: Test Connection
1. Ensure FORA02 is powered on
2. Tap "Connect Sensor" button
3. Watch the status messages
4. Should see: "Scanning..." → "Found 1 device(s)..." → "Connecting..." → "Connected..."
5. Place finger on sensor
6. Should see: "Data received: XX"
7. Both fields should fill

### Step 4: If Still Not Working

**Enable USB Debugging:**
1. Settings → About Phone → Tap "Build Number" 7 times
2. Settings → Developer Options → USB Debugging → ON
3. Connect phone to computer
4. Run: `adb devices` (should show your device)

**Capture Logs:**
```bash
# Clear old logs
adb logcat -c

# Start capturing
adb logcat > sensor_debug.log

# In another terminal, or just let it run
# Now try connecting to sensor in the app
# After it fails, stop the log (Ctrl+C)
```

**Check the log file for:**
```
BLE_SCAN: Starting continuous unfiltered scan
```
- If you see this: Scan started successfully
- If you don't see this: Scan never started (permission issue)

```
BLE_DEVICE: Found: name='FORA02'
```
- If you see this: Device detected
- If you don't see this: Device not being discovered (permission or Bluetooth issue)

```
BLE_MATCH: FORA/O2 name matched
```
- If you see this: Device matched and should connect
- If you don't see this: Name matching failed (shouldn't happen with "FORA02")

```
SecurityException
```
- If you see this: PERMISSIONS NOT GRANTED - this is the problem

## Most Likely Solution

Based on your symptoms ("Waiting for sensor..." forever), the issue is **99% likely to be permissions**.

**Fix:**
1. Go to Settings → Apps → DHIS2 → Permissions
2. Grant **Location** permission (CRITICAL)
3. Grant **Nearby devices** permission
4. Go to Settings → Location → Turn ON
5. Try again

## Verification

After granting permissions, you should see in the logs:
```
FormViewModel: startSensorScan called: primary=VqwQWWDmYLn, secondary=tZbUrUbhUNy
FormViewModel: Starting BLE scan via bleManager
BleManager: startScan called - clearing device list and starting BLE scan
BLE_SCAN: Starting continuous unfiltered scan (LOW_LATENCY)...
BLE_DEVICE: Found: name='FORA02' mac=C0:26:DA:17:D5:7D services=[...]
BLE_MATCH: FORA/O2 name matched: 'FORA02' (C0:26:DA:17:D5:7D)
BLE_SCAN: Scan stopped
BleManager: Target sensor detected: SPO2 (C0:26:DA:17:D5:7D) — connecting now
BLE_CONNECT: Connecting to C0:26:DA:17:D5:7D (type=SPO2)...
BLE_CONNECT: Connected to C0:26:DA:17:D5:7D
```

## If Permissions Are Already Granted

If you've confirmed permissions are granted and it still doesn't work:

1. **Check Android version:**
   - Android 12+: Needs BLUETOOTH_SCAN, BLUETOOTH_CONNECT
   - Android 11-: Needs ACCESS_FINE_LOCATION

2. **Check Location Services:**
   - Must be ON system-wide
   - Not just permission, but the actual Location toggle

3. **Check Bluetooth:**
   - Must be ON
   - Try turning off and on again

4. **Check for conflicts:**
   - Close nRF Connect if running
   - Unpair FORA02 from Bluetooth settings
   - Restart phone

5. **Check app manifest:**
   - Verify permissions are declared in AndroidManifest.xml
   - Should have BLUETOOTH_SCAN, BLUETOOTH_CONNECT (Android 12+)
   - Should have ACCESS_FINE_LOCATION (Android 11-)

## Expected Behavior After Fix

1. Tap "Connect Sensor"
2. Dialog shows "Scanning for sensor..." (1-2 seconds)
3. Dialog shows "Found 1 device(s), searching for sensor..." (1-2 seconds)
4. Dialog shows "Connecting to sensor..." (2-3 seconds)
5. Dialog shows "Connected - waiting for data..." (immediate)
6. Place finger on sensor
7. Wait 5-10 seconds for stable reading
8. Dialog shows "Data received: 95" (or similar)
9. Dialog closes automatically
10. Both SpO2 and Pulse fields are filled

**Total time: 10-20 seconds**

## Contact Information

If after all this it still doesn't work, provide:
1. Android version
2. Phone model
3. Screenshot of app permissions screen
4. Screenshot of Location settings
5. Log file (sensor_debug.log)
6. Screenshot of dialog showing status message

The logs will tell us exactly what's happening!
