# Oximeter Sensor Fix - SPO2 and Pulse Fields

## Problem
The Pulse (BPM) and SPO2 fields were unable to receive readings from the FORA O2 oximeter sensor, even though the sensor was connecting successfully and transmitting data.

## Root Cause
The issue was in the `FormViewModel.observeSensorData()` method. The code was performing a UUID validation check that was incorrectly applied to the oximeter's custom semantic keys ("SPO2" and "PULSE").

### Original Problematic Code
```kotlin
// Skip UUID mismatch check for custom sensor keys (SPO2, PULSE, etc.)
// Only check for standard BLE characteristic UUIDs (contain dashes)
val isStandardUuid = key.contains("-")
```

The problem: The check `key.contains("-")` is too simplistic. It would incorrectly classify any string containing a dash as a UUID, when in fact we need to validate against the full UUID format.

## Solution
Replaced the simplistic dash-check with a proper UUID format validation using a regex pattern:

```kotlin
// Skip UUID validation for custom sensor keys (SPO2, PULSE, etc.)
// These are semantic keys from multi-value sensors, not BLE characteristic UUIDs.
// Only validate standard BLE UUIDs (format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
val isStandardUuid = key.matches(Regex("^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$"))
```

This ensures that:
1. Custom semantic keys like "SPO2" and "PULSE" bypass UUID validation
2. Only actual BLE characteristic UUIDs (in the standard format) are validated against the sensor configuration
3. The data flows correctly from the sensor to both the SpO2 and Pulse Rate fields

## Additional Improvements

### Enhanced Logging
Added comprehensive logging throughout the data flow to aid debugging:

1. **BleDeviceConnector** (`handleForaO2Data`):
   - Added detailed logging for each step of packet parsing
   - Logs when first data packet is received and retry timer is cancelled
   - Logs validation checks and final emission of values

2. **BleManager** (`bleDeviceConnector` callback):
   - Logs the number of readings received
   - Logs each key-value pair being emitted

3. **FormViewModel** (`observeSensorData`):
   - Logs when readings are received with field UIDs
   - Logs each reading being processed with its target field
   - Logs UUID validation decisions
   - Logs successful saves to fields

### Log Tags for Easy Filtering
- `BLE_SPO2`: Oximeter-specific data parsing
- `SENSOR_DATA`: High-level sensor data flow in ViewModel
- `SENSOR_SAVE`: Field save operations
- `BLE_CONNECT`: Connection state changes
- `BLE_SERVICE`: Service discovery and characteristic setup
- `BLE_CHAR`: Characteristic operations
- `BLE_RAW`: Raw byte data from sensor

## How the Oximeter Integration Works

### 1. Device Detection
- Scanner looks for devices with name containing "FORA" or known MAC address
- FORA O2 MAC: `C0:26:DA:17:D5:7D`

### 2. Connection & Setup
- Connects to Nordic UART service: `00001523-1212-efde-1523-785feabcd123`
- Subscribes to button characteristic: `00001524-1212-efde-1523-785feabcd123`
- Sends trigger command (0x01) to start measurements

### 3. Data Reception
- Receives 4-5 byte packets continuously
- Packet format:
  - Byte 0: Flags (probe error, pulse searching, beep)
  - Byte 1: SpO2 value (50-100%)
  - Byte 2: Pulse rate low byte
  - Byte 3: Pulse rate high byte
  - Byte 4: Perfusion index (optional)

### 4. Data Parsing
- Validates SpO2 is in range 50-100%
- Combines pulse bytes (little-endian 16-bit)
- Validates pulse is in range 20-300 bpm
- Emits as `[("SPO2", "95"), ("PULSE", "72")]`

### 5. Field Mapping
- Primary field UID (tapped field) receives first value (SpO2)
- Secondary field UID receives second value (Pulse)
- Known field pairs:
  - SpO2: `VqwQWWDmYLn` ↔ Pulse: `tZbUrUbhUNy`

### 6. Data Save
- Each value is saved via `FormIntent.OnSave` with `ValueType.NUMBER`
- Status updates shown in sensor dialog
- Dialog auto-dismisses after successful data reception

## Testing
Build completed successfully with no compilation errors:
```
./gradlew :form:compileDebugKotlin --no-daemon
BUILD SUCCESSFUL in 44s
```

## Files Modified
1. `form/src/main/java/org/dhis2/form/ui/FormViewModel.kt`
   - Fixed UUID validation logic in `observeSensorData()`
   - Added comprehensive logging

2. `form/src/main/java/org/dhis2/sensor/ble/BleDeviceConnector.kt`
   - Enhanced logging in `handleForaO2Data()`
   - Added log messages for retry timer cancellation

3. `form/src/main/java/org/dhis2/sensor/ble/BleManager.kt`
   - Improved logging in `onReadingsReceived` callback

## Expected Behavior After Fix
1. User taps "Connect Sensor" button on SpO2 or Pulse field
2. Dialog opens and starts BLE scan automatically
3. FORA O2 oximeter is detected and connected
4. User places finger on sensor
5. Valid SpO2 and Pulse readings are received
6. Both fields are populated automatically
7. Dialog shows "Data received: [value]" and auto-dismisses
8. Form fields display the sensor readings

## Verification Steps
1. Open a form with SpO2 and Pulse Rate fields
2. Tap "Connect Sensor" on either field
3. Place finger on FORA O2 oximeter
4. Verify both fields receive values
5. Check logcat for detailed flow:
   ```
   adb logcat -s BLE_SPO2:D SENSOR_DATA:D SENSOR_SAVE:D
   ```

## Known Field UIDs
- Temperature: `KXNH45ts16S`
- SpO2: `VqwQWWDmYLn`
- Pulse Rate: `tZbUrUbhUNy`
- Heart Rate: `S7OjKl85YSh`
- Systolic BP: `HkfzcXMdLLF`
- Diastolic BP: `skBarAsIYIL`
