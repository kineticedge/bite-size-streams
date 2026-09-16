#!/bin/bash

export KAFKA_ADVERTISED_LISTENERS="BROKER_SSL://localhost:1090,INTERNAL://kafka-broker-0:1092"

if [[ -n "${KAFKA_ADVERTISED_LISTENERS-}" ]] && [[ $KAFKA_ADVERTISED_LISTENERS == *"SSL://"* ]]
then
  echo "SSL is enabled."
fi

