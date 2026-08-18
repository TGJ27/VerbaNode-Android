@echo off
setlocal EnableExtensions
cd /d "%~dp0"

title VerbaNode Android Release APK Builder

echo ============================================================
echo   VerbaNode Android - Signed Release APK Builder
echo ============================================================
echo.
echo This builder needs no manually configured VN_* variables.
echo It creates/reuses one permanent signing identity under:
echo   %USERPROFILE%\.verbanode-signing
echo.

rem ------------------------------------------------------------
rem Detect the same Java/SDK environment already used by debug builds.
rem ------------------------------------------------------------
if not exist "%~dp0detect_android_env.bat" (
    echo ERROR: detect_android_env.bat is missing from the project root.
    goto :fatal
)

call "%~dp0detect_android_env.bat"
if errorlevel 1 goto :fatal

if not exist "%~dp0bootstrap_gradle.bat" (
    echo ERROR: bootstrap_gradle.bat is missing from the project root.
    goto :fatal
)

call "%~dp0bootstrap_gradle.bat"
if errorlevel 1 goto :fatal

rem ------------------------------------------------------------
rem Permanent local signing configuration.
rem This is deliberately OUTSIDE the Git repository.
rem ------------------------------------------------------------
set "VN_SIGNING_DIR=%USERPROFILE%\.verbanode-signing"
set "VN_SIGNING_FILE=%VN_SIGNING_DIR%\verbanode-signing.env"
set "VN_DEFAULT_KEYSTORE=%VN_SIGNING_DIR%\verbanode-release.jks"
set "VN_KEY_ALIAS=verbanode"

if not exist "%VN_SIGNING_DIR%" mkdir "%VN_SIGNING_DIR%"
if errorlevel 1 (
    echo ERROR: Could not create:
    echo   %VN_SIGNING_DIR%
    goto :fatal
)

if exist "%VN_SIGNING_FILE%" goto :load_signing

if exist "%VN_DEFAULT_KEYSTORE%" (
    echo ERROR: A release keystore already exists but its signing config is missing.
    echo.
    echo Keystore:
    echo   %VN_DEFAULT_KEYSTORE%
    echo.
    echo Config expected:
    echo   %VN_SIGNING_FILE%
    echo.
    echo I will NOT overwrite the existing key because doing so can break
    echo updates for users who already installed a signed APK.
    goto :fatal
)

rem ------------------------------------------------------------
rem Locate keytool from the Java installation detect_android_env selected.
rem ------------------------------------------------------------
set "VN_KEYTOOL="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\keytool.exe" set "VN_KEYTOOL=%JAVA_HOME%\bin\keytool.exe"

if not defined VN_KEYTOOL (
    for /f "delims=" %%K in ('where keytool.exe 2^>nul') do if not defined VN_KEYTOOL set "VN_KEYTOOL=%%K"
)

if not defined VN_KEYTOOL (
    echo ERROR: Java was found for Gradle but keytool.exe was not found.
    echo JAVA_HOME:
    echo   %JAVA_HOME%
    echo.
    echo Expected:
    echo   %JAVA_HOME%\bin\keytool.exe
    goto :fatal
)

rem ------------------------------------------------------------
rem Generate strong one-time passwords automatically.
rem They are saved ONLY in the local signing folder.
rem ------------------------------------------------------------
set "VN_KEYSTORE_PASSWORD="
set "VN_KEY_PASSWORD="

for /f "delims=" %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "Write-Output ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))"') do set "VN_KEYSTORE_PASSWORD=%%P"
for /f "delims=" %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "Write-Output ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))"') do set "VN_KEY_PASSWORD=%%P"

if not defined VN_KEYSTORE_PASSWORD (
    echo ERROR: Could not generate the keystore password.
    goto :fatal
)
if not defined VN_KEY_PASSWORD (
    echo ERROR: Could not generate the key password.
    goto :fatal
)

set "VN_KEYSTORE_PATH=%VN_DEFAULT_KEYSTORE%"

echo Creating the PERMANENT VerbaNode Android release key...
echo.
echo Java:
echo   %JAVA_HOME%
echo Keytool:
echo   %VN_KEYTOOL%
echo Keystore:
echo   %VN_KEYSTORE_PATH%
echo.

"%VN_KEYTOOL%" -genkeypair -v ^
  -keystore "%VN_KEYSTORE_PATH%" ^
  -storetype JKS ^
  -alias "%VN_KEY_ALIAS%" ^
  -keyalg RSA ^
  -keysize 4096 ^
  -validity 36500 ^
  -dname "CN=VerbaNode, OU=Software, O=VerbaNode" ^
  -storepass "%VN_KEYSTORE_PASSWORD%" ^
  -keypass "%VN_KEY_PASSWORD%"

if errorlevel 1 (
    echo.
    echo ERROR: keytool failed.
    if exist "%VN_KEYSTORE_PATH%" del /q "%VN_KEYSTORE_PATH%" >nul 2>&1
    goto :fatal
)

> "%VN_SIGNING_FILE%" echo VN_KEYSTORE_PATH=%VN_KEYSTORE_PATH%
>>"%VN_SIGNING_FILE%" echo VN_KEYSTORE_PASSWORD=%VN_KEYSTORE_PASSWORD%
>>"%VN_SIGNING_FILE%" echo VN_KEY_ALIAS=%VN_KEY_ALIAS%
>>"%VN_SIGNING_FILE%" echo VN_KEY_PASSWORD=%VN_KEY_PASSWORD%

