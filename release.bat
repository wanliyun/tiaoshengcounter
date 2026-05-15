@echo off
setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
set "KEYSTORE=%PROJECT_DIR%tiaosheng.jks"
set "PROPS=%PROJECT_DIR%keystore.properties"
set "APK_SRC=%PROJECT_DIR%app\build\outputs\apk\release\app-release.apk"
set "OUT_DIR=%PROJECT_DIR%release"
set "KEY_ALIAS=tiaosheng"
set "STORE_PASS=tiaosheng2024"
set "KEY_PASS=tiaosheng2024"
set "CERT_CN=Tiaosheng Counter"
set "CERT_OU=Dev"
set "CERT_O=Tiaosheng"
set "CERT_C=CN"

echo ============================================
echo    Tiaosheng Counter - Release Builder
echo ============================================
echo.

:: --- Locate tools ---
:: JAVA_HOME / keytool — pick highest JDK version
if not defined JAVA_HOME (
    for /f "delims=" %%D in ('dir /b /ad /o-n "C:\Program Files\Java\jdk-*" 2^>nul') do (
        set "JAVA_HOME=C:\Program Files\Java\%%D"
        goto :java_done
    )
    for /f "delims=" %%D in ('dir /b /ad /o-n "%USERPROFILE%\.jdks\*" 2^>nul') do (
        set "JAVA_HOME=%USERPROFILE%\.jdks\%%D"
        goto :java_done
    )
)
:java_done
if not defined JAVA_HOME (
    echo ERROR: JAVA_HOME not set and no JDK found.
    echo Please set JAVA_HOME to your JDK directory.
    exit /b 1
)
set "KEYTOOL=%JAVA_HOME%\bin\keytool.exe"
echo JAVA_HOME=%JAVA_HOME%

:: Android SDK / apksigner
if not defined ANDROID_HOME (
    if exist "%PROJECT_DIR%local.properties" (
        for /f "tokens=2 delims==" %%A in ('findstr "sdk.dir" "%PROJECT_DIR%local.properties"') do (
            set "SDK_DIR=%%A"
        )
        if defined SDK_DIR set "ANDROID_HOME=!SDK_DIR!"
    )
)
if defined ANDROID_HOME (
    for /f %%V in ('dir /b /ad /o-n "%ANDROID_HOME%\build-tools" 2^>nul') do (
        set "BT_VER=%%V"
        goto :bt_found
    )
)
:bt_found
if defined BT_VER (
    set "APKSIGNER=%ANDROID_HOME%\build-tools\%BT_VER%\apksigner.bat"
    echo ANDROID_HOME=%ANDROID_HOME%
) else (
    set "APKSIGNER="
    echo WARNING: apksigner not found, signature verification skipped.
)
echo.

:: Step 1: Generate keystore if not exists
if not exist "%KEYSTORE%" (
    echo [1/4] Generating keystore: %KEYSTORE%
    "%KEYTOOL%" -genkeypair -v ^
        -keystore "%KEYSTORE%" ^
        -alias "%KEY_ALIAS%" ^
        -keyalg RSA -keysize 2048 -validity 10000 ^
        -storepass "%STORE_PASS%" -keypass "%KEY_PASS%" ^
        -dname "CN=%CERT_CN%, OU=%CERT_OU%, O=%CERT_O%, C=%CERT_C%"
    if errorlevel 1 (
        echo ERROR: Failed to generate keystore
        exit /b 1
    )
    echo Keystore created successfully.
) else (
    echo [1/4] Keystore already exists, skip.
)

:: Step 2: Write keystore.properties
echo [2/4] Writing keystore.properties
(
echo storeFile=../tiaosheng.jks
echo storePassword=%STORE_PASS%
echo keyAlias=%KEY_ALIAS%
echo keyPassword=%KEY_PASS%
) > "%PROPS%"
echo keystore.properties written.

:: Step 3: Build release APK
echo [3/4] Building release APK...
call "%PROJECT_DIR%gradlew.bat" assembleRelease
if errorlevel 1 (
    echo ERROR: Build failed
    exit /b 1
)
echo Build successful.

:: Step 4: Copy APK to release directory
echo [4/4] Copying APK to release directory...
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
copy /Y "%APK_SRC%" "%OUT_DIR%\tiaosheng-counter-release.apk" >nul

:: Show result
echo.
echo ============================================
echo    Build Complete
echo ============================================
for %%A in ("%OUT_DIR%\tiaosheng-counter-release.apk") do (
    set "SIZE=%%~zA"
    set /a "SIZE_KB=!SIZE! / 1024"
    set /a "SIZE_MB=!SIZE! / 1048576"
)
echo Output: %OUT_DIR%\tiaosheng-counter-release.apk (!SIZE_MB! MB)
echo.

:: Verify signature
if defined APKSIGNER (
    echo Verifying signature...
    call "%APKSIGNER%" verify --print-certs "%OUT_DIR%\tiaosheng-counter-release.apk"
    if errorlevel 1 (
        echo WARNING: Signature verification failed
    ) else (
        echo Signature verified.
    )
) else (
    echo Signature verification skipped ^(apksigner not found^).
)
echo.
echo Done.
endlocal
