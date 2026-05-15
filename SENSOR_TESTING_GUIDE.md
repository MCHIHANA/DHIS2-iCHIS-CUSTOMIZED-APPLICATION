# Sensor Testing Guide

## Quick Test Procedure

### Prerequisites
- FORA O2 Pulse Oximeter (MAC: C0:26:DA:17:D5:7D)
- Android device with Bluetooth enabled
- DHIS2 Android app with sensor integration
- Form containing SpO2 and Pulse Rate fields

### Test Steps

1. **Open Form**
   - Navigate to a data entry form
   - Locate SpO2 field (UID: VqwQWWDmYLn)
   - Locate Pulse Rate field (UID: tZbUrUbhUNy)

2. **Connect Sensor**
   - Tap "Connect Sensor" button on either field
   - Dialog opens with "Waiting for sensor..." message
   - BLE scan starts automatically

3. **Use Oximeter**
   - Turn on FORA O2 oximeter
   - Place finger on sensor
   - Wait for stable reading (LED stops flashing)

4. **Verify Data Reception**
   - Dialog shows "Connecting..." when device found
   - Dialog shows "Data received: [value]" for each field
   - Dialog auto-dismisses after ~1 second
   - Both SpO2 and Pulse fields populated with values

### Expected Results
-  SpO2 field shows value between 50-100%
-  Pulse field shows value between 20-300 bpm
-  Both fields populated from single sensor connection
-  Dialog dismisses automatically
-  No error messages

### Troubleshooting

#### Sensor Not Detected
**Symptoms:** Dialog stays on "Waiting for sensor..."

**Solutions:**
1. Check Bluetooth is enabled
2. Check location permission granted (required for BLE on Android)
3. Ensure oximeter is powered on and advertising
4. Check logcat for scan results:
   ```bash
   adb logcat -s BLE_SCAN:D BLE_DEVICE:D BLE_MATCH:D
   ```

#### Connection Fails
**Symptoms:** Dialog shows "Connecting..." but never connects

**Solutions:**
1. Move device closer to oximeter
2. Restart oximeter
3. Check logcat for connection errors:
   ```bash
   adb logcat -s BLE_CONNECT:D
   ```

#### No Data Received
**Symptoms:** Connected but no values appear

**Solutions:**
1. Ensure finger is properly placed on sensor
2. Wait for oximeter LED to stabilize
3. Check logcat for data parsing:
   ```bash
   adb logcat -s BLE_SPO2:D BLE_RAW:D
   ```

#### Only One Field Populated
**Symptoms:** SpO2 OR Pulse filled, but not both

**Solutions:**
1. Check field UIDs match expected values
2. Verify secondary field UID is passed to dialog
3. Check logcat for field mapping:
   ```bash
   adb logcat -s SENSOR_DATA:D SENSOR_SAVE:D
   ```

### Debug Logging

#### View All Sensor Logs
```bash
adb logcat -s BLE_SPO2:D SENSOR_DATA:D SENSOR_SAVE:D BLE_CONNECT:D BLE_SERVICE:D BLE_CHAR:D BLE_RAW:D BLE_SCAN:D BLE_DEVICE:D BLE_MATCH:D
```

#### View Only Critical Logs
```bash
adb logcat -s BLE_SPO2:D SENSOR_DATA:D SENSOR_SAVE:D
```

#### Save Logs to File
```bash
adb logcat -s BLE_SPO2:D SENSOR_DATA:D SENSOR_SAVE:D > sensor_test.log
```

### Sample Log Output (Successful Connection)

