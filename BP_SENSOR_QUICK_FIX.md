# Blood Pressure Sensor - Quick Fix Summary

## Problem Identified
Your BP sensor sends **3 values** but only **1 value** (108) is being saved to the Diastolic field.

**From your logs:**
```
BleManager: Readings received from device: 3 values
  → SYSTOLIC = 108
  → DIASTOLIC = 55
  → PULSE = 62

SENSOR_DATA: Sensor config found: null 
SENSOR_DATA: isMultiMeasurement: false 
SENSOR_DATA: Using legacy index-based mapping 
SENSOR_DATA: Processing reading 0: key=SYSTOLIC, value=108, targetField=skBarAsIYIL
SENSOR_SAVE: Saving SYSTOLIC=108 to field skBarAsIYIL
```

## Root Cause
1. **No DataStore configuration** - The app cannot find sensor config in DHIS2 DataStore
2. **Falls back to legacy mapping** - Only saves the first value to the primary field
3. **Wrong field mapping** - Saves SYSTOLIC (108) to Diastolic field (skBarAsIYIL)

## Solution Applied

### 1. Updated Diastolic UID 
Changed hardcoded UID from `skBarAsIYIL` to `BaGxiB8AsNI` to match your DHIS2 instance.

**File**: `form/src/main/java/org/dhis2/form/ui/provider/inputfield/FieldProvider.kt`

### 2. Created DataStore Configuration 
Created `BP_SENSOR_DATASTORE_CONFIG.json` with proper mapping:
```json
{
  "measurements": {
    "systolic": {"dataElement": "HkfzcXMdLLF", "unit": "mmHg"},
    "diastolic": {"dataElement": "BaGxiB8AsNI", "unit": "mmHg"},
    "pulse": {"dataElement": "S7OjKl85YSh", "unit": "bpm"}
  }
}
```

### 3. Created Setup Guide 
See `HOW_TO_CONFIGURE_BP_SENSOR_DATASTORE.md` for detailed instructions.

## Next Steps (REQUIRED)

### Option A: Add DataStore Configuration (Recommended)

1. **Open your DHIS2 web interface**
2. **Login as admin**
3. **Open browser console** (F12)
4. **Run this command**:

```javascript
fetch('/api/dataStore/sensorConfig/config', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    "sensors": [{
      "name": "Blood Pressure Monitor",
      "type": "multi",
      "serviceUUID": "00001810-0000-1000-8000-00805f9b34fb",
      "characteristicUUID": "00002A35-0000-1000-8000-00805f9b34fb",
      "macAddress": "C0:26:DA:19:D4:FE",
      "measurements": {
        "systolic": {"dataElement": "HkfzcXMdLLF", "unit": "mmHg"},
        "diastolic": {"dataElement": "BaGxiB8AsNI", "unit": "mmHg"},
        "pulse": {"dataElement": "S7OjKl85YSh", "unit": "bpm"}
      }
    }]
  })
});
```

5. **Rebuild and install the app**
6. **Test the BP sensor**

### Option B: Test Without DataStore (Temporary)

The code has fallback logic, but it won't work properly without DataStore configuration. **You must add the DataStore configuration for proper multi-value mapping.**

## Expected Result After Fix

When you connect the BP sensor, you should see in logs:

```
SENSOR_DATA: Sensor config found: Blood Pressure Monitor 
SENSOR_DATA: isMultiMeasurement: true 
SENSOR_DATA: Multi-measurement sensor detected 
SENSOR_DATA: Mapping SYSTOLIC → HkfzcXMdLLF 
SENSOR_DATA: Mapping DIASTOLIC → BaGxiB8AsNI 
SENSOR_DATA: Mapping PULSE → S7OjKl85YSh 
SENSOR_SAVE: Saving SYSTOLIC=108 to field HkfzcXMdLLF 
SENSOR_SAVE: Saving DIASTOLIC=55 to field BaGxiB8AsNI 
SENSOR_SAVE: Saving PULSE=62 to field S7OjKl85YSh 
```

And all three fields will be filled:
- **Systolic Pressure (HkfzcXMdLLF)**: 108 mmHg
- **Diastolic Pressure (BaGxiB8AsNI)**: 55 mmHg
- **Heart Rate (S7OjKl85YSh)**: 62 bpm

## Files Changed
1. `form/src/main/java/org/dhis2/form/ui/provider/inputfield/FieldProvider.kt` - Updated Diastolic UID
2. `BP_SENSOR_DATASTORE_CONFIG.json` - DataStore configuration
3. `HOW_TO_CONFIGURE_BP_SENSOR_DATASTORE.md` - Detailed setup guide

## Verification Steps

After adding DataStore config and rebuilding:

1. **Clear app data** on your device
2. **Sync metadata** in the app
3. **Connect BP sensor**
4. **Check logs** for "SENSOR_DATA" and "SENSOR_SAVE"
5. **Verify all 3 fields** are filled with correct values

## Support

If you still have issues after adding DataStore configuration:
1. Check the logs for "SENSOR_DATA" tag
2. Verify the DataStore config exists: `GET /api/dataStore/sensorConfig/config`
3. Ensure UIDs match your DHIS2 data elements
4. Clear app data and sync again
