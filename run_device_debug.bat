@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
set "APP_ID=com.example.moneymanager"
set "APK_PATH=%ROOT%MoneyManager-debug.apk"
set "INSTALL_SCRIPT=%ROOT%install_debug_apk.bat"

echo [1/4] Checking ADB availability...
where adb >nul 2>nul
if errorlevel 1 (
    echo ERROR: adb not found in PATH.
    echo Install Android Platform Tools and ensure adb is in PATH.
    exit /b 1
)

echo [2/4] Checking device authorization...
set "HAS_DEVICE="
set "HAS_UNAUTHORIZED="
for /f "skip=1 tokens=1,2" %%A in ('adb devices') do (
    if "%%B"=="device" set "HAS_DEVICE=1"
    if "%%B"=="unauthorized" set "HAS_UNAUTHORIZED=1"
)

if not defined HAS_DEVICE (
    if defined HAS_UNAUTHORIZED (
        echo ERROR: Device detected but unauthorized.
        echo - Unlock phone and accept USB debugging prompt.
        echo - Run again after authorization.
    ) else (
        echo ERROR: No authorized Android device found.
        echo - Connect device and verify with: adb devices
    )
    exit /b 1
)

echo [3/4] Build / install / launch...
if exist "%INSTALL_SCRIPT%" (
    echo Using existing script: install_debug_apk.bat
    call "%INSTALL_SCRIPT%"
    if errorlevel 1 (
        echo ERROR: install_debug_apk.bat failed.
        exit /b 1
    )
) else (
    echo install_debug_apk.bat not found. Falling back to manual flow.
    call "%ROOT%gradlew.bat" :app:exportDebugApkToRoot
    if errorlevel 1 (
        echo ERROR: Gradle build/export failed.
        exit /b 1
    )

    if not exist "%APK_PATH%" (
        echo ERROR: APK not found at "%APK_PATH%"
        exit /b 1
    )

    adb install -r -d "%APK_PATH%"
    if errorlevel 1 (
        echo ERROR: APK install failed.
        exit /b 1
    )

    adb shell am start -n %APP_ID%/.MainActivity
    if errorlevel 1 (
        echo ERROR: App launch failed.
        exit /b 1
    )
)

echo [4/4] Done.
echo App ID: %APP_ID%
echo Next: run capture_log_debug.bat to save focused logs under .\logs\
exit /b 0

