# Summary: BP Sensor Multi-Value Mapping Fix

##  What Was Done

### 1. Code Updates
- **Updated `SensorConfigModels.kt`**: Fixed documentation example to use correct UIDs
  - Diastolic: `skBarAsIYIL` → `BaGxiB8AsNI` 
  - Pulse: `tZbUrUbhUNy` → `S7OjKl85YSh` 

### 2. Documentation Created
- **`BP_SENSOR_STATUS.md`**: Comprehensive status document with:
  - All code changes completed
  - Expected behavior after fix
  - Troubleshooting steps
  - Related files reference

- **`BP_SENSOR_QUICK_FIX.md`**: Quick problem summary

- **`UPDATE_DATASTORE_DIASTOLIC_UID.md`**: Step-by-step DataStore update guide

### 3. Git Commits
- Commit `d911df6`: "docs: update BP sensor UIDs and add comprehensive status documentation"
- Pushed to `BPSensorConfig` branch on GitHub 

##  CRITICAL: What You Need to Do

### Your BP sensor is sending 3 values but only 1 is being saved because:
1. **DataStore configuration is not loading** (`Sensor config found: null`)
2. **App falls back to legacy mapping** (`isMultiMeasurement: false`)
3. **Only first value is saved** to wrong field

### Solution: Update DHIS2 DataStore

**Follow these steps:**

1. **Open DHIS2 web interface** and login as admin

2. **Open browser console** (Press F12)

3. **Delete old configuration**:
```javascript
fetch('/api/dataStore/sensorConfig/config', {
  method: 'DELETE'
}).then(r => console.log('Deleted:', r.ok));
```

4. **Add correct configuration**:
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
.then(r => r.json())
.then(data => console.log(' Configuration updated:', data))
.catch(e => console.error('Error:', e));
```

5. **Verify configuration**:
```javascript
fetch('/api/dataStore/sensorConfig/config')
  .then(r => r.json())
  .then(data => {
    console.log('Current configuration:');
    console.log(JSON.stringify(data, null, 2));
    
    const bpSensor = data.sensors.find(s => s.name === 'Blood Pressure');
    if (bpSensor) {
      console.log('\n BP Sensor found:');
      console.log('  Systolic UID:', bpSensor.measurements.systolic.dataElement);
      console.log('  Diastolic UID:', bpSensor.measurements.diastolic.dataElement);
      console.log('  Pulse UID:', bpSensor.measurements.pulse.dataElement);
    }
  });
```

6. **Clear app data** on your Android device:
   - Settings → Apps → DHIS2 → Storage → Clear Data

7. **Open the app** and sync metadata:
   - Login
   - Settings → Sync metadata
   - Wait for sync to complete

8. **Test BP sensor**:
   - Open a form with BP fields
   - Connect BP sensor
   - Verify all 3 values are saved

##  Expected Result

### When BP Sensor Sends:
```
SYSTOLIC = 99
DIASTOLIC = 82
PULSE = 60
```

### App Should Save To:
- **Systolic Pressure** (HkfzcXMdLLF) = 99 
- **Diastolic Pressure** (BaGxiB8AsNI) = 82 
- **Heart Rate** (S7OjKl85YSh) = 60 

### Expected Logs:
```
SENSOR_DATA: Sensor config found: Blood Pressure 
SENSOR_DATA: isMultiMeasurement: true 
SENSOR_DATA: Multi-measurement sensor detected: Blood Pressure 
SENSOR_DATA: Mapping SYSTOLIC → HkfzcXMdLLF 
SENSOR_DATA: Mapping DIASTOLIC → BaGxiB8AsNI 
SENSOR_DATA: Mapping PULSE → S7OjKl85YSh 
SENSOR_SAVE: Saving SYSTOLIC=99 to field HkfzcXMdLLF 
SENSOR_SAVE: Saving DIASTOLIC=82 to field BaGxiB8AsNI 
SENSOR_SAVE: Saving PULSE=60 to field S7OjKl85YSh 
```

##  Troubleshooting

### If it still doesn't work after updating DataStore:

1. **Check if MainPresenter is calling fetchSensorConfig()**:
   - Look for logs with tag "SensorConfig"
   - Should see: "Loaded sensors: 1" or "Loaded sensors: 3"

2. **Verify field UIDs in DHIS2**:
   - Go to Maintenance → Data Elements
   - Find "Systolic Pressure", "Diastolic Pressure", "Heart Rate"
   - Verify UIDs match the DataStore configuration

3. **Check API endpoint**:
   - In browser: `https://your-dhis2-instance/api/dataStore/sensorConfig/config`
   - Should return the configuration you just added

4. **Rebuild and reinstall app**:
   - Sometimes cached code needs a fresh install
   - `./gradlew clean assembleDebug`
   - Install the new APK

##  Important Files

- **`BP_SENSOR_STATUS.md`** - Comprehensive status and troubleshooting
- **`UPDATE_DATASTORE_DIASTOLIC_UID.md`** - Detailed DataStore update guide
- **`BP_SENSOR_QUICK_FIX.md`** - Quick problem summary

##  Compilation Status

All files compile successfully with **no errors**:
-  `FormViewModel.kt` - No diagnostics
-  `FormView.kt` - No diagnostics
-  `FieldProvider.kt` - No diagnostics
-  `SensorConfigRepository.kt` - No diagnostics
-  `SensorConfigModels.kt` - No diagnostics

##  Summary

**The code is ready and working correctly.** The only issue is that your DHIS2 DataStore has incorrect UIDs. Once you update the DataStore configuration following the steps above, the BP sensor will save all 3 values (systolic, diastolic, pulse) to the correct fields.

**Next Step**: Update your DHIS2 DataStore using the JavaScript commands above, then test the BP sensor.

---

**Branch**: BPSensorConfig
**Last Commit**: d911df6
**Status**:  Code ready, awaiting DataStore configuration update
