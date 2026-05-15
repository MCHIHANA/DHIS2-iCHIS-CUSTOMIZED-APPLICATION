# Fix: Unresolved Reference 'completeEvent'

## Problem

Android Studio shows:
```
Unresolved reference 'completeEvent'
at FormView.kt:280:47
```

## Root Cause

This is a **Gradle sync issue**, not a code issue. The `completeEvent()` method exists in `FormViewModel.kt` but Android Studio's cache is out of sync.

## Solutions

### Solution 1: Invalidate Caches and Restart (Recommended)

1. In Android Studio: **File → Invalidate Caches...**
2. Check **all boxes**:
   -  Invalidate and Restart
   -  Clear file system cache
   -  Clear downloaded shared indexes
3. Click **Invalidate and Restart**
4. Wait for Android Studio to restart and re-index

### Solution 2: Clean and Rebuild

```bash
# Stop Gradle daemon
./gradlew --stop

# Clean build
./gradlew clean

# Rebuild
./gradlew assembleDebug
```

### Solution 3: Delete Build Folders

```bash
# Windows PowerShell
Get-ChildItem -Path . -Include build,.gradle -Recurse -Directory | Remove-Item -Recurse -Force

# Then in Android Studio
File → Sync Project with Gradle Files
```

### Solution 4: Gradle Sync in Android Studio

1. **File → Sync Project with Gradle Files**
2. Wait for sync to complete
3. If still showing error, try Solution 1

### Solution 5: Restart Gradle Daemon

```bash
./gradlew --stop
./gradlew tasks
```

Then sync in Android Studio.

## Verification

After applying any solution:

1. Open `FormView.kt`
2. Line 280 should no longer show error
3. `viewModel.completeEvent()` should be recognized
4. Build should succeed

## If Still Not Working

### Check 1: Verify Method Exists

Open `FormViewModel.kt` and search for:
```kotlin
fun completeEvent() {
```

It should be around line 1141.

### Check 2: Check Imports

Make sure `FormView.kt` has proper imports at the top.

### Check 3: Gradle Version

Check `gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
```

### Check 4: Android Studio Version

Make sure you're using a recent version of Android Studio (Hedgehog or later).

## Quick Fix Command

Run this in PowerShell:
```powershell
# Stop Gradle
./gradlew --stop

# Clean
./gradlew clean

# Sync in Android Studio
# File → Sync Project with Gradle Files
```

## Why This Happens

- Gradle cache corruption
- Android Studio index out of sync
- Build files not properly generated
- Kotlin compiler cache issues

## Prevention

To avoid this in the future:
1. Always sync after pulling changes
2. Clean build after major changes
3. Invalidate caches if seeing weird errors
4. Keep Android Studio updated

---

**This is NOT a code issue. The method exists. Just need to sync properly!** 
