# Blood Pressure Sensor Testing Guide

## Overview

This guide provides comprehensive testing procedures for the FORA D40b Blood Pressure sensor integration.

---

## Prerequisites

### Hardware
- ✅ FORA D40b Blood Pressure Monitor (MAC: C0:26:DA:19:D4:FE)
- ✅ Android device with BLE support (Android 4.3+ / API 18+)
- ✅ Fresh batteries in BP monitor

### Software
- ✅ DHIS2 Android app with BP sensor integration
- ✅ Android Studio with Logcat access
- ✅ nRF Connect app (for BLE verification)

### Permissions
Ensure the following permissions are granted:
- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`
- `ACCESS_FINE_LOCATION` (Android <12)

---

## Pre-Testing Verification

### 1. Verify BLE Advertisement (using nRF Connect)

1. Open nRF Connect app
2. Start scanning
3. Look for device with MAC: `C0:26:DA:19:D4:FE`
4. Verify advertised services include:
   - Blood Pressure Service: `0x1810`
5. Connect and verify characteristics:
   - Blood Pressure Measurement: `0x2A35` (INDICATE property)

### 2. Verify Datastore Configuration

Check that the sensor configuration is loaded:

```kotlin
// In Android Studio Logcat, filter by "SensorConfig"
// You should see:
SensorConfig: Loaded sensors: 3
SensorConfig: Blood Pressure
SensorConfig: 00001810-0000-1000-8000-00805f9b34fb
```

### 3. Verify Known Devices

```kotlin
// Verify MAC address is registered
Log.d("TEST", "BP MAC: ${KnownDevices.BP_SENSOR}")
// Should output: C0:26:DA:19:D4:FE

