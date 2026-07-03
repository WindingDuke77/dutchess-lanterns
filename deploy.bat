@echo off
rem Copies the newest built jar into the Tekxit instance - refuses while the game is open
set "MODS=E:\PrismLauncher\instances\Tekxit 3.14 Pi (Official)\minecraft\mods"

tasklist /FI "IMAGENAME eq javaw.exe" 2>nul | find /I "javaw.exe" >nul
if not errorlevel 1 (
    echo A javaw.exe game process is running - close Minecraft first, then re-run this.
    exit /b 1
)

set "JAR="
for /f "delims=" %%f in ('dir /b /o:d "%~dp0build\libs\lantern-*.jar" ^| findstr /v sources') do set "JAR=%%f"
if not defined JAR (
    echo No built jar found - run build.bat first.
    exit /b 1
)

del /q "%MODS%\lantern-*.jar" 2>nul
copy /y "%~dp0build\libs\%JAR%" "%MODS%" >nul
echo Deployed %JAR% to %MODS%
