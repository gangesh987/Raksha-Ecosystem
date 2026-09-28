#!/usr/bin/env bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "[1/3] Checking Python virtual environment..."
if [ ! -f ".venv/bin/python" ]; then
    echo "Creating .venv..."
    python3 -m venv .venv
    echo "Installing dependencies..."
    .venv/bin/python -m pip install -r server/requirements.txt
    .venv/bin/python -m pip install requests httpx
fi

echo "[2/3] Configuring environment..."
export RAKSHA_API_TOKEN="demo-token-raksha-video"
export RAKSHA_DB_PATH="./data/raksha.db"
export RAKSHA_VERSION="1.0.0"
export RAKSHA_RATE_LIMIT="300"
export WEBRTC_STUN_URL="stun:stun.l.google.com:19302"
export ALLOW_PROTECTION_TEST_EVENTS="true"

# Detect IP address
LAN_IP=$(ip route get 1.1.1.1 2>/dev/null | awk '{print $7}' || ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -n1)
if [ -z "$LAN_IP" ]; then
    LAN_IP="172.17.35.95"
fi

echo ""
echo "=================================================="
echo "RAKSHA BACKEND — DEMO READY"
echo "=================================================="
echo "Host: 0.0.0.0"
echo "Port: 8000"
echo "Local: http://127.0.0.1:8000"
echo "LAN: http://${LAN_IP}:8000"
echo "Docs: http://${LAN_IP}:8000/docs"
echo "Health: http://${LAN_IP}:8000/health"
echo "Readiness: http://${LAN_IP}:8000/ready"
echo "WebSocket Signaling: ws://${LAN_IP}:8000/ws/call"
echo "Protection API: http://${LAN_IP}:8000/api/protection/sessions"
echo "Database: SQLite (initialized)"
echo "Two-Phone Mode: ACTIVE"
echo "Role:"
echo "  Phone A: Raksha Video + RakshaCall"
echo "  Phone B: Raksha Video"
echo "Connected Phones:"
echo "  Phone A: WAITING FOR CONNECTION"
echo "  Phone B: WAITING FOR CONNECTION"
echo "=================================================="
echo ""
echo "Starting uvicorn on 0.0.0.0:8000... Press Ctrl+C to stop."
.venv/bin/python -m uvicorn app.main:app --app-dir server --host 0.0.0.0 --port 8000
