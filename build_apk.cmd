@echo off
REM Quick Build Script for BP Sensor APK
REM This bypasses Android Studio IDE cache issues

echo ========================================
echo Building DHIS2 BP Sensor APK
echo ========================================
echo.

echo This will build the APK from command line, bypassing any IDE cache issues.
echo.
echo Estimated time: 5-10 minutes (depending on your system)
echo.
pause

echo.
echo [Step 1/3] Cleaning previous build...
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo.
    echo ✗ Clean failed. Check the error above.
    pause
    exit /b 1
)
echo ✓ Clean complete
echo.

echo [Step 2/3] Building debug APK...
echo This may take several minutes...
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo.
    echo ✗ Build failed. Check the error above.
    echo.
    echo Common issues:
    echo - Network connection required for dependencies
    echo - Check if you have enough disk space
    echo - Try running: gradlew --stop
    echo   Then run this script again
    pause
    exit /b 1
)
echo ✓ Build complete
echo.

echo [Step 3/3] Locating APK...
set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
if exist "%APK_PATH%" (
    echo ✓ APK generated successfully!
    echo.
    echo Location: %APK_PATH%
    echo.
    echo File size:
    dir "%APK_PATH%" | findstr "app-debug.apk"
    echo.
) else (
    echo ✗ APK not found at expected location
    echo.
    echo Searching for APK...
    dir /s /b app-debug.apk
)

echo.
echo ========================================
echo Build Complete!
echo ========================================
echo.
echo NEXT STEPS:
echo.
echo 1. Install the APK on your device:
echo    adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.
echo 2. IMPORTANT: Clear app data before testing:
echo    adb shell pm clear org.dhis2.usescases.main
echo.
echo 3. Test with FORA D40b Blood Pressure Monitor:
echo    - Open app and navigate to a form with BP fields
echo    - Tap any BP field (Systolic, Diastolic, or Pulse)
echo    - Tap "Connect to Sensor"
echo    - Take a measurement on the FORA D40b
echo    - All 3 fields should auto-populate
echo.
echo 4. View logs to verify:
echo    adb logcat ^| findstr "SENSOR_DATA"
echo.
echo See QUICK_FIX_STEPS.md for detailed testing instructions.
echo.
pause
