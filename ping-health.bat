@echo off
set PORT=8080
if not "%1"=="" set PORT=%1

echo Checking Health at http://localhost:%PORT%/ping ...
curl -s http://localhost:%PORT%/ping | findstr "UP" > nul

if %errorlevel% equ 0 (
    echo [SUCCESS] Service is UP!
    curl -s http://localhost:%PORT%/ping
) else (
    echo [FAILURE] Service is NOT UP or not responding.
    curl -s http://localhost:%PORT%/ping
)
pause
