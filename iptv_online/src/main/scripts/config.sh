#!/bin/bash
########################################################## load setenv.sh #########################################################
if [ -x "$ROOT_DIR/bin/setenv.sh" ]; then
  source "$ROOT_DIR/bin/setenv.sh"
fi

########################################################## get main class #########################################################
if [ -z "$MAIN_CLASS" ]; then
  if [ -e $ROOT_DIR/app/META-INF/MANIFEST.MF ]; then
    MAIN_CLASS=$(cat $ROOT_DIR/app/META-INF/MANIFEST.MF | grep "^Main-Class" | cut -d : -f 2)
    MAIN_CLASS=$(echo $MAIN_CLASS | tr -d '\r')
  else
    printf "unknown main class, $ROOT_DIR/app/META-INF/MANIFEST.MF file not exist"
    exit 1
  fi
fi
if [ -z $MAIN_CLASS ]; then
  printf "unknown main class, MAIN_CLASS not set"
  exit 1
fi

#PROJECT_NAME=$(basename $(pwd)).jar
#if [ -n "$JAR_NAME" ]; then
#  PROJECT_NAME=$JAR_NAME
#fi

########################################################## set app home path ######################################################
if [ -z "$APP_HOME" ]; then
  APP_HOME=$ROOT_DIR
fi

########################################################## set java command #######################################################
JAVA_HOME_PATH=""
HAS_JAVA_COMMAND="false"
if [ -n "$JRE_HOME" ]; then
  JAVA_HOME_PATH=$JRE_HOME
  printf "Setting JAVA_HOME property to %0s\n" "$JRE_HOME"
fi

if [[ -z "$JAVA_HOME_PATH" && -n "$JAVA_HOME" ]]; then
  JAVA_HOME_PATH=$JAVA_HOME
  printf "Setting JAVA_HOME property to %0s\n" $JAVA_HOME
fi

if hash java 2>/dev/null; then
  HAS_JAVA_COMMAND="true"
fi

if [[ -z "$JAVA_HOME_PATH" && $HAS_JAVA_COMMAND = "false" ]]; then
  printf "JAVA_HOME path doesn't exist\n"
  exit 1
fi

JAVA=""
if [ $HAS_JAVA_COMMAND = "true" ]; then
  JAVA=java
else
  JAVA=$JAVA_HOME_PATH/bin/java
fi

########################################################## set class path ##########################################################
CLASS_PATH="$ROOT_DIR/app/:$ROOT_DIR/config/:$ROOT_DIR/lib/*:$CLASS_PATH"

########################################################## set java opts ###########################################################
JAVA_OPTS="-Dapp.home=$APP_HOME -Dstdout.print.2console=false -Dspring.config.location=classpath:/,classpath:/config/,file:$ROOT_DIR/config/,file:$ROOT_DIR/config/,file:$ROOT_DIR/config/application.yml -Dspring.profiles.active=test -XX:-OmitStackTraceInFastThrow -server -Xmx2048m -Xms2048m -Xmn1024m -Xss2m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:+CMSParallelInitialMarkEnabled -XX:+DisableExplicitGC -XX:+CMSScavengeBeforeRemark -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/ott/ottIPTVoom $JAVA_OPTS"
