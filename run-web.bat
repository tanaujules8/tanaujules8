@echo off
where gradle >nul 2>&1
if %ERRORLEVEL%==0 (
  gradle run --args="web"
  exit /b %ERRORLEVEL%
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-web.ps1"
exit /b %ERRORLEVEL%
