# Blood Pressure Sensor - Current Status

## ✅ Code Changes Completed

### 1. Updated Diastolic UID in FieldProvider.kt
**File**: `form/src/main/java/org/dhis2/form/ui/provider/inputfield/FieldProvider.kt`

Changed hardcoded Diastolic UID:
- **Old**: `skBarAsIYIL` ❌
- **New**: `BaGxiB8AsNI` ✅

### 2. Updated Documentation in SensorConfigModels.kt
**File**: `form/src/main/java/org/dhis2/sensor/config/SensorConfigModels.kt`

Updated example configuration with correct UIDs:
```json
{
  "measurements": {
    "systolic": {"dataElement": "HkfzcXMdLLF", "unit": "mmHg"},
    "diastolic": {"dataElement": "BaGxiB8AsNI", "unit": "mmHg"},
    "pulse": {"dataElement": "S7OjKl85YSh", "unit": "bpm"}
  }
}
```

### 3. Multi-Value Mapping Logic
**File**: `form/src/main/java/org/dhis2/form/ui/FormViewModel.kt`

The `observeSensorData()` function now:
- ✅ Checks for sensor configuration from DataStore
- ✅ Uses multi-measurement mapping when config is found
- ✅ Maps semantic keys (SYSTOLIC, DIASTOLIC, PULSE) to data element UIDs
- ✅ Falls back to legacy index-based mapping if no config found

## ⚠️ User Action Required

### Critical: Update DHIS2 DataStore Configuration

Your DHIS2 DataStore currently has **incorrect UIDs**. You must update it:

**Current DataStore (WRONG)**:
```json
{
  "measurements": {
    "systolic": {"dataElement": "HkfzcXMdLLF"},
    "diastolic": {"dataElement": "skBarAsIYIL"},  // ❌ WRONG
    "pulse": {"dataElement": "tZbUrUbhUNy"}       // ❌ WRONG
  }
}
```

**Correct DataStore**:
```json
{
  "measurements": {
    "systolic": {"dataElement": "HkfzcXMdLLF"},
    "diastolic": {"dataElement": "BaGxiB8AsNI"},  // ✅ CORRECT
    "pulse": {"dataElement": "S7OjKl85YSh"}       // ✅ CORRECT
  }
}
```

### How to Update DataStore

See detailed instructions in: **`UPDATE_DATASTORE_DIASTOLIC_UID.md`**

Quick steps:
1. Open DHIS2 web interface
2. Login as admin
3. Open browser console (F12)
4. Delete old config: `DELETE /api/dataStore/sensorConfig/config`
5. Add new config with correct UIDs (see UPDATE_DATASTORE_DIASTOLIC_UID.md)
6. Clear app data on Android device
7. Sync metadata in the app
8. Test BP sensor

## 📊 Expected Behavior After Fix

### When BP Sensor Sends Data:
```
SYSTOLIC = 99
DIASTOLIC = 82
PULSE = 60
```

### App Should Save To:
- **Systolic Pressure** (HkfzcXMdLLF) = 99 ✅
- **Diastolic Pressure** (BaGxiB8AsNI) = 82 ✅
- **Heart Rate** (S7OjKl85YSh) = 60 ✅

### Expected Logs:
```
SENSOR_DATA: Sensor config found: Blood Pressure ✅
SENSOR_DATA: isMultiMeasurement: true ✅
SENSOR_DATA: Multi-measurement sensor detected: Blood Pressure ✅
SENSOR_DATA: Mapping SYSTOLIC → HkfzcXMdLLF ✅
SENSOR_DATA: Mapping DIASTOLIC → BaGxiB8AsNI ✅
SENSOR_DATA: Mapping PULSE → S7OjKl85YSh ✅
SENSOR_SAVE: Saving SYSTOLIC=99 to field HkfzcXMdLLF ✅
SENSOR_SAVE: Saving DIASTOLIC=82 to field BaGxiB8AsNI ✅
SENSOR_SAVE: Saving PULSE=60 to field S7OjKl85YSh ✅
```

## 🔍 Current Problem

From your logs, the issue is:
1. **DataStore config not loading**: `Sensor config found: null`
2. **Falls back to legacy mapping**: `isMultiMeasurement: false`
3. **Only saves first value**: Only SYSTOLIC (99) is saved to wrong field

## 🛠️ Troubleshooting

### If DataStore Update Doesn't Work:

1. **Verify MainPresenter calls fetchSensorConfig()**
   - Check logs for tag "SensorConfig"
   - Should see: "Loaded sensors: 3"

2. **Verify Field UIDs in DHIS2**
   - Go to Maintenance → Data Elements
   - Find "Systolic Pressure", "Diastolic Pressure", "Heart Rate"
   - Verify UIDs match:
     - Systolic: `HkfzcXMdLLF`
     - Diastolic: `BaGxiB8AsNI`
     - Heart Rate: `S7OjKl85YSh`

3. **Clear App Cache**
   - Settings → Apps → DHIS2 → Clear Data
   - Reinstall app if needed

4. **Check API Endpoint**
   - Verify `/api/dataStore/sensorConfig/config` returns correct data
   - Check network logs in the app

## 📁 Related Files

- `UPDATE_DATASTORE_DIASTOLIC_UID.md` - Step-by-step DataStore update guide
- `BP_SENSOR_QUICK_FIX.md` - Problem summary
- `HOW_TO_CONFIGURE_BP_SENSOR_DATASTORE.md` - Detailed configuration guide
- `BP_SENSOR_DATASTORE_CONFIG.json` - Correct configuration template

## ✅ Compilation Status

All files compile successfully with no errors:
- ✅ `FormViewModel.kt` - No diagnostics
- ✅ `FormView.kt` - No diagnostics
- ✅ `FieldProvider.kt` - No diagnostics
- ✅ `SensorConfigRepository.kt` - No diagnostics

## 🚀 Next Steps

1. **Update DataStore** (see UPDATE_DATASTORE_DIASTOLIC_UID.md)
2. **Clear app data** on Android device
3. **Sync metadata** in the app
4. **Test BP sensor** and verify all 3 values are saved
5. **Check logs** for "SENSOR_DATA" and "SENSOR_SAVE" tags

---

**Last Updated**: Current session
**Branch**: BPSensorConfig
**Status**: Code ready, awaiting DataStore configuration update
