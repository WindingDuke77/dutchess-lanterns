@echo off
rem Publishes the newest built jar as a GitHub release so friends' pre-launch
rem updater (update-lantern.ps1) picks it up. Run after build.bat + git push.

set "JAR="
for /f "delims=" %%f in ('dir /b /o:d "%~dp0build\libs\lantern-*.jar" ^| findstr /v sources') do set "JAR=%%f"
if not defined JAR (
    echo No built jar found - run build.bat first.
    exit /b 1
)

rem lantern-3.3.1.jar -> v3.3.1
set "VER=%JAR:lantern-=%"
set "VER=%VER:.jar=%"

gh release create v%VER% "%~dp0build\libs\%JAR%" --title "%VER%" --generate-notes
if errorlevel 1 (
    echo Release failed - does v%VER% already exist?
    exit /b 1
)
echo Published v%VER%
