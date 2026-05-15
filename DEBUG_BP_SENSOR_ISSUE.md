#  Debug: BP Sensor Only Saving One Value

##  What's Correct

1. **DataStore Configuration** 
   - Namespace: `sensor-config` 
   - Key: `vital-sensor-mapping` 
   - UIDs are correct:
     - Systolic: `HkfzcXMdLLF` 
     - Diastolic: `BaGxiB8AsNI` 
     - Pulse: `S7OjKl85YSh` 

2. **App Code** 
   - Looking for correct namespace/key 
   - Multi-measurement mapping logic implemented 

##  The Problem

**Only the field you tap first gets the systolic value.** This means:
- The app is NOT loading the DataStore configuration
- Falls back to legacy single-value mapping
- Only saves to the "primary" field (whichever you tapped)

##  Root Cause Analysis

The BP sensor sends data in this order:
1. **SYSTOLIC** (first value, big font on device)
2. **DIASTOLIC** (second value, big font on device)
3. **PULSE** (third value, small font bottom right)

But the app is using **legacy index-based mapping**:
```kotlin
readings.forEachIndexed { index, (key, value) ->
    val fieldUid = when (index) {
        0 -> primaryUid  // ← Only this gets saved!
        1 -> secondarySensorFieldUid ?: return@forEachIndexed
        else -> return@forEachIndexed
    }
}
```

This means:
- Index 0 (SYSTOLIC) → saved to whichever field you tapped
- Index 1 (DIASTOLIC) → skipped (secondarySensorFieldUid is null)
- Index 2 (PULSE) → skipped (else branch)

##  Why DataStore Isn't Loading

### Possible Reasons:

1. **App hasn't synced metadata yet**
   - DataStore is fetched during metadata sync
   - If you haven't synced since adding the config, app won't have it

2. **MainPresenter not calling fetchSensorConfig()**
   - Check if `MainPresenter.fetchSensorConfig()` is being called
   - Should be called in `onCreate()` or similar

3. **Network/API error**
   - DataStore fetch might be failing silently
   - Check for exceptions in logs

4. **Cache issue**
   - Old cached config might be interfering
   - SharedPreferences might have old data

##  Debugging Steps

### Step 1: Check if DataStore is Being Fetched

Look for these logs (tag: "SensorConfig"):
```
SensorConfig: Loaded sensors: 3
SensorConfig: Blood Pressure
SensorConfig: 00001810-0000-1000-8000-00805f9b34fb
```

**If you DON'T see these logs:**
- DataStore is NOT being fetched
- Go to Step 2

**If you DO see these logs:**
- DataStore IS being fetched
- Go to Step 3

### Step 2: Force Metadata Sync

1. **Clear app data**: Settings → Apps → DHIS2 → Clear Data
2. **Open app** and login
3. **Sync metadata**: Settings → Sync metadata
4. **Wait** for sync to complete
5. **Check logs** for "SensorConfig" tag
6. **Test BP sensor** again

### Step 3: Check Sensor Data Logs

When you connect the BP sensor, look for these logs (tag: "SENSOR_DATA"):

**Expected (if DataStore loaded):**
```
SENSOR_DATA: Received 3 readings for primary field: HkfzcXMdLLF
SENSOR_DATA: Sensor config found: Blood Pressure 
SENSOR_DATA: isMultiMeasurement: true 
SENSOR_DATA: Multi-measurement sensor detected: Blood Pressure 
SENSOR_DATA: Mapping SYSTOLIC → HkfzcXMdLLF
SENSOR_DATA: Mapping DIASTOLIC → BaGxiB8AsNI
SENSOR_DATA: Mapping PULSE → S7OjKl85YSh
```

**Actual (if DataStore NOT loaded):**
```
SENSOR_DATA: Received 3 readings for primary field: HkfzcXMdLLF
SENSOR_DATA: Sensor config found: null 
SENSOR_DATA: isMultiMeasurement: false 
SENSOR_DATA: Using legacy index-based mapping 
SENSOR_DATA: Processing reading 0: key=SYSTOLIC, value=120, targetField=HkfzcXMdLLF
```

### Step 4: Verify DataStore API Endpoint

In your browser, check if the DataStore is accessible:
```
https://your-dhis2-instance/api/dataStore/sensor-config/vital-sensor-mapping
```

Should return your JSON configuration.

### Step 5: Check MainPresenter

Verify `MainPresenter.fetchSensorConfig()` is being called:

1. Look for this log (tag: "MainPresenter" or "SensorConfig"):
   ```
   Fetching sensor config...
   ```

2. If you don't see it, the method might not be called on app start

##  Quick Fix

### Option 1: Force Sync (Recommended)

1. **Clear app data** completely
2. **Reinstall app** (optional but recommended)
3. **Login**
4. **Sync metadata** (Settings → Sync)
5. **Test BP sensor**

### Option 2: Check Logs First

Before clearing data, check the logs to understand what's happening:

1. **Connect BP sensor**
2. **Filter logs** by tag: `SENSOR_DATA`
3. **Look for**: "Sensor config found: null" or "Sensor config found: Blood Pressure"
4. **Share the logs** so we can see exactly what's happening

##  What to Check in Logs

When you test the BP sensor, look for these specific log lines:

```
BleManager: Readings received from device: 3 values
BleManager:   → SYSTOLIC = 120
BleManager:   → DIASTOLIC = 80
BleManager:   → PULSE = 72

SENSOR_DATA: Received 3 readings for primary field: [FIELD_UID]
SENSOR_DATA: Sensor config found: [null or Blood Pressure]
SENSOR_DATA: isMultiMeasurement: [true or false]
```

**If "Sensor config found: null":**
- DataStore is NOT loaded
- Clear app data and sync metadata

**If "Sensor config found: Blood Pressure":**
- DataStore IS loaded
- But mapping might still be failing
- Share the full logs

##  Expected Behavior After Fix

When DataStore is loaded correctly:

1. **Tap any BP field** (systolic, diastolic, or pulse)
2. **Connect BP sensor**
3. **All 3 values** should be saved automatically:
   - Systolic → HkfzcXMdLLF
   - Diastolic → BaGxiB8AsNI
   - Pulse → S7OjKl85YSh

You should NOT need to tap each field individually!

##  Next Steps

1. **Clear app data** and **sync metadata**
2. **Test BP sensor** and check logs
3. **Share the logs** (filter by "SENSOR_DATA" and "SensorConfig" tags)
4. We'll debug from there

---

**Most likely issue**: App hasn't synced metadata since you added the DataStore config. Clear data + sync should fix it!
