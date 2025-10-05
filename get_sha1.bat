@echo off
echo Getting SHA-1 Fingerprint for Debug Keystore...
echo.

REM Try to find keytool in Android Studio's bundled JDK
set "KEYTOOL="

REM Try D:\Android location first
if exist "D:\Android\jbr\bin\keytool.exe" (
    set "KEYTOOL=D:\Android\jbr\bin\keytool.exe"
    goto :found
)

REM Common Android Studio JDK locations
if exist "%LOCALAPPDATA%\Android\Sdk\jdk\*" (
    for /d %%i in ("%LOCALAPPDATA%\Android\Sdk\jdk\*") do (
        if exist "%%i\bin\keytool.exe" (
            set "KEYTOOL=%%i\bin\keytool.exe"
            goto :found
        )
    )
)

REM Try Program Files
if exist "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" (
    set "KEYTOOL=C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
    goto :found
)

if exist "C:\Program Files\Android\Android Studio\jre\bin\keytool.exe" (
    set "KEYTOOL=C:\Program Files\Android\Android Studio\jre\bin\keytool.exe"
    goto :found
)

REM Try java in PATH
where keytool >nul 2>&1
if %errorlevel% equ 0 (
    set "KEYTOOL=keytool"
    goto :found
)

echo ERROR: keytool not found!
echo.
echo Please find keytool.exe manually in one of these locations:
echo - C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe
echo - %LOCALAPPDATA%\Android\Sdk\jdk\[version]\bin\keytool.exe
echo.
echo Then run this command manually:
echo keytool -list -v -alias androiddebugkey -keystore "%USERPROFILE%\.android\debug.keystore" -storepass android -keypass android
echo.
pause
exit /b 1

:found
echo Found keytool: %KEYTOOL%
echo.
echo Debug Keystore Location: %USERPROFILE%\.android\debug.keystore
echo.

"%KEYTOOL%" -list -v -alias androiddebugkey -keystore "%USERPROFILE%\.android\debug.keystore" -storepass android -keypass android

echo.
echo ========================================
echo COPY THE SHA1 FINGERPRINT FROM ABOVE
echo ========================================
echo.
pause
