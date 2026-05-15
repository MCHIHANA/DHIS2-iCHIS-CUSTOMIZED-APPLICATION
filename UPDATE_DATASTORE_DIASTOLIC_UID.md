# Update DataStore - Fix Diastolic UID

## Problem
Your current DataStore has the wrong Diastolic UID:
- **Current**: `skBarAsIYIL` 
- **Correct**: `BaGxiB8AsNI` 

## Solution: Update DataStore Configuration

### Step 1: Delete Old Configuration

Open your DHIS2 web interface, login, and run this in the browser console (F12):

```javascript
fetch('/api/dataStore/sensorConfig/config', {
  method: 'DELETE'
})
.then(response => {
  if (response.ok) {
    console.log(' Old configuration deleted');
  } else {
    console.log('Configuration not found or already deleted');
  }
});
```

### Step 2: Add Correct Configuration

```javascript
fetch('/api/dataStore/sensorConfig/config', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    "globalSettings": {
      "allowManualFallback": true,
      "allowManualWhenNoSensorAvailable": true,
      "allowManualWhenSensorFails": true,
      "enableMockMode": false
    },
    "sensors": [
      {
        "name": "Temperature",
        "type": "single",
        "serviceUUID": "00001809-0000-1000-8000-00805f9b34fb",
        "characteristicUUID": "00002A1C-0000-1000-8000-00805f9b34fb",
        "macAddress": "C0:26:DA:1B:06:A4",
        "measurements": {
          "temperature": {
            "dataElement": "KXNH45ts16S",
            "unit": "°C"
          }
        },
        "sensorRequired": true,
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
            "dataElement": "VqwQWWDmYLn",
            "unit": "%"
          },
          "pulseRate": {
            "dataElement": "tZbUrUbhUNy",
            "unit": "bpm"
          }
        },
        "sensorRequired": true,
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
            "dataElement": "BaGxiB8AsNI",
            "unit": "mmHg"
          },
          "pulse": {
            "dataElement": "S7OjKl85YSh",
            "unit": "bpm"
          }
        },
        "sensorRequired": true,
        "manualAllowed": true
      }
    ]
  })
})
.then(response => response.json())
.then(data => console.log(' Configuration updated:', data))
.catch(error => console.error('Error:', error));
```

### Step 3: Verify Configuration

```javascript
fetch('/api/dataStore/sensorConfig/config')
  .then(response => response.json())
  .then(data => {
    console.log('Current configuration:');
    console.log(JSON.stringify(data, null, 2));
    
    // Check BP sensor specifically
    const bpSensor = data.sensors.find(s => s.name === 'Blood Pressure');
    if (bpSensor) {
      console.log('\n BP Sensor found:');
      console.log('  Systolic UID:', bpSensor.measurements.systolic.dataElement);
      console.log('  Diastolic UID:', bpSensor.measurements.diastolic.dataElement);
      console.log('  Pulse UID:', bpSensor.measurements.pulse.dataElement);
    }
  });
```

### Step 4: Clear App Cache and Sync

1. **On your Android device**:
   - Go to Settings → Apps → DHIS2 → Storage
   - Click "Clear Data" or "Clear Cache"
   
2. **Open the app**:
   - Login
   - Go to Settings → Sync metadata
   - Wait for sync to complete

3. **Test BP sensor**:
   - Open a form with BP fields
   - Connect BP sensor
   - Check logs for:
     ```
     SENSOR_DATA: Sensor config found: Blood Pressure 
     SENSOR_DATA: isMultiMeasurement: true 
     SENSOR_DATA: Mapping SYSTOLIC → HkfzcXMdLLF 
     SENSOR_DATA: Mapping DIASTOLIC → BaGxiB8AsNI 
     SENSOR_DATA: Mapping PULSE → S7OjKl85YSh 
     ```

## Expected Result

After updating DataStore and clearing app cache:

**Sensor sends:**
- SYSTOLIC = 99
- DIASTOLIC = 82
- PULSE = 60

**App saves to:**
- Systolic Pressure (HkfzcXMdLLF) = 99 
- Diastolic Pressure (BaGxiB8AsNI) = 82 
- Heart Rate (S7OjKl85YSh) = 60 

## Troubleshooting

### If config is still not loading:

1. **Check if MainPresenter is calling fetchSensorConfig**:
   - Look for logs with tag "SensorConfig"
   - Should see: "Loaded sensors: 3"

2. **Check SharedPreferences cache**:
   - The config is cached in SharedPreferences
   - Clearing app data will clear this cache

3. **Check API endpoint**:
   - Verify `/api/dataStore/sensorConfig/config` returns the correct data
   - Check network logs in the app

### If values still not mapping:

1. **Verify field UIDs in DHIS2**:
   - Go to Maintenance → Data Elements
   - Find "Systolic Pressure", "Diastolic Pressure", "Heart Rate"
   - Copy their UIDs and verify they match the DataStore config

2. **Check logs for "SENSOR_DATA" tag**:
   - Should see "Sensor config found: Blood Pressure"
   - Should see "isMultiMeasurement: true"
   - Should see mapping for all 3 values

3. **Rebuild and reinstall app**:
   - Sometimes cached code needs a fresh install
   - `./gradlew clean assembleDebug`
   - Install the new APK
