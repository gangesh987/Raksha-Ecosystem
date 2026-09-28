# Raksha Ecosystem — PowerShell Demo Backend Launcher
Set-Location $PSScriptRoot

Write-Host "[1/3] Checking Python virtual environment..." -ForegroundColor Cyan
if (-not (Test-Path ".venv\Scripts\python.exe")) {
    Write-Host "Creating .venv..." -ForegroundColor Yellow
    python -m venv .venv
    Write-Host "Installing dependencies..." -ForegroundColor Yellow
    & ".\.venv\Scripts\python.exe" -m pip install -r server\requirements.txt
    & ".\.venv\Scripts\python.exe" -m pip install requests httpx
}

Write-Host "[2/3] Configuring environment..." -ForegroundColor Cyan
$env:RAKSHA_API_TOKEN = "demo-token-raksha-video"
$env:RAKSHA_DB_PATH = "./data/raksha.db"
$env:RAKSHA_VERSION = "1.0.0"
$env:RAKSHA_RATE_LIMIT = "300"
$env:WEBRTC_STUN_URL = "stun:stun.l.google.com:19302"
$env:ALLOW_PROTECTION_TEST_EVENTS = "true"

# Detect IP address
$lanIp = (Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias "Wi-Fi*" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty IPAddress -First 1)
if (-not $lanIp) {
    $lanIp = "172.17.35.95"
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "RAKSHA BACKEND — DEMO READY" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host "Host: 0.0.0.0"
Write-Host "Port: 8000"
Write-Host "Local: http://127.0.0.1:8000"
Write-Host "LAN: http://${lanIp}:8000"
Write-Host "Docs: http://${lanIp}:8000/docs"
Write-Host "Health: http://${lanIp}:8000/health"
Write-Host "Readiness: http://${lanIp}:8000/ready"
Write-Host "WebSocket Signaling: ws://${lanIp}:8000/ws/call"
Write-Host "Protection API: http://${lanIp}:8000/api/protection/sessions"
Write-Host "Database: SQLite (initialized)"
Write-Host "Two-Phone Mode: ACTIVE"
Write-Host "Role:"
Write-Host "  Phone A: Raksha Video + RakshaCall"
Write-Host "  Phone B: Raksha Video"
Write-Host "Connected Phones:"
Write-Host "  Phone A: WAITING FOR CONNECTION"
Write-Host "  Phone B: WAITING FOR CONNECTION"
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Starting uvicorn on 0.0.0.0:8000... Press Ctrl+C to stop." -ForegroundColor Cyan

& ".\.venv\Scripts\python.exe" -m uvicorn app.main:app --app-dir server --host 0.0.0.0 --port 8000
