# Quick Diagnostic Checklist - Oximeter Not Working

## Immediate Checks (Do These First!)

### 1. Permissions ✓
- [ ] Open Settings → Apps → DHIS2 → Permissions
- [ ] **Nearby devices** permission: GRANTED
- [ ] **Location** permission: GRANTED
- [ ] Location services: ON (Settings → Location)

### 2. Bluetooth ✓
- [ ] Swipe down notification panel
- [ ] Bluetooth icon is ON (blue/highlighted)
- [ ] If off, tap to turn on

### 3. Oximeter ✓
- [ ] Press power button
- [ ] Display lights up
- [ ] Battery indicator shows charge
- [ ] Device is NOT paired in Bluetooth settings (should be unpaired)

### 4. Field Configuration ✓
Open the form and check:
- [ ] SpO2 field has "Connect Sensor" button
- [ ] Pulse field has "Connect Sensor" button
- [ ] Fields are NOT grayed out (editable)

## Watch the Status Messages

When you tap "Connect Sensor", the dialog should show these messages in order:

1. **"Initializing..."** (1 second)
   - ✓ Normal - dialog just opened

2. **"Scanning for sensor..."** (2-10 seconds)
   - ✓ Normal - looking for oximeter
   - ❌ If stuck here > 15 seconds: Oximeter not advertising
     - Turn oximeter off and on again
     - Check oximeter battery
     - Move phone closer to oximeter

3. **"Found X device(s), searching..."** (appears if other BLE devices nearby)
   - ✓ Normal - devices detected, still looking for oximeter
   - ❌ If stuck here: Your oximeter doesn't match expected criteria
     - Check MAC address (see below)
     - Check device name (see below)

4. **"Connecting to sensor..."** (2-5 seconds)
   - ✓ Normal - oximeter found, connecting
   - ❌ If stuck here > 10 seconds: Connection failing
     - Move phone closer
     - Restart oximeter
     - Restart Bluetooth on phone

5. **"Connected - waiting for data..."** (5-15 seconds)
   - ✓ Normal - connected, waiting for finger
   - **ACTION REQUIRED:** Place finger on sensor NOW
   - ❌ If stuck here > 30 seconds: Sensor not sending data
     - Ensure finger is fully inserted
     - Keep finger still
     - Wait for LED to stop flashing

6. **"Data received: XX"** (appears briefly)
   - ✓ SUCCESS! Dialog will close automatically
   - Both fields should be filled

## If Stuck at "Scanning for sensor..."

Your oximeter might have a different MAC address or name. Check:

### Expected Configuration:
- **MAC Address:** C0:26:DA:17:D5:7D
- **Device Name:** Contains "FORA", "O2", or "IR42"
- **Service UUID:** 00001523-1212-efde-1523-785feabcd123

### Find Your Oximeter's Details:

1. **Install nRF Connect app** (free from Play Store)
2. **Open nRF Connect** → Tap "SCAN"
3. **Turn on your oximeter**
4. **Look for device** named "FORA O2" or similar
5. **Note the MAC address** (format: XX:XX:XX:XX:XX:XX)
6. **Tap the device** → Check "ADVERTISED SERVICES"
7. **Look for service** 00001523-1212-efde-1523-785feabcd123

### If MAC Address is Different:

**Option A: Add your MAC to the app (requires rebuild)**
1. Open `form/src/main/java/org/dhis2/sensor/ble/KnownDevices.kt`
2. Change line 12:
   ```kotlin
   const val SPO2_SENSOR = "YOUR:MAC:ADDRESS:HERE"
   ```
3. Rebuild and install app

**Option B: Rename your oximeter (if supported)**
- Some FORA devices allow renaming via their mobile app
- Rename to include "FORA" or "O2" in the name

## If Stuck at "Connected - waiting for data..."

The sensor is connected but not sending readings. This usually means:

