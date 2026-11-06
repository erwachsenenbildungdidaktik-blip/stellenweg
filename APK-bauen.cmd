@echo off
rem Baut die Android-App neu und legt sie unter web\download\Stellenweg.apk ab.
rem Werkzeuge liegen portabel im Ordner tools (Java 17, Android SDK). Vorher Versionsnummer in android\app\build.gradle erhoehen.
setlocal
cd /d "%~dp0"
for /d %%J in ("%~dp0tools\jdk-17*") do set "JAVA_HOME=%%~fJ"
set "ANDROID_HOME=%~dp0tools\android-sdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd android
call gradlew.bat assembleRelease --no-daemon
if errorlevel 1 (echo. & echo Build fehlgeschlagen. & pause & exit /b 1)
copy /y "app\build\outputs\apk\release\app-release.apk" "..\web\download\Stellenweg.apk" >nul
echo. & echo Fertig: web\download\Stellenweg.apk
pause
