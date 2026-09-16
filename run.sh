#!/bin/bash

if [ $# -eq 0 ]; then
  echo "usage: $0 topology <options>"
  exit
fi

set -e

cd "$(dirname "$0")/tutorial"

gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.kstutorial.common.main.Main"

REMOTE=""
#REMOTE="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"


TOPOLOGY=$1
shift

java ${REMOTE} -cp "${CP}" $MAIN --topology "io.kineticedge.${TOPOLOGY}" "$@"
