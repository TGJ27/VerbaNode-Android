@echo off
setlocal
cd /d "%~dp0"

call "%~dp0detect_android_env.bat"
if errorlevel 1 (
  echo Could not prepare the Android build environment.
  pause
  exit /b 1
)

call "%~dp0bootstrap_gradle.bat"
if errorlevel 1 (
  echo Could not prepare Gradle.
  pause
  exit /b 1
)

if not defined VN_KEYSTORE_PATH goto missing_signing
if not defined VN_KEYSTORE_PASSWORD goto missing_signing
if not defined VN_KEY_ALIAS goto missing_signing
if not defined VN_KEY_PASSWORD goto missing_signing

call gradlew.bat clean testDebugUnitTest assembleRelease
if errorlevel 1 (
  echo Android release build failed.
  pause
  exit /b 1
)

echo.
echo Signed APK ready:
echo %CD%\app\build\outputs\apk\release\app-release.apk
pause
exit /b 0

:missing_signing
echo Release signing variables are incomplete.
echo Set VN_KEYSTORE_PATH, VN_KEYSTORE_PASSWORD, VN_KEY_ALIAS and VN_KEY_PASSWORD.
echo See README.md for details.
pause
exit /b 1
