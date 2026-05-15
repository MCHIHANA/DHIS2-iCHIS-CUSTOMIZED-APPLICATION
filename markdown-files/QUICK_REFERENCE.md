# Blood Pressure Sensor - Quick Reference Guide

##  Quick Start

### For Developers

**Add a new sensor in 3 steps:**

1. **Add to datastore config**:
```json
{
  "name": "Your Sensor",
  "type": "multi",
  "serviceUUID": "0000XXXX-0000-1000-8000-00805f9b34fb",
  "characteristicUUID": "0000YYYY-0000-1000-8000-00805f9b34fb",
  "macAddress": "AA:BB:CC:DD:EE:FF",
  "measurements": {
    "value1": {"dataElement": "UID1", "unit": "unit1"},
    "value2": {"dataElement": "UID2", "unit": "unit2"}
  }
}
```

2. **Add MAC to KnownDevices.kt**:
```kotlin
const val YOUR_SENSOR = "AA:BB:CC:DD:EE:FF"
val ALL = setOf(TEMP_SENSOR, SPO2_SENSOR, BP_SENSOR, YOUR_SENSOR)
```

3. **Add parser to BleDataParser.kt**:
```kotlin
fun parseYourSensor(data: ByteArray): YourReading {
    // Parse according to Bluetooth SIG spec
    return YourReading(...)
}
```

---

##  Blood Pressure Sensor Specs

### Device
- **Model**: FORA D40b
- **MAC**: C0:26:DA:19:D4:FE
- **Protocol**: Bluetooth SIG Blood Pressure Profile 1.0

### BLE Services
- **Service UUID**: `00001810-0000-1000-8000-00805f9b34fb` (0x1810)
- **Characteristic UUID**: `00002A35-0000-1000-8000-00805f9b34fb` (0x2A35)
- **Property**: INDICATE

### Data Format
- **Encoding**: IEEE-11073 16-bit SFLOAT
- **Values**: Systolic, Diastolic, MAP, Pulse (optional)
- **Units**: mmHg or kPa (flag-dependent)

---

##  Key Files

| File | Purpose |
|------|---------|
| `BleScanner.kt` | Device discovery |
| `BleDeviceConnector.kt` | Connection & notifications |
| `BleDataParser.kt` | Packet parsing |
| `SensorConfigModels.kt` | Configuration models |
| `SensorConfigRepository.kt` | Config management |
| `KnownDevices.kt` | Device registry |
| `FormViewModel.kt` | UI integration |

---

##  Debugging

### Essential Logcat Filters

```bash
# All BLE logs
adb logcat | grep "BLE_"

# Blood Pressure only
adb logcat | grep "BLE_BP"

# Sensor data flow
adb logcat | grep "SENSOR_"

# Connection issues
adb logcat | grep "BLE_CONNECT\|BLE_SERVICE"

# Raw packets
adb logcat | grep "BLE_RAW"
```

### Common Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| Sensor not found | Off/out of range | Turn on, move closer |
| Connection fails | Already connected | Disconnect other devices |
| No data | Not subscribed | Check CCCD write |
| Wrong values | Parsing error | Check raw packet |
| Crash on scan | Missing permission | Grant BLE permissions |

---

##  Packet Examples

### Basic BP Reading (mmHg, no pulse)
```
00 78 00 50 00 5E 00
â”‚  â”‚  â”‚  â”‚  â”‚  â”‚  â”‚
â”‚  â””â”€â”€â”´â”€â”€â”˜  â””â”€â”€â”´â”€â”€â”˜
â”‚  Systolic Diastolic MAP
â””â”€ Flags (0x00 = mmHg, no optional fields)

Result: 120/80 mmHg
```

### BP with Pulse Rate
```
04 78 00 50 00 5E 00 4B 00
â”‚  â”‚  â”‚  â”‚  â”‚  â”‚  â”‚  â”‚  â”‚
â”‚  â””â”€â”€â”´â”€â”€â”˜  â””â”€â”€â”´â”€â”€â”˜  â””â”€â”€â”´â”€â”€â”˜
â”‚  Systolic Diastolic MAP Pulse
â””â”€ Flags (0x04 = mmHg, pulse present)

Result: 120/80 mmHg, 75 bpm
```

### BP in kPa
```
01 10 00 0B 00 0D 00
â”‚  â”‚  â”‚  â”‚  â”‚  â”‚  â”‚
â”‚  â””â”€â”€â”´â”€â”€â”˜  â””â”€â”€â”´â”€â”€â”˜
â”‚  16 kPa  11 kPa  13 kPa
â””â”€ Flags (0x01 = kPa units)

Result: 120/82.5 mmHg (converted)
```

---

##  IEEE-11073 SFLOAT