Log.d("TEST", "Type: ${KnownDevices.typeFor(KnownDevices.BP_SENSOR)}")
// Should output: BLOOD_PRESSURE
```

---

## Test Cases

### Test 1: Basic Connection Flow

**Objective**: Verify the app can scan, discover, and connect to the BP sensor.

**Steps**:
1. Open DHIS2 form with BP fields (systolic, diastolic, pulse)
2. Click "Connect Blood Pressure" button
3. Turn on FORA D40b (should power on automatically when cuff is inflated)
4. Observe connection process

**Expected Logcat Output**:
```
BLE_SCAN: Starting continuous unfiltered scan (LOW_LATENCY)...
BLE_DEVICE: Found: name='FORA D40b' mac=C0:26:DA:19:D4:FE services=[00001810-...]
BLE_MATCH: Blood Pressure service UUID matched: C0:26:DA:19:D4:FE
BLE_SCAN: Scan stopped
BLE_CONNECT: Connecting to C0:26:DA:19:D4:FE (type=BLOOD_PRESSURE)...
BLE_CONNECT: Connected to C0:26:DA:19:D4:FE
BLE_SERVICE: Services discovered (status=0) for BLOOD_PRESSURE
BLE_SERVICE: ✓ Found Blood Pressure service: 00001810-...
BLE_SERVICE: ✓ Found BP Measurement characteristic: 00002a35-..., properties=32
BLE_SERVICE: ✓ Found CCCD descriptor, writing ENABLE_INDICATION_VALUE
BLE_SERVICE: *** onDescriptorWrite: 00002a35-... status=0 ***
BLE_SERVICE: ✓ Descriptor write SUCCESS for 00002a35-...
BLE_SERVICE: Waiting for data from sensor...
```

**Expected UI**:
- Status: "Connecting to sensor..."
- Status: "Connected - Place finger on sensor now!"

**Pass Criteria**:
- ✅ Scan starts
- ✅ Device discovered within 5 seconds
- ✅ Connection established
- ✅ Services discovered
- ✅ Characteristic subscribed
- ✅ No errors in Logcat

---

### Test 2: Blood Pressure Measurement

**Objective**: Verify the app can receive and parse BP measurements.

**Steps**:
1. Ensure sensor is connected (from Test 1)
2. Place cuff on arm
3. Press START button on FORA D40b
4. Wait for measurement to complete (cuff inflates, deflates, beeps)
5. Observe data reception

**Expected Logcat Output**:
```
BLE_RAW: *** onCharacteristicChanged (API 33+) *** [00002A35-...] 00 78 00 50 00 5E 00 4B 00
BLE_BP: === Blood Pressure Packet (9 bytes) ===
BLE_BP: Raw: 00 78 00 50 00 5E 00 4B 00
BLE_BP: Flags: 0x0
BLE_BP:   Units: mmHg
BLE_BP:   Timestamp: false
BLE_BP:   Pulse Rate: false
BLE_BP:   User ID: false
BLE_BP:   Status: false
BLE_BP: SFLOAT[offset=1]: raw=0x78, mantissa=120, exponent=0, value=120.0
BLE_BP: Systolic: 120.0 mmHg
BLE_BP: SFLOAT[offset=3]: raw=0x50, mantissa=80, exponent=0, value=80.0
BLE_BP: Diastolic: 80.0 mmHg
BLE_BP: SFLOAT[offset=5]: raw=0x5e, mantissa=94, exponent=0, value=94.0
BLE_BP: MAP: 94.0 mmHg
BLE_BP: === Final Values (mmHg) ===
BLE_BP: Systolic: 120.0 mmHg
BLE_BP: Diastolic: 80.0 mmHg
BLE_BP: MAP: 94.0 mmHg
BLE_BP: ✓ Valid BP reading: 120/80 mmHg
SENSOR_DATA: Received 2 readings for primary field: HkfzcXMdLLF
SENSOR_DATA: Processing reading 0: key=SYSTOLIC, value=120, targetField=HkfzcXMdLLF
SENSOR_SAVE: Saving SYSTOLIC=120 to field HkfzcXMdLLF
SENSOR_DATA: Processing reading 1: key=DIASTOLIC, value=80, targetField=skBarAsIYIL
SENSOR_SAVE: Saving DIASTOLIC=80 to field skBarAsIYIL
```

**Expected UI**:
- Systolic field: `120`
- Diastolic field: `80`
- Status: "Data received: 120" (systolic field)
- Status: "Data received: 80" (diastolic field)

**Pass Criteria**:
- ✅ Notification received
- ✅ Packet parsed correctly
- ✅ Values in valid range (50-250 systolic, 30-150 diastolic)
- ✅ Both fields populated
- ✅ Values match device display

---

### Test 3: Blood Pressure with Pulse Rate

**Objective**: Verify pulse rate is captured when present.

**Steps**:
1. Connect to sensor
2. Take BP measurement
3. Check if pulse rate is included in packet

**Expected Logcat Output** (if pulse present):
```
BLE_BP: Flags: 0x4
BLE_BP:   Pulse Rate: true
BLE_BP: SFLOAT[offset=7]: raw=0x4b, mantissa=75, exponent=0, value=75.0
BLE_BP: Pulse Rate: 75.0 bpm
BLE_BP:   Pulse: 75 bpm
SENSOR_DATA: Received 3 readings for primary field: HkfzcXMdLLF
SENSOR_SAVE: Saving SYSTOLIC=120 to field HkfzcXMdLLF
SENSOR_SAVE: Saving DIASTOLIC=80 to field skBarAsIYIL
SENSOR_SAVE: Saving PULSE=75 to field tZbUrUbhUNy
```

**Expected UI**:
- Systolic field: `120`
- Diastolic field: `80`
- Pulse field: `75`

**Pass Criteria**:
- ✅ Three readings received
- ✅ Pulse rate in valid range (30-250 bpm)
- ✅ All three fields populated

**Note**: Some BP monitors send pulse rate in a separate packet or don't send it at all. This is device-dependent.

---

### Test 4: Multiple Consecutive Measurements

**Objective**: Verify the app can handle multiple measurements without reconnection.

**Steps**:
1. Connect to sensor
2. Take first BP measurement
3. Wait 30 seconds
4. Take second BP measurement
5. Wait 30 seconds
6. Take third BP measurement

**Pass Criteria**:
- ✅ All three measurements received
- ✅ No disconnection between measurements
- ✅ Fields updated with each new reading
- ✅ No memory leaks (check Android Profiler)

---

### Test 5: Connection Loss Recovery

**Objective**: Verify the app handles connection loss gracefully.

**Steps**:
1. Connect to sensor
2. Turn off sensor (or move out of range)
3. Observe disconnection
4. Turn sensor back on
5. Click "Connect Blood Pressure" again

**Expected Logcat Output**:
```
BLE_CONNECT: Disconnected from C0:26:DA:19:D4:FE (status=8)
```

**Expected UI**:
- Status: "Disconnected"

**Pass Criteria**:
- ✅ Disconnection detected
- ✅ UI updated to show disconnected state
- ✅ Can reconnect successfully
- ✅ No app crash

---

### Test 6: Scan Timeout

**Objective**: Verify the app handles sensor not found scenario.

**Steps**:
1. Ensure BP sensor is OFF
2. Click "Connect Blood Pressure" button
3. Wait for scan timeout (if implemented)

**Expected Behavior**:
- Scan continues until user dismisses dialog
- OR timeout after configured duration
- User can manually enter values

**Pass Criteria**:
- ✅ No crash
- ✅ User can dismiss scan dialog
- ✅ Manual entry still works

---

### Test 7: Bluetooth Disabled

**Objective**: Verify error handling when Bluetooth is off.

**Steps**:
1. Disable Bluetooth in Android settings
2. Click "Connect Blood Pressure" button

**Expected Logcat Output**:
```
BLE_SCAN: Bluetooth is disabled - cannot scan
```

**Expected UI**:
- Error message: "Bluetooth is disabled"
- OR prompt to enable Bluetooth

**Pass Criteria**:
- ✅ No crash
- ✅ Clear error message
- ✅ User can enable Bluetooth and retry

---

### Test 8: Permission Denial

**Objective**: Verify error handling when BLE permissions are denied.

**Steps**:
1. Revoke BLUETOOTH_SCAN permission
2. Click "Connect Blood Pressure" button

**Expected Logcat Output**:
```
BLE_SCAN: SecurityException starting scan - missing permissions?
```

**Expected UI**:
- Permission request dialog
- OR error message about missing permissions

**Pass Criteria**:
- ✅ No crash
- ✅ Permission requested
- ✅ Works after permission granted

---

### Test 9: Invalid Data Handling

**Objective**: Verify the app rejects out-of-range values.

**Test Data** (simulated):
```kotlin
// Systolic too high
val invalidPacket1 = byteArrayOf(0x00, 0xFA.toByte(), 0x00, 0x50, 0x00, 0x5E, 0x00)
// Expected: 250 mmHg systolic - should be rejected (>250)

