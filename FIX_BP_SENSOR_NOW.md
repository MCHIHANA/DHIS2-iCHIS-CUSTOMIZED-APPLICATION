# ✅ Fix BP Sensor - Simple Steps

## 🎯 The Problem

Your DataStore configuration is **CORRECT** ✅, but the app is only saving one value because it hasn't loaded the configuration yet.

## 🚀 Solution (3 Steps)

### Step 1: Clear App Data

On your Android device:
1. Go to **Settings** → **Apps** → **DHIS2**
2. Tap **Storage**
3. Tap **Clear Data** (or "Clear Storage")
4. Confirm

### Step 2: Sync Metadata

1. **Open the app** and login
2. Go to **Settings** → **Sync metadata**
3. **Wait** for sync to complete (this fetches your DataStore config)

### Step 3: Test BP Sensor

1. **Open a form** with BP fields
2. **Tap ANY ONE field** (systolic, diastolic, or pulse - doesn't matter which)
3. **Connect BP sensor**
4. **All 3 values should save automatically** ✅

## 📊 Expected Result

After syncing metadata, when you connect the BP sensor:

**BP Device Shows:**
- SYS: 120 (big font)
- DIA: 80 (big font)
- Pulse: 72 bpm (small font, bottom right)

**App Should Save:**
- Systolic Pressure (HkfzcXMdLLF) = 120 ✅
- Diastolic Pressure (BaGxiB8AsNI) = 80 ✅
- Pulse Rate (S7OjKl85YSh) = 72 ✅

**You should only need to tap ONE field, not all three!**

## 🔍 If It Still Doesn't Work

Check the logs for these lines (filter by "SENSOR_DATA"):

**Good (DataStore loaded):**
```
SENSOR_DATA: Sensor config found: Blood Pressure ✅
SENSOR_DATA: isMultiMeasurement: true ✅
SENSOR_DATA: Mapping SYSTOLIC → HkfzcXMdLLF
SENSOR_DATA: Mapping DIASTOLIC → BaGxiB8AsNI
SENSOR_DATA: Mapping PULSE → S7OjKl85YSh
```

**Bad (DataStore NOT loaded):**
```
SENSOR_DATA: Sensor config found: null ❌
SENSOR_DATA: isMultiMeasurement: false ❌
SENSOR_DATA: Using legacy index-based mapping
```

If you see "Sensor config found: null", the DataStore didn't load. Try:
1. **Verify DataStore** in browser: `https://your-dhis2-instance/api/dataStore/sensor-config/vital-sensor-mapping`
2. **Reinstall the app** (sometimes helps with cache issues)
3. **Sync metadata again**

## 📝 Summary

Your configuration is correct! The app just needs to:
1. **Fetch** the DataStore config (happens during metadata sync)
2. **Load** it into memory
3. **Use** it to map all 3 BP values

**Clear data → Sync metadata → Test** should fix it! 🎉

---

**If it still doesn't work after these steps, share the logs (filter by "SENSOR_DATA" and "SensorConfig") and we'll debug further.**
