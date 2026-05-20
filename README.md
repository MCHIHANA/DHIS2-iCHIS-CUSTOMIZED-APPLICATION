
# DHIS2 (iCHIS) Android Capture App — Sensor Integration Project

This project is a customized version of the global DHIS2 Android Capture application enhanced with medical sensor integration for real-time patient vital sign collection and monitoring.

The system enables healthcare workers to collect patient readings directly from connected medical devices and automatically save the readings into DHIS2 program events and data elements.

The project currently supports Bluetooth Low Energy (BLE) medical sensors including:

* Temperature Sensor
* Blood Pressure Sensor
* blood glucose sensor
* pulse (bpm) and oxygen concetration(SpO₂) in blood 

The architecture is also being prepared for future support of:

* Wi-Fi based sensors
* USB medical devices
* Additional BLE sensors such as SpO₂, glucose meters, heart rate monitors, and weight scales

The main goal of the project is to improve healthcare data collection by reducing manual data entry, improving accuracy, and enabling direct sensor-to-DHIS2 integration.

## Current Features

* BLE sensor scanning and connection
* Automatic reading retrieval from sensors
* Real-time form population inside DHIS2 Capture
* Automatic saving of readings into DHIS2 data elements
* Sensor status monitoring and connection feedback
* Manual fallback entry when sensors are unavailable
* Modular sensor architecture for future scalability
* Support for configurable sensor settings using datastore configuration

## Datastore Configuration

The project uses a datastore-based configuration approach to manage sensors dynamically without hardcoding all configurations inside the application.

The datastore stores:

* sensor names
* MAC addresses
* UUIDs
* data element mappings
* units
* sensor requirements
* manual entry permissions

This approach makes it easier to:

* add new sensors
* update sensor mappings
* support multiple device types
* scale deployments across facilities
* simplify maintenance and future expansion

## Project Objective

The objective of this project is to enhance healthcare data collection by integrating medical sensors directly into DHIS2 workflows. The system is designed to support efficient patient monitoring, improve the accuracy of captured data, and provide a scalable foundation for future digital health integrations.

## Future Improvements

Planned improvements include:

* automatic sensor reconnection
* real-time vital sign validation
* vital signs dashboard
* multi-sensor management
* support for Wi-Fi and USB medical devices
* offline synchronization improvements
* advanced analytics and reporting
* modular sensor expansion

## Technologies Used

* Kotlin
* Android SDK
* DHIS2 Android SDK
* Bluetooth Low Energy (BLE)
* Gradle
* MVVM Architecture

## Contributors

* Misheck Chihana
* Lexah Mbale
* Shadreck Mkandawire
* Lusungu Mhango
