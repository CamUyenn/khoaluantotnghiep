@echo off
set SCRIPT_DIR=%~dp0
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%migrate-sqlserver-to-postgres.ps1" %*
exit /b %ERRORLEVEL%
