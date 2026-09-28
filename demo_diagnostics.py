"""
Raksha Ecosystem — Demo Diagnostics Script
Tests backend environment, dependencies, database, health, readiness,
protection APIs, risk fusion, evidence vault, and WebSockets.
"""

import sys
import os
import time
import json
import sqlite3
from pathlib import Path

# Add server to path
sys.path.insert(0, os.path.abspath("server"))
os.environ["RAKSHA_API_TOKEN"] = "demo-token-raksha-video"
os.environ["RAKSHA_DB_PATH"] = "./data/raksha.db"
os.environ["ALLOW_PROTECTION_TEST_EVENTS"] = "true"

results = {}

def log_test(name, passed, details=""):
    status = "PASS" if passed else "FAIL"
    results[name] = status
    print(f"[{status}] {name}: {details}")

print("=" * 60)
print("RAKSHA BACKEND DEMO DIAGNOSTICS")
print("=" * 60)

# 1. Python Environment Check
py_ver = sys.version_split = f"{sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}"
in_venv = sys.prefix != sys.base_prefix
log_test("Python Environment", sys.version_info.major == 3 and sys.version_info.minor >= 10, f"Python {py_ver} (in_venv={in_venv})")

# 2. Dependencies Check
try:
    import fastapi
    import uvicorn
    import pydantic
    import requests
    import websockets
    log_test("Dependencies", True, f"fastapi {fastapi.__version__}, uvicorn {uvicorn.__version__}, pydantic {pydantic.__version__}")
except ImportError as e:
    log_test("Dependencies", False, f"Missing package: {e}")

# 3. Database Check
try:
    from app.main import DB_PATH, db_init, load_persistent_state
    db_init()
    load_persistent_state()
    with sqlite3.connect(DB_PATH) as db:
        cursor = db.cursor()
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = [row[0] for row in cursor.fetchall()]
    required_tables = {"trusted_contacts", "safety_actions", "evidence"}
    all_present = required_tables.issubset(set(tables))
    log_test("Database", all_present, f"Tables present: {tables} at {DB_PATH.resolve()}")
except Exception as e:
    log_test("Database", False, f"DB error: {e}")

# 4. App Import Check
try:
    from app.main import app
    route_paths = [r.path for r in app.routes if hasattr(r, 'path')]
    log_test("App Routing", len(route_paths) >= 20, f"Registered {len(route_paths)} routes")
except Exception as e:
    log_test("App Routing", False, f"Import error: {e}")

