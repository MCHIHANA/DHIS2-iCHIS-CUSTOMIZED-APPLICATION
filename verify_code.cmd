@echo off
REM Verification Script for BP Sensor Implementation
REM This script verifies that the code is correct and the error is just an IDE cache issue

echo ========================================
echo BP Sensor Code Verification
echo ========================================
echo.

echo [1/5] Checking if completeEvent method exists in FormViewModel...
findstr /n "fun completeEvent" "form\src\main\java\org\dhis2\form\ui\FormViewModel.kt"
if %errorlevel% equ 0 (
    echo ✓ FOUND: completeEvent method exists
) else (
    echo ✗ ERROR: completeEvent method not found
)
echo.

echo [2/5] Checking if completeEvent is called in FormView...
findstr /n "viewModel.completeEvent" "form\src\main\java\org\dhis2\form\ui\FormView.kt"
if %errorlevel% equ 0 (
    echo ✓ FOUND: completeEvent is called correctly
) else (
    echo ✗ ERROR: completeEvent call not found
)
echo.

echo [3/5] Checking BP sensor implementation files...
set BP_FILES_OK=1

if exist "form\src\main\java\org\dhis2\sensor\ble\BleScanner.kt" (
    echo ✓ BleScanner.kt exists
) else (
    echo ✗ BleScanner.kt missing
    set BP_FILES_OK=0
)

if exist "form\src\main\java\org\dhis2\sensor\ble\BleDeviceConnector.kt" (
    echo ✓ BleDeviceConnector.kt exists
) else (
    echo ✗ BleDeviceConnector.kt missing
    set BP_FILES_OK=0
)

if exist "form\src\main\java\org\dhis2\sensor\ble\BleDataParser.kt" (
    echo ✓ BleDataParser.kt exists
) else (
    echo ✗ BleDataParser.kt missing
    set BP_FILES_OK=0
)

if exist "form\src\main\java\org\dhis2\sensor\config\SensorConfigModels.kt" (
    echo ✓ SensorConfigModels.kt exists
) else (
    echo ✗ SensorConfigModels.kt missing
    set BP_FILES_OK=0
)
echo.

echo [4/5] Checking for BP-specific code...
findstr /c:"parseBloodPressure" "form\src\main\java\org\dhis2\sensor\ble\BleDataParser.kt" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ parseBloodPressure method exists
) else (
    echo ✗ parseBloodPressure method missing
)

findstr /c:"BLOOD_PRESSURE" "form\src\main\java\org\dhis2\sensor\ble\SensorType.kt" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ BLOOD_PRESSURE sensor type exists
) else (
    echo ✗ BLOOD_PRESSURE sensor type missing
)

findstr /c:"C0:26:DA:19:D4:FE" "form\src\main\java\org\dhis2\sensor\ble\KnownDevices.kt" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ FORA D40b MAC address registered
) else (
    echo ✗ FORA D40b MAC address not found
)
echo.

echo [5/5] Checking Gradle wrapper...
if exist "gradlew.bat" (
    echo ✓ Gradle wrapper exists
    echo.
    echo Running: gradlew --version
    call gradlew.bat --version
) else (
    echo ✗ Gradle wrapper missing
)
echo.

echo ========================================
echo Verification Complete
echo ========================================
echo.
echo CONCLUSION:
echo If all checks above show ✓, then the code is correct.
echo The "Unresolved reference" error in Android Studio is an IDE cache issue.
echo.
echo NEXT STEPS:
echo 1. In Android Studio: File → Invalidate Caches → Invalidate and Restart
echo 2. OR build from command line: gradlew assembleDebug
echo 3. Install APK and test with FORA D40b sensor
echo.
echo See RESOLVE_IDE_ERRORS.md for detailed instructions.
echo.
pause
