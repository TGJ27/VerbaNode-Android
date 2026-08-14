@echo off
setlocal
cd /d "%~dp0"

call "%~dp0detect_android_env.bat" java-only
if errorlevel 1 exit /b 1

set "GRADLE_VERSION=9.5.0"
set "CACHE_DIR=%CD%\.gradle-bootstrap"
set "DIST_DIR=%CACHE_DIR%\gradle-%GRADLE_VERSION%"
set "ZIP_PATH=%CACHE_DIR%\gradle-%GRADLE_VERSION%-bin.zip"

if exist "gradlew.bat" exit /b 0
if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"
if not exist "%DIST_DIR%\bin\gradle.bat" (
  echo Downloading Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP_PATH%'"
  if errorlevel 1 exit /b 1
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%CACHE_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

echo Generating Gradle wrapper...
call "%DIST_DIR%\bin\gradle.bat" wrapper --gradle-version %GRADLE_VERSION% --distribution-type bin
exit /b %ERRORLEVEL%
