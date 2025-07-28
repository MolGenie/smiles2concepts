#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
PID_FILE="$SCRIPT_DIR/../run/smiles2structure.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "PID file not found. Application may not be running."
    exit 1
fi

pid=$(cat "$PID_FILE")

if ! ps -p $pid > /dev/null 2>&1; then
    echo "Process $pid is not running. Removing stale PID file."
    rm "$PID_FILE"
    exit 1
fi

echo "Stopping smiles2concepts application (PID: $pid)..."
kill $pid

# Wait for the process to terminate (up to 30 seconds)
for i in {1..30}; do
    if ! ps -p $pid > /dev/null 2>&1; then
        echo "Application stopped successfully."
        rm "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# If process is still running after 30 seconds, force kill
if ps -p $pid > /dev/null 2>&1; then
    echo "Application did not stop gracefully. Forcing termination..."
    kill -9 $pid
    rm "$PID_FILE"
    echo "Application forcefully terminated."
fi 