### Format
```
15 14 13 12 | 11 10 09 08 07 06 05 04 03 02 01 00
[Exponent  ] [        Mantissa                   ]
4-bit signed  12-bit signed
```

### Calculation
```
Value = Mantissa Ã— 10^Exponent
```

### Examples

| Hex | Mantissa | Exponent | Value |
|-----|----------|----------|-------|
| 0x0078 | 120 | 0 | 120.0 |
| 0xF0B8 | 1200 | -1 | 120.0 |
| 0x1010 | 16 | 1 | 160.0 |
| 0x07FF | 0x7FF | - | NaN |

### Code
```kotlin
val raw = (data[offset].toInt() and 0xFF) or 
          ((data[offset + 1].toInt() and 0xFF) shl 8)

val mantissa = raw and 0x0FFF
val mantissaSigned = if (mantissa and 0x0800 != 0) 
    mantissa or -0x1000 else mantissa

val exponent = (raw shr 12) and 0x0F
val exponentSigned = if (exponent and 0x08 != 0) 
    exponent or -0x10 else exponent

val value = mantissaSigned * Math.pow(10.0, exponentSigned.toDouble())
```

---

##  Testing Checklist

### Before Committing
- [ ] Sensor discovered within 5s
- [ ] Connection established
- [ ] Values parsed correctly
- [ ] All fields populated
- [ ] No memory leaks
- [ ] Existing sensors still work
- [ ] Logcat shows no errors

### Before Release
- [ ] Tested on 3+ Android versions
- [ ] Tested with physical device
- [ ] Tested connection loss
- [ ] Tested Bluetooth off
- [ ] Tested permission denial
- [ ] Tested rapid measurements
- [ ] Tested app backgrounding
- [ ] E2E form submission works
- [ ] Data syncs to DHIS2 server

---

##  Code Snippets

### Add New Sensor Type

```kotlin
// 1. Add to SensorType.kt
enum class SensorType {
    TEMPERATURE,
    SPO2,
    BLOOD_PRESSURE,
    GLUCOSE,  // â† New
    UNKNOWN
}

// 2. Add to KnownDevices.kt
const val GLUCOSE_SENSOR = "AA:BB:CC:DD:EE:FF"
val ALL = setOf(TEMP_SENSOR, SPO2_SENSOR, BP_SENSOR, GLUCOSE_SENSOR)

fun typeFor(mac: String): SensorType = when (mac) {
    TEMP_SENSOR -> SensorType.TEMPERATURE
    SPO2_SENSOR -> SensorType.SPO2
    BP_SENSOR -> SensorType.BLOOD_PRESSURE
    GLUCOSE_SENSOR -> SensorType.GLUCOSE  // â† New
    else -> SensorType.UNKNOWN
}

// 3. Add to BleScanner.kt
private const val GLUCOSE_UUID = "00001808-0000-1000-8000-00805f9b34fb"

if (advertisedServices.any {
    it.uuid.toString().equals(GLUCOSE_UUID, ignoreCase = true)
}) {
    Log.d("BLE_MATCH", "Glucose service UUID matched: $address")
    stopScan()
    onTargetFound?.invoke(device)
    return
}

// 4. Add to BleDeviceConnector.kt
private val GLUCOSE_SERVICE_UUID = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb")
private val GLUCOSE_MEASUREMENT_UUID = UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb")

override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
    when (currentSensorType) {
        SensorType.GLUCOSE -> subscribeGlucose(gatt)  // â† New
        // ... other types
    }
}

private fun subscribeGlucose(gatt: BluetoothGatt) {
    val service = gatt.getService(GLUCOSE_SERVICE_UUID)
    val char = service?.getCharacteristic(GLUCOSE_MEASUREMENT_UUID)
    char?.let { enableCharacteristic(gatt, it, indicate = true) }
}

private fun dispatchReading(uuid: String, data: ByteArray) {
    when (currentSensorType) {
        SensorType.GLUCOSE -> handleGlucoseData(uuid, data)  // â† New
        // ... other types
    }
}

private fun handleGlucoseData(uuid: String, data: ByteArray) {
    val reading = BleDataParser.parseGlucose(data)
    onReadingsReceived(listOf(Pair("GLUCOSE", reading.value.toString())))
}

// 5. Add to BleDataParser.kt
fun parseGlucose(data: ByteArray): GlucoseReading {
    // Parse according to Bluetooth SIG Glucose Profile
    val flags = data[0].toInt() and 0xFF
    val glucose = parseSFloat(data, 3)
    return GlucoseReading(glucose)
}

data class GlucoseReading(val value: Float)
```

### Parse Custom Packet Format

