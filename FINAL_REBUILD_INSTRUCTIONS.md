#  Final Rebuild with Complete Logging

## What I Added

I've added comprehensive logging at **every step** of the DataStore loading process:

1. **MainPresenter** - Logs when `fetchSensorConfig()` is called
2. **SensorConfigRepository** - Logs the entire fetch process
3. **SensorConfigApi** - Logs the DataStore query and parsing

##  Rebuild and Test

### Step 1: Rebuild the App

```bash
./gradlew clean assembleDebug
```

### Step 2: Install on Device

Install the new APK on your Android device.

### Step 3: Clear App Data

Settings → Apps → DHIS2 → Storage → Clear Data

### Step 4: Open App and Watch Logs

**Filter by these tags:**
- `MainPresenter`
- `SensorConfig`
- `SensorConfigApi`

**You should see:**
```
MainPresenter: === fetchSensorConfig() called in MainPresenter ===
MainPresenter: Launching coroutine to fetch sensor config...
SensorConfig: === fetchSensorConfig() called ===
SensorConfig: Calling API to fetch sensor config...
SensorConfigApi: === getSensorConfig() called ===
SensorConfigApi: Querying DataStore: namespace='sensor-config', key='vital-sensor-mapping'
```

### Step 5: Test BP Sensor

1. Open a form with BP fields
2. Tap any BP field
3. Connect BP sensor
4. Check logs for `SENSOR_DATA` tag

##  What the Logs Will Tell Us

### Scenario 1: fetchSensorConfig() Not Called

**Logs show**: Nothing with `MainPresenter` or `SensorConfig` tags

**Meaning**: The method isn't being called at all

**Solution**: Need to check when MainPresenter is initialized

### Scenario 2: fetchSensorConfig() Called But Fails

**Logs show**:
```
MainPresenter: === fetchSensorConfig() called in MainPresenter ===
MainPresenter: Launching coroutine to fetch sensor config...
SensorConfig: === fetchSensorConfig() called ===
SensorConfigApi: === getSensorConfig() called ===
SensorConfigApi: DataStore query returned 0 entries 
```

**Meaning**: DataStore entry doesn't exist or can't be accessed

**Solution**: Verify DataStore at `https://project.ccdev.org/ictprojects/api/dataStore/sensor-config/vital-sensor-mapping`

### Scenario 3: fetchSensorConfig() Succeeds

**Logs show**:
```
MainPresenter: === fetchSensorConfig() called in MainPresenter ===
SensorConfig:  Loaded sensors: 3
SensorConfig:   - Temperature
SensorConfig:   - Pulse Oximeter
SensorConfig:   - Blood Pressure
MainPresenter:  Sensor config fetch completed
```

**Then when testing BP sensor**:
```
SENSOR_DATA: Sensor config found: Blood Pressure 
SENSOR_DATA: isMultiMeasurement: true 
SENSOR_DATA: Mapping SYSTOLIC → HkfzcXMdLLF
SENSOR_DATA: Mapping DIASTOLIC → BaGxiB8AsNI
SENSOR_DATA: Mapping PULSE → S7OjKl85YSh
```

**Meaning**: Everything works! All 3 values should be saved.

##  What to Share

After rebuilding and testing, share logs with these tags:

1. **MainPresenter** - Shows if fetchSensorConfig is called
2. **SensorConfig** - Shows the fetch process
3. **SensorConfigApi** - Shows the DataStore query
4. **SENSOR_DATA** - Shows the BP sensor data processing

##  Expected Outcome

With this comprehensive logging, we'll know **exactly** where the DataStore loading is failing:

-  Is MainPresenter calling fetchSensorConfig?
-  Is the coroutine launching?
-  Is the API being called?
-  Is the DataStore query finding the entry?
-  Is the JSON parsing successfully?
-  Are the sensors being loaded into memory?

Then we can fix the exact issue!

---

**Next**: Rebuild → Install → Clear data → Open app → Check logs → Share results
