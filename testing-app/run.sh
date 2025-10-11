#!/bin/sh

set -e

cd "$(dirname "$0")"
gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.kstutorial.tapp.Main"

exec java -Dapp.name=TESTING_APP -cp "${CP}" $MAIN "$@"
