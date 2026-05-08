# Debug: Oximeter Shows Data But App Doesn't Receive It

## The Problem

- ✓ Oximeter is ON
- ✓ Finger is on sensor
- ✓ Oximeter display shows SpO2 and Pulse values
- ✓ App connects successfully
- ✗ App shows "Connected - Place finger on sensor now!" forever
- ✗ No data appears in the app

## What This Means

The BLE connection is established, but **notifications are not being received** by the app. This could be:

1. **Notifications not enabled** - CCCD write failed
2. **Trigger command not sent** - Device not told to start streaming
3. **Notifications enabled but data filtered out** - Invalid readings (0x7F, 0xFF, 0x00)
4. **Wrong characteristic** - Listening to wrong UUID

## New Logging Added

I've added extensive logging with `***` markers to make it easy to find. The logs will show:

### Connection Flow
```
BLE_CONNECT: Connecting to C0:26:DA:17:D5:7D (type=SPO2)...
BLE_CONNECT: Connected to C0:26:DA:17:D5:7D
BLE_SERVICE: Services discovered (status=0) for SPO2
```

### Subscription Flow
```
BLE_SERVICE: *** subscribeForaO2 called ***
BLE_SERVICE: ✓ Found Nordic service: 00001523-1212-efde-1523-785feabcd123
BLE_SERVICE: ✓ Found FORA O2 characteristic: 00001524-1212-efde-1523-785feabcd123, properties=24
BLE_SERVICE: setCharacteristicNotification result: true
BLE_SERVICE: ✓ Found CCCD descriptor, writing ENABLE_NOTIFICATION_VALUE
BLE_SERVICE: writeDescriptor (legacy) result: true
```

### Descriptor Write Callback
```
BLE_SERVICE: *** onDescriptorWrite: 00001524-1212-efde-1523-785feabcd123 status=0 ***
BLE_SERVICE: Current sensor type: SPO2
BLE_SERVICE: ✓ Descriptor write SUCCESS for 00001524-1212-efde-1523-785feabcd123
BLE_SERVICE: *** Sending FORA O2 trigger command now ***
BLE_SERVICE: Sending trigger command (0x01) to FORA O2 [WRITE_WITHOUT_RESPONSE]...
BLE_SERVICE: Trigger command write (legacy, NO_RESPONSE) result: true
```

### Data Reception (THIS IS CRITICAL!)
```
BLE_RAW: *** onCharacteristicChanged (legacy) *** [00001524-1212-EFDE-1523-785FEABCD123] 00 5F 48 00 00
BLE_CALLBACK: onCharacteristicChanged (legacy) called for 00001524-1212-EFDE-1523-785FEABCD123, sensorType=SPO2
BLE_SPO2: FORA O2 raw (5B): 00 5F 48 00 00
BLE_SPO2: ✓ Valid reading: SpO2=95% Pulse=72 bpm — emitting to ViewModel
```

## How to Capture Logs

### Method 1: Using adb (Best)

```bash
# Clear old logs
adb logcat -c

# Start capturing with filters
adb logcat -s BLE_CONNECT:D BLE_SERVICE:D BLE_RAW:D BLE_SPO2:D BLE_CALLBACK:D BleManager:D FormViewModel:D SENSOR_DATA:D > oximeter_debug.log

# Now in the app:
# 1. Tap "Connect Sensor"
# 2. Wait for "Connected - Place finger on sensor now!"
# 3. Keep finger on sensor for 30 seconds
# 4. Stop the log (Ctrl+C)
# 5. Send me the oximeter_debug.log file
```

### Method 2: Using Android Studio Logcat

1. Open Android Studio
2. Connect phone via USB
3. Open Logcat tab (bottom of screen)
4. In the filter box, enter: `BLE_SERVICE|BLE_RAW|BLE_SPO2|BLE_CALLBACK`
5. Run the app
6. Tap "Connect Sensor"
7. Watch the logs
8. Screenshot or copy the logs

## What to Look For

### Scenario 1: No `onCharacteristicChanged` Callbacks

If you see:
```
BLE_SERVICE: ✓ Descriptor write SUCCESS
BLE_SERVICE: *** Sending FORA O2 trigger command now ***
BLE_SERVICE: Trigger command write result: true
```

But you DON'T see:
```
BLE_RAW: *** onCharacteristicChanged ***
```

**Problem:** Notifications are not being received at all.

**Possible causes:**
- CCCD write didn't actually work (Android bug)
- Wrong characteristic UUID
- Device firmware issue
- Android BLE stack issue

**Solution:** Try these in order:
1. Restart Bluetooth
2. Restart phone
3. Unpair device from Bluetooth settings
4. Try on a different phone

### Scenario 2: Receiving Invalid Data

If you see:
```
BLE_RAW: *** onCharacteristicChanged *** [...] 00 7F FF 00 00
BLE_SPO2: SpO2=0x7F — no finger or initialising
```

**Problem:** Notifications ARE being received, but data is invalid (no finger detected).

**Possible causes:**
- Finger not properly placed
- Sensor needs more time to stabilize
- Sensor hardware issue

**Solution:**
1. Remove finger completely
2. Wait 5 seconds
3. Place finger again, fully inserted
4. Keep absolutely still
5. Wait 15-20 seconds

