# FORA O2 Oximeter - Quick Start Guide

## Launch the Oximeter Feature

### From Code

```kotlin
import android.content.Intent
import org.dhis2.sensors.oximeter.ui.OximeterActivity

// Launch the oximeter activity
val intent = Intent(context, OximeterActivity::class.java)
startActivity(intent)
```

### Add to Menu/Navigation

To add the oximeter feature to your app's navigation:

1. **Add menu item** (e.g., in `res/menu/main_menu.xml`):
```xml
<item
    android:id="@+id/menu_oximeter"
    android:icon="@drawable/ic_oximeter"
    android:title="Pulse Oximeter" />
```

2. **Handle menu click**:
```kotlin
override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
        R.id.menu_oximeter -> {
            startActivity(Intent(this, OximeterActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
```

### Add to Dashboard/Home Screen

```kotlin
// In your dashboard/home screen
Button(
    onClick = {
        val intent = Intent(context, OximeterActivity::class.java)
        context.startActivity(intent)
    }
) {
    Icon(Icons.Default.Favorite, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("Pulse Oximeter")
}
```

## User Flow

1. **Launch** → Opens oximeter screen
2. **Permissions** → Grants BLE permissions (automatic)
3. **Connect** → Taps "Connect" button
4. **Scan** → App scans for FORA O2 (30s timeout)
5. **Connect** → Auto-connects when found
6. **Bond** → Pairs with device if needed
7. **Monitor** → Views real-time SpO2 and heart rate
8. **Stabilize** → Waits for stable readings (green indicator)
9. **Submit** → Taps "Submit to DHIS2"
10. **Confirm** → Reviews and confirms submission
11. **Success** → Sees success message

## Prerequisites

### Device Requirements
- FORA O2 Pulse Oximeter powered on
- Device within 10 meters
- Bluetooth enabled on phone
- Device not connected to another phone

### App Requirements
- BLE permissions granted
- Internet connection (for DHIS2 submission)
- Valid DHIS2 credentials configured

### DHIS2 Requirements
- User logged in
- Organisation unit assigned to user
- Data elements exist:
  - SpO2: `gAFXupYQDOb`
  - Heart Rate: `VqwQWWDmYLn`

## Configuration

### DHIS2 Credentials

**Current (Development)**:
Hardcoded in `OximeterModule.kt`:
- Base URL: https://project.ccdev.org/
- Username: admin
- Password: district

**Production (Recommended)**:
Use `local.properties` or EncryptedSharedPreferences:

```properties
# local.properties
dhis2.oximeter.baseUrl=https://your-server.com/
dhis2.oximeter.username=your_username
dhis2.oximeter.password=your_password
```

## Testing

### Quick Test Checklist

1. **Launch Activity**
   ```kotlin
   startActivity(Intent(this, OximeterActivity::class.java))
   ```

2. **Grant Permissions** (automatic prompt)

3. **Connect Device**
   - Tap "Connect"
   - Wait for green "Connected" status

4. **Verify Readings**
   - SpO2 displays (0-100%)
   - Heart rate displays (30-250 BPM)
   - Timestamp updates

5. **Check Stability**
   - Wait for green checkmark
   - "Readings are stable" message

6. **Submit**
   - Tap "Submit to DHIS2"
   - Confirm in dialog
   - See success message

### Test Without Device

For UI testing without hardware:

1. Modify `BleManager.kt` to emit mock data:
```kotlin
// In BleManager.kt, add test method
fun emitMockReading() {
    val mockReading = OximeterReading(
        spo2 = 98,
        heartRateBpm = 72
    )
    _latestReading.value = mockReading
    _connectionStatus.value = ConnectionStatus.CONNECTED
}
```

2. Call from ViewModel:
```kotlin
// In OximeterViewModel.kt
fun testWithMockData() {
    bleManager.emitMockReading()
}
```

## Troubleshooting

### Device Not Found
- Ensure FORA O2 is powered on
- Check Bluetooth is enabled
- Move device closer (< 10m)
- Restart Bluetooth
- Try scanning again

### Connection Fails
- Unpair device from phone settings
- Restart app
- Restart device
- Clear app data

### No Readings
- Check connection status is green
- Verify device is sending data
- Check Logcat for parsing errors
- Verify data format matches device

### Submission Fails
- Check internet connection
- Verify DHIS2 server is accessible
- Check credentials in `OximeterModule.kt`
- Verify data element UIDs
- Check user has organisation unit assigned

## Logcat Debugging

Filter by tag:
```
adb logcat -s BleManager OximeterViewModel Dhis2Repository
```

Key log messages:
- `Starting BLE scan for FORA O2`
- `Found device: FORA O2 (C0:26:DA:17:D5:7D)`
- `Connected to GATT server`
- `Notifications enabled successfully`
- `Parsed reading: SpO2=98%, HR=72 BPM`
- `Submitting data: SpO2=98, HR=72`
- `Submission successful: imported=2, updated=0`

## Common Issues

### "Bluetooth permissions not granted"
**Solution**: Grant permissions in app settings or reinstall app

### "Device not found"
**Solution**: Ensure device is on, nearby, and not connected elsewhere

### "Connection lost"
**Solution**: App will auto-reconnect (3 attempts). If fails, tap Connect again

### "Readings not stable"
**Solution**: Wait for 2 consecutive readings within ±2 units

### "Authentication failed"
**Solution**: Check credentials in `OximeterModule.kt`

### "No organisation units assigned to user"
**Solution**: Assign org unit to user in DHIS2

## Next Steps

1. **Test with real device** to verify BLE data format
2. **Configure production credentials** using EncryptedSharedPreferences
3. **Add to app navigation** for easy access
4. **Train users** on device usage and app workflow

## Support

For detailed documentation, see:
- **`OXIMETER_README.md`** - Complete feature documentation
- **`OXIMETER_IMPLEMENTATION_SUMMARY.md`** - Technical implementation details
- **`local.properties.example`** - Configuration template

## Quick Reference

| Item | Value |
|------|-------|
| Activity | `org.dhis2.sensors.oximeter.ui.OximeterActivity` |
| Device Name | FORA O2 |
| MAC Address | C0:26:DA:17:D5:7D |
| Service UUID | 00001523-1212-efde-1523-785feabcd123 |
| Characteristic UUID | 00001524-1212-efde-1523-785feabcd123 |
| SpO2 Data Element | gAFXupYQDOb |
| Heart Rate Data Element | VqwQWWDmYLn |
| DHIS2 Server | https://project.ccdev.org/ |
| DHIS2 Endpoint | POST /api/dataValueSets |