// Diastolic too low
val invalidPacket2 = byteArrayOf(0x00, 0x78, 0x00, 0x14, 0x00, 0x5E, 0x00)
// Expected: 20 mmHg diastolic - should be rejected (<30)
```

**Expected Logcat Output**:
```
BLE_BP: Blood pressure values out of range — skipping
```

**Pass Criteria**:
- ✅ Invalid values rejected
- ✅ No fields populated with invalid data
- ✅ No crash

---

### Test 10: kPa to mmHg Conversion

**Objective**: Verify unit conversion when device sends kPa.

**Test Data** (simulated):
```kotlin
// Flags byte with bit 0 set (kPa units)
val kpaPacket = byteArrayOf(0x01, 0x10, 0x00, 0x0B, 0x00, 0x0D, 0x00)
// 16 kPa systolic = 120 mmHg
// 11 kPa diastolic = 82.5 mmHg
```

**Expected Logcat Output**:
```
BLE_BP: Flags: 0x1
BLE_BP:   Units: kPa
BLE_BP: Systolic: 16.0 kPa
BLE_BP: Diastolic: 11.0 kPa
BLE_BP: === Final Values (mmHg) ===
BLE_BP: Systolic: 120.0 mmHg
BLE_BP: Diastolic: 82.5 mmHg
```

**Pass Criteria**:
- ✅ kPa detected from flags
- ✅ Conversion applied (1 kPa = 7.50062 mmHg)
- ✅ Values in mmHg saved to fields

---

### Test 11: IEEE-11073 SFLOAT Edge Cases

**Objective**: Verify SFLOAT parsing handles special values.

**Test Cases**:

#### NaN (Not a Number)
```kotlin
val nanPacket = byteArrayOf(0x00, 0xFF.toByte(), 0x07, 0x50, 0x00, 0x5E, 0x00)
// Mantissa = 0x07FF (NaN)
```

**Expected**: Value rejected, no field update

#### Negative Exponent
```kotlin
val negExpPacket = byteArrayOf(0x00, 0xB8.toByte(), 0xF0.toByte(), 0x50, 0x00, 0x5E, 0x00)
// Mantissa = 1200, Exponent = -1 → 1200 × 10^-1 = 120.0
```

**Expected**: 120 mmHg systolic

**Pass Criteria**:
- ✅ NaN handled correctly
- ✅ Negative exponents work
- ✅ Sign extension correct

---

### Test 12: Backward Compatibility

**Objective**: Verify legacy config format still works.

**Legacy Config**:
```json
{
  "name": "Blood Pressure",
  "dataElements": {
    "systolic": "HkfzcXMdLLF",
    "diastolic": "skBarAsIYIL"
  }
}
```

**Steps**:
1. Load legacy config
2. Connect to sensor
3. Take measurement

**Pass Criteria**:
- ✅ Config loaded successfully
- ✅ Fields populated correctly
- ✅ No errors

---

## Performance Testing

### Test 13: Scan Performance

**Objective**: Measure time to discover sensor.

**Steps**:
1. Start scan
2. Note timestamp
3. Wait for device found
4. Note timestamp

**Metrics**:
- Discovery time: < 5 seconds (typical)
- CPU usage: < 10% during scan
- Battery drain: < 1% per scan

---

### Test 14: Connection Performance

**Objective**: Measure connection establishment time.

**Metrics**:
- Connection time: < 3 seconds
- Service discovery: < 2 seconds
- Total time to ready: < 5 seconds

---

### Test 15: Memory Usage

**Objective**: Verify no memory leaks.

**Steps**:
1. Open Android Studio Profiler
2. Connect/disconnect 10 times
3. Take 10 measurements
4. Check memory graph

**Pass Criteria**:
- ✅ No memory leaks
- ✅ Memory returns to baseline after disconnect
- ✅ No retained objects

---

## Integration Testing

### Test 16: End-to-End Form Submission

**Objective**: Verify BP data is saved to DHIS2.

**Steps**:
1. Open DHIS2 event/enrollment form
2. Connect to BP sensor
3. Take measurement
4. Verify fields populated
5. Save form
6. Sync with server
7. Verify data on DHIS2 web

**Pass Criteria**:
- ✅ Data saved locally
- ✅ Data synced to server
- ✅ Data visible on DHIS2 web
- ✅ Correct data element UIDs
- ✅ Correct units

---

### Test 17: Multi-Sensor Workflow

**Objective**: Verify BP sensor works alongside other sensors.

**Steps**:
1. Take temperature reading
2. Take SpO2 reading
3. Take BP reading
4. Verify all fields populated correctly

**Pass Criteria**:
- ✅ No interference between sensors
- ✅ Correct values in correct fields
- ✅ No data mixing

---

## Regression Testing

### Test 18: Existing Sensors Still Work

**Objective**: Verify BP integration didn't break existing sensors.

**Steps**:
1. Test temperature sensor (FORA IR42)
2. Test SpO2 sensor (FORA O2)
3. Verify both still work

**Pass Criteria**:
- ✅ Temperature sensor works
- ✅ SpO2 sensor works
- ✅ No regressions

---

## Edge Cases

### Test 19: Rapid Measurements

**Objective**: Verify app handles rapid consecutive measurements.

**Steps**:
1. Connect to sensor
2. Take 5 measurements back-to-back (30 sec apart)

**Pass Criteria**:
- ✅ All measurements received
- ✅ No data loss
- ✅ No UI freezing

---

### Test 20: App Backgrounding

**Objective**: Verify connection survives app backgrounding.

**Steps**:
1. Connect to sensor
2. Press Home button (app goes to background)
3. Wait 30 seconds
4. Return to app
5. Take measurement

**Pass Criteria**:
- ✅ Connection maintained
- OR ✅ Graceful reconnection
- ✅ Measurement received

---

## Automated Testing

### Unit Test: SFLOAT Parsing

```kotlin
@Test
fun `parseSFloat should handle positive values`() {
    val data = byteArrayOf(0x00, 0x78, 0x00) // 120 with exponent 0
    val result = BleDataParser.parseSFloat(data, 1)
    assertEquals(120.0f, result, 0.01f)
}

