#  Rebuild App with Enhanced Logging

## What I Did

I added **detailed logging** to the sensor config loading code so we can see exactly what's happening when the app tries to load your DataStore configuration.

##  Next Steps

### Step 1: Rebuild the App

You need to rebuild and reinstall the app with the new logging:

```bash
./gradlew clean assembleDebug
```

Then install the new APK on your device.

### Step 2: Clear App Data

On your Android device:
1. Settings â†’ Apps â†’ DHIS2
2. Storage â†’ Clear Data
3. Confirm

### Step 3: Open App and Check Logs

1. **Open the app** and login
2. **Immediately check logs** for tag: `SensorConfig` or `SensorConfigApi`

You should see logs like:
```
SensorConfig: === fetchSensorConfig() called ===
SensorConfig: Calling API to fetch sensor config...
SensorConfigApi: === getSensorConfig() called ===
SensorConfigApi: Querying DataStore: namespace='sensor-config', key='vital-sensor-mapping'
```

### Step 4: Sync Metadata

1. Go to **Settings** â†’ **Sync metadata**
2. **Watch the logs** during sync
3. Look for:
   - `SensorConfig:  Loaded sensors: 3` (SUCCESS)
   - OR `SensorConfigApi:  No DataStore entry found!` (FAILURE)

### Step 5: Test BP Sensor

1. Open a form with BP fields
2. Tap any BP field
3. Connect BP sensor
4. **Check logs** for:
   ```
   SENSOR_DATA: Sensor config found: Blood Pressure 
   ```

##  What the Logs Will Tell Us

### If DataStore is Loading Successfully:

```
SensorConfig: === fetchSensorConfig() called ===
SensorConfigApi: === getSensorConfig() called ===
SensorConfigApi: DataStore query returned 1 entries
SensorConfigApi:  Found DataStore entry (XXX chars)
SensorConfigApi:  Successfully parsed 3 sensors
SensorConfig:  Loaded sensors: 3
SensorConfig:   - Temperature
SensorConfig:   - Pulse Oximeter
SensorConfig:   - Blood Pressure
SensorConfig: === Config loaded successfully ===
```

**Then when you test BP sensor:**
```
SENSOR_DATA: Sensor config found: Blood Pressure 
SENSOR_DATA: isMultiMeasurement: true 
SENSOR_DATA: Mapping SYSTOLIC â†’ HkfzcXMdLLF
SENSOR_DATA: Mapping DIASTOLIC â†’ BaGxiB8AsNI
SENSOR_DATA: Mapping PULSE â†’ S7OjKl85YSh
```

### If DataStore is NOT Loading:

```
SensorConfig: === fetchSensorConfig() called ===
SensorConfigApi: === getSensorConfig() called ===
SensorConfigApi: DataStore query returned 0 entries 
SensorConfigApi:  No DataStore entry found!
SensorConfigApi: Make sure you have created the DataStore entry:
SensorConfigApi:   Namespace: sensor-config
SensorConfigApi:   Key: vital-sensor-mapping
SensorConfig:  Fetch failed! Error: Config not found in DataStore
```

**This means:**
- The DataStore entry doesn't exist in DHIS2
- OR the namespace/key is wrong
- OR the app doesn't have permission to access it

##  Possible Issues and Solutions

### Issue 1: DataStore Entry Doesn't Exist

**Solution**: Verify in browser:
```
https://your-dhis2-instance/api/dataStore/sensor-config/vital-sensor-mapping
```

Should return your JSON config. If it returns 404, the entry doesn't exist!

### Issue 2: Wrong Namespace or Key

**Solution**: Check what's actually in your DataStore:
```
https://your-dhis2-instance/api/dataStore
```

This lists all namespaces. Find yours and verify the key.

### Issue 3: Permission Issue

**Solution**: Make sure your user has permission to read DataStore entries.

### Issue 4: Metadata Not Syncing

**Solution**: 
1. Check network connection
2. Check DHIS2 server is accessible
3. Try manual sync: Settings â†’ Sync metadata

##  What to Share

After rebuilding and testing, share these logs:

1. **Logs with tag `SensorConfig`** (from app start and metadata sync)
2. **Logs with tag `SensorConfigApi`** (from DataStore query)
3. **Logs with tag `SENSOR_DATA`** (from BP sensor test)

This will tell us exactly why the DataStore isn't loading!

##  Expected Outcome

After rebuilding with enhanced logging, we'll be able to see:
-  Is `fetchSensorConfig()` being called?
-  Is the DataStore query returning results?
-  Is the JSON parsing successfully?
-  Are the sensors being loaded into memory?

Then we can fix the exact issue preventing the config from loading!

---

**Next**: Rebuild app â†’ Clear data â†’ Check logs â†’ Share results
