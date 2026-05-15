#  Verify DataStore Access

## Your DataStore Entry Exists!

**URL**: https://project.ccdev.org/ictprojects/dhis-web-datastore/index.html#/edit/sensor-config/vital-sensor-mapping

This confirms the DataStore entry is created correctly! 

##  Now Let's Verify API Access

The app uses the **API endpoint**, not the web UI. Let's verify the API is accessible:

### Step 1: Check API Endpoint in Browser

Open this URL in your browser (while logged into DHIS2):

```
https://project.ccdev.org/ictprojects/api/dataStore/sensor-config/vital-sensor-mapping
```

**Expected result**: You should see your JSON configuration.

**If you see JSON**:  API is accessible!

**If you see 404 or error**:  There's an issue with the API endpoint.

### Step 2: Check All DataStore Entries

To see all namespaces:
```
https://project.ccdev.org/ictprojects/api/dataStore
```

To see all keys in `sensor-config` namespace:
```
https://project.ccdev.org/ictprojects/api/dataStore/sensor-config
```

### Step 3: Verify the JSON Structure

When you access the API endpoint, verify the JSON has this structure:

```json
{
  "globalSettings": { ... },
  "sensors": [
    {
      "name": "Temperature",
      ...
    },
    {
      "name": "Pulse Oximeter",
      ...
    },
    {
      "name": "Blood Pressure",
      "measurements": {
        "systolic": {"dataElement": "HkfzcXMdLLF", ...},
        "diastolic": {"dataElement": "BaGxiB8AsNI", ...},
        "pulse": {"dataElement": "S7OjKl85YSh", ...}
      },
      ...
    }
  ]
}
```

##  Important: API Path

I notice your DHIS2 is at:
```
https://project.ccdev.org/ictprojects/
```

The app needs to know this base URL. Let me check if the app is configured correctly...

### Possible Issue: Base URL

If your app is configured with:
-  `https://project.ccdev.org/` (missing `/ictprojects/`)

Then the API calls will go to:
-  `https://project.ccdev.org/api/dataStore/...` (WRONG!)

Instead of:
-  `https://project.ccdev.org/ictprojects/api/dataStore/...` (CORRECT!)

### How to Check

1. **Open the app**
2. **Go to Settings** → **About** or **Server URL**
3. **Verify the server URL** is: `https://project.ccdev.org/ictprojects/`

If it's wrong, you need to:
1. **Logout**
2. **Login again** with the correct server URL
3. **Sync metadata**

##  Quick Test

Before rebuilding the app, let's verify the API is accessible:

### Test 1: Browser Test

1. **Login to DHIS2** in your browser
2. **Open**: `https://project.ccdev.org/ictprojects/api/dataStore/sensor-config/vital-sensor-mapping`
3. **Verify** you see your JSON config

### Test 2: Check Server URL in App

1. **Open the app**
2. **Check** what server URL is configured
3. **Verify** it includes `/ictprojects/`

##  What to Share

Please share:

1. **What you see** when you open:
   ```
   https://project.ccdev.org/ictprojects/api/dataStore/sensor-config/vital-sensor-mapping
   ```
   (Copy the JSON or screenshot)

2. **Server URL** configured in your app
   (Settings → About or Server URL)

3. **Any error messages** from the browser or app

This will help us understand if the issue is:
-  API not accessible
-  Wrong server URL in app
-  Permission issue
-  Something else

##  Expected Outcome

If the API is accessible and the server URL is correct, then after rebuilding with enhanced logging, we should see:

```
SensorConfigApi: DataStore query returned 1 entries 
SensorConfigApi:  Found DataStore entry (XXX chars) 
SensorConfig:  Loaded sensors: 3 
```

If not, the logs will tell us exactly what's wrong!

---

**Next**: Verify API access → Check server URL → Rebuild app → Test with logs
