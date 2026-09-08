@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (gradle %*) else (echo Gradle is not installed and gradle-wrapper.jar is unavailable in this checkout. & exit /b 127)

