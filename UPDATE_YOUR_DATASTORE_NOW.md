#  UPDATE YOUR DATASTORE NOW

## Problem Found

Your current DataStore has **WRONG UIDs** for Diastolic and Pulse fields:

| Field | Current (WRONG) | Should Be (CORRECT) |
|-------|----------------|---------------------|
| Systolic | `HkfzcXMdLLF` | `HkfzcXMdLLF`  |
| Diastolic | `skBarAsIYIL`  | `BaGxiB8AsNI`  |
| Pulse | `tZbUrUbhUNy`  | `S7OjKl85YSh`  |

## Quick Fix (2 Steps)

### Step 1: Delete Old Config

Open DHIS2 web interface → Login → Press F12 (browser console) → Run:

```javascript
fetch('/api/dataStore/sensorConfig/config', {
  method: 'DELETE'
}).then(r => console.log('Deleted:', r.ok));
```

### Step 2: Add Corrected Config

Copy the **entire JSON** from `CORRECTED_DATASTORE_CONFIG.json` and run:

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
        "characteristicUUID": "00002A1C-0000-1000-8000-00805f9b34fb",
        "macAddress": "C0:26:DA:1B:06:A4",
        "manualAllowed": true,
        "measurements": {
          "temperature": {
            "dataElement": "KXNH45ts16S",
            "unit": "°C"
          }
        },
        "name": "Temperature",
        "sensorRequired": true,
        "serviceUUID": "00001809-0000-1000-8000-00805f9b34fb",
        "type": "single"
      },
      {
        "characteristicUUID": "00001524-1212-efde-1523-785feabcd123",
        "macAddress": "C0:26:DA:17:D5:7D",
        "manualAllowed": true,
        "measurements": {
          "pulseRate": {
            "dataElement": "tZbUrUbhUNy",
            "unit": "bpm"
          },
          "spo2": {
            "dataElement": "VqwQWWDmYLn",
            "unit": "%"
          }
        },
        "name": "Pulse Oximeter",
        "sensorRequired": true,
        "serviceUUID": "00001523-1212-efde-1523-785feabcd123",
        "type": "multi"
      },
      {
        "characteristicUUID": "00002A35-0000-1000-8000-00805f9b34fb",
        "macAddress": "C0:26:DA:19:D4:FE",
        "manualAllowed": true,
        "measurements": {
          "diastolic": {
            "dataElement": "BaGxiB8AsNI",
            "unit": "mmHg"
          },
          "pulse": {
            "dataElement": "S7OjKl85YSh",
            "unit": "bpm"
          },
          "systolic": {
            "dataElement": "HkfzcXMdLLF",
            "unit": "mmHg"
          }
        },
        "name": "Blood Pressure",
        "sensorRequired": true,
        "serviceUUID": "00001810-0000-1000-8000-00805f9b34fb",
        "type": "multi"
      }
    ]
  })
})
.then(r => r.json())
.then(data => console.log(' Updated:', data))
.catch(e => console.error('Error:', e));
```

### Step 3: Verify

```javascript
fetch('/api/dataStore/sensorConfig/config')
  .then(r => r.json())
  .then(data => {
    const bp = data.sensors.find(s => s.name === 'Blood Pressure');
    console.log('BP Sensor UIDs:');
    console.log('  Systolic:', bp.measurements.systolic.dataElement);
    console.log('  Diastolic:', bp.measurements.diastolic.dataElement);
    console.log('  Pulse:', bp.measurements.pulse.dataElement);
  });
```

**Expected output:**
```
BP Sensor UIDs:
  Systolic: HkfzcXMdLLF
  Diastolic: BaGxiB8AsNI
  Pulse: S7OjKl85YSh
```

### Step 4: Clear App & Test

1. **Android device**: Settings → Apps → DHIS2 → Clear Data
2. **Open app** → Login → Settings → Sync metadata
3. **Test BP sensor** → All 3 values should save correctly

##  IMPORTANT: Verify UIDs First!

Before updating DataStore, **verify your actual UIDs** in DHIS2:

1. Go to **Maintenance** → **Data Elements**
2. Search for "Diastolic Pressure" → Copy UID
3. Search for "Pulse Rate" → Copy UID

If the UIDs are different from what I have (`BaGxiB8AsNI` and `S7OjKl85YSh`), update the DataStore JSON with YOUR actual UIDs before running the script.

## How to Find UIDs

In DHIS2 Maintenance → Data Elements:

1. Click on "Diastolic Pressure" data element
2. Look for **UID** field (usually at the top)
3. Copy the UID (format: `XXXXXXXXXXX` - 11 characters)
4. Repeat for "Pulse Rate"

## Why This Matters

The app uses UIDs to save data, not names. If the DataStore has wrong UIDs:
-  BP sensor sends 3 values
-  App tries to save to wrong fields
-  Only 1 value gets saved (or none)

With correct UIDs:
-  BP sensor sends 3 values
-  App maps to correct fields
-  All 3 values saved successfully

---

**Next Step**: Update DataStore using the script above, then test!
