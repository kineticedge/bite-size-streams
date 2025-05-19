#!/bin/bash

set -e

cd "$(dirname "$0")"
gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.kstutorial.common.main.Main"


TOPOLOGIES=(
    "ks101.HelloWorld"
    "ks101.Serde"
    "ks101.HiddenSerde"
    "ks201.EmitOnChange"
    "ks201.KTableOptimized"
    "ks301.StreamToWindow"
    "ks301.SessionWindow"
)

#if [ $# -eq 0 ]; then

if [[ $# -gt 0 && ! $1 =~ ^- ]]; then

  TOPOLOGY=$1
  shift

else

display_menu() {
    echo "Select a topology:"
    echo ""
    for ((i=1; i<=${#TOPOLOGIES[@]}; i++)); do
        echo "$i. ${TOPOLOGIES[$i-1]}"
    done
    echo ""
}

display_menu
tput setaf 3; printf "Enter the number of your choice: "; tput sgr 0
read -p "" choice
if [[ $choice -ge 1 && $choice -le ${#TOPOLOGIES[@]} ]]; then
  TOPOLOGY=${TOPOLOGIES[$choice-1]}
else
  echo "invalid selection"
  exit
fi

fi

#REMOTE="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
REMOTE=""

java ${REMOTE} -cp "${CP}" $MAIN --topology "io.kineticedge.${TOPOLOGY}" "$@"
