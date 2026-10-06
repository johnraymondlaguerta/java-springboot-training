#!/usr/bin/env bash
# Starts TWO separate MongoDB servers: one per microservice.
#   mongo1 -> localhost:27017  (product-service -> productdb)
#   mongo2 -> localhost:27018  (order-service   -> orderdb)
MDB_VERSION=7

start_mongodb() {
  port=$((27016 + $1))
  echo "Starting MongoDB container 'mongo$1' on port $port"
  docker run --rm -d -p "${port}:27017" --name "mongo$1" "mongo:${MDB_VERSION}"
}

for i in $(seq 2); do
  start_mongodb "$i"
done
