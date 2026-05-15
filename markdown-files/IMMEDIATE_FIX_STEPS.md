# Immediate Fix Steps - Try These NOW

## Your Device is Correct
-  Device Name: FORA02
-  MAC: C0:26:DA:17:D5:7D (matches app)
-  Works in nRF Connect
-  Android 11 with Location enabled

## The Problem
The app shows "Waiting for sensor..." forever, which means the BLE scan is either:
1. Not starting (permission issue)
2. Starting but not finding the device (pairing issue)
3. Finding device but not matching it (code issue - unlikely)

## Try These Steps IN ORDER

### Step 1: UNPAIR the Device (CRITICAL!)
**This is probably the issue!**

1. Go to **Settings â†’ Bluetooth**
2. Look for **FORA02** or **C0:26:DA:17:D5:7D** in paired devices
3. If you see it, tap the gear icon â†’ **FORGET** or **UNPAIR**
4. Confirm

**Why?** When a device is paired/bonded, Android's BLE scanner sometimes doesn't report it in scan results. The app needs to discover it fresh.

### Step 2: Restart Bluetooth
1. Turn Bluetooth **OFF**
2. Wait 5 seconds
3. Turn Bluetooth **ON**

### Step 3: Restart the App
1. Force stop DHIS2 app (Settings â†’ Apps â†’ DHIS2 â†’ Force Stop)
2. Open app again
3. Navigate to form
4. Tap "Connect Sensor"

### Step 4: Watch the Dialog
It should now show:
- "Scanning for sensor..." (2-5 seconds)
- "Found 1 device(s), searching..." (1-2 seconds)
- "Connecting to sensor..." (2-3 seconds)
- "Connected - waiting for data..."

## If Still Not Working

### Check Permission Again
Even though you said Location is enabled, double-check:

1. **Settings â†’ Apps â†’ DHIS2 â†’ Permissions**
   - Location: Should say "Allowed" or "Allow all the time"
   - NOT "Allow only while using the app" - this might not work

2. **Settings â†’ Location**
   - Toggle should be ON
   - Mode should be "High accuracy" (not "Battery saving")

### Try Permission Reset
1. Settings â†’ Apps â†’ DHIS2 â†’ Permissions
2. Tap Location â†’ **Deny**
3. Go back to app
4. Tap "Connect Sensor"
5. When permission dialog appears, tap **Allow**

## Build and Install New Version

The new code has much better logging and error messages:

```bash
# Build
./gradlew :app:assembleDebug

# Install
adb install -r app/build/outputs/apk/dhis2/debug/app-dhis2-debug.apk
```

After installing:
1. Open app
2. Tap "Connect Sensor"
3. The dialog will now show detailed status messages
4. Watch what it says

## Get Logs

If you have adb access:

```bash
# Clear old logs
adb logcat -c

# Start logging
adb logcat -s BleManager:D BLE_SCAN:D BLE_DEVICE:D BLE_MATCH:D FormViewModel:D > sensor_log.txt

# Now try connecting in the app
# After it fails, stop the log (Ctrl+C)
# Send me the sensor_log.txt file
```

Look for these lines:
```
BleManager: === BLE SCAN STARTING ===
BleManager: Bluetooth adapter: true
BleManager: Bluetooth enabled: true
BleManager: BLE scanner: true
BLE_SCAN: Starting continuous unfiltered scan (LOW_LATENCY)...
```

If you see these, the scan started successfully.

Then look for:
```
BleManager: === DEVICE FOUND: FORA02 (C0:26:DA:17:D5:7D) ===
BLE_MATCH: FORA/O2 name matched: 'FORA02' (C0:26:DA:17:D5:7D)
BleManager: === TARGET SENSOR FOUND: SPO2 (C0:26:DA:17:D5:7D) ===
```

If you see these, the device was found and matched.

## Most Likely Solutions

### Solution 1: Unpair Device (90% chance this is it)
Your device is BONDED according to nRF Connect. This means it's paired. Unpair it from Bluetooth settings.

### Solution 2: Location Permission Mode
Change Location permission from "Allow only while using app" to "Allow all the time"

### Solution 3: Location Mode
Change Location mode from "Battery saving" to "High accuracy"

## Quick Test

After unpairing and restarting:

1. Turn on FORA02
2. Open DHIS2 app
3. Go to form
4. Tap "Connect Sensor"
5. Count to 10
6. Should connect by then

If it doesn't connect within 10 seconds, something else is wrong.

## What to Report Back

Tell me:
1.  or  - Did you unpair the device?
2.  or  - Did the dialog show different messages after unpairing?
3. What message is the dialog stuck on?
4. If you have logs, what do they show?

## Emergency Workaround

If nothing works and you need to enter data NOW:

1. Open nRF Connect
2. Connect to FORA02
3. Enable notifications on characteristic 00001524...
4. Place finger on sensor
5. Read the values from nRF Connect
6. Manually type them into the DHIS2 form fields

The values in nRF Connect will be in hex. To convert:
- Byte 1 (second byte) = SpO2 percentage
- Bytes 2-3 = Pulse rate (little-endian)

Example: `00 5F 48 00 00`
- Byte 1: `5F` = 95 decimal = 95% SpO2
- Bytes 2-3: `48 00` = 72 decimal = 72 bpm

## Next Steps

1. **UNPAIR the device** - do this first!
2. **Rebuild and install** the new version
3. **Try connecting** - watch the status messages
4. **Report back** what you see

The new version will tell you exactly what's happening!
