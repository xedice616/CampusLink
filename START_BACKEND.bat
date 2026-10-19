@echo off
cd /d "%~dp0backend"
where mvn >nul 2>nul
if %errorlevel%==0 (
  mvn spring-boot:run
) else (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-backend.ps1"
)
pause