@Test
fun `parseSFloat should handle negative exponent`() {
    val data = byteArrayOf(0x00, 0xB8.toByte(), 0xF0.toByte()) // 1200 × 10^-1
    val result = BleDataParser.parseSFloat(data, 1)
    assertEquals(120.0f, result, 0.01f)
}

@Test
fun `parseSFloat should handle NaN`() {
    val data = byteArrayOf(0x00, 0xFF.toByte(), 0x07) // NaN
    val result = BleDataParser.parseSFloat(data, 1)
    assertEquals(0.0f, result, 0.01f)
}
```

### Unit Test: Blood Pressure Parsing

```kotlin
@Test
fun `parseBloodPressure should extract systolic and diastolic`() {
    val data = byteArrayOf(
        0x00,                    // Flags: mmHg, no optional fields
        0x78, 0x00,             // Systolic: 120
        0x50, 0x00,             // Diastolic: 80
        0x5E, 0x00              // MAP: 94
    )
    val result = BleDataParser.parseBloodPressure(data)
    assertEquals(120.0f, result.systolic, 0.01f)
    assertEquals(80.0f, result.diastolic, 0.01f)
    assertNull(result.pulseRate)
}

@Test
fun `parseBloodPressure should extract pulse rate when present`() {
    val data = byteArrayOf(
        0x04,                    // Flags: mmHg, pulse rate present
        0x78, 0x00,             // Systolic: 120
        0x50, 0x00,             // Diastolic: 80
        0x5E, 0x00,             // MAP: 94
        0x4B, 0x00              // Pulse: 75
    )
    val result = BleDataParser.parseBloodPressure(data)
    assertEquals(120.0f, result.systolic, 0.01f)
    assertEquals(80.0f, result.diastolic, 0.01f)
    assertEquals(75.0f, result.pulseRate, 0.01f)
}

