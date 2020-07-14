#!/bin/bash

PORT=$1

echo "Port: $PORT"
echo "Warmup"
#WARM-UP
wrk -H 'Host: localhost' -H 'Accept: application/json,text/html;q=0.9,application/xhtml+xml;q=0.9,application/xml;q=0.8,*/*;q=0.7' -H 'Connection: keep-alive' --latency -d 60 -c 128 --timeout 8 -t 24  "http://localhost:$PORT/db"

echo ""
echo "Starting measurements"
echo ""


for CONNECTIONS in 128 256 512 1024
do
	wrk -H 'Host: localhost' -H 'Accept: application/json,text/html;q=0.9,application/xhtml+xml;q=0.9,application/xml;q=0.8,*/*;q=0.7' -H 'Connection: keep-alive' --latency -d 15 -c $CONNECTIONS --timeout 8 -t 24  "http://localhost:$PORT/db"

done

