
alias d='docker'
alias docker-compose='docker compose'
alias dn='docker network'
alias kt='kafka-topics --bootstrap-server localhost:19092'

if ! [ -x "$(command -v docker)" ]; then
    echo "docker is not installed." >&2
    exit 1
fi

docker info > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "docker server is not running." >&2
  exit
fi

#
# creates a network unique to this project that can be shared between docker compose instances
# kafka-streams-101 -> ks101
#
NETWORK=$(docker network inspect -f '{{.Name}}' ks101 2>/dev/null)
if [ "$NETWORK" != "ks101" ]; then
  (docker network create ks101 >/dev/null)
fi

(cd cluster_zk; docker-compose up -d --wait)
#(cd cluster; docker-compose up -d --wait)

./gradlew build

kt --create --replication-factor 3 --partitions 4 --topic names --if-not-exists
kt --create --replication-factor 3 --partitions 4 --topic emails --if-not-exists
kt --create --replication-factor 3 --partitions 4 --topic phones --if-not-exists
kt --create --replication-factor 3 --partitions 4 --topic customers360 --if-not-exists
kt --create --replication-factor 3 --partitions 4 --topic duplicate-phones --if-not-exists
kt --create --if-not-exists --replication-factor 3 --partitions 4 --topic duplicate-phones-groupByPhoneNumber-repartition


(cd applications; docker-compose up -d)
(cd monitoring; docker-compose up -d)

