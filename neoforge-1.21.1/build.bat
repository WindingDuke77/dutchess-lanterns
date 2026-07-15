@echo off
rem NeoForge 1.21.1 build - needs JDK 21. Prefers a portable tools\jdk21, falls back
rem to the Prism launcher's java-runtime-delta (a full JDK 21 with javac).
if exist "%~dp0tools\jdk21\bin\javac.exe" (
    set "JAVA_HOME=%~dp0tools\jdk21"
) else (
    set "JAVA_HOME=E:\PrismLauncher\java\java-runtime-delta"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0tools\gradle-8.10.2\bin\gradle.bat" -p "%~dp0." build %*
