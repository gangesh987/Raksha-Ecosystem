#!/usr/bin/env bash
# Raksha Ecosystem - Stop Backend Server
echo "Stopping Raksha Demo Backend on port 8000..."
PID=$(lsof -ti :8000 2>/dev/null || fuser 8000/tcp 2>/dev/null)
if [ -n "$PID" ]; then
    kill -9 $PID 2>/dev/null
    echo "Backend stopped successfully (PID $PID)."
else
    echo "No backend process found listening on port 8000."
fi
