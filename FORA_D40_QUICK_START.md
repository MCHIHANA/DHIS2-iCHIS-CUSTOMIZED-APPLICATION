# FORA D40 Glucometer - Quick Start Guide

## Device Information

- **Model**: FORA D40 (TD-3261B V4)
- **MAC Address**: C0:26:DA:19:D4:FE
- **Service UUID**: 0x1808 (Glucose Service)
- **Characteristic**: 0x2A18 (Glucose Measurement)

## Quick Integration Checklist

### ✅ Completed

- [x] Added Glucose Service UUID (0x1808)
- [x] Added Glucose Measurement Characteristic UUID (0x2A18)
- [x] Implemented IEEE-11073 SFLOAT parser
- [x] Added glucose packet decoder
- [x] Extended BleConnectionManager with Glucose support
- [x] Added glucoseFlow to SensorRepository
- [x] Added GLUCOSE to SensorType enum
- [x] Implemented unit conversion (mmol/L → mg/dL)
- [x] Added comprehensive logging
- [x] Created documentation

### 🔄 Next Steps (UI Integration)

- [ ] Connect glucoseFlow to FormViewModel
- [ ] Add "Connect Glucose Sensor" button to form
- [ ] Display real-time glucose readings
- [ ] Save glucose readings to database
- [ ] Sync glucose readings to DHIS2 API
- [ ] Add glucose history view
- [ ] Implement error handling UI
- [ ] Add user notifications

## How to Use (Once UI is Integrated)

### Step 1: Open Form
Navigate to a DHIS2 form with a glucose data element.

### Step 2: Connect Sensor
Tap "Connect Glucose Sensor" button.

### Step 3: Wait for Connection
- App scans for FORA D40 (5-10 seconds)
- App connects automatically when found
- Status shows "Connected"

### Step 4: Take Measurement
1. Insert test strip into FORA D40
2. Apply blood sample to test strip
3. Wait for device to process (5-10 seconds)
4. Device beeps when complete

### Step 5: View Result
- Glucose value appears automatically in form field
- Value is in mg/dL
- Timestamp is recorded
- Reading is saved locally

### Step 6: Sync
- Reading syncs to DHIS2 backend automatically
- Patient history is updated

## Code Examples

### Observing Glucose Readings

```kotlin
// In ViewModel or Repository
sensorRepository.glucoseFlow.collect { glucose ->
    glucose?.let {
        Log.d("GLUCOSE", "Value: ${it.value} ${it.unit}")
        Log.d("GLUCOSE", "Timestamp: ${it.timestamp}")
        Log.d("GLUCOSE", "Sequence: ${it.sequenceNumber}")
        Log.d("GLUCOSE", "Location: ${it.typeSampleLocation}")
        
        // Update UI
        _glucoseValue.value = it.value.toString()
    }
}
```

### Connecting to FORA D40

```kotlin
// Scan for device
bleScanner.startScan { device ->
    if (device.address == BleHealthUUIDs.FORA_D40_GLUCOMETER_MAC) {
        // Connect
        sensorRepository.connect(device)
    }
}
```

### Parsing Glucose Packet

```kotlin
// Automatically handled by BleConnectionManager
// When glucose notification arrives:
val glucose = BleDataParser.parseGlucose(data)
// Returns: GlucoseReading(value=90.0, unit="mg/dL", ...)
```

## Expected Values

### Normal Glucose Ranges

| Condition | Range (mg/dL) |
|-----------|---------------|
| Fasting | 70-100 |
| Pre-meal | 70-130 |
| Post-meal (2h) | <180 |
| Bedtime | 100-140 |

### Validation

- **Minimum**: 20 mg/dL
- **Maximum**: 600 mg/dL
- **Typical**: 70-140 mg/dL

## Troubleshooting

### Sensor Not Found

1. Check Bluetooth is enabled
2. Ensure FORA D40 is powered on
3. Verify MAC address: C0:26:DA:19:D4:FE
4. Move phone closer to sensor
5. Restart Bluetooth

### No Readings

1. Insert test strip
2. Apply blood sample
3. Wait 5-10 seconds
4. Check sensor battery
5. Verify test strip is not expired

### Invalid Values

1. Check test strip expiration
2. Ensure proper blood sample size
3. Clean sensor contacts
4. Replace test strip

## Log Tags

Use these tags to debug:

```bash
adb logcat -s BLE_SCAN:D BLE_CONN:D BLE_SERVICE:D BLE_GLUCOSE:D SENSOR_DATA:D
```

## Testing

### Simulate Glucose Reading

```kotlin
// For testing without physical device
val testGlucose = BleConnectionManager.SensorData.Glucose(
    value = 95.0f,
    unit = "mg/dL",
    sequenceNumber = 1,
    timestamp = "2022-05-10 14:30:45",
    typeSampleLocation = "Capillary Whole blood, Finger"
)
```

## Build and Install

```bash
# Build APK
./gradlew :app:assembleDebug

# Install
adb install -r app/build/outputs/apk/dhis2/debug/app-dhis2-debug.apk

# View logs
adb logcat -s BLE_GLUCOSE:D
```

## Files Modified

1. `app/src/main/java/org/dhis2/sensor/ble/BleHealthUUIDs.kt`
2. `app/src/main/java/org/dhis2/sensor/ble/BleDataParser.kt`
3. `app/src/main/java/org/dhis2/sensor/ble/BleConnectionManager.kt`
4. `app/src/main/java/org/dhis2/sensor/ble/SensorRepository.kt`
5. `app/src/main/java/org/dhis2/sensors/SensorType.kt`
6. `app/src/main/java/org/dhis2/sensors/SensorManager.kt`

## Documentation Files

1. `FORA_D40_GLUCOMETER_INTEGRATION.md` - Complete technical documentation
2. `FORA_D40_QUICK_START.md` - This file

## Support

For issues or questions:
1. Check logs using tags above
2. Review `FORA_D40_GLUCOMETER_INTEGRATION.md`
3. Test with nRF Connect app
4. Verify device MAC address

---

**Status**: BLE Layer Complete ✅  
**Next**: UI Integration 🔄  
**Version**: 1.0  
**Date**: 2026-05-10
