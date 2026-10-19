@echo off
cd /d "%~dp0frontend"
if not exist node_modules call npm.cmd install
call npm.cmd run dev
pause
