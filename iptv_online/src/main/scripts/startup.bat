@echo off
SET ROOT_DIR=%~dp0/../
cd %ROOT_DIR%
SET ROOT_DIR=%cd%

call "%ROOT_DIR%/bin/config.bat"
SET "EXEC=%JAVA% -classpath %CLASS_PATH% %JAVA_OPTS% %MAIN_CLASS%"

SET LOGS_DIR=%ROOT_DIR%
SET START_LOG=%LOGS_DIR%\start.log
if not EXIST "%START_LOG%" (
  ECHO > %START_LOG%
)

ECHO exec command is: %EXEC%
START "%APP_HOME%" cmd /c %EXEC% ^> %START_LOG%