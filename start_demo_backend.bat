@echo off
setlocal enabledelayedexpansion
title Raksha Ecosystem — Demo Backend Server
cd /d "%~dp0"

echo [1/3] Checking Python virtual environment...
if not exist ".venv\Scripts\python.exe" (
    echo Creating .venv...
    python -m venv .venv
    echo Installing dependencies...
    ".venv\Scripts\python.exe" -m pip install -r server\requirements.txt
    ".venv\Scripts\python.exe" -m pip install requests httpx
)

echo [2/3] Initializing persistent storage and configuration...
set RAKSHA_API_TOKEN=demo-token-raksha-video
set RAKSHA_DB_PATH=./data/raksha.db
set RAKSHA_VERSION=1.0.0
set RAKSHA_RATE_LIMIT=300
set WEBRTC_STUN_URL=stun:stun.l.google.com:19302
set ALLOW_PROTECTION_TEST_EVENTS=true

:: Detect IPv4 address on active interface
for /f "tokens=4" %%a in ('route print 0.0.0.0 ^| findstr 0.0.0.0 ^| findstr /v "0.0.0.0.*0.0.0.0.*0.0.0.0"') do (
    set "LAN_IP=%%a"
)
if "!LAN_IP!"=="" set "LAN_IP=172.17.35.95"

echo [3/3] Launching FastAPI WebRTC and Safety Server...
echo.
echo ==================================================
echo RAKSHA BACKEND — DEMO READY
echo ==================================================
echo Host: 0.0.0.0
echo Port: 8000
echo Local: http://127.0.0.1:8000
echo LAN: http://!LAN_IP!:8000
echo Docs: http://!LAN_IP!:8000/docs
echo Health: http://!LAN_IP!:8000/health
echo Readiness: http://!LAN_IP!:8000/ready
echo WebSocket Signaling: ws://!LAN_IP!:8000/ws/call
echo Protection API: http://!LAN_IP!:8000/api/protection/sessions
echo Database: SQLite (initialized)
echo Two-Phone Mode: ACTIVE
echo Role:
echo   Phone A: Raksha Video + RakshaCall
echo   Phone B: Raksha Video
echo Connected Phones:
echo   Phone A: WAITING FOR CONNECTION
echo   Phone B: WAITING FOR CONNECTION
echo ==================================================
echo.
echo Starting uvicorn on 0.0.0.0:8000... Press Ctrl+C to stop.
".venv\Scripts\python.exe" -m uvicorn app.main:app --app-dir server --host 0.0.0.0 --port 8000
pause
