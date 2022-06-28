#!/bin/bash
DIR1="${BASH_SOURCE[0]}"
DIR2="$( dirname $DIR1 )"
ROOT_DIR="$( cd $DIR2 && pwd )/../"
cd $ROOT_DIR
ROOT_DIR=$(pwd)

source "$ROOT_DIR/bin/config.sh"

EXEC="$JAVA -classpath $CLASS_PATH $JAVA_OPTS $MAIN_CLASS"

LOGS_DIR=$ROOT_DIR
START_LOG=$LOGS_DIR/start.log
if [ ! -e "$START_LOG" ]; then 
  touch $START_LOG
fi

echo "exec command is: $EXEC" > $START_LOG
nohup $EXEC >> $START_LOG 2>&1 &
tail -50f $START_LOG

