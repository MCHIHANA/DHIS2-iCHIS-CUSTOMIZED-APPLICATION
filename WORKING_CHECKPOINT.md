# ✅ WORKING CHECKPOINT — BP + Temperature Sensors

## Date: May 2026
## Branch: BPSensorConfig
## Commit: This one — "CHECKPOINT: BP and Temperature sensors fully working"

---

## What is working at this point

### Blood Pressure Sensor (FORA D40b — MAC: C0:26:DA:19:D4:FE)
- ✅ Connects automatically when you tap any BP field
- ✅ Saves all 3 values to correct fields:
  - Systolic Pressure → HkfzcXMdLLF
  - Diastolic Pressure → BaGxiB8AsNI
  - Pulse Rate → S7OjKl85YSh
- ✅ No DataStore dependency — hardcoded mapping in FormViewModel.kt
- ✅ Fast connection (direct GATT, not background autoConnect)

### Temperature Sensor (FORA IR42 — MAC: C0:26:DA:1B:06:A4)
- ✅ Connects automatically when you tap the temperature field
- ✅ Saves temperature value → KXNH45ts16S
- ✅ Parses IEEE-11073 FLOAT format correctly

---

## Key files and what they do

| File | Purpose |
|------|---------|
| `FormViewModel.kt` | Routes sensor readings to correct DHIS2 fields. Contains hardcoded BP map. |
| `BleDeviceConnector.kt` | GATT connection, service discovery, data parsing for all sensors |
| `BleManager.kt` | Orchestrates scan + connection lifecycle |
| `BleScanner.kt` | Unfiltered BLE scan, matches by MAC address or device name |
| `KnownDevices.kt` | MAC addresses for all 3 sensors |
| `SensorConnectionBottomSheet.kt` | The "Connect to Sensor" dialog UI |
| `FieldProvider.kt` | Shows "Connect to Sensor" button on known sensor fields |

---

## If something breaks — revert to this commit

```bash
git checkout BPSensorConfig
git log --oneline   # find this commit hash
git reset --hard <this-commit-hash>
git push origin BPSensorConfig --force
```

Then rebuild:
```bash
./gradlew :app:assembleDhis2Debug
```

---

## Known sensor field UIDs (hardcoded in FormViewModel.kt)

| Sensor | Field | DHIS2 UID |
|--------|-------|-----------|
| Temperature | Temperature | KXNH45ts16S |
| BP | Systolic Pressure | HkfzcXMdLLF |
| BP | Diastolic Pressure | BaGxiB8AsNI |
| BP | Pulse Rate | S7OjKl85YSh |
| SpO2 | SpO2 | VqwQWWDmYLn |
| SpO2 | Pulse Rate | tZbUrUbhUNy |

---

## Build command that works

```bash
./gradlew :app:assembleDhis2Debug
```

NOT `assembleDebug` — that builds all 3 flavors and runs out of memory.
