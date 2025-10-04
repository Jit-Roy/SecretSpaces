@echo off
echo ========================================
echo Secret Spaces - Build Script
echo ========================================
echo.

echo Checking Gradle...
call gradlew.bat --version
echo.

echo ========================================
echo Building the project...
echo ========================================
call gradlew.bat clean assembleDebug

echo.
echo ========================================
echo Build Complete!
echo ========================================
echo.
echo APK Location: app\build\outputs\apk\debug\app-debug.apk
echo.
echo Next steps:
echo 1. Configure Firebase (see FIREBASE_SETUP.md)
echo 2. Replace app\google-services.json with your Firebase config
echo 3. Install APK on device or run from Android Studio
echo.
pause

