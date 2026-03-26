@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem Usage:
rem   release.bat [skip-tests] [output-dir] [deploy-plugins-dir]
rem Examples:
rem   release.bat
rem   release.bat skip-tests
rem   release.bat skip-tests release "E:\MC\PluginTestServers\paper\plugins"

set "PROJECT_ROOT=%~dp0.."
for %%I in ("%PROJECT_ROOT%") do set "PROJECT_ROOT=%%~fI"
set "GRADLEW=%PROJECT_ROOT%\gradlew.bat"
set "LIBS_DIR=%PROJECT_ROOT%\dummy-core\build\libs"

set "SKIP_TESTS=0"
if /I "%~1"=="skip-tests" set "SKIP_TESTS=1"

set "OUTPUT_DIR=%~2"
if "%OUTPUT_DIR%"=="" set "OUTPUT_DIR=%PROJECT_ROOT%\release"

set "DEPLOY_DIR=%~3"

if not exist "%GRADLEW%" (
  echo [Release] ERROR: gradlew.bat not found: %GRADLEW%
  exit /b 1
)

echo [Release] Starting build...
pushd "%PROJECT_ROOT%" >nul
if "%SKIP_TESTS%"=="1" (
  call "%GRADLEW%" :dummy-core:clean :dummy-core:build -x test
) else (
  call "%GRADLEW%" :dummy-core:clean :dummy-core:build
)
if errorlevel 1 (
  popd >nul
  echo [Release] ERROR: Gradle build failed.
  exit /b 1
)
popd >nul

if not exist "%LIBS_DIR%" (
  echo [Release] ERROR: Artifact dir not found: %LIBS_DIR%
  exit /b 1
)

set "JAR_FILE="
for /f "delims=" %%F in ('dir /b /o-d "%LIBS_DIR%\Dummy-*.jar" 2^>nul') do (
  if /I not "%%~nxF"=="" (
    set "NAME=%%~nxF"
    echo !NAME! | findstr /I /C:"-sources" /C:"-javadoc" >nul
    if errorlevel 1 (
      set "JAR_FILE=%LIBS_DIR%\%%F"
      goto :jar_found
    )
  )
)

:jar_found
if "%JAR_FILE%"=="" (
  echo [Release] ERROR: Release jar not found in %LIBS_DIR%
  exit /b 1
)

set "VERSION=unknown"
for /f "usebackq tokens=2 delims==" %%V in (`findstr /R /C:"^version[ ]*=[ ]*\".*\"" "%PROJECT_ROOT%\build.gradle.kts"`) do (
  set "RAW_VER=%%V"
  set "RAW_VER=!RAW_VER: =!"
  set "RAW_VER=!RAW_VER:"=!"
  set "VERSION=!RAW_VER!"
)

for /f %%T in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "TS=%%T"
set "RELEASE_DIR=%OUTPUT_DIR%\v%VERSION%-%TS%"
if not exist "%RELEASE_DIR%" mkdir "%RELEASE_DIR%"

for %%N in ("%JAR_FILE%") do set "JAR_NAME=%%~nxN"
copy /Y "%JAR_FILE%" "%RELEASE_DIR%\%JAR_NAME%" >nul
if errorlevel 1 (
  echo [Release] ERROR: Failed to copy artifact to release dir.
  exit /b 1
)

set "RELEASE_JAR=%RELEASE_DIR%\%JAR_NAME%"
set "SHA_FILE=%RELEASE_JAR%.sha256"
powershell -NoProfile -Command "$h=(Get-FileHash -Algorithm SHA256 -Path '%RELEASE_JAR%').Hash; Set-Content -Path '%SHA_FILE%' -Value ($h + ' *%JAR_NAME%') -Encoding UTF8"
if errorlevel 1 (
  echo [Release] ERROR: Failed to generate SHA256 file.
  exit /b 1
)

if not "%DEPLOY_DIR%"=="" (
  if not exist "%DEPLOY_DIR%" (
    echo [Release] ERROR: Deploy dir not found: %DEPLOY_DIR%
    exit /b 1
  )
  copy /Y "%RELEASE_JAR%" "%DEPLOY_DIR%\%JAR_NAME%" >nul
  if errorlevel 1 (
    echo [Release] ERROR: Failed to deploy jar.
    exit /b 1
  )
  echo [Release] Deployed to: %DEPLOY_DIR%
)

echo [Release] Build complete
echo [Release] Version: %VERSION%
echo [Release] Artifact: %RELEASE_JAR%
echo [Release] SHA256: %SHA_FILE%

exit /b 0
