@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "APP_ID=com.example.moneymanager"
set "APK_NAME=MoneyManager-debug.apk"
set "APK_PATH=%~dp0%APK_NAME%"

echo [1/4] Checking ADB connection...
for /f "skip=1 tokens=1,2" %%A in ('adb devices') do (
    if "%%B"=="device" set "HAS_DEVICE=1"
    if "%%B"=="unauthorized" set "HAS_UNAUTHORIZED=1"
)

if not defined HAS_DEVICE (
    if defined HAS_UNAUTHORIZED (
        echo ERROR: Device detected but unauthorized.
        echo - Unlock phone and accept the USB debugging prompt.
        echo - Then run this script again.
    ) else (
        echo ERROR: No authorized Android device found.
        echo - Connect a device with USB debugging enabled.
        echo - Verify with: adb devices
    )
    exit /b 1
)

echo [2/4] Building and exporting debug APK...
call "%~dp0gradlew.bat" :app:exportDebugApkToRoot
if errorlevel 1 (
    echo ERROR: Gradle build/export failed.
    exit /b 1
)

if not exist "%APK_PATH%" (
    echo ERROR: APK not found at "%APK_PATH%"
    exit /b 1
)

echo [3/4] Installing APK to connected device...
adb install -r -d "%APK_PATH%"
if errorlevel 1 (
    echo ERROR: APK install failed.
    echo Tips:
    echo - If INSTALL_FAILED_UPDATE_INCOMPATIBLE: uninstall old app first:
    echo   adb uninstall %APP_ID%
    echo - If INSTALL_FAILED_VERSION_DOWNGRADE: keep -d flag ^(already used^) or uninstall then reinstall.
    exit /b 1
)

echo [4/4] Launching app...
adb shell monkey -p %APP_ID% -c android.intent.category.LAUNCHER 1 >nul 2>nul
if errorlevel 1 (
    echo WARNING: Could not auto-launch app. You can open it manually on device.
)

echo.
echo Done.
echo APK: "%APK_PATH%"
echo View logs:
echo   adb logcat ^| findstr /I "AndroidRuntime %APP_ID% CRASH Exception"
exit /b 0
