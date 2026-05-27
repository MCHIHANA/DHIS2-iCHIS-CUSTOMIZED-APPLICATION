# FORA O2 Pulse Oximeter Integration

This document describes the implementation of BLE connectivity for the FORA O2 Pulse Oximeter and integration with DHIS2.

## Features

- **BLE Connectivity**: Automatic scanning, connection, and bonding with FORA O2 device
- **Real-time Readings**: Live SpO2 and heart rate monitoring
- **Stability Detection**: Ensures readings are stable before submission
- **DHIS2 Integration**: Automatic submission of readings to DHIS2 server
- **Error Handling**: Comprehensive error handling with user-friendly messages
- **Reconnection**: Automatic reconnection with exponential backoff

## Architecture

The implementation follows MVVM architecture with clean separation of concerns:

```
oximeter/
├── data/           # Data models (OximeterReading, OximeterState, etc.)
├── ble/            # BLE Manager for device communication
├── dhis2/          # DHIS2 API service and repository
├── di/             # Dependency injection module
├── viewmodel/      # ViewModel for state management
└── ui/             # Compose UI screens
```

## Device Specifications

- **Device Name**: FORA O2
- **MAC Address**: C0:26:DA:17:D5:7D
- **Service UUID**: 00001523-1212-efde-1523-785feabcd123
- **Characteristic UUID**: 00001524-1212-efde-1523-785feabcd123
- **CCCD UUID**: 00002902-0000-1000-8000-00805f9b34fb
- **Bonding**: Required

## DHIS2 Integration

### Server Configuration

- **Base URL**: https://project.ccdev.org
- **Authentication**: Basic Auth (admin:district)
- **Endpoint**: POST /api/dataValueSets

### Data Element Mappings

| Sensor Reading | DHIS2 Data Element UID |
|----------------|------------------------|
| SpO2 (%)       | gAFXupYQDOb           |
| Heart Rate (BPM)| VqwQWWDmYLn          |

### Submission Rules

