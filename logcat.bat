@echo off
echo Starting Logcat - Filtering for DEBUG messages...
echo ============================================
echo.
D:\Android_SDK\platform-tools\adb.exe logcat -v time | findstr "DEBUG:"
