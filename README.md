# DHIS2-iCHIS-CUSTOMIZED-APPLICATION
````markdown
# DHIS2 Capture App — BLE Sensor Integration System

A customized version of the DHIS2 Android Capture application enhanced with Bluetooth Low Energy (BLE) medical sensor integration for real-time patient vital sign collection and monitoring.

This project extends the global DHIS2 Capture App by integrating medical sensors such as temperature sensors, blood pressure monitors, pulse oximeters, and future modular healthcare devices directly into DHIS2 workflows.

---

# Project Overview

The system enables healthcare workers to:

- Connect medical sensors via Bluetooth Low Energy (BLE)
- Capture real-time patient vital signs
- Automatically populate DHIS2 program data elements
- Store readings as DHIS2 events
- Reduce manual data entry
- Improve accuracy and speed of patient monitoring

The project is designed using a modular sensor architecture for scalability and future expansion.

---

# Implemented Features

## BLE Sensor Integration
- Bluetooth Low Energy (BLE) scanning
- Automatic sensor discovery
- Sensor connection management
- GATT service communication
- Characteristic notifications

## Working Sensors
### Temperature Sensor
- Real-time body temperature reading
- Automatic field population
- DHIS2 event saving

### Blood Pressure Sensor
- Systolic pressure
- Diastolic pressure
- Pulse rate (beats/min)
- Automatic parsing and saving

## DHIS2 Integration
- Automatic data element mapping
- Event data saving
- Form integration
- Sensor-enabled capture workflows

## Sensor UI Components
- Sensor connection dialogs
- Reading status indicators
- Error handling
- Manual fallback support

## Modular Sensor Framework
- Sensor-specific handlers
- Independent parsers
- Reusable BLE layer
- Scalable architecture for future sensors

---

# Planned Features

- Sensor Auto-Reconnect
- Real-Time Vital Sign Validation
- Vital Signs Dashboard
- Role-Based Dashboard Access
- Glucose Sensor Integration
- SpO₂ Sensor Stabilization
- Offline Sensor Queueing
- Patient Trend Monitoring
- Abnormal Reading Alerts
- Sensor Health Diagnostics

---

# System Architecture

```text
Medical Sensor
       ↓
Bluetooth Low Energy (BLE)
       ↓
DHIS2 Capture App
       ↓
Sensor Parsing Layer
       ↓
DHIS2 Event/Data Element Mapping
       ↓
DHIS2 Server
       ↓
Dashboard & Analytics
````

---

# Modular Architecture Structure

```text
form/src/main/java/org/dhis2/sensors/

sensors/
│
├── core/
│   ├── BleManager.kt
│   ├── BleScanner.kt
│   ├── BleConnector.kt
│   ├── SensorRegistry.kt
│   ├── SensorType.kt
│   ├── BaseSensorHandler.kt
│   └── SensorReading.kt
│
├── devices/
│   ├── temperature/
│   ├── bloodpressure/
│   ├── spo2/
│   └── glucose/
│
├── models/
│
├── ui/
│
└── utils/
```

---

# Technologies Used

## Mobile Development

* Kotlin
* Android SDK
* Kotlin Multiplatform

## BLE Communication

* Bluetooth Low Energy (BLE)
* GATT Services
* BLE Notifications

## Backend/Data Platform

* DHIS2 Android SDK
* DHIS2 Event Capture

## Development Tools

* Android Studio
* Git & GitHub
* Gradle

---

# Medical Sensors Used

## FORA O2 Bluetooth Pulse Oximeter

Measures:

* SpO₂ (%)
* Pulse Rate (BPM)

## FORA D40b Blood Pressure Monitor

Measures:

* Systolic Pressure
* Diastolic Pressure
* Pulse Rate

## BLE Temperature Sensor

Measures:

* Body Temperature (°C)

---

# Installation & Setup

## Clone Repository

```bash
git clone <repository-url>
```

## Open Project

Open the project in Android Studio.

## Build Project

```bash
./gradlew clean assembleDebug
```

## Run Application

Connect an Android device and run the app.

---

# Git Workflow

## Create Feature Branch

```bash
git checkout -b feature/modular-sensor-architecture
```

## Commit Changes

```bash
git add .
git commit -m "refactor: create modular sensor architecture"
```

## Push Branch

```bash
git push origin feature/modular-sensor-architecture
```

---

# Development Guidelines

## Important Rules

* Do NOT modify DHIS2 program structure without approval
* Do NOT create duplicate data elements
* Do NOT remove existing mappings
* Do NOT break existing sensor functionality
* Maintain backward compatibility
* Test sensors after every major change

## Recommended Workflow

1. Create branch
2. Implement feature incrementally
3. Test functionality
4. Commit changes
5. Push branch
6. Open Pull Request
7. Review before merge

---

# Contributors

## Project Team

* MISHECK CHIHANA
* LEXAH MBALE
* SHADRECK MKANDAWIRE
* LUSUNGU MHANGO

---

# Academic Project

This project is developed as part of a Computer Science academic research and software engineering initiative focused on digital healthcare systems, BLE medical sensor integration, and DHIS2 customization.

---

# Future Vision

The long-term goal is to transform the DHIS2 Capture application into a smart healthcare monitoring platform capable of:

* Real-time patient monitoring
* Automated sensor integration
* Clinical decision support
* Remote healthcare tracking
* Intelligent vital sign analytics

---

# License

This project is built on top of the DHIS2 Android Capture platform and follows the respective DHIS2 licensing guidelines.

```
```

---

[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=dhis2_dhis2-android-capture-app&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=dhis2_dhis2-android-capture-app)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=dhis2_dhis2-android-capture-app&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=dhis2_dhis2-android-capture-app)

Check the [Wiki](https://github.com/dhis2/dhis2-android-capture-app/wiki) for information about how to build the project and its architecture **(WIP)**