@Test
fun `parseBloodPressure should convert kPa to mmHg`() {
    val data = byteArrayOf(
        0x01,                    // Flags: kPa units
        0x10, 0x00,             // Systolic: 16 kPa
        0x0B, 0x00,             // Diastolic: 11 kPa
        0x0D, 0x00              // MAP: 13 kPa
    )
    val result = BleDataParser.parseBloodPressure(data)
    assertEquals(120.0f, result.systolic, 1.0f)  // 16 × 7.50062 ≈ 120
    assertEquals(82.5f, result.diastolic, 1.0f)  // 11 × 7.50062 ≈ 82.5
}
```

---

## Test Report Template

```markdown
# Blood Pressure Sensor Test Report

**Date**: YYYY-MM-DD
**Tester**: [Name]
**Device**: [Android device model]
**Android Version**: [e.g., Android 13]
**App Version**: [e.g., 1.0.0]
**BP Monitor**: FORA D40b (MAC: C0:26:DA:19:D4:FE)

## Test Results

| Test # | Test Name | Status | Notes |
|--------|-----------|--------|-------|
| 1 | Basic Connection | ✅ PASS | Connected in 3s |
| 2 | BP Measurement | ✅ PASS | 120/80 received |
| 3 | Pulse Rate | ✅ PASS | 75 bpm received |
| 4 | Multiple Measurements | ✅ PASS | 3 readings OK |
| 5 | Connection Loss | ✅ PASS | Reconnected OK |
| 6 | Scan Timeout | ✅ PASS | Manual entry works |
| 7 | Bluetooth Disabled | ✅ PASS | Error shown |
| 8 | Permission Denial | ✅ PASS | Permission requested |
| 9 | Invalid Data | ✅ PASS | Rejected correctly |
| 10 | kPa Conversion | ⚠️ SKIP | Device sends mmHg only |
| 11 | SFLOAT Edge Cases | ✅ PASS | NaN handled |
| 12 | Backward Compat | ✅ PASS | Legacy config works |
| 13 | Scan Performance | ✅ PASS | 2.5s discovery |
| 14 | Connection Perf | ✅ PASS | 4s total |
| 15 | Memory Usage | ✅ PASS | No leaks |
| 16 | E2E Form Submit | ✅ PASS | Data synced |
| 17 | Multi-Sensor | ✅ PASS | No interference |
| 18 | Existing Sensors | ✅ PASS | Temp/SpO2 OK |
| 19 | Rapid Measurements | ✅ PASS | 5 readings OK |
| 20 | App Backgrounding | ✅ PASS | Connection maintained |

