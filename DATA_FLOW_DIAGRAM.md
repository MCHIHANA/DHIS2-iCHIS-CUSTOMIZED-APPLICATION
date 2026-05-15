# Blood Pressure Sensor - Data Flow Diagram

## Complete Data Flow: User Action → DHIS2 Server

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          USER INTERACTION LAYER                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
                    ┌──────────────────────────────────┐
                    │  User clicks "Connect BP" button │
                    │  in DHIS2 form                   │
                    └──────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            UI LAYER (Compose)                                │
│  FormView.kt → FormViewModel.kt                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
                    ┌──────────────────────────────────┐
                    │  FormViewModel.startSensorScan() │
                    │  activeSensorFieldUid = "HkfzcX" │
                    │  secondaryFieldUid = "skBarAsI"  │
                    └──────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BLE MANAGER LAYER                                    │
│  BleManager.kt                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
                    ┌──────────────────────────────────┐
                    │  BleManager.startScan()          │
                    │  → BleScanner.startScan()        │
                    └──────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          BLE SCANNER LAYER                                   │
│  BleScanner.kt                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  Android BLE Stack                                  │
        │  - Start LOW_LATENCY scan                           │
        │  - Listen for advertisements                        │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  FORA D40b Blood Pressure Monitor                   │
        │  MAC: C0:26:DA:19:D4:FE                             │
        │  Advertises: Service UUID 0x1810                    │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  BleScanner.onScanResult()                          │
        │  - Match by MAC OR Service UUID OR Name             │
        │  - Stop scan                                        │
        │  - Call onTargetFound(device)                       │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      BLE CONNECTION LAYER                                    │
│  BleDeviceConnector.kt                                                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  BleDeviceConnector.connect()                       │
        │  - device.connectGatt(autoConnect=true)             │
        │  - sensorType = BLOOD_PRESSURE                      │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  onConnectionStateChange()                          │
        │  - STATE_CONNECTED                                  │
        │  - gatt.discoverServices()                          │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  onServicesDiscovered()                             │
        │  - Find service 0x1810                              │
        │  - Find characteristic 0x2A35                       │
        │  - subscribeBloodPressure(gatt)                     │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  enableCharacteristic()                             │
        │  - setCharacteristicNotification(true)              │
        │  - Write ENABLE_INDICATION_VALUE to CCCD            │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  onDescriptorWrite()                                │
        │  - CCCD write SUCCESS                               │
        │  - Waiting for data...                              │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        PHYSICAL MEASUREMENT                                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  User performs BP measurement on FORA D40b          │
        │  1. Place cuff on arm                               │
        │  2. Press START button                              │
        │  3. Cuff inflates                                   │
        │  4. Cuff deflates                                   │
        │  5. Device beeps (measurement complete)             │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  FORA D40b sends BLE INDICATION                     │
        │  Characteristic: 0x2A35                             │
        │  Data: [00 78 00 50 00 5E 00 4B 00]                 │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      BLE NOTIFICATION HANDLER                                │
│  BleDeviceConnector.kt                                                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  onCharacteristicChanged()                          │
        │  - Receive byte array                               │
        │  - Log raw packet (BLE_RAW)                         │
        │  - dispatchReading(uuid, data)                      │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  handleBloodPressureData()                          │
        │  - Call BleDataParser.parseBloodPressure(data)      │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PARSER LAYER                                        │
│  BleDataParser.kt                                                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  parseBloodPressure(data)                           │
        │  Raw: [00 78 00 50 00 5E 00 4B 00]                  │
        │                                                     │
        │  Byte 0: Flags = 0x00                               │
        │    → Units: mmHg                                    │
        │    → Pulse rate: false                              │
        │                                                     │
        │  Bytes 1-2: Systolic SFLOAT                         │
        │    → parseSFloat(data, 1)                           │
        │    → raw = 0x0078                                   │
        │    → mantissa = 120, exponent = 0                   │
        │    → value = 120 × 10^0 = 120.0 mmHg                │
        │                                                     │
        │  Bytes 3-4: Diastolic SFLOAT                        │
        │    → parseSFloat(data, 3)                           │
        │    → raw = 0x0050                                   │
        │    → mantissa = 80, exponent = 0                    │
        │    → value = 80 × 10^0 = 80.0 mmHg                  │
        │                                                     │
        │  Bytes 5-6: MAP SFLOAT                              │
        │    → parseSFloat(data, 5)                           │
        │    → raw = 0x005E                                   │
        │    → mantissa = 94, exponent = 0                    │
        │    → value = 94 × 10^0 = 94.0 mmHg                  │
        │                                                     │
        │  Bytes 7-8: Pulse SFLOAT (if present)               │
        │    → parseSFloat(data, 7)                           │
        │    → raw = 0x004B                                   │
        │    → mantissa = 75, exponent = 0                    │
        │    → value = 75 × 10^0 = 75.0 bpm                   │
        │                                                     │
        │  Return: BloodPressureReading(120, 80, 94, 75)      │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  Validate ranges                                    │
        │  - Systolic: 50-250 mmHg                           │
        │  - Diastolic: 30-150 mmHg                          │
        │  - Pulse: 30-250 bpm                               │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  Build readings list with semantic keys             │
        │  [                                                  │
        │    ("SYSTOLIC", "120"),                             │
        │    ("DIASTOLIC", "80"),                             │
        │    ("PULSE", "75")                                  │
        │  ]                                                  │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  onReadingsReceived(readings)                       │
        │  → Emit to BleManager                               │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BLE MANAGER LAYER                                    │
