# Run Debug APK On Device (Windows)

## Prerequisites
- Android SDK Platform Tools installed (`adb` available in PATH).
- USB debugging enabled on device.
- Device connected by USB and authorized (`adb devices` shows `device`).

## Build + Export APK To Project Root
From `MoneyManager` project root:

```bat
.\gradlew.bat :app:exportDebugApkToRoot
```

Output file:
- `.\MoneyManager-debug.apk`

## Build + Install + Launch (One Command)
From `MoneyManager` project root:

```bat
.\install_debug_apk.bat
```

This script:
1. Checks `adb devices` for an authorized device.
2. Runs `:app:exportDebugApkToRoot`.
3. Installs with `adb install -r -d`.
4. Launches `com.example.moneymanager`.

## Run Kit (Recommended)
From `MoneyManager` project root:

```bat
.\run_device_debug.bat
```

This script:
1. Verifies `adb` is available.
2. Verifies an authorized device is connected.
3. Uses existing `install_debug_apk.bat` when present (fallbacks to manual Gradle + install + launch flow).

## Capture Focused Logs To File
From `MoneyManager` project root:

```bat
.\capture_log_debug.bat
```

Behavior:
1. Clears logcat.
2. Launches `com.example.moneymanager`.
3. Waits 90 seconds.
4. Exports filtered logs to a timestamped file.

Saved location:
- `.\logs\logcat_YYYYMMDD_HHMMSS.txt`

## Common Errors And Fixes
- `unauthorized` in `adb devices`
  - Unlock phone, accept RSA prompt, run again.
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
  - Existing app was signed with different key.
  - Fix:
    ```bat
    adb uninstall com.example.moneymanager
    .\install_debug_apk.bat
    ```
- `INSTALL_FAILED_VERSION_DOWNGRADE`
  - Device has newer version than APK.
  - Script already uses `-d`; if still failing, uninstall first:
    ```bat
    adb uninstall com.example.moneymanager
    .\install_debug_apk.bat
    ```

## Logs
```bat
adb logcat | findstr /I "AndroidRuntime com.example.moneymanager CRASH Exception"
```
