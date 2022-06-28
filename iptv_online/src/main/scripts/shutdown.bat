@echo off
SET ROOT_DIR=%~dp0/../
cd %ROOT_DIR%
SET ROOT_DIR=%cd%

call "%ROOT_DIR%/bin/config.bat"

FOR /f %%a IN (%ROOT_DIR%/application.pid) DO (
  SET PID=%%a
)

taskkill /F /PID %PID%