## Issues Found

1. [Issue description]
2. [Issue description]

## Recommendations

1. [Recommendation]
2. [Recommendation]

## Conclusion

Overall Status: ✅ PASS / ⚠️ PARTIAL / ❌ FAIL

[Summary paragraph]
```

---

## Troubleshooting

### Issue: Sensor not discovered

**Possible Causes**:
- Sensor is off
- Sensor battery dead
- Bluetooth disabled
- Out of range
- Permissions missing

**Solutions**:
1. Check sensor is powered on
2. Replace batteries
3. Enable Bluetooth
4. Move closer to sensor
5. Grant BLE permissions

---

### Issue: Connection fails

**Possible Causes**:
- Sensor already connected to another device
- BLE stack error
- Android BLE cache corruption

**Solutions**:
1. Disconnect from other devices
2. Restart app
3. Clear Bluetooth cache (Settings → Apps → Bluetooth → Clear Cache)
4. Restart Android device

---

### Issue: No data received

**Possible Causes**:
- Characteristic not subscribed
- CCCD write failed
- Sensor not sending data

**Solutions**:
1. Check Logcat for subscription errors
2. Verify INDICATE property on characteristic
3. Take measurement on physical device
4. Check sensor manual for trigger procedure

---

### Issue: Wrong values

**Possible Causes**:
- Parsing error
- Unit conversion error
- Byte order error

**Solutions**:
1. Check raw packet in Logcat (BLE_RAW tag)
2. Verify SFLOAT parsing
3. Compare with nRF Connect raw values
4. Check device manual for packet format

---

## Conclusion

This testing guide covers all aspects of the Blood Pressure sensor integration. Follow these tests systematically to ensure a robust, production-ready implementation.

**Recommended Testing Order**:
1. Pre-testing verification
2. Basic tests (1-8)
3. Edge cases (9-12)
4. Performance tests (13-15)
5. Integration tests (16-17)
6. Regression tests (18)
7. Advanced tests (19-20)

**Minimum Required Tests for Production**:
- ✅ Test 1: Basic Connection
- ✅ Test 2: BP Measurement
- ✅ Test 5: Connection Loss
- ✅ Test 7: Bluetooth Disabled
- ✅ Test 16: E2E Form Submit
- ✅ Test 18: Existing Sensors

---

**Document Version**: 1.0  
**Last Updated**: 2026-05-09  
**Status**: Ready for Testing ✅
