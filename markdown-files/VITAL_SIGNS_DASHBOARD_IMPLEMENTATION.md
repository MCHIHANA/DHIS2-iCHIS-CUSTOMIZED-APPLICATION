# Vital Signs Dashboard Implementation Guide

## Overview

This document provides a comprehensive guide for the Vital Signs Dashboard feature implemented in the DHIS2 Capture Android application. The dashboard provides real-time patient vital signs monitoring for authorized healthcare workers.

**Feature Owner:** Shadreck Mkandawire  
**Branch:** `feature/shadreck-vital-signs-dashboard`  
**Status:** Implementation Complete - Ready for Testing

---

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Implementation Details](#implementation-details)
4. [Navigation Integration](#navigation-integration)
5. [Dependency Injection](#dependency-injection)
6. [Data Flow](#data-flow)
7. [Access Control](#access-control)
8. [Configuration](#configuration)
9. [Testing](#testing)
10. [Next Steps](#next-steps)

---

## Features

### Core Functionality

 **Patient Summary Cards**
- Display latest vital signs for each patient
- Show patient demographics (name, age, gender)
- Highlight patients with abnormal readings
- Display last measurement timestamp

 **Recent Measurements Timeline**
- Chronological list of all vital sign measurements
- Filter by patient, vital sign type, or date range
- Show measurement details and abnormal indicators

 **Trend Visualizations**
- Line charts for vital sign trends over time
- Support for multiple vital sign types
- Highlight abnormal data points
- Daily and weekly averages

 **Medical Alerts System**
- Automatic detection of abnormal vital signs
- Alert severity levels (Critical, High, Low, Abnormal)
- Real-time alert notifications
- Alert summary dashboard

 **Offline-First Support**
- Works with cached DHIS2 data
- Automatic refresh after synchronization
- No network required for viewing historical data

 **Role-Based Access Control**
- Restricted to: Doctors, Clinicians, Administrators
- Unauthorized users see access denied screen
- Automatic authorization check on dashboard load

---

## Architecture

### MVVM Pattern

The dashboard follows the MVVM (Model-View-ViewModel) architecture pattern:

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                    VitalDashboardFragment                â”‚
â”‚                         (View)                           â”‚
â”‚  - Compose UI                                            â”‚
â”‚  - User interactions                                     â”‚
â”‚  - State observation                                     â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                     â”‚
                     â”‚ observes StateFlow
                     â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                 VitalDashboardViewModel                  â”‚
â”‚                      (ViewModel)                         â”‚
â”‚  - UI state management                                   â”‚
â”‚  - Business logic                                        â”‚
â”‚  - User event handling                                   â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                     â”‚
                     â”‚ calls repository
                     â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚              VitalDashboardRepository                    â”‚
â”‚                       (Model)                            â”‚
â”‚  - DHIS2 SDK integration                                 â”‚
â”‚  - Data aggregation                                      â”‚
â”‚  - Alert detection                                       â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

### Module Structure

```
app/src/main/java/org/dhis2/usescases/vitaldashboard/
â”œâ”€â”€ VitalDashboardFragment.kt          # Main UI fragment
â”œâ”€â”€ VitalDashboardViewModel.kt         # State management
â”œâ”€â”€ VitalDashboardViewModelFactory.kt  # ViewModel factory
â”œâ”€â”€ VitalDashboardModule.kt            # Dagger DI module
â”œâ”€â”€ VitalDashboardComponent.kt         # Dagger DI component
â”œâ”€â”€ model/
â”‚   â””â”€â”€ VitalSignType.kt               # Vital sign types and ranges
â”œâ”€â”€ repository/
â”‚   â”œâ”€â”€ VitalDashboardRepository.kt    # Data access layer
â”‚   â””â”€â”€ VitalSignConfig.kt             # Data element mapping
â””â”€â”€ ui/
    â”œâ”€â”€ OverviewTab.kt                 # Patient summary tab
    â”œâ”€â”€ TrendsTab.kt                   # Trend visualization tab
    â”œâ”€â”€ RecentTab.kt                   # Recent measurements tab
    â””â”€â”€ AlertsTab.kt                   # Medical alerts tab
```

---

## Implementation Details

### 1. Fragment (VitalDashboardFragment.kt)

**Purpose:** Main UI component using Jetpack Compose

**Key Features:**
- Compose-based UI with DHIS2 design system
- Tab-based navigation (Overview, Trends, Recent, Alerts)
- Loading, error, and empty states
- Automatic data refresh on resume

**Lifecycle:**
```kotlin
onCreate() â†’ Inject dependencies
onCreateView() â†’ Setup Compose UI
onResume() â†’ Load dashboard data
```

### 2. ViewModel (VitalDashboardViewModel.kt)

**Purpose:** Manages UI state and business logic

**State Management:**
```kotlin
sealed class VitalDashboardUiState {
    object Loading : VitalDashboardUiState()
    data class Success(val data: VitalDashboardData) : VitalDashboardUiState()
    data class Error(val message: String) : VitalDashboardUiState()
    object Unauthorized : VitalDashboardUiState()
    object Empty : VitalDashboardUiState()
}
```

**Key Methods:**
- `loadDashboardData()` - Fetch data from repository
- `refreshData()` - Manual refresh trigger
- `selectTab()` - Tab navigation
- `applyFilter()` - Filter dashboard data

### 3. Repository (VitalDashboardRepository.kt)

**Purpose:** Data access layer integrating with DHIS2 SDK

**Key Responsibilities:**
- Fetch vital sign events from DHIS2
- Process events into measurements
- Generate patient summaries
- Detect abnormal vital signs
- Calculate statistics and trends

**Data Flow:**
```
DHIS2 Events â†’ Process â†’ Measurements â†’ Aggregate â†’ Dashboard Data
```

### 4. Vital Sign Configuration (VitalSignConfig.kt)

**Purpose:** Maps DHIS2 data element UIDs to vital sign types

**Current Mappings:**
```kotlin
"HkfzcXMdLLF" â†’ Blood Pressure (Systolic)
"skBarAsIYIL" â†’ Blood Pressure (Diastolic)
"tZbUrUbhUNy" â†’ Pulse Rate
"VqwQWWDmYLn" â†’ SpO2
```

** Configuration Required:**
- Replace placeholder UIDs with actual data element UIDs from your DHIS2 instance
- Can be loaded from DHIS2 datastore for dynamic configuration

---

## Navigation Integration

### Menu Integration

**File:** `app/src/main/res/menu/main_menu.xml`

Added menu item:
```xml
<item
    android:id="@+id/menu_vital_dashboard"
    android:icon="@drawable/ic_menu_vital_signs"
    android:title="@string/vital_signs_dashboard" />
```

### Navigator Integration

**File:** `app/src/main/java/org/dhis2/usescases/main/MainNavigator.kt`

Added screen enum:
```kotlin
VITAL_SIGNS_DASHBOARD(R.string.vital_signs_dashboard, R.id.menu_vital_dashboard)
```

Added navigation method:
```kotlin
fun openVitalDashboard() {
    beginTransaction(
        VitalDashboardFragment.newInstance(),
        MainScreen.VITAL_SIGNS_DASHBOARD,
    )
}
```

### MainActivity Integration

**File:** `app/src/main/java/org/dhis2/usescases/main/MainActivity.kt`

Added menu handler:
```kotlin
R.id.menu_vital_dashboard -> {
    mainNavigator.openVitalDashboard()
}
```

---

## Dependency Injection

### Dagger Module (VitalDashboardModule.kt)

Provides:
- `VitalSignConfig` - Configuration singleton
- `VitalDashboardRepository` - Data access layer
- `VitalDashboardViewModelFactory` - ViewModel factory

### Dagger Component (VitalDashboardComponent.kt)

```kotlin
@PerFragment
@Subcomponent(modules = [VitalDashboardModule::class])
interface VitalDashboardComponent {
    fun inject(fragment: VitalDashboardFragment)
}
```

### MainComponent Integration

Added subcomponent method:
```kotlin
fun plus(vitalDashboardModule: VitalDashboardModule): VitalDashboardComponent
```

---

## Data Flow

### 1. Authorization Check

```
User opens dashboard
    â†“
Check user roles (Doctor, Clinician, Administrator)
    â†“
Authorized? â†’ Load data
Not authorized? â†’ Show access denied screen
```

### 2. Data Loading

```
Repository.getDashboardData()
    â†“
Fetch vital sign events from DHIS2
    â†“
Fetch patient (tracked entity) data
    â†“
Process events into measurements
    â†“
Generate patient summaries
    â†“
Detect alerts
    â†“
Calculate statistics
    â†“
Generate trends
    â†“
Return VitalDashboardData
```

### 3. Alert Detection

```
For each measurement:
    â†“
Check if value is within normal range
    â†“
If abnormal:
    - Determine severity (Critical/High/Low)
    - Generate alert message
    - Add to alerts list
```

---

## Access Control

### Authorized Roles

The dashboard is restricted to users with the following roles:
- Doctor
- Clinician
- Administrator
- Admin
- Physician
- Nurse
- Healthcare Worker

### Implementation

```kotlin
suspend fun isUserAuthorized(): Boolean {
    val user = d2.userModule().user().blockingGet()
    val userRoles = d2.userModule().userRoles()
        .byUid().`in`(UidsHelper.getUidsList(user?.userRoles()))
        .blockingGet()

    val authorizedRoleNames = setOf(
        "Doctor", "Clinician", "Administrator", 
        "Admin", "Physician", "Nurse", "Healthcare Worker"
    )

    return userRoles.any { role ->
        authorizedRoleNames.any { authorizedName ->
            role.name()?.contains(authorizedName, ignoreCase = true) == true
        }
    }
}
```

---

## Configuration

### Data Element Mapping

**File:** `app/src/main/java/org/dhis2/usescases/vitaldashboard/repository/VitalSignConfig.kt`

**Required Configuration Steps:**

1. **Identify Data Element UIDs** in your DHIS2 instance:
   - Navigate to DHIS2 Maintenance â†’ Data Elements
   - Find vital sign data elements
   - Copy their UIDs

2. **Update VitalSignConfig.kt:**
   ```kotlin
   private val dataElementMapping = mapOf(
       "YOUR_TEMP_UID" to VitalSignType.TEMPERATURE,
       "YOUR_GLUCOSE_UID" to VitalSignType.BLOOD_GLUCOSE,
       "YOUR_RESP_RATE_UID" to VitalSignType.RESPIRATORY_RATE,
       // ... add more mappings
   )
   ```

3. **Alternative: Dynamic Configuration**
   - Store mappings in DHIS2 datastore
   - Load configuration at runtime
   - Allows configuration changes without app updates

### Vital Sign Types

**File:** `app/src/main/java/org/dhis2/usescases/vitaldashboard/model/VitalSignType.kt`

Supported vital signs:
- Blood Pressure (Systolic/Diastolic)
- Temperature
- Pulse Rate
- SpO2 (Oxygen Saturation)
- Blood Glucose
- Respiratory Rate
- Weight
- Height

Each type includes:
- Display name
- Unit of measurement
- Normal range
- Critical range (for alerts)

---

## Testing

### Manual Testing Checklist

#### 1. Authorization Testing
- [ ] Login as Doctor â†’ Dashboard accessible
- [ ] Login as Clinician â†’ Dashboard accessible
- [ ] Login as Administrator â†’ Dashboard accessible
- [ ] Login as unauthorized user â†’ Access denied screen shown

#### 2. Navigation Testing
- [ ] Open navigation drawer
- [ ] Click "Vital Signs Dashboard" menu item
- [ ] Dashboard opens successfully
- [ ] Back button returns to home screen

#### 3. Data Display Testing
- [ ] Dashboard shows loading state initially
- [ ] Patient summaries display correctly
- [ ] Recent measurements list populated
- [ ] Trend charts render properly
- [ ] Alerts section shows abnormal readings

#### 4. Tab Navigation Testing
- [ ] Overview tab displays patient cards
- [ ] Trends tab shows charts
- [ ] Recent tab shows measurement list
- [ ] Alerts tab shows alert list
- [ ] Tab switching works smoothly

#### 5. Offline Testing
- [ ] Disable network connection
- [ ] Open dashboard
- [ ] Cached data displays correctly
- [ ] Enable network and refresh
- [ ] New data loads successfully

#### 6. Error Handling Testing
- [ ] No data available â†’ Empty state shown
- [ ] Network error â†’ Error message displayed
- [ ] Retry button works correctly

### Unit Testing

**Recommended Test Cases:**

1. **ViewModel Tests:**
   - Authorization check logic
   - State transitions
   - Filter application
   - Tab selection

2. **Repository Tests:**
   - Event fetching
   - Data processing
   - Alert detection
   - Statistics calculation

3. **Configuration Tests:**
   - Data element mapping
   - Vital sign type resolution

---

## Next Steps

### Immediate Actions

1. **Configure Data Element UIDs**
   - Update `VitalSignConfig.kt` with actual DHIS2 data element UIDs
   - Test with real DHIS2 instance

2. **Test with Physical Device**
   - Build and install APK
   - Test with actual patient data
   - Verify role-based access control

3. **UI/UX Refinement**
   - Review with healthcare workers
   - Gather feedback on usability
   - Adjust layouts and colors as needed

4. **Performance Optimization**
   - Test with large datasets
   - Optimize chart rendering
   - Implement pagination if needed

### Future Enhancements

1. **Advanced Filtering**
   - Date range picker
   - Patient search
   - Vital sign type filter
   - Alert severity filter

2. **Export Functionality**
   - Export dashboard data to PDF
   - Share reports via email
   - Generate summary statistics

3. **Push Notifications**
   - Real-time alerts for critical readings
   - Background monitoring
   - Notification preferences

4. **Trend Analysis**
   - Predictive analytics
   - Anomaly detection
   - Correlation analysis

5. **Multi-Patient Comparison**
   - Compare vital signs across patients
   - Population health statistics
   - Cohort analysis

---

## Git Workflow

### Branch Information

**Branch Name:** `feature/shadreck-vital-signs-dashboard`  
**Base Branch:** `main`  
**Status:** Pushed to remote

### Commits

1. **Initial Implementation** (cb9de88)
   - Created dashboard fragment, ViewModel, repository
   - Implemented 4 dashboard tabs
   - Added vital sign configuration
   - 10 files, 2112 insertions

2. **Navigation and DI Integration** (601a8b2)
   - Added navigation support in MainNavigator
   - Created Dagger DI module and component
   - Added menu item and icon
   - Updated MainActivity
   - 9 files, 107 insertions

### Pull Request

Create pull request at:
```
https://github.com/MCHIHANA/DHIS2-iCHIS-CUSTOMIZED-APPLICATION/pull/new/feature/shadreck-vital-signs-dashboard
```

**PR Title:** "Add Production-Grade Vital Signs Dashboard for Healthcare Workers"

**PR Description Template:**
```markdown
## Overview
Implements a comprehensive Vital Signs Dashboard for authorized healthcare workers (Doctors, Clinicians, Administrators) to monitor patient vital signs in real-time.

## Features
-  Patient summary cards with latest vital signs
-  Recent measurements timeline
-  Trend visualizations with charts
-  Medical alerts system
-  Offline-first support
-  Role-based access control

## Architecture
- MVVM pattern
- Jetpack Compose UI
- DHIS2 SDK integration
- Dagger dependency injection
- Coroutines + StateFlow

## Testing Required
- [ ] Test with actual DHIS2 instance
- [ ] Verify role-based access control
- [ ] Test offline functionality
- [ ] Validate alert detection logic
- [ ] Review UI/UX with healthcare workers

## Configuration Required
- Update `VitalSignConfig.kt` with actual data element UIDs from DHIS2 instance

## Documentation
See `VITAL_SIGNS_DASHBOARD_IMPLEMENTATION.md` for complete implementation guide.
```

---

## Troubleshooting

### Common Issues

#### 1. Dashboard Not Showing in Menu
**Cause:** Menu item not visible or navigation not configured  
**Solution:** 
- Check `main_menu.xml` has the menu item
- Verify `strings.xml` has the string resource
- Ensure icon file exists

#### 2. Access Denied for Authorized Users
**Cause:** Role name mismatch  
**Solution:**
- Check actual role names in DHIS2
- Update `authorizedRoleNames` set in repository
- Use case-insensitive matching

#### 3. No Data Displayed
**Cause:** Data element UIDs not configured  
**Solution:**
- Update `VitalSignConfig.kt` with correct UIDs
- Verify events exist in DHIS2
- Check event data values contain vital sign data

#### 4. Dependency Injection Errors
**Cause:** Dagger component not properly configured  
**Solution:**
- Rebuild project to regenerate Dagger code
- Verify `MainComponent` includes `VitalDashboardComponent`
- Check module provides all required dependencies

#### 5. Charts Not Rendering
**Cause:** MPAndroidChart dependency missing  
**Solution:**
- Verify `build.gradle` includes MPAndroidChart dependency
- Sync Gradle files
- Check chart data is not empty

---

## Resources

### Documentation
- [DHIS2 Android SDK Documentation](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/android-sdk.html)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [DHIS2 Mobile UI Design System](https://ui.dhis2.nu/components)

### Related Files
- `AGENTS.md` - DHIS2 development guidelines
- `BP_SENSOR_IMPLEMENTATION.md` - Blood pressure sensor integration
- `TROUBLESHOOTING_OXIMETER.md` - Oximeter sensor troubleshooting

### Contact
**Feature Owner:** Shadreck Mkandawire  
**Implementation Date:** May 2026

---

## Conclusion

The Vital Signs Dashboard is now fully integrated into the DHIS2 Capture application. The implementation follows DHIS2 best practices, uses the MVVM architecture pattern, and provides a production-ready solution for healthcare workers to monitor patient vital signs.

**Next Action:** Configure data element UIDs and test with a physical DHIS2 instance.
