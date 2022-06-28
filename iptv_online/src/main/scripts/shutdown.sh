#!/bin/bash
DIR1="${BASH_SOURCE[0]}"
DIR2="$( dirname $DIR1 )"
ROOT_DIR="$( cd $DIR2 && pwd )/../"
cd $ROOT_DIR
ROOT_DIR=$(pwd)

source "$ROOT_DIR/bin/config.sh"

PID=$(cat $APP_HOME/application.pid)
kill $PID
