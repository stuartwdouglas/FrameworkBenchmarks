#!/bin/bash

PORT=$1
NAME=$2
IFS=', ' read -r -a LOADS <<< "$3"
OUTPUT_DIR=$4


#CONNECTIONS=(128 256 512 1024)
CONNECTIONS=(256)

QUERIES=(1 4 8)

echo "$(date): Running Update Load: port; $PORT"
echo "$(date): Running Update Warmup"
#WARM-UP
wrk -H 'Host: localhost' -H 'Accept: application/json,text/html;q=0.9,application/xhtml+xml;q=0.9,application/xml;q=0.8,*/*;q=0.7' -H 'Connection: keep-alive' --latency -d 60 -c 128 --timeout 8 -t 24  "http://localhost:$PORT/updates?queries=1" > /dev/null

echo ""
echo "$(date): Starting measurements"
echo ""

mkdir -p $OUTPUT_DIR

for CONNECTION in $CONNECTIONS 
do
  for load in "${LOADS[@]}"
  do
      for QUERY in "${QUERIES[@]}"
      do
        FILENAME="$NAME.$CONNECTIONS.$load.$QUERY.wrk"
        echo "$(date): Running Updates:  $NAME (connections; $CONNECTIONS, load; $load, queries; $QUERY). Writing to $FILENAME"
        wrk2 -R $load -H 'Host: localhost' -H 'Accept: application/json,text/html;q=0.9,application/xhtml+xml;q=0.9,application/xml;q=0.8,*/*;q=0.7' -H 'Connection: keep-alive' --latency -d 15 -c $CONNECTIONS --timeout 8 -t 24  "http://localhost:$PORT/updates?queries=$QUERY" > "$OUTPUT_DIR/$FILENAME"
      done
  done
done

echo "$(date): Done Update Load"