#!/bin/sh

set -e

cd "$(dirname "$0")"
gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.kstutorial.producer.Main"

exec java -cp "${CP}" $MAIN "$@"
#exec java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 -cp "${CP}" $MAIN "$@"
