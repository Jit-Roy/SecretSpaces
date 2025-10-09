@echo off
echo ========================================
echo Secret Spaces - Release Build Script
echo ========================================
echo.

echo This will build a RELEASE APK
echo Make sure you have a keystore configured!
echo.
pause

echo Cleaning previous builds...
call gradlew.bat clean
echo.

echo Building Release APK...
call gradlew.bat assembleRelease

echo.
echo ========================================
echo Build Complete!
echo ========================================
echo.
echo Release APK Location: app\build\outputs\apk\release\app-release.apk
echo.
echo IMPORTANT: Release APKs must be signed before distribution!
echo Use Android Studio to generate a signed APK.
echo.
pause

