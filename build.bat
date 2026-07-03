@echo off
rem Gradle 4.10.3 cannot launch on modern JVMs - force the portable JDK 8
set "JAVA_HOME=%~dp0tools\jdk8"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0gradlew.bat" build %*
