import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.db import init_db

@pytest.fixture(autouse=True)
def setup_db():
    init_db()

client = TestClient(app)

def test_health():
    res = client.get("/api/health")
    assert res.status_code == 200
    data = res.json()
    assert data["status"] == "ok"
    assert data["service"] == "rakshacall"

def test_auth_and_session_flow():
    # 1. Register a user
    reg_res = client.post("/api/auth/register", json={
        "email": "auditor@rakshacall.safe",
        "name": "Safety Auditor",
        "password": "SecurePassword123!"
    })
    assert reg_res.status_code in [200, 409]
    
    # 2. Login
    login_res = client.post("/api/auth/login", json={
        "email": "auditor@rakshacall.safe",
        "password": "SecurePassword123!"
    })
    assert login_res.status_code == 200
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 3. Get profile
    me_res = client.get("/api/auth/me", headers=headers)
    assert me_res.status_code == 200
    assert me_res.json()["email"] == "auditor@rakshacall.safe"

    # 4. Create protection session
    sess_res = client.post("/api/sessions", json={"source": "VOICE_CALL"}, headers=headers)
    assert sess_res.status_code == 200
    sid = sess_res.json()["id"]

    # 5. Analyze speech transcript with authority and payment pressure
    analyze_res = client.post(f"/api/sessions/{sid}/analyze", json={
        "transcript": "This is CBI Police. You are under investigation for illegal narcotics contraband. Transfer 50000 rupees immediately to avoid arrest.",
        "visual_score": 0.1,
        "liveness_score": 0.1
    }, headers=headers)
    assert analyze_res.status_code == 200
    analysis = analyze_res.json()
    assert analysis["risk_level"] in ["MEDIUM", "HIGH", "CRITICAL"]
    assert analysis["fused_score"] >= 0.3

    # 6. Check honest SMS alert (unconfigured Twilio must not fake delivery)
    contact_res = client.post("/api/contacts", json={
        "name": "Guardian Contact",
        "phone": "+919876543210",
        "consent_enabled": True
    }, headers=headers)
    assert contact_res.status_code == 200

    alert_res = client.post(f"/api/sessions/{sid}/trusted-alert", headers=headers)
    assert alert_res.status_code == 200
    alert_data = alert_res.json()
    # Must report unconfigured status, never fake delivered!
    assert alert_data["ok"] is False
    assert alert_data["status"] == "provider_not_configured"

    # 7. Generate evidence report and verify 64-zero genesis and SHA-256 chain
    ev_res = client.post(f"/api/sessions/{sid}/evidence", headers=headers)
    assert ev_res.status_code == 200
    ev_report = ev_res.json()["report"]
    assert ev_report["integrity"] == "VALID"
    assert len(ev_report["events"]) >= 1
    assert ev_report["events"][0]["previous_hash"] == "0" * 64

def test_password_longer_than_72_bytes_handled_safely():
    long_pw = "SuperSecretAndExtremelyLongPasswordThatExceedsTheStandardBcryptLimitOf72CharactersByFar1234567890!"
    reg_res = client.post("/api/auth/register", json={
        "email": "longpw@rakshacall.safe",
        "name": "Long Password User",
        "password": long_pw
    })
    assert reg_res.status_code in [200, 409]
    login_res = client.post("/api/auth/login", json={
        "email": "longpw@rakshacall.safe",
        "password": long_pw
    })
    assert login_res.status_code == 200
    assert "access_token" in login_res.json()

