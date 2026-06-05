@echo off
set SCRIPT_DIR=%~dp0
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%start-redis.ps1" %*
exit /b %ERRORLEVEL%
