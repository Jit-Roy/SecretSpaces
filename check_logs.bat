@echo off
echo Clearing logcat buffer...
adb logcat -c

echo.
echo Starting app...
adb shell am force-stop com.secretspaces32.android
timeout /t 2 >nul
adb shell am start -n com.secretspaces32.android/.MainActivity

echo.
echo Capturing logs (press Ctrl+C to stop)...
echo ================================================
adb logcat | findstr /i "secretspaces32 AndroidRuntime FATAL MapView MapLibre"

