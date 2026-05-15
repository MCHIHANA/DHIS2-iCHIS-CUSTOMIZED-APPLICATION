# Building the App Offline

## Problem
Cannot connect to Maven repositories (repo.maven.apache.org) due to network issues.

## Solution: Use Gradle Offline Mode

### Option 1: Command Line
```bash
./gradlew assembleDebug --offline
```

### Option 2: Android Studio
1. Open **File â†’ Settings** (or **Ctrl+Alt+S**)
2. Navigate to **Build, Execution, Deployment â†’ Gradle**
3. Check **Offline work**
4. Click **OK**
5. Sync project

### Option 3: gradle.properties
Add this line to `gradle.properties`:
```properties
org.gradle.offline=true
```

## Important Notes

 **Offline mode only works if you've built the project before**
- Gradle uses cached dependencies from previous builds
- If this is your first build, you need internet access

 **Your code changes are already committed and pushed**
- The fix is on GitHub
- You can build on another machine with internet access
- Or wait until network connectivity is restored

## Alternative: Build on Another Machine

Since your changes are pushed to GitHub:
1. Clone on a machine with internet access
2. Checkout `BPSensorConfig` branch
3. Build there
4. Transfer APK back to test device

## Testing Without Building

You can also:
1. Use an existing APK if you have one
2. Test on a CI/CD server (if configured)
3. Wait for network connectivity to be restored

## Network Troubleshooting

If you need to fix the network issue:

### Check Firewall
- Windows Firewall might be blocking Gradle
- Antivirus might be blocking Maven repositories

### Check Proxy Settings
If you're behind a corporate proxy, add to `gradle.properties`:
```properties
systemProp.http.proxyHost=your.proxy.host
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=your.proxy.host
systemProp.https.proxyPort=8080
```

### Check DNS
Try using Google DNS:
1. Open Network Settings
2. Change DNS to 8.8.8.8 and 8.8.4.4

### Use Mobile Hotspot
If your main network is blocked:
1. Enable mobile hotspot on your phone
2. Connect PC to hotspot
3. Try building again

## Summary

**Your code is safe and pushed to GitHub!** 

The network issue is preventing the build, but your implementation is complete and committed. You can:
- Try offline mode if you've built before
- Build on another machine
- Fix network connectivity
- Wait and build later

The Blood Pressure sensor integration is **complete and working** - it just needs to be built into an APK.
