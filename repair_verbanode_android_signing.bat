@echo off
setlocal
title VerbaNode Android Signing Repair

set "SIGNING_DIR=%USERPROFILE%\.verbanode-signing"
set "KEYSTORE=%SIGNING_DIR%\verbanode-release.jks"
set "CONFIG=%SIGNING_DIR%\verbanode-signing.env"

echo ============================================================
echo   VerbaNode Android - Signing Repair
echo ============================================================
echo.

if not exist "%KEYSTORE%" (
    echo No orphaned release keystore was found.
    echo Run build_release_apk.bat normally.
    echo.
    pause
    exit /b 0
)

if exist "%CONFIG%" (
    echo Signing configuration already exists.
    echo Nothing to repair.
    echo.
    pause
    exit /b 0
)

echo Found an orphaned release keystore:
echo   %KEYSTORE%
echo.
echo Its password/configuration is missing.
echo.
echo IMPORTANT:
echo If you have EVER distributed or installed a RELEASE APK signed with
echo this exact keystore and need future updates to work, DO NOT delete it.
echo You must recover the original signing passwords instead.
echo.
echo If NO release APK signed with this key has been distributed/relied on,
echo it is safe to remove this unusable orphan and let the builder create
echo a fresh permanent signing identity.
echo.
set /p CONFIRM=Type DELETE-ORPHAN to remove it, or anything else to cancel: 

if /i not "%CONFIRM%"=="DELETE-ORPHAN" (
    echo.
    echo Cancelled. Nothing was changed.
    pause
    exit /b 1
)

del /q "%KEYSTORE%"
if errorlevel 1 (
    echo.
    echo ERROR: Could not delete the orphaned keystore.
    pause
    exit /b 1
)

echo.
echo Orphaned keystore removed.
echo Now run:
echo   build_release_apk.bat
echo.
echo The builder will create a fresh permanent signing key and matching
echo local signing configuration together.
echo.
pause
exit /b 0