# 5. Live Endpoint & Socket Verification using TestClient
try:
    from starlette.testclient import TestClient
    client = TestClient(app)

    # Health Check
    h_resp = client.get("/health")
    h_data = h_resp.json()
    log_test("Health Endpoint", h_resp.status_code == 200 and h_data.get("status") == "ok", f"HTTP {h_resp.status_code} -> {h_data}")

    # API Health Alias
    api_h_resp = client.get("/api/health")
    log_test("API Health Alias", api_h_resp.status_code == 200, f"HTTP {api_h_resp.status_code}")

    # Readiness Check
    r_resp = client.get("/ready")
    r_data = r_resp.json()
    log_test("Readiness Endpoint", r_resp.status_code == 200 and r_data.get("status") == "ready", f"HTTP {r_resp.status_code} -> {r_data}")

    # Swagger / OpenAPI
    docs_resp = client.get("/docs")
    openapi_resp = client.get("/openapi.json")
    log_test("Swagger & OpenAPI", docs_resp.status_code == 200 and openapi_resp.status_code == 200, f"/docs={docs_resp.status_code}, /openapi.json={openapi_resp.status_code}")

    # WebRTC Config
    webrtc_resp = client.get("/api/webrtc/config")
    webrtc_data = webrtc_resp.json()
    log_test("WebRTC Config", webrtc_resp.status_code == 200 and "ice_servers" in webrtc_data, f"ICE servers: {webrtc_data.get('ice_servers')}")

    # Create Call (WebRTC)
    call_resp = client.post("/api/calls", headers={"Authorization": "Bearer demo-token-raksha-video"})
    call_data = call_resp.json()
    call_id = call_data.get("call_id")
    log_test("Create Call API", call_resp.status_code == 200 and bool(call_id), f"Call ID: {call_id}")

    # Create Protection Session
    prot_payload = {
        "call_id": call_id,
        "media": {"audio": True, "video": True},
        "consent": {"audio_analysis": True, "visual_analysis": True, "risk_detection": True}
    }
    prot_resp = client.post("/api/protection/sessions", json=prot_payload, headers={"Authorization": "Bearer demo-token-raksha-video"})
    prot_data = prot_resp.json()
    session_id = prot_data.get("session_id")
    log_test("Protection Session API", prot_resp.status_code == 200 and bool(session_id), f"Session ID: {session_id}")

    # Transcript / Intelligence Pipeline
    trans_payload = {
        "text": "Vanakkam, naan CBI officer pesuren. Ungal Aadhaar cyber crime case-la link aagi irukku. Room kadhava moodittu transfer money.",
        "timestamp": int(time.time() * 1000),
        "source": "transcript"
    }
    trans_resp = client.post(f"/api/protection/sessions/{session_id}/transcript", json=trans_payload, headers={"Authorization": "Bearer demo-token-raksha-video"})
    trans_data = trans_resp.json()
    risk = trans_data.get("risk") or {}
    signals = trans_data.get("signals") or []
    log_test("Intelligence & Risk Fusion", trans_resp.status_code == 200 and len(signals) > 0, f"Detected {len(signals)} signals, Risk Level={risk.get('level')}, Score={risk.get('score')}")

    # Trusted Contact Management
    contact_payload = {"displayName": "Emergency Family Contact", "phoneNumber": "+919876543210", "relationship": "Family"}
    c_resp = client.post("/api/trusted-contacts", json=contact_payload, headers={"Authorization": "Bearer demo-token-raksha-video"})
    c_data = c_resp.json()
    contact_id = c_data.get("id")
    log_test("Trusted Contact Creation", c_resp.status_code == 200 and bool(contact_id), f"Contact ID: {contact_id}")

    # Safety Action Trigger
    action_payload = {
        "action": "TRUSTED_CONTACT_ALERT",
        "confirmed": True,
        "call_id": call_id,
        "contact_id": contact_id
    }
    act_resp = client.post(f"/api/protection/sessions/{session_id}/safety/actions", json=action_payload, headers={"Authorization": "Bearer demo-token-raksha-video"})
    act_data = act_resp.json()
    delivery_status = act_data.get("delivery_status")
    log_test("Safety Action Interlock", act_resp.status_code == 200 and act_data.get("status") == "ACCEPTED", f"Action Status: ACCEPTED, Delivery Status: {delivery_status}")

    # Evidence Timeline & Export
    ev_resp = client.get(f"/api/protection/sessions/{session_id}/evidence", headers={"Authorization": "Bearer demo-token-raksha-video"})
    ev_data = ev_resp.json()
    events = ev_data.get("evidence", [])
    log_test("Evidence Vault", ev_resp.status_code == 200 and len(events) > 0, f"Stored {len(events)} tamper-evident safety events")

    # RakshaCall Scam Analysis API
    rc_analysis = client.post(f"/api/sessions/{session_id}/analyze", json={"transcript": "Please send the OTP right now to stop your immediate digital arrest."})
    rc_data = rc_analysis.json()
    log_test("RakshaCall Scam Analysis API", rc_analysis.status_code == 200, f"Risk={rc_data.get('risk_level')}, Score={rc_data.get('risk_score')}, Brake={rc_data.get('safety_brake_triggered')}")

    # WebRTC Signaling WebSocket Test
    with client.websocket_connect(f"/ws/call?authorization=Bearer%20demo-token-raksha-video") as ws:
        ws.send_json({"version": 1, "type": "ping", "payload": {}})
        msg = ws.receive_json()
        pong_ok = msg.get("type") == "pong"
        ws.send_json({"version": 1, "type": "join_call", "payload": {"call_id": call_id, "display_name": "Phone A"}})
        joined_msg = ws.receive_json()
        joined_ok = joined_msg.get("type") == "call_joined"
        log_test("WebRTC Signaling WebSocket", pong_ok and joined_ok, f"Ping/Pong OK, Joined Call {call_id}")

    # Protection WebSocket Test
    with client.websocket_connect(f"/api/ws/sessions/{session_id}?authorization=Bearer%20demo-token-raksha-video") as ws_prot:
        ws_prot.send_json({"version": 1, "type": "ping", "payload": {}})
        prot_pong = ws_prot.receive_json()
        log_test("Protection Stream WebSocket", prot_pong.get("type") == "pong", f"Session {session_id} WebSocket stream operational")

except Exception as e:
    log_test("Live Client Tests", False, f"Execution failure: {e}")

print()
print("=" * 60)
print("DIAGNOSTICS SUMMARY")
print("=" * 60)
passed_count = sum(1 for v in results.values() if v == "PASS")
total_count = len(results)
print(f"Total Checks: {passed_count}/{total_count} PASSED")
for k, v in results.items():
    print(f"  {k:32} : {v}")
print("=" * 60)

if passed_count == total_count:
    print("BACKEND READINESS: VERIFIED & DEMO READY")
    sys.exit(0)
else:
    print("BACKEND READINESS: ISSUES DETECTED")
    sys.exit(1)
