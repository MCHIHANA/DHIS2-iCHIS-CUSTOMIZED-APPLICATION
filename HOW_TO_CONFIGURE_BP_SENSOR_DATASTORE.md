# How to Configure Blood Pressure Sensor in DHIS2 DataStore

## Problem
The BP sensor sends 3 values (SYSTOLIC, DIASTOLIC, PULSE) but only one value is being saved because the sensor configuration is not found in DataStore.

## Solution
Add the sensor configuration to DHIS2 DataStore so the app can map the semantic keys (SYSTOLIC, DIASTOLIC, PULSE) to the correct data element UIDs.

## Steps to Add Configuration

### Option 1: Using DHIS2 Web API (Recommended)

1. **Open your browser** and navigate to your DHIS2 instance
2. **Login** with admin credentials
3. **Open the browser console** (F12 or Right-click → Inspect → Console)
4. **Run this command** in the console:

```javascript
fetch('/api/dataStore/sensorConfig/config', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    "sensors": [
      {
        "name": "Blood Pressure Monitor",
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
            "dataElement": "BaGxiB8AsNI",
            "unit": "mmHg"
          },
          "pulse": {
            "dataElement": "S7OjKl85YSh",
            "unit": "bpm"
          }
        },
        "sensorRequired": false,
        "manualAllowed": true
      }
    ]
  })
})
.then(response => response.json())
.then(data => console.log('Success:', data))
.catch(error => console.error('Error:', error));
```

5. **Verify** the configuration was added:

```javascript
fetch('/api/dataStore/sensorConfig/config')
  .then(response => response.json())
  .then(data => console.log('Current config:', data));
```

### Option 2: Using cURL

```bash
curl -X POST "https://your-dhis2-instance.com/api/dataStore/sensorConfig/config" \
  -H "Content-Type: application/json" \
  -u admin:district \
  -d @BP_SENSOR_DATASTORE_CONFIG.json
```

### Option 3: Using Postman

1. **Method**: POST
2. **URL**: `https://your-dhis2-instance.com/api/dataStore/sensorConfig/config`
3. **Auth**: Basic Auth (username: admin, password: your-password)
4. **Headers**: `Content-Type: application/json`
5. **Body**: Raw JSON (copy content from `BP_SENSOR_DATASTORE_CONFIG.json`)

## Verify Configuration

After adding the configuration, restart your Android app and check the logs. You should see:

```
SENSOR_DATA: Sensor config found: Blood Pressure Monitor, type: multi, measurements: [systolic, diastolic, pulse]
SENSOR_DATA: isMultiMeasurement: true
SENSOR_DATA: Multi-measurement sensor detected: Blood Pressure Monitor
SENSOR_DATA: Mapping SYSTOLIC → HkfzcXMdLLF
SENSOR_DATA: Mapping DIASTOLIC → BaGxiB8AsNI
SENSOR_DATA: Mapping PULSE → S7OjKl85YSh
```

## Important Notes

### Data Element UIDs
Make sure these UIDs match your DHIS2 instance:
- **Systolic Pressure**: `HkfzcXMdLLF`
- **Diastolic Pressure**: `BaGxiB8AsNI`
- **Heart Rate**: `S7OjKl85YSh`

### MAC Address
The configuration includes the MAC address of your BP sensor:
- **FORA D40b**: `C0:26:DA:19:D4:FE`

If you have a different sensor, update the `macAddress` field.

### Service UUIDs
The configuration uses standard Bluetooth SIG UUIDs for Blood Pressure:
- **Service**: `00001810-0000-1000-8000-00805f9b34fb` (Blood Pressure Service)
- **Characteristic**: `00002A35-0000-1000-8000-00805f9b34fb` (Blood Pressure Measurement)

## Troubleshooting

### If configuration is not loading:

1. **Check DataStore namespace**:
```javascript
fetch('/api/dataStore')
  .then(response => response.json())
  .then(data => console.log('Namespaces:', data));
```

2. **Check if config exists**:
```javascript
fetch('/api/dataStore/sensorConfig')
  .then(response => response.json())
  .then(data => console.log('Keys:', data));
```

3. **Delete and recreate** (if needed):
```javascript
// Delete
fetch('/api/dataStore/sensorConfig/config', { method: 'DELETE' })
  .then(() => console.log('Deleted'));

// Then run the POST command again
```

### If values are still not mapping correctly:

1. **Clear app data** on your Android device
2. **Sync metadata** in the app
3. **Restart the app**
4. **Check logs** for "SENSOR_DATA" and "SENSOR_SAVE" tags

## Expected Behavior After Configuration

When you connect the BP sensor:
1. App detects sensor and connects
2. Sensor sends: SYSTOLIC=108, DIASTOLIC=55, PULSE=62
3. App maps:
   - SYSTOLIC (108) → Field HkfzcXMdLLF (Systolic Pressure)
   - DIASTOLIC (55) → Field BaGxiB8AsNI (Diastolic Pressure)
   - PULSE (62) → Field S7OjKl85YSh (Heart Rate)
4. All three fields are filled automatically

## Alternative: Hardcoded Configuration

If you cannot access DataStore, the app has a fallback mechanism. The UIDs have been updated in the code to match your DHIS2 instance. However, **DataStore configuration is strongly recommended** for production use.