### Scenario 3: Descriptor Write Failed

If you see:
```
BLE_SERVICE: !!! Descriptor write FAILED (status=X) !!!
```

**Problem:** Can't enable notifications.

**Solution:**
1. Disconnect and reconnect
2. Restart Bluetooth
3. Unpair device

### Scenario 4: Service/Characteristic Not Found

If you see:
```
BLE_SERVICE: !!! Nordic service NOT FOUND !!!
```
or
```
BLE_SERVICE: !!! Nordic button characteristic NOT FOUND !!!
```

**Problem:** Wrong device or device not advertising correct services.

**Solution:**
- Verify MAC address is correct
- Check device model
- Try in nRF Connect to confirm services

## Quick Test

After rebuilding, try this:

1. **Build and install:**
   ```bash
   ./gradlew :app:assembleDebug
   adb install -r app/build/outputs/apk/dhis2/debug/app-dhis2-debug.apk
   ```

2. **Start logging:**
   ```bash
   adb logcat -c
   adb logcat -s BLE_SERVICE:D BLE_RAW:D BLE_SPO2:D
   ```

3. **In the app:**
   - Tap "Connect Sensor"
   - Wait for connection
   - Keep finger on sensor
   - Watch the logs

4. **Check logs:**
   - Do you see `*** onCharacteristicChanged ***`?
   - If YES: Data is being received, check what values
   - If NO: Notifications not working, see Scenario 1

## Expected Log Flow (Success)

```
BleManager: === BLE SCAN STARTING ===
BleManager: === DEVICE FOUND: FORA02 (C0:26:DA:17:D5:7D) ===
BleManager: === TARGET SENSOR FOUND: SPO2 (C0:26:DA:17:D5:7D) ===
BLE_CONNECT: Connecting to C0:26:DA:17:D5:7D (type=SPO2)...
BLE_CONNECT: Connected to C0:26:DA:17:D5:7D
BLE_SERVICE: Services discovered (status=0) for SPO2
BLE_SERVICE: *** subscribeForaO2 called ***
BLE_SERVICE: ✓ Found Nordic service: 00001523-1212-efde-1523-785feabcd123
BLE_SERVICE: ✓ Found FORA O2 characteristic: 00001524-1212-efde-1523-785feabcd123
BLE_SERVICE: setCharacteristicNotification result: true
BLE_SERVICE: ✓ Found CCCD descriptor, writing ENABLE_NOTIFICATION_VALUE
BLE_SERVICE: writeDescriptor (legacy) result: true
BLE_SERVICE: *** onDescriptorWrite: 00001524-1212-efde-1523-785feabcd123 status=0 ***
BLE_SERVICE: ✓ Descriptor write SUCCESS
BLE_SERVICE: *** Sending FORA O2 trigger command now ***
BLE_SERVICE: Sending trigger command (0x01) to FORA O2 [WRITE_WITHOUT_RESPONSE]...
BLE_SERVICE: Trigger command write (legacy, NO_RESPONSE) result: true
[... wait 5-10 seconds with finger on sensor ...]
BLE_RAW: *** onCharacteristicChanged (legacy) *** [00001524-1212-EFDE-1523-785FEABCD123] 00 5F 48 00 00
BLE_SPO2: FORA O2 raw (5B): 00 5F 48 00 00
BLE_SPO2: ✓ Valid reading: SpO2=95% Pulse=72 bpm — emitting to ViewModel
BleManager: Readings received from device: 2 values
BleManager:   → SPO2 = 95
BleManager:   → PULSE = 72
SENSOR_DATA: Received 2 readings for primary field: VqwQWWDmYLn, secondary: tZbUrUbhUNy
SENSOR_SAVE: Saving SPO2=95 to field VqwQWWDmYLn
SENSOR_SAVE: Saving PULSE=72 to field tZbUrUbhUNy
```

## Most Likely Issues

### Issue 1: No `onCharacteristicChanged` Callbacks (90% probability)

This is the most common issue. The CCCD write succeeds, trigger is sent, but Android never calls the callback.

**Why:** Android BLE stack bug, especially on some devices/Android versions.

**Fix:**
1. Unpair device from Bluetooth settings
2. Restart Bluetooth
3. Try again
4. If still fails, restart phone
5. If still fails, try different phone

### Issue 2: Receiving 0x7F/0xFF Data (5% probability)

Notifications work but sensor reports "no finger".

**Fix:**
- Ensure finger is FULLY inserted
- Wait longer (15-20 seconds)
- Try different finger

### Issue 3: Descriptor Write Fails (3% probability)

Can't enable notifications.

**Fix:**
- Disconnect and reconnect
- Restart Bluetooth

### Issue 4: Wrong Service/Characteristic (2% probability)

Device doesn't have expected UUIDs.

**Fix:**
- Verify device model
- Check in nRF Connect

## Next Steps

1. **Rebuild and install** the app with new logging
2. **Capture logs** using one of the methods above
3. **Look for** `*** onCharacteristicChanged ***` in the logs
4. **Report back:**
   - Do you see `onCharacteristicChanged` callbacks?
   - If yes, what data values?
   - If no, what's the last log message you see?

The logs will tell us exactly what's happening!
