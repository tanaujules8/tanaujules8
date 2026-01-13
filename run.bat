@echo off
where gradle >nul 2>&1
if %ERRORLEVEL%==0 (
  gradle run
  exit /b %ERRORLEVEL%
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1"
exit /b %ERRORLEVEL%