if errorlevel 1 (
    echo ERROR: Could not save the local signing configuration.
    echo.
    echo IMPORTANT: The keystore may already have been created:
    echo   %VN_KEYSTORE_PATH%
    echo Do NOT generate another key if this APK has been distributed.
    goto :fatal
)

echo.
echo Signing identity created successfully.
echo BACK UP THIS ENTIRE FOLDER:
echo   %VN_SIGNING_DIR%
echo.

goto :signing_ready

:load_signing
for /f "usebackq tokens=1,* delims==" %%A in ("%VN_SIGNING_FILE%") do (
    if /i "%%A"=="VN_KEYSTORE_PATH" set "VN_KEYSTORE_PATH=%%B"
    if /i "%%A"=="VN_KEYSTORE_PASSWORD" set "VN_KEYSTORE_PASSWORD=%%B"
    if /i "%%A"=="VN_KEY_ALIAS" set "VN_KEY_ALIAS=%%B"
    if /i "%%A"=="VN_KEY_PASSWORD" set "VN_KEY_PASSWORD=%%B"
)

:signing_ready
if not defined VN_KEYSTORE_PATH (
    echo ERROR: VN_KEYSTORE_PATH is missing.
    goto :fatal
)
if not defined VN_KEYSTORE_PASSWORD (
    echo ERROR: VN_KEYSTORE_PASSWORD is missing.
    goto :fatal
)
if not defined VN_KEY_ALIAS (
    echo ERROR: VN_KEY_ALIAS is missing.
    goto :fatal
)
if not defined VN_KEY_PASSWORD (
    echo ERROR: VN_KEY_PASSWORD is missing.
    goto :fatal
)
if not exist "%VN_KEYSTORE_PATH%" (
    echo ERROR: Release keystore not found:
    echo   %VN_KEYSTORE_PATH%
    echo Restore it from your backup. Do NOT create a new key after distribution.
    goto :fatal
)

rem ------------------------------------------------------------
rem Read Android version without PowerShell regex parsing.
rem ------------------------------------------------------------
set "APP_VERSION="
for /f "tokens=3" %%V in ('findstr /r /c:"versionName *= *" "app\build.gradle.kts"') do if not defined APP_VERSION set "APP_VERSION=%%~V"
if not defined APP_VERSION set "APP_VERSION=unknown"

echo.
echo Building signed VerbaNode Android v%APP_VERSION%...
echo.

call "%~dp0gradlew.bat" clean testDebugUnitTest assembleRelease
if errorlevel 1 (
    echo.
    echo ERROR: Android release APK build failed.
    goto :fatal
)

set "SOURCE_APK=%CD%\app\build\outputs\apk\release\app-release.apk"
if not exist "%SOURCE_APK%" (
    echo ERROR: Gradle completed but the APK was not found:
    echo   %SOURCE_APK%
    goto :fatal
)

if not exist "%CD%\dist" mkdir "%CD%\dist"
set "DIST_APK=%CD%\dist\VerbaNode-Android-v%APP_VERSION%.apk"

copy /y "%SOURCE_APK%" "%DIST_APK%" >nul
if errorlevel 1 (
    echo ERROR: Could not copy the APK to:
    echo   %DIST_APK%
    goto :fatal
)

rem ------------------------------------------------------------
rem Verify signature when apksigner is available.
rem ------------------------------------------------------------
set "APK_SIGNER="
if defined ANDROID_HOME if exist "%ANDROID_HOME%\build-tools" (
    for /f "delims=" %%D in ('dir /b /ad "%ANDROID_HOME%\build-tools" 2^>nul ^| sort /r') do (
        if not defined APK_SIGNER if exist "%ANDROID_HOME%\build-tools\%%D\apksigner.bat" set "APK_SIGNER=%ANDROID_HOME%\build-tools\%%D\apksigner.bat"
    )
)

if defined APK_SIGNER (
    echo.
    echo Verifying APK signature...
    call "%APK_SIGNER%" verify --verbose --print-certs "%DIST_APK%"
    if errorlevel 1 (
        echo ERROR: APK signature verification failed.
        del /q "%DIST_APK%" >nul 2>&1
        goto :fatal
    )
) else (
    echo.
    echo WARNING: apksigner.bat was not found, so explicit signature verification was skipped.
)

rem ------------------------------------------------------------
rem SHA-256
rem ------------------------------------------------------------
set "APK_SHA256="
for /f "skip=1 tokens=* delims=" %%H in ('certutil -hashfile "%DIST_APK%" SHA256 2^>nul') do (
    if not defined APK_SHA256 set "APK_SHA256=%%H"
)
set "APK_SHA256=%APK_SHA256: =%"

if defined APK_SHA256 (
    for %%F in ("%DIST_APK%") do set "APK_FILENAME=%%~nxF"
    >"%DIST_APK%.sha256" echo %APK_SHA256%  %APK_FILENAME%
)

echo.
echo ============================================================
echo   RELEASE APK READY
echo ============================================================
echo.
echo   %DIST_APK%
if defined APK_SHA256 echo   SHA-256: %APK_SHA256%
echo.
echo Signing key:
echo   %VN_KEYSTORE_PATH%
echo.
echo IMPORTANT:
echo Back up this folder and never regenerate it after distribution:
echo   %VN_SIGNING_DIR%
echo.
goto :success

:fatal
echo.
echo ============================================================
echo   BUILD STOPPED
echo ============================================================
echo.
echo The window will stay open so you can read the error.
echo.
pause
exit /b 1

:success
pause
exit /b 0
