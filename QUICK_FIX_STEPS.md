# Quick Fix Steps - Blood Pressure Field Mapping

## ✅ Your Datastore Configuration is Correct!

Your configuration has:
- ✅ `"type": "multi"`
- ✅ `"measurements"` with systolic, diastolic, pulse
- ✅ Correct data element UIDs

## 🔧 Steps to Fix

### Step 1: Clear App Data

The app may have cached the old configuration. Clear it:

**Settings → Apps → DHIS2 → Storage → Clear Data**

OR

**Uninstall and reinstall the app**

### Step 2: Rebuild and Install

```bash
# Build the app
./gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Verify Configuration is Loaded

1. Open the app
2. Open Logcat in Android Studio
3. Filter by: `SensorConfig`
4. Look for these logs:

```
SensorConfig: Loaded sensors: 3
SensorConfig: Blood Pressure
SensorConfig: 00001810-0000-1000-8000-00805f9b34fb
```

If you see this, the configuration is loaded correctly.

### Step 4: Test Blood Pressure Sensor

1. Click on **any BP field** (systolic, diastolic, or pulse)
2. Click "Connect Blood Pressure"
3. Take measurement on FORA D40b
4. Check Logcat for these NEW logs:

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

### Step 5: Verify UI

All 3 fields should now be populated:
```
Systolic BP:    [102] mmHg  ✅
Diastolic BP:   [55]  mmHg  ✅
Pulse Rate:     [62]  bpm   ✅
```

---

## 🐛 If It Still Doesn't Work

### Check 1: Is the configuration loaded?

Filter Logcat by `SensorConfig` and look for:
```
SensorConfig: Loaded sensors: 3
```

**If you DON'T see this:**
- App cannot reach datastore API
- Check network connectivity
- Check datastore namespace/key

### Check 2: Is multi-measurement detected?

Filter Logcat by `SENSOR_DATA` and look for:
```
SENSOR_DATA: isMultiMeasurement: true
```

**If you see `isMultiMeasurement: false`:**
- Configuration doesn't have `type: "multi"`
- Configuration doesn't have `measurements` map
- Check the logs for what the app is reading:
  ```
  SENSOR_DATA: Sensor config found: Blood Pressure, type: ???, measurements: ???
  ```

### Check 3: Are measurements being mapped?

Look for:
```
SENSOR_DATA: Mapping SYSTOLIC → HkfzcXMdLLF
```

**If you DON'T see this:**
- Measurement keys don't match
- Check that keys are lowercase: "systolic", "diastolic", "pulse"
- NOT "Systolic", "SYSTOLIC", etc.

---

## 📝 Your Datastore Configuration (Verified Correct)

```json
{
  "name": "Blood Pressure",
  "type": "multi",
  "serviceUUID": "00001810-0000-1000-8000-00805f9b34fb",
  "characteristicUUID": "00002A35-0000-1000-8000-00805f9b34fb",
  "macAddress": "C0:26:DA:19:D4:FE",
  "manualAllowed": true,
  "sensorRequired": true,
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

✅ This configuration is **perfect**!

---

## 🎯 Most Likely Issue

The app is using **cached old configuration**. 

**Solution**: Clear app data or reinstall.

---

## 💡 Quick Test Without Rebuilding

If you can't rebuild right now:

1. **Clear app data**: Settings → Apps → DHIS2 → Clear Data
2. **Open app** and login again
3. **Test BP sensor**
4. **Check logs** for the new debug messages

The latest code with debug logging is already on GitHub, so if you rebuild from the `BPSensorConfig` branch, you'll get the enhanced logging.

---

## 🚀 Expected Result

After clearing cache and testing:

**Logcat**:
```
SENSOR_DATA: Multi-measurement sensor detected: Blood Pressure
SENSOR_DATA: Mapping SYSTOLIC → HkfzcXMdLLF
SENSOR_DATA: Mapping DIASTOLIC → skBarAsIYIL
SENSOR_DATA: Mapping PULSE → tZbUrUbhUNy
```

**UI**:
```
Systolic BP:    [102] mmHg  ✅
Diastolic BP:   [55]  mmHg  ✅
Pulse Rate:     [62]  bpm   ✅
```

---

**Your configuration is correct. Just need to clear the cache!** ✅
