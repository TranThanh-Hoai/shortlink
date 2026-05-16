#!/bin/bash
PORT=${1:-8080}
URL="http://localhost:$PORT/ping"

echo "Checking Health at $URL ..."
RESPONSE=$(curl -s $URL)

if [[ $RESPONSE == *"UP"* ]]; then
    echo "[SUCCESS] Service is UP!"
    echo $RESPONSE
else
    echo "[FAILURE] Service is NOT UP or not responding."
    echo $RESPONSE
    exit 1
fi
