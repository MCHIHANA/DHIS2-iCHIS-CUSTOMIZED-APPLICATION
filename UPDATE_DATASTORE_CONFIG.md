# Update DHIS2 Datastore Configuration

##  CRITICAL: Your Sensor Configuration Needs Updating

The logs show the app is using **legacy index-based mapping** instead of the new semantic key mapping. This means your DHIS2 datastore configuration doesn't have the new `measurements` structure.

## Current Problem

Your datastore probably has this (OLD):
```json
{
  "name": "Blood Pressure",
  "dataElements": {
    "systolic": "HkfzcXMdLLF",
    "diastolic": "skBarAsIYIL"
  }
}
```

But it needs this (NEW):
```json
{
  "name": "Blood Pressure",
  "type": "multi",
  "serviceUUID": "00001810-0000-1000-8000-00805f9b34fb",
  "characteristicUUID": "00002A35-0000-1000-8000-00805f9b34fb",
  "macAddress": "C0:26:DA:19:D4:FE",
  "measurements": {
    "systolic": {
      "dataElement": "HkfzcXMdLLF",
      "unit": "mmHg"
    },
    "diastolic": {
      "dataElement": "skBarAsIYIL",
      "unit": "mmHg"
    },
    "pulse": {
      "dataElement": "tZbUrUbhUNy",
      "unit": "bpm"
    }
  }
}
```

## How to Update

### Option 1: Update via DHIS2 Web Interface

1. **Login to DHIS2 web**
2. **Go to**: Apps → Data Store
3. **Find namespace**: `sensor-config` (or whatever namespace you're using)
4. **Find key**: `sensors`
5. **Update the JSON** with the complete configuration below
6. **Save**
7. **Restart the app** (or clear app data)

### Option 2: Update via API

```bash
# Replace with your DHIS2 server URL and credentials
DHIS2_URL="https://your-dhis2-server.com"
USERNAME="admin"
PASSWORD="district"

curl -X PUT "$DHIS2_URL/api/dataStore/sensor-config/sensors" \
  -H "Content-Type: application/json" \
  -u "$USERNAME:$PASSWORD" \
  -d @sensor_config.json
```

## Complete Configuration File

Save this as `sensor_config.json`:

```json
{
  "sensors": [
    {
      "name": "Temperature",
      "type": "single",
      "serviceUUID": "00001809-0000-1000-8000-00805f9b34fb",
      "characteristicUUID": "00002A1C-0000-1000-8000-00805f9b34fb",
      "macAddress": "C0:26:DA:1B:06:A4",
      "measurements": {
        "temperature": {
          "dataElement": "TEMP_DATA_ELEMENT_UID",
          "unit": "°C"
        }
      },
      "sensorRequired": false,
      "manualAllowed": true
    },
    {
      "name": "Pulse Oximeter",
      "type": "multi",
      "serviceUUID": "00001523-1212-efde-1523-785feabcd123",
      "characteristicUUID": "00001524-1212-efde-1523-785feabcd123",
      "macAddress": "C0:26:DA:17:D5:7D",
      "measurements": {
        "spo2": {
          "dataElement": "SPO2_DATA_ELEMENT_UID",
          "unit": "%"
        },
        "pulse": {
          "dataElement": "PULSE_DATA_ELEMENT_UID",
          "unit": "bpm"
        }
      },
      "sensorRequired": false,
      "manualAllowed": true
    },
    {
      "name": "Blood Pressure",
      "type": "multi",
      "serviceUUID": "00001810-0000-1000-8000-00805f9b34fb",
      "characteristicUUID": "00002A35-0000-1000-8000-00805f9b34fb",
      "macAddress": "C0:26:DA:19:D4:FE",
      "measurements": {
        "systolic": {
          "dataElement": "HkfzcXMdLLF",
          "unit": "mmHg"
        },
        "diastolic": {
          "dataElement": "skBarAsIYIL",
          "unit": "mmHg"
        },
        "pulse": {
          "dataElement": "tZbUrUbhUNy",
          "unit": "bpm"
        }
      },
      "sensorRequired": false,
      "manualAllowed": true
    }
  ]
}
```

##  IMPORTANT: Replace Data Element UIDs

Make sure to replace these with your actual DHIS2 data element UIDs:
- `TEMP_DATA_ELEMENT_UID` - Your temperature data element
- `SPO2_DATA_ELEMENT_UID` - Your SpO2 data element  
- `PULSE_DATA_ELEMENT_UID` - Your pulse rate data element (for oximeter)
- `HkfzcXMdLLF` - Your systolic BP data element (already correct)
- `skBarAsIYIL` - Your diastolic BP data element (already correct)
- `tZbUrUbhUNy` - Your pulse rate data element (for BP monitor)

## After Updating

1. **Clear app data** or **reinstall app**
2. **Open app**
3. **Check logs** for:
   ```
   SENSOR_DATA: Sensor config found: Blood Pressure, type: multi, measurements: [systolic, diastolic, pulse]
   SENSOR_DATA: isMultiMeasurement: true
   SENSOR_DATA: Multi-measurement sensor detected: Blood Pressure
   ```

4. **Take BP measurement**
5. **Verify all 3 fields populate**

## Verification

After updating, you should see these logs:
```
SENSOR_DATA: Received 3 readings for primary field: skBarAsIYIL, secondary: null
SENSOR_DATA: Sensor config found: Blood Pressure, type: multi, measurements: [systolic, diastolic, pulse]
SENSOR_DATA: isMultiMeasurement: true
SENSOR_DATA: Multi-measurement sensor detected: Blood Pressure
SENSOR_DATA: Mapping SYSTOLIC → HkfzcXMdLLF
SENSOR_SAVE: Saving SYSTOLIC=102 to field HkfzcXMdLLF
SENSOR_DATA: Mapping DIASTOLIC → skBarAsIYIL
SENSOR_SAVE: Saving DIASTOLIC=55 to field skBarAsIYIL
SENSOR_DATA: Mapping PULSE → tZbUrUbhUNy
SENSOR_SAVE: Saving PULSE=62 to field tZbUrUbhUNy
```

## Troubleshooting

### If you still see "Using legacy index-based mapping"

1. **Check datastore namespace**: Make sure you're updating the correct namespace
2. **Check datastore key**: Make sure you're updating the correct key
3. **Clear app cache**: Settings → Apps → DHIS2 → Clear Data
4. **Reinstall app**: Uninstall and reinstall
5. **Check logs**: Look for "Sensor config found" log to see what the app is reading

### If sensor config is null

1. **Check API endpoint**: Make sure the app can reach the datastore API
2. **Check permissions**: Make sure the user has permission to read datastore
3. **Check network**: Make sure the device has internet connectivity
4. **Check logs**: Look for "SensorConfig" logs during app startup

## Quick Test

To quickly test if the configuration is loaded:

1. Open app
2. Filter Logcat by "SensorConfig"
3. Look for:
   ```
   SensorConfig: Loaded sensors: 3
   SensorConfig: Blood Pressure
   SensorConfig: 00001810-0000-1000-8000-00805f9b34fb
   ```

If you don't see this, the configuration is not being loaded from the datastore.

---

**The code is correct. You just need to update the datastore configuration!** 
