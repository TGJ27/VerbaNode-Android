@echo off
rem Detect the JDK bundled with Android Studio and the default Android SDK.
rem Intentionally no SETLOCAL: values must remain available to the calling script.

set "VN_DETECTED_JAVA="
for /f "usebackq delims=" %%J in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$c=@([Environment]::GetEnvironmentVariable('JAVA_HOME','Process'), [Environment]::GetEnvironmentVariable('STUDIO_JDK','Process'), (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'), (Join-Path $env:LOCALAPPDATA 'Programs\Android Studio\jbr'), (Join-Path $env:ProgramFiles 'Android\Android Studio\jre')); foreach($p in $c){ if($p -and (Test-Path (Join-Path $p 'bin\java.exe'))){ Write-Output $p; exit 0 } }; if(Get-Command java.exe -ErrorAction SilentlyContinue){ Write-Output '__PATH__'; exit 0 }; exit 1"`) do if not defined VN_DETECTED_JAVA set "VN_DETECTED_JAVA=%%J"

if not defined VN_DETECTED_JAVA (
  echo ERROR: Java was not found.
  echo Install Android Studio, then retry. The build scripts automatically use Android Studio's bundled JDK.
  echo Expected default JDK location:
  echo   %ProgramFiles%\Android\Android Studio\jbr
  echo.
  echo If Android Studio is installed somewhere else, set JAVA_HOME to its jbr folder.
  exit /b 1
)

if /i "%VN_DETECTED_JAVA%"=="__PATH__" (
  rem A malformed JAVA_HOME makes Gradle fail even when java.exe is on PATH.
  set "JAVA_HOME="
) else (
  set "JAVA_HOME=%VN_DETECTED_JAVA%"
  set "PATH=%JAVA_HOME%\bin;%PATH%"
)
set "VN_DETECTED_JAVA="

if /i "%~1"=="java-only" exit /b 0

set "VN_DETECTED_SDK="
for /f "usebackq delims=" %%S in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$c=@([Environment]::GetEnvironmentVariable('ANDROID_HOME','Process'), [Environment]::GetEnvironmentVariable('ANDROID_SDK_ROOT','Process'), (Join-Path $env:LOCALAPPDATA 'Android\Sdk')); foreach($p in $c){ if($p -and (Test-Path $p)){ Write-Output $p; exit 0 } }; exit 1"`) do if not defined VN_DETECTED_SDK set "VN_DETECTED_SDK=%%S"

if not defined VN_DETECTED_SDK (
  echo ERROR: Android SDK was not found.
  echo Open Android Studio ^> SDK Manager and install Android SDK Platform 37.
  echo The normal Windows SDK location is:
  echo   %LOCALAPPDATA%\Android\Sdk
  echo.
  echo If your SDK is elsewhere, set ANDROID_HOME or ANDROID_SDK_ROOT to that folder.
  exit /b 1
)

set "ANDROID_HOME=%VN_DETECTED_SDK%"
set "ANDROID_SDK_ROOT=%VN_DETECTED_SDK%"
set "VN_DETECTED_SDK="
exit /b 0
