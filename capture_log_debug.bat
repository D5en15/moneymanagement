@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
set "APP_ID=com.example.moneymanager"
set "LOG_DIR=%ROOT%logs"
set "DURATION_SEC=90"
set "FILTER=AndroidRuntime CRASH Exception FATAL MainActivity MainViewModel Passcode Onboarding Splash CalendarViewModel monthBuildMs Room SQLite"

echo [1/6] Checking ADB availability...
where adb >nul 2>nul
if errorlevel 1 (
    echo ERROR: adb not found in PATH.
    exit /b 1
)

echo [2/6] Checking device authorization...
set "HAS_DEVICE="
set "HAS_UNAUTHORIZED="
for /f "skip=1 tokens=1,2" %%A in ('adb devices') do (
    if "%%B"=="device" set "HAS_DEVICE=1"
    if "%%B"=="unauthorized" set "HAS_UNAUTHORIZED=1"
)

if not defined HAS_DEVICE (
    if defined HAS_UNAUTHORIZED (
        echo ERROR: Device detected but unauthorized.
    ) else (
        echo ERROR: No authorized Android device found.
    )
    exit /b 1
)

if not exist "%LOG_DIR%" (
    mkdir "%LOG_DIR%"
    if errorlevel 1 (
        echo ERROR: Could not create log directory "%LOG_DIR%"
        exit /b 1
    )
)

for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format ''yyyyMMdd_HHmmss''"') do set "TS=%%I"
set "LOG_FILE=%LOG_DIR%\logcat_%TS%.txt"

echo [3/6] Clearing logcat buffer...
adb logcat -c
if errorlevel 1 (
    echo ERROR: Failed to clear logcat.
    exit /b 1
)

echo [4/6] Launching app...
adb shell am start -n %APP_ID%/.MainActivity >nul
if errorlevel 1 (
    echo ERROR: Failed to launch app.
    exit /b 1
)

echo [5/6] Waiting %DURATION_SEC%s to collect logs...
timeout /t %DURATION_SEC% /nobreak >nul

echo [6/6] Exporting filtered logs to:
echo %LOG_FILE%
adb logcat -v time -d | findstr /I "%FILTER%" > "%LOG_FILE%"
if errorlevel 1 (
    echo WARNING: No lines matched the filter. Saving full dump instead.
    adb logcat -v time -d > "%LOG_FILE%"
)

echo Done. Log file saved.
echo %LOG_FILE%
exit /b 0

