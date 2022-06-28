REM ########################################################## load setenv.sh #########################################################
if EXIST "%ROOT_DIR%/bin/setenv.bat" (
  CALL "%ROOT_DIR%/bin/setenv.bat
)

REM ########################################################## get main class #########################################################
IF DEFINED MAIN_CLASS (
  IF "%MAIN_CLASS%" NEQ "" GOTO MAIN_CLASS_DONE
)

SET MAIN_CLASS_VAR=Main-Class
IF EXIST %ROOT_DIR%/app/META-INF/MANIFEST.MF (
  FOR /f "tokens=1,2 delims=:" %%a IN (%ROOT_DIR%/app/META-INF/MANIFEST.MF) DO (
    IF %%a EQU %MAIN_CLASS_VAR% (
      SET MAIN_CLASS=%%b
      GOTO MAIN_CLASS_DONE
    )
  )
)

ECHO "unknown main class, MAIN_CLASS not set"
EXIT
:MAIN_CLASS_DONE

REM ########################################################## set app home path ######################################################
IF DEFINED APP_HOME (
  IF "%APP_HOME%" NEQ "" GOTO APP_HOME_DONE
)

SET "APP_HOME=%ROOT_DIR%"
:APP_HOME_DONE

REM ########################################################## set java command #######################################################
IF DEFINED JRE_HOME (
  IF "%JRE_HOME%" NEQ "" (
    ECHO Setting JAVA_HOME property to %JRE_HOME%
    SET "JAVA=%JRE_HOME%\bin\java.exe"
    GOTO JAVA_COMMAND_DONE
  )
)

IF DEFINED JAVA_HOME (
  IF "%JAVA_HOME%" NEQ "" (
    ECHO Setting JAVA_HOME property to %JAVA_HOME%
    SET "JAVA=%JAVA_HOME%\bin\java.exe"
    GOTO JAVA_COMMAND_DONE
  )
)

FOR /F "delims=" %%a IN ("java.exe") DO (
  IF EXIST %%~$PATH:a (
    SET "JAVA=java.exe"
    GOTO JAVA_COMMAND_DONE
  )
)

ECHO JAVA_HOME path doesn't exist
EXIT
:JAVA_COMMAND_DONE

REM ########################################################## set class path ##########################################################
SET "CLASS_PATH=%ROOT_DIR%\app\;%ROOT_DIR%\config\;%ROOT_DIR%\lib\*;%CLASS_PATH%"

REM ########################################################## set java opts ###########################################################
SET "JAVA_OPTS=-Dapp.home=%APP_HOME% -Dstdout.print.2console=false -Dspring.config.location=classpath:/,classpath:/config/,file:%ROOT_DIR%\config\,file:%ROOT_DIR%\config\,file:%ROOT_DIR%\config\application-prod.properties -Dspring.profiles.active=prod -XX:-OmitStackTraceInFastThrow %JAVA_OPTS%"