│  BleManager.kt                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  _sensorData.value = readings                       │
        │  → StateFlow emits to ViewModel                     │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         VIEWMODEL LAYER                                      │
│  FormViewModel.kt                                                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  observeSensorData()                                │
        │  bleManager.sensorData.collect { readings ->        │
        │                                                     │
        │    readings.forEachIndexed { index, (key, value) -> │
        │      val fieldUid = when (index) {                  │
        │        0 -> "HkfzcXMdLLF"  // systolic              │
        │        1 -> "skBarAsIYIL"  // diastolic             │
        │        2 -> "tZbUrUbhUNy"  // pulse                 │
        │      }                                              │
        │                                                     │
        │      submitIntent(                                  │
        │        FormIntent.OnSave(                           │
        │          fieldUid,                                  │
        │          value,                                     │
        │          ValueType.NUMBER                           │
        │        )                                            │
        │      )                                              │
        │    }                                                │
        │  }                                                  │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         REPOSITORY LAYER                                     │
│  FormRepository.kt                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  Save values to local database                      │
        │  - Field HkfzcXMdLLF = "120"                        │
        │  - Field skBarAsIYIL = "80"                         │
        │  - Field tZbUrUbhUNy = "75"                         │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DHIS2 SDK LAYER                                      │
│  D2 (DHIS2 Android SDK)                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  Store in local SQLite database                     │
        │  - TrackedEntityDataValue / EventDataValue          │
        │  - Mark as pending sync                             │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            UI UPDATE                                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  Form fields auto-populated:                        │
        │  ┌─────────────────────────────────────┐            │
        │  │ Systolic BP:    [120] mmHg          │            │
        │  │ Diastolic BP:   [80]  mmHg          │            │
        │  │ Pulse Rate:     [75]  bpm           │            │
        │  └─────────────────────────────────────┘            │
        │                                                     │
        │  Status: "Data received: 120"                       │
        │  Status: "Data received: 80"                        │
        │  Status: "Data received: 75"                        │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         USER SAVES FORM                                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  User clicks "Save" button                          │
        │  → Form validation                                  │
        │  → Save to local database                           │
        │  → Mark for sync                                    │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SYNC TO SERVER                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  DHIS2 SDK Sync Manager                             │
        │  - Upload pending data                              │
        │  - POST to DHIS2 API                                │
        │  - Receive confirmation                             │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────┐
        │  DHIS2 Server                                       │
        │  - Store in PostgreSQL database                     │
        │  - Data available in web interface                  │
        │  - Data available in analytics                      │
        └─────────────────────────────────────────────────────┘
                                      │
                                      ▼
                             COMPLETE 
```

---

## Key Components Explained

### 1. BleScanner
- **Purpose**: Discover BLE devices
- **Input**: None (starts scan)
- **Output**: BluetoothDevice when target found
- **Matching**: MAC address, Service UUID, or device name

### 2. BleDeviceConnector
- **Purpose**: Connect and subscribe to notifications
- **Input**: BluetoothDevice, SensorType
- **Output**: Raw byte arrays from notifications
- **Lifecycle**: Connect → Discover → Subscribe → Receive

### 3. BleDataParser
- **Purpose**: Parse BLE packets into readable values
- **Input**: Raw byte array
- **Output**: BloodPressureReading(systolic, diastolic, map, pulse)
- **Format**: IEEE-11073 16-bit SFLOAT

### 4. BleManager
- **Purpose**: Coordinate BLE operations
- **Input**: Scan/connect commands
- **Output**: StateFlow<List<Pair<String, String>>>
- **State**: Manages connection state and readings

### 5. FormViewModel
- **Purpose**: Bridge between BLE and DHIS2 form
- **Input**: Sensor readings
- **Output**: Form field updates
- **Mapping**: Semantic keys → Data element UIDs

### 6. FormRepository
- **Purpose**: Save data to DHIS2 SDK
- **Input**: Field UID + value
- **Output**: Saved to local database
- **Sync**: Marked for server sync

---

## Data Transformation Flow

```
Raw BLE Packet
[00 78 00 50 00 5E 00 4B 00]
           ↓
