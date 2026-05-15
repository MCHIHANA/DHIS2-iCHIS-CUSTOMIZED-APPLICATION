# Oximeter Troubleshooting Guide

## Issue: "Waiting for sensor..." - No Connection

If the dialog shows "Waiting for sensor..." or "Scanning for sensor..." but never connects, follow these steps:

### Step 1: Check Bluetooth Permissions

**Android 12+ (API 31+):**
1. Go to Settings â†’ Apps â†’ DHIS2 App â†’ Permissions
2. Ensure these permissions are granted:
   - **Nearby devices** (BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
   - **Location** (required for BLE scanning)

**Android 11 and below:**
1. Go to Settings â†’ Apps â†’ DHIS2 App â†’ Permissions
2. Ensure **Location** permission is granted
3. Ensure Location services are enabled system-wide

### Step 2: Check Bluetooth is Enabled
1. Swipe down notification panel
2. Ensure Bluetooth icon is ON (blue/highlighted)
3. If off, tap to enable

### Step 3: Check Oximeter is Powered On
1. Press power button on FORA O2 oximeter
2. Verify display lights up
3. Device should be in idle state (not measuring)

### Step 4: Check Oximeter MAC Address

The app is configured to look for:
- **Known MAC:** `C0:26:DA:17:D5:7D`
- **Device name containing:** "FORA", "O2", or "IR42"
- **Service UUID:** `00001523-1212-efde-1523-785feabcd123` (Nordic UART)

**If your oximeter has a different MAC address:**

1. Find your oximeter's MAC address:
   - Use a BLE scanner app (e.g., "nRF Connect" from Nordic Semiconductor)
   - Look for device named "FORA O2" or similar
   - Note the MAC address (format: XX:XX:XX:XX:XX:XX)

2. Add it to the app:
   - Open `form/src/main/java/org/dhis2/sensor/ble/KnownDevices.kt`
   - Add your MAC address:
     ```kotlin
     const val SPO2_SENSOR = "YOUR:MAC:ADDRESS:HERE"
     ```
   - Rebuild the app

### Step 5: Verify Field Configuration

The app needs to know which fields are for SpO2 and Pulse:

**Check field UIDs in your form:**
1. SpO2 field UID should be: `VqwQWWDmYLn`
2. Pulse Rate field UID should be: `tZbUrUbhUNy`

**If your field UIDs are different:**

Edit `form/src/main/java/org/dhis2/form/ui/FormView.kt` around line 250:
```kotlin
val SPO2_UID  = "YOUR_SPO2_FIELD_UID"
val PULSE_UID = "YOUR_PULSE_FIELD_UID"
```

### Step 6: Check Status Messages

The dialog now shows detailed status messages:

| Message | Meaning | Action |
|---------|---------|--------|
| "Initializing..." | Dialog just opened | Wait 1-2 seconds |
| "Scanning for sensor..." | BLE scan started | Ensure oximeter is on |
| "Found X device(s), searching..." | Devices detected but not matched | Check MAC/name matching |
| "Connecting to sensor..." | Target device found | Wait for connection |
| "Connected - waiting for data..." | Connected successfully | Place finger on sensor |
| "Data received: XX" | Reading received | Success! Dialog will close |

### Step 7: Enable Debug Logging

To see what's happening, enable Android logging:

1. **Enable Developer Options:**
   - Settings â†’ About Phone â†’ Tap "Build Number" 7 times

2. **Connect device to computer via USB**

3. **View logs** (if you have Android SDK installed):
   ```bash
   adb logcat -s BLE_SCAN:D BLE_DEVICE:D BLE_MATCH:D BLE_CONNECT:D BLE_SERVICE:D BLE_SPO2:D SENSOR_DATA:D FormViewModel:D
   ```

4. **Look for these log messages:**
   - `BLE_SCAN: Starting continuous unfiltered scan` - Scan started
   - `BLE_DEVICE: Found: name='...' mac=...` - Devices being discovered
   - `BLE_MATCH: ... matched` - Target device found
   - `BLE_CONNECT: Connecting to ...` - Connection attempt
   - `BLE_CONNECT: Connected to ...` - Connection successful
   - `BLE_SPO2:  Valid reading: SpO2=...` - Data received

## Common Issues and Solutions

### Issue: "Found X device(s), searching for sensor..."

**Cause:** Devices are being discovered but none match the criteria.

**Solutions:**
1. Check your oximeter's name contains "FORA", "O2", or "IR42"
2. Check your oximeter's MAC address is in `KnownDevices.kt`
3. Check your oximeter advertises the Nordic UART service UUID

### Issue: "Connecting to sensor..." (stuck)

**Cause:** Connection attempt failing.

**Solutions:**
1. Move phone closer to oximeter (within 1 meter)
2. Restart oximeter (power off/on)
3. Restart Bluetooth on phone
4. Clear Bluetooth cache: Settings â†’ Apps â†’ Bluetooth â†’ Storage â†’ Clear Cache

### Issue: "Connected - waiting for data..." (no readings)

**Cause:** Connected but sensor not sending data.

**Solutions:**
1. **Place finger on sensor properly:**
   - Insert finger fully into sensor
   - Ensure good contact with both LED and detector
   - Keep finger still

2. **Wait for sensor to stabilize:**
   - FORA O2 takes 5-10 seconds to get a stable reading
   - LED will flash while searching for pulse
   - LED becomes steady when reading is stable

3. **Check sensor battery:**
   - Low battery can cause connection but no data
   - Replace batteries if needed

### Issue: Only SpO2 OR Pulse filled (not both)

**Cause:** Secondary field UID not configured correctly.

**Solution:**
Check `FormView.kt` around line 250:
```kotlin
val secondaryUid: String? = when (uid) {
    SPO2_UID  -> PULSE_UID   // tapped SpO2  â†’ secondary is pulse
    PULSE_UID -> SPO2_UID    // tapped pulse â†’ secondary is SpO2
    else -> { /* fallback logic */ }
}
```

Ensure both UIDs are correct for your form.

### Issue: Dialog closes immediately

**Cause:** Auto-dismiss triggered incorrectly.

**Check:** Status message should show "Data received: XX" before closing.

**If closing without data:**
- Check logs for errors
- Verify field UIDs are correct
- Ensure fields are editable (not read-only)

## Testing Checklist

Before reporting an issue, verify:

- [ ] Bluetooth is enabled
- [ ] Location permission granted
- [ ] Location services enabled (Android 11 and below)
- [ ] Oximeter is powered on
- [ ] Oximeter battery is good
- [ ] Phone is within 1 meter of oximeter
- [ ] Finger is properly placed on sensor
- [ ] Field UIDs match configuration
- [ ] Fields are editable (not read-only)
- [ ] App has latest code changes

## Manual Testing Steps

1. **Open form with SpO2 and Pulse fields**
2. **Tap "Connect Sensor" button** on either field
3. **Observe status messages** in dialog
4. **Turn on oximeter** if not already on
5. **Wait for "Connecting..."** message (should appear within 5-10 seconds)
6. **Wait for "Connected - waiting for data..."** message
7. **Place finger on sensor**
8. **Wait for stable reading** (LED stops flashing)
9. **Verify "Data received: XX"** appears
10. **Verify both fields populated**
11. **Verify dialog auto-closes**

## Expected Timeline

- Dialog opens: **Immediate**
- Scan starts: **< 1 second**
- Device found: **2-10 seconds** (depends on advertising interval)
- Connection: **2-5 seconds**
- Data received: **5-15 seconds** (after finger placement)
- Dialog closes: **1.2 seconds** after data received

**Total time:** 10-30 seconds from opening dialog to fields populated

## If Still Not Working

1. **Capture logs:**
   ```bash
   adb logcat > oximeter_debug.log
   ```
   (Run this before opening the sensor dialog, then reproduce the issue)

2. **Check logs for:**
   - Permission denials
   - Bluetooth errors
   - Connection failures
   - Data parsing errors

3. **Try with nRF Connect app:**
   - Install "nRF Connect" from Play Store
   - Scan for devices
   - Find your FORA O2 oximeter
   - Connect to it
   - Check if you can see services and characteristics
   - Try to enable notifications on characteristic `00001524-1212-efde-1523-785feabcd123`
   - Check if you receive data

4. **If nRF Connect works but app doesn't:**
   - Compare MAC address
   - Compare service UUIDs
   - Check if device name matches expected patterns

5. **Report issue with:**
   - Android version
   - Phone model
   - Oximeter model and MAC address
   - Log file
   - Screenshots of status messages
   - nRF Connect screenshots (if available)

## Quick Fixes

### Reset Bluetooth
```
Settings â†’ System â†’ Reset Options â†’ Reset Wi-Fi, mobile & Bluetooth
```
(Note: This will forget all paired Bluetooth devices)

### Clear App Data
```
Settings â†’ Apps â†’ DHIS2 App â†’ Storage â†’ Clear Data
```
(Note: This will reset all app settings)

### Reinstall App
1. Uninstall DHIS2 app
2. Restart phone
3. Reinstall app
4. Grant all permissions
5. Try again

## Known Limitations

1. **One sensor at a time:** Can only connect to one sensor per dialog
2. **No pairing required:** Sensor should NOT be paired in Bluetooth settings
3. **Continuous scan:** Scan runs until sensor found or user cancels
4. **Auto-connect:** Automatically connects when target sensor found
5. **Single reading:** Dialog closes after first valid reading (not continuous monitoring)

## Advanced Debugging

### Check if BLE is supported
```kotlin
val bleAvailable = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
```

### Check Bluetooth adapter state
```kotlin
val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
val adapter = bluetoothManager.adapter
val isEnabled = adapter?.isEnabled == true
```

### Check permissions at runtime
```kotlin
val hasScanPermission = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.BLUETOOTH_SCAN
) == PackageManager.PERMISSION_GRANTED
```

### Force sensor type
If auto-detection fails, you can force the sensor type in `BleManager.connectDevice()`:
```kotlin
connectDevice(device, SensorType.SPO2)  // Force SPO2 type
```