```kotlin
fun parseCustomSensor(data: ByteArray): CustomReading {
    if (data.size < 7) {
        Log.w(TAG, "Packet too short")
        return CustomReading(0f, 0f)
    }
    
    // Example: 2-byte little-endian values
    val value1 = ((data[1].toInt() and 0xFF) or 
                  ((data[2].toInt() and 0xFF) shl 8)).toFloat() / 10f
    
    val value2 = ((data[3].toInt() and 0xFF) or 
                  ((data[4].toInt() and 0xFF) shl 8)).toFloat() / 10f
    
    Log.d(TAG, "Parsed: value1=$value1, value2=$value2")
    return CustomReading(value1, value2)
}
```

### Add Validation

```kotlin
private fun handleBloodPressureData(uuid: String, data: ByteArray) {
    val reading = BleDataParser.parseBloodPressure(data)
    
    // Validate ranges
    if (reading.systolic < 50f || reading.systolic > 250f) {
        Log.w("BLE_BP", "Systolic out of range: ${reading.systolic}")
        return
    }
    
    if (reading.diastolic < 30f || reading.diastolic > 150f) {
        Log.w("BLE_BP", "Diastolic out of range: ${reading.diastolic}")
        return
    }
    
    // Validate pulse if present
    reading.pulseRate?.let { pulse ->
        if (pulse < 30f || pulse > 250f) {
            Log.w("BLE_BP", "Pulse out of range: $pulse")
            return
        }
    }
    
    // All valid - emit readings
    val readings = mutableListOf(
        Pair("SYSTOLIC", reading.systolic.toInt().toString()),
        Pair("DIASTOLIC", reading.diastolic.toInt().toString())
    )
    
    reading.pulseRate?.let { pulse ->
        readings.add(Pair("PULSE", pulse.toInt().toString()))
    }
    
    onReadingsReceived(readings)
}
```

---

##  Useful Links

### Bluetooth SIG Specifications
- [Blood Pressure Profile](https://www.bluetooth.com/specifications/specs/blood-pressure-profile-1-0/)
- [Health Thermometer Profile](https://www.bluetooth.com/specifications/specs/health-thermometer-profile-1-0/)
- [Pulse Oximeter Profile](https://www.bluetooth.com/specifications/specs/pulse-oximeter-profile-1-0/)
- [GATT Specifications](https://www.bluetooth.com/specifications/specs/)

### IEEE Standards
- [IEEE-11073-20601](https://standards.ieee.org/standard/11073-20601-2019.html)

### Android Documentation
- [BLE Overview](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview)
- [BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt)
- [BluetoothGattCharacteristic](https://developer.android.com/reference/android/bluetooth/BluetoothGattCharacteristic)

### DHIS2
- [Android SDK](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/android-sdk.html)
- [Mobile UI](https://ui.dhis2.nu/components)

### Tools
- [nRF Connect](https://play.google.com/store/apps/details?id=no.nordicsemi.android.mcp)
- [BLE Scanner](https://play.google.com/store/apps/details?id=com.macdom.ble.blescanner)

---

##  Pro Tips

### Performance
- Use `LOW_LATENCY` scan mode for fast discovery
- Stop scan immediately when target found
- Use `autoConnect=true` for reliability
- Clear seen devices on each scan start

### Reliability
- Always validate packet size before parsing
- Handle both API 33+ and legacy callbacks
- Implement retry logic for WRITE failures
- Use INDICATE for critical data (BP, glucose)

### Debugging
- Log raw packets in hex format
- Log every step of parsing
- Use descriptive log tags
- Include device MAC in logs

### Testing
- Test on multiple Android versions
- Test with physical devices, not simulators
- Test connection loss scenarios
- Test rapid consecutive measurements

### Code Quality
- Follow DHIS2 architecture guidelines
- Use sealed classes for state
- Prefer Flow over LiveData
- Write unit tests for parsers

---

##  Learning Resources

### Bluetooth Basics
1. Read Bluetooth SIG profile specifications
2. Use nRF Connect to inspect real devices
3. Study IEEE-11073 SFLOAT format
4. Understand GATT services/characteristics

### Android BLE
1. Follow Android BLE guide
2. Study BluetoothGatt API
3. Learn about CCCD descriptors
4. Understand notification vs indication

### DHIS2 Development
1. Read AGENTS.md guidelines
2. Study existing sensor implementations
3. Follow MVVM architecture
4. Use DHIS2 design system components

---

##  Support

### Issues
- Check Logcat first
- Verify device is advertising
- Test with nRF Connect
- Check permissions

### Questions
- Review this guide
- Check AGENTS.md
- Read Bluetooth SIG specs
- Search Android documentation

---

**Document Version**: 1.0  
**Last Updated**: 2026-05-09  
**Quick Reference**: Always keep this handy! 