IEEE-11073 SFLOAT Parsing
mantissa × 10^exponent
           ↓
BloodPressureReading
(systolic=120, diastolic=80, pulse=75)
           ↓
Semantic Key Pairs
[("SYSTOLIC", "120"), ("DIASTOLIC", "80"), ("PULSE", "75")]
           ↓
Data Element Mapping
[("HkfzcXMdLLF", "120"), ("skBarAsIYIL", "80"), ("tZbUrUbhUNy", "75")]
           ↓
DHIS2 Database
TrackedEntityDataValue / EventDataValue
           ↓
DHIS2 Server
PostgreSQL database
```

---

## State Flow

```
┌──────────────┐
│ DISCONNECTED │ ← Initial state
└──────────────┘
       │
       │ User clicks "Connect BP"
       ▼
┌──────────────┐
│   SCANNING   │ ← Looking for device
└──────────────┘
       │
       │ Device found
       ▼
┌──────────────┐
│  CONNECTING  │ ← Establishing connection
└──────────────┘
       │
       │ Connection established
       ▼
┌──────────────┐
│  CONNECTED   │ ← Waiting for measurement
└──────────────┘
       │
       │ User takes measurement
       ▼
┌──────────────┐
│ DATA RECEIVED│ ← Notification received
└──────────────┘
       │
       │ Parsed and saved
       ▼
┌──────────────┐
│   COMPLETE   │ ← Fields populated
└──────────────┘
```

---

## Error Handling Flow

```
                    ┌─────────────────┐
                    │  Any Operation  │
                    └─────────────────┘
                            │
                            ▼
                    ┌─────────────────┐
                    │  Try Operation  │
                    └─────────────────┘
                            │
                ┌───────────┴───────────┐
                ▼                       ▼
        ┌──────────────┐        ┌──────────────┐
        │   SUCCESS    │        │    ERROR     │
        └──────────────┘        └──────────────┘
                │                       │
                │                       ▼
                │               ┌──────────────┐
                │               │  Log Error   │
                │               └──────────────┘
                │                       │
                │                       ▼
                │               ┌──────────────┐
                │               │ Notify User  │
                │               └──────────────┘
                │                       │
                │                       ▼
                │               ┌──────────────┐
                │               │ Retry Logic  │
                │               │ (if applicable)│
                │               └──────────────┘
                │                       │
                └───────────┬───────────┘
                            ▼
                    ┌─────────────────┐
                    │    Continue     │
                    └─────────────────┘
```

---

## Timing Diagram

```
Time →

User Action:     [Click]                    [Measure]
                    │                           │
BLE Scan:           ├─[Scan 2s]─[Found]        │
                    │                           │
Connection:         │           ├─[Connect 3s]─[Connected]
                    │                           │
Subscription:       │                           ├─[Subscribe 1s]─[Ready]
                    │                           │
Measurement:        │                           │                ├─[30s]─[Complete]
                    │                           │                │
Notification:       │                           │                │      ├─[Received]
                    │                           │                │      │
Parsing:            │                           │                │      ├─[Parse <1ms]
                    │                           │                │      │
UI Update:          │                           │                │      ├─[Update <1ms]
                    │                           │                │      │
Total Time:         0s                          5s               35s    36s

Legend:
├─[Action]─  = Operation with duration
[Event]      = Instant event
```

---

## Memory Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Process                          │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              FormViewModel                          │   │
│  │  - activeSensorFieldUid: String?                    │   │
│  │  - secondarySensorFieldUid: String?                 │   │
│  │  - _sensorStatuses: MutableStateFlow                │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              BleManager                             │   │
│  │  - _devices: MutableStateFlow<List<Device>>         │   │
│  │  - _connectionState: MutableStateFlow<State>        │   │
│  │  - _sensorData: MutableStateFlow<List<Pair>>        │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │          BleDeviceConnector                         │   │
│  │  - bluetoothGatt: BluetoothGatt?                    │   │
│  │  - currentSensorType: SensorType                    │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │          Android BLE Stack                          │   │
│  │  - Native BLE operations                            │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │  FORA D40b      │
                  │  BLE Device     │
                  └─────────────────┘
```

---

## Configuration Flow

```
DHIS2 Server Datastore
        │
        │ API Call
        ▼
SensorConfigApi.getSensorConfig()
        │
        ▼
SensorConfigRepository
        │
        ├─ Cache in SharedPreferences
        │
        └─ Emit to StateFlow
                │
                ▼
        FormViewModel observes
                │
                ▼
        Find config by data element UID
                │
                ▼
        Extract measurements map
                │
                ▼
        Map semantic keys to data element UIDs
                │
                ▼
        Save to correct fields
```

---

This diagram shows the complete end-to-end flow from user interaction to DHIS2 server storage, including all intermediate layers, data transformations, and state changes.