### Finger Placement Issues:
- [ ] Finger is fully inserted into sensor
- [ ] Fingertip touches both LED and detector
- [ ] Finger is not moving
- [ ] No nail polish or dirt on finger
- [ ] Try different finger (index finger works best)

### Sensor Issues:
- [ ] LED is flashing (searching for pulse)
- [ ] Wait 10-15 seconds for LED to stabilize
- [ ] Battery is not low (check display)
- [ ] Sensor is clean (no dust in measurement area)

### App Issues:
- [ ] Field UIDs are correct (see below)
- [ ] Fields are editable (not read-only)
- [ ] No error messages in logs

## Field UID Configuration

The app expects these specific field UIDs:

| Field | Expected UID |
|-------|--------------|
| SpO2 | VqwQWWDmYLn |
| Pulse Rate | tZbUrUbhUNy |

### Check Your Field UIDs:

1. **In DHIS2 web interface:**
   - Go to Maintenance → Data Element
   - Search for your SpO2 field
   - Copy the UID
   - Search for your Pulse field
   - Copy the UID

2. **If UIDs are different:**
   - Open `form/src/main/java/org/dhis2/form/ui/FormView.kt`
   - Find lines around 250:
     ```kotlin
     val SPO2_UID  = "VqwQWWDmYLn"
     val PULSE_UID = "tZbUrUbhUNy"
     ```
   - Replace with your actual UIDs
   - Rebuild and install app

## Quick Test with nRF Connect

To verify your oximeter is working:

1. **Install nRF Connect** from Play Store
2. **Open app** → Tap "SCAN"
3. **Turn on oximeter**
4. **Find your device** in the list
5. **Tap "CONNECT"**
6. **Wait for connection**
7. **Expand service** 00001523-1212-efde-1523-785feabcd123
8. **Find characteristic** 00001524-1212-efde-1523-785feabcd123
9. **Tap the ↓ icon** (enable notifications)
10. **Place finger on sensor**
11. **Watch for data** in "Value" field

**If you see data in nRF Connect:**
- Your oximeter is working correctly
- Problem is in the app configuration
- Check MAC address and field UIDs

**If you don't see data in nRF Connect:**
- Your oximeter has a hardware issue
- Try new batteries
- Try a different oximeter

## Emergency Workaround

If you need to enter data manually while troubleshooting:

1. Use nRF Connect to read values from sensor
2. Manually type values into SpO2 and Pulse fields
3. Continue with form submission

## Get Help

If none of the above works, collect this information:

1. **Phone details:**
   - Model: _______________
   - Android version: _______________

2. **Oximeter details:**
   - Model: _______________
   - MAC address: _______________
   - Device name: _______________

3. **Status message when stuck:**
   - Message: _______________
   - How long stuck: _______________

4. **nRF Connect test:**
   - Can connect: YES / NO
   - Can see data: YES / NO

5. **Screenshots:**
   - Sensor dialog showing status
   - nRF Connect showing device
   - Permissions screen

6. **Logs** (if possible):
   ```bash
   adb logcat -d > oximeter_log.txt
   ```

## Success Indicators

You'll know it's working when:
- ✓ Dialog shows "Connecting..." within 10 seconds
- ✓ Dialog shows "Connected - waiting for data..."
- ✓ After placing finger, dialog shows "Data received: XX"
- ✓ Both SpO2 and Pulse fields are filled
- ✓ Dialog closes automatically
- ✓ Values are reasonable (SpO2: 90-100%, Pulse: 50-120 bpm)

## Typical Timeline

| Step | Expected Time |
|------|---------------|
| Open dialog | Immediate |
| Start scanning | < 1 second |
| Find device | 2-10 seconds |
| Connect | 2-5 seconds |
| Place finger | User action |
| Get reading | 5-15 seconds |
| Fill fields | < 1 second |
| Close dialog | 1.2 seconds |
| **TOTAL** | **10-35 seconds** |

If any step takes significantly longer, refer to the troubleshooting section above.
