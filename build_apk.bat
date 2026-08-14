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

call gradlew.bat clean testDebugUnitTest assembleDebug
if errorlevel 1 (
  echo Android build failed.
  pause
  exit /b 1
)

echo.
echo APK ready:
echo %CD%\app\build\outputs\apk\debug\app-debug.apk
pause