- Readings must be stable (2 consecutive readings within ±2 units)
- User confirmation required before submission
- Period format: yyyyMMdd (today's date)
- Organisation unit resolved from logged-in user

## Permissions

The following permissions are required and already added to AndroidManifest.xml:

### API < 31
- `ACCESS_FINE_LOCATION`
- `BLUETOOTH`
- `BLUETOOTH_ADMIN`

### API 31+
- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`

### All APIs
- `INTERNET`
- `ACCESS_NETWORK_STATE`

## Usage

### Starting the Oximeter Activity

```kotlin
val intent = Intent(context, OximeterActivity::class.java)
startActivity(intent)
```

### Workflow

1. **Launch Activity**: Opens the oximeter screen
2. **Request Permissions**: Automatically requests BLE permissions
3. **Scan**: Tap "Connect" to scan for FORA O2 device
4. **Connect**: Automatically connects when device is found
5. **Bond**: Pairs with the device if not already bonded
6. **Read**: Receives real-time SpO2 and heart rate readings
7. **Stabilize**: Waits for stable readings (2 consecutive within ±2 units)
8. **Submit**: Tap "Submit to DHIS2" and confirm
9. **Success**: Shows success message or error if submission fails

## Data Flow

```
FORA O2 Device
    ↓ (BLE Notifications)
BleManager
    ↓ (StateFlow)
OximeterViewModel
    ↓ (StateFlow)
OximeterScreen (Compose UI)
    ↓ (User Action)
OximeterViewModel
    ↓ (Suspend Function)
Dhis2Repository
    ↓ (Retrofit)
DHIS2 Server
```

## Error Handling

### BLE Errors
- **Bluetooth Disabled**: Prompts user to enable Bluetooth
- **Permissions Denied**: Shows permission rationale
- **Device Not Found**: Timeout after 30 seconds with error message
- **Connection Lost**: Automatic reconnection (3 attempts with exponential backoff)
- **Service/Characteristic Not Found**: Clear error message

### DHIS2 Errors
- **401 Unauthorized**: Authentication failed
- **403 Forbidden**: Insufficient permissions
- **409 Conflict**: Duplicate value (already submitted)
- **422 Unprocessable**: Invalid data format
- **5xx Server Error**: Retry once, then show error
- **Network Timeout**: User-friendly timeout message

## Security Considerations

### Current Implementation
⚠️ **WARNING**: Credentials are currently hardcoded in `OximeterModule.kt` for development purposes.

### Production Recommendations

1. **Use EncryptedSharedPreferences**:
```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "dhis2_credentials",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

2. **Store credentials securely**:
```kotlin
encryptedPrefs.edit()
    .putString("dhis2_username", username)
    .putString("dhis2_password", password)
    .apply()
```

3. **Never log credentials or payloads in production builds**

4. **Use ProGuard/R8 to obfuscate code**

## Testing

### Manual Testing Checklist

- [ ] BLE permissions requested on first launch
- [ ] Scan finds FORA O2 device
- [ ] Connection established successfully
- [ ] Bonding/pairing works
- [ ] Real-time readings displayed
- [ ] Stability indicator shows when readings are stable
- [ ] Submit button enabled only when stable
- [ ] Confirmation dialog appears
- [ ] Submission succeeds with valid credentials
- [ ] Success message displayed
- [ ] Error handling works for various scenarios
- [ ] Reconnection works after disconnection
- [ ] Device info card displays correctly

### Test Scenarios

1. **Happy Path**: Connect → Read → Submit → Success
2. **No Device**: Scan timeout → Error message
3. **Connection Lost**: Auto-reconnect → Resume reading
4. **Invalid Credentials**: Submit → 401 error → User message
5. **Network Error**: Submit → Timeout → Retry → Error message
6. **Duplicate Submission**: Submit twice → 409 error → User message

## Known Issues & TODOs

### Data Format Verification
⚠️ **TODO**: The BLE data parsing format in `BleManager.parseOximeterData()` is based on typical pulse oximeter protocols:
- Byte 0: SpO2 percentage
- Byte 1: Heart rate high byte
- Byte 2: Heart rate low byte

**This format needs to be verified with actual FORA O2 device data packets.** If the format differs, update the parsing logic accordingly.

### Future Enhancements

1. **Credential Management**: Move to EncryptedSharedPreferences
2. **Offline Support**: Queue submissions when offline, sync when online
3. **History**: Store reading history locally
4. **Export**: Export readings to CSV/PDF
5. **Multiple Devices**: Support for multiple oximeter models
6. **Calibration**: Device calibration settings
7. **Alerts**: Configurable alerts for abnormal readings
8. **Bluetooth Classic**: Fallback to Bluetooth Classic if BLE fails

## Dependencies

Added to `app/build.gradle.kts`:

```kotlin
// Retrofit for DHIS2 API
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
```

## Files Created

1. **Data Models**: `OximeterModels.kt`
2. **BLE Manager**: `BleManager.kt`
3. **DHIS2 API**: `Dhis2ApiService.kt`, `Dhis2Repository.kt`
4. **DI Module**: `OximeterModule.kt`
5. **ViewModel**: `OximeterViewModel.kt`
6. **UI**: `OximeterScreen.kt`, `OximeterActivity.kt`
7. **Documentation**: `OXIMETER_README.md`, `local.properties.example`

## Troubleshooting

### Device Not Found
- Ensure FORA O2 is powered on
- Check device is within range (< 10 meters)
- Verify Bluetooth is enabled
- Try restarting Bluetooth on phone
- Check device is not connected to another phone

### Connection Fails
- Unpair device from phone's Bluetooth settings
- Restart the app
- Restart the device
- Clear app data and try again

### Readings Not Appearing
- Check device is properly connected (green status)
- Verify characteristic notifications are enabled
- Check Logcat for parsing errors
- Verify data format matches device protocol

### Submission Fails
- Check internet connectivity
- Verify DHIS2 server is accessible
- Check credentials are correct
- Verify data element UIDs are correct
- Check organisation unit is assigned to user

## Support

For issues or questions:
1. Check Logcat for detailed error messages
2. Verify all prerequisites are met
3. Review this documentation
4. Check DHIS2 server logs for API errors

## License

This implementation is part of the DHIS2 Android Capture App.