```
BLE_SCAN: Starting continuous unfiltered scan (LOW_LATENCY)...
BLE_DEVICE: Found: name='FORA O2' mac=C0:26:DA:17:D5:7D services=[00001523-1212-efde-1523-785feabcd123]
BLE_MATCH: FORA name matched: 'FORA O2' (C0:26:DA:17:D5:7D)
BLE_SCAN: Scan stopped
BLE_CONNECT: Connecting to C0:26:DA:17:D5:7D (type=SPO2)...
BLE_CONNECT: Connected to C0:26:DA:17:D5:7D
BLE_SERVICE: Services discovered (status=0) for SPO2
BLE_SERVICE: Found FORA O2 characteristic: 00001524-1212-efde-1523-785feabcd123, properties=24
BLE_SERVICE: No CCCD on 00001524-1212-efde-1523-785feabcd123 — sending trigger directly
BLE_SERVICE: Sending trigger command (0x01) to FORA O2 [WRITE_WITHOUT_RESPONSE]...
BLE_RAW: [00001524-1212-EFDE-1523-785FEABCD123] 00 5F 48 00 00
BLE_SPO2: FORA O2 raw (5B): 00 5F 48 00 00
BLE_SPO2: First data packet received — cancelling retry timer
BLE_SPO2:  Valid reading: SpO2=95% Pulse=72 bpm — emitting to ViewModel
BleManager: Readings received from device: 2 values
BleManager:   → SPO2 = 95
BleManager:   → PULSE = 72
SENSOR_DATA: Received 2 readings for primary field: VqwQWWDmYLn, secondary: tZbUrUbhUNy
SENSOR_DATA: Processing reading 0: key=SPO2, value=95, targetField=VqwQWWDmYLn
SENSOR_SAVE: Saving SPO2=95 to field VqwQWWDmYLn
SENSOR_DATA: Processing reading 1: key=PULSE, value=72, targetField=tZbUrUbhUNy
SENSOR_SAVE: Saving PULSE=72 to field tZbUrUbhUNy
```

## Other Sensors

### FORA IR42 Thermometer
- **Type:** Temperature
- **MAC:** C0:26:DA:1B:06:A4
- **Service:** Health Thermometer (0x1809)
- **Characteristic:** Temperature Measurement (0x2A1C)
- **Field UID:** KXNH45ts16S
- **Value Range:** 30.0 - 45.0°C

### Blood Pressure Monitor
- **Type:** Blood Pressure
- **MAC:** C0:26:DA:19:D4:FE
- **Service:** Blood Pressure (0x1810)
- **Characteristic:** Blood Pressure Measurement (0x2A35)
- **Field UIDs:** 
  - Systolic: HkfzcXMdLLF
  - Diastolic: skBarAsIYIL
- **Value Range:** 40-250 mmHg

## Permissions Required

### Android 12+ (API 31+)
- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`

### Android 11 and below
- `ACCESS_FINE_LOCATION`
- `BLUETOOTH`
- `BLUETOOTH_ADMIN`

## Common Issues

### Issue: "Bluetooth permission denied"
**Solution:** Grant Bluetooth permissions in app settings

### Issue: "No sensor found. Try again."
**Solution:** 
1. Ensure sensor is powered on
2. Check sensor is in pairing/advertising mode
3. Move closer to sensor
4. Restart sensor

### Issue: "Data received" but fields empty
**Solution:**
1. Check field UIDs match configuration
2. Verify field is editable
3. Check form is not read-only
4. Review logcat for save errors

### Issue: Dialog doesn't open
**Solution:**
1. Check BLE is supported on device
2. Verify field has sensor configuration
3. Check field UID is in known sensor UIDs list
4. Verify field is editable

## Performance Notes

- **Scan Duration:** Continuous until sensor found or user cancels
- **Connection Time:** ~2-5 seconds
- **Data Reception:** Immediate after finger placement
- **Auto-dismiss Delay:** 1.2 seconds after data received
- **Retry Logic:** Automatic retry with different write type if no data in 2 seconds

## Field Configuration

Fields are identified as sensor-compatible by:
1. **DataStore configuration** (primary method)
2. **Hardcoded UID list** (fallback)
3. **Label keyword matching** (last resort)

### Label Keywords
- temperature
- weight
- heart rate / heartrate
- systolic / diastolic
- blood pressure
- spo2 / sp02
- oxygen / saturation
- pulse / bpm
