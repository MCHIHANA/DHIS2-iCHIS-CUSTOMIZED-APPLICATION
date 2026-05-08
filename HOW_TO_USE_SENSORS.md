# How to Use Sensors - Quick Guide

## Changes Made

✓ Removed device counting logic (was causing confusion)
✓ Changed status message to "Connected - Place finger on sensor now!"
✓ Added green icon and clearer UI when waiting for finger placement

## For SPO2/Pulse Oximeter (FORA O2)

### Step-by-Step

1. **Tap "Connect Sensor"** on SpO2 or Pulse field
2. **Wait 5-10 seconds** - Dialog shows "Scanning for sensor..."
3. **Device connects** - Dialog shows "Connecting to sensor..."
4. **See green message** - "Connected - Place finger on sensor now!"
5. **Place finger on sensor** - Insert fully, keep still
6. **Wait 5-15 seconds** - LED will flash then stabilize
7. **Data received** - Dialog shows "Data received: 95"
8. **Both fields fill** - SpO2 and Pulse populated automatically
9. **Dialog closes** - Done!

### Important Notes

**MUST place finger on sensor!**
- The sensor won't send data until it detects a finger
- Insert finger fully into the sensor
- Keep finger still
- Wait for LED to stop flashing (means stable reading)

**If stuck on "Place finger on sensor now!":**
- Your finger IS on the sensor but reading is invalid
- Common causes:
  - Finger not inserted fully
  - Finger moving
  - Nail polish or dirt on finger
  - Low battery in sensor
  - Try different finger (index finger works best)

## For Temperature Sensor (FORA IR42)

### Step-by-Step

1. **Tap "Connect Sensor"** on Temperature field
2. **Wait 5-10 seconds** - Dialog shows "Scanning for sensor..."
3. **Device connects** - Dialog shows "Connecting to sensor..."
4. **See message** - "Connected - Place finger on sensor now!" (or similar)
5. **Take measurement** - Point sensor at forehead/ear
6. **Press sensor button** - Sensor takes reading
7. **Data received** - Dialog shows "Data received: 36.5"
8. **Field fills** - Temperature populated
9. **Dialog closes** - Done!

### Important Notes

**For IR thermometer:**
- You may need to press the sensor's button to trigger a reading
- Point at forehead or in ear (depending on sensor type)
- Keep sensor steady during measurement
- Wait for sensor beep (if it has one)

## Troubleshooting

### "Scanning for sensor..." (stuck)
**Problem:** Can't find sensor
**Solutions:**
1. Ensure sensor is powered on
2. Check sensor battery
3. Move phone closer to sensor
4. Unpair sensor from Bluetooth settings if paired
5. Restart Bluetooth

### "Connecting to sensor..." (stuck)
**Problem:** Found sensor but can't connect
**Solutions:**
1. Move phone closer
2. Restart sensor (power off/on)
3. Restart Bluetooth
4. Restart phone

### "Connected - Place finger on sensor now!" (stuck)
**Problem:** Connected but no valid reading
**Solutions:**

**For Oximeter:**
1. **Check finger placement:**
   - Insert finger FULLY
   - Fingertip should touch both LED and detector
   - Keep finger STILL
   - No nail polish or dirt

2. **Wait longer:**
   - Can take 10-15 seconds for first reading
   - LED will flash while searching for pulse
   - LED becomes steady when reading is stable

3. **Try different finger:**
   - Index finger usually works best
   - Middle finger is second best
   - Avoid thumb (too thick)

4. **Check sensor:**
   - Battery level OK?
   - Display shows readings?
   - Try in nRF Connect to verify sensor works

**For Thermometer:**
1. **Trigger measurement:**
   - Press sensor button
   - Point at target (forehead/ear)
   - Wait for beep

2. **Check sensor mode:**
   - Some sensors need to be in "continuous" mode
   - Check sensor manual

### No data after 30 seconds
**Problem:** Sensor not sending data
**Solutions:**
1. Cancel and try again
2. Restart sensor
3. Check sensor battery
4. Test sensor in nRF Connect app
5. Check logs (if you have adb access)

## Expected Timeline

| Step | Time | Status Message |
|------|------|----------------|
| Open dialog | Immediate | "Initializing..." |
| Start scan | < 1 sec | "Scanning for sensor..." |
| Find device | 2-10 sec | (same) |
| Connect | 2-5 sec | "Connecting to sensor..." |
| Ready | Immediate | "Connected - Place finger on sensor now!" |
| **Place finger** | **User action** | (same, green icon) |
| Get reading | 5-15 sec | (same, waiting) |
| Receive data | < 1 sec | "Data received: XX" |
| Fill fields | < 1 sec | (dialog closes) |
| **TOTAL** | **10-35 sec** | |

## Tips for Success

### Oximeter
- ✓ Finger fully inserted
- ✓ Finger still (don't move!)
- ✓ Wait for LED to stabilize
- ✓ Clean finger (no nail polish)
- ✓ Try index finger first
- ✓ Sensor battery good
- ✓ Wait at least 10 seconds

### Thermometer
- ✓ Press sensor button
- ✓ Point at target area
- ✓ Keep steady
- ✓ Wait for beep
- ✓ Sensor battery good

## What the Colors Mean

- **Blue icon** - Scanning/Connecting
- **Green icon** - Connected, ready for measurement
- **Green text** - Success, data received
- **Red text** - Error

## Quick Reference

**Oximeter not working?**
1. Unpair from Bluetooth settings
2. Restart sensor
3. Place finger FULLY in sensor
4. Keep finger STILL
5. Wait 10-15 seconds

**Thermometer not working?**
1. Unpair from Bluetooth settings
2. Restart sensor
3. Press sensor button
4. Point at target
5. Wait for beep

**Still not working?**
1. Check sensor battery
2. Test in nRF Connect app
3. Check app permissions (Location!)
4. Restart phone
5. Check logs

## Success Indicators

You'll know it's working when:
- ✓ Dialog shows green "Place finger" message
- ✓ After placing finger, values appear within 15 seconds
- ✓ Both SpO2 and Pulse fields fill (for oximeter)
- ✓ Temperature field fills (for thermometer)
- ✓ Values are reasonable:
  - SpO2: 90-100%
  - Pulse: 50-120 bpm
  - Temperature: 35-38°C (95-100°F)

## Build and Install

```bash
# Build
./gradlew :app:assembleDebug

# Install
adb install -r app/build/outputs/apk/dhis2/debug/app-dhis2-debug.apk
```

## The Key Point

**The sensor won't send data until you place your finger on it!**

When you see "Connected - Place finger on sensor now!" in green, that's your cue to:
1. Place finger on oximeter (or point thermometer at target)
2. Keep still
3. Wait 5-15 seconds
4. Data will appear

Don't cancel! Just wait with your finger on the sensor.
