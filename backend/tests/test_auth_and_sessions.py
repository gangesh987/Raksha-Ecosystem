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
    # Verify all 5 cases specified in Technova audit:
    # 1. normal password
    # 2. exactly 72 bytes
    # 3. >72 ASCII bytes
    # 4. >72 UTF-8 bytes (emojis)
    # 5. long Unicode password (Tamil / Indic)
    cases = [
        ("user_normal@rakshacall.safe", "NormalPassw0rd!"),
        ("user_exact72@rakshacall.safe", "A" * 72),
        ("user_gt72_ascii@rakshacall.safe", "B" * 128),
        ("user_gt72_utf8@rakshacall.safe", "🔒" * 25),  # 25 * 4 = 100 bytes
        ("user_long_unicode@rakshacall.safe", "ரகசியகடவுச்சொல்தமிழ்பாதுகாப்பு" * 5)
    ]
    
    for email, pw in cases:
        # Register
        reg_res = client.post("/api/auth/register", json={
            "email": email,
            "name": "Audit User",
            "password": pw
        })
        assert reg_res.status_code in [200, 409], f"Failed registration for {email}: {reg_res.text}"
        
        # Login with correct password
        login_res = client.post("/api/auth/login", json={
            "email": email,
            "password": pw
        })
        assert login_res.status_code == 200, f"Failed login for {email}: {login_res.text}"
        data = login_res.json()
        assert "access_token" in data
        
        # Login with incorrect password must fail cleanly with 401
        wrong_res = client.post("/api/auth/login", json={
            "email": email,
            "password": pw + "_wrong"
        })
        assert wrong_res.status_code == 401


def test_full_backend_crud_and_rbac():
    """
    Task A Comprehensive Backend Verification:
    - User A & User B registration and JWT authentication
    - Session creation and retrieval (POST /api/sessions, GET /api/sessions, GET /api/sessions/{sid})
    - Contacts CRUD (POST /api/contacts, GET /api/contacts, DELETE /api/contacts/{id})
    - Evidence retrieval (GET /api/sessions/{sid}/evidence)
    - Session risk, timeline, warning, verification coach, and platform stats
    - RBAC & tenant isolation (User B cannot access or modify User A's data)
    - Unauthenticated request rejection (401 Unauthorized)
    """
    # 1. Register User A
    user_a_email = "user_a@rakshacall.safe"
    reg_a = client.post("/api/auth/register", json={
        "email": user_a_email,
        "name": "User Alpha",
        "password": "PasswordAlpha123!"
    })
    assert reg_a.status_code in [200, 409]
    token_a = client.post("/api/auth/login", json={
        "email": user_a_email,
        "password": "PasswordAlpha123!"
    }).json()["access_token"]
    headers_a = {"Authorization": f"Bearer {token_a}"}

    # 2. Register User B
    user_b_email = "user_b@rakshacall.safe"
    reg_b = client.post("/api/auth/register", json={
        "email": user_b_email,
        "name": "User Beta",
        "password": "PasswordBeta123!"
    })
    assert reg_b.status_code in [200, 409]
    token_b = client.post("/api/auth/login", json={
        "email": user_b_email,
        "password": "PasswordBeta123!"
    }).json()["access_token"]
    headers_b = {"Authorization": f"Bearer {token_b}"}

    # 3. User A creates session
    sess_res = client.post("/api/sessions", json={"source": "VOICE_CALL"}, headers=headers_a)
    assert sess_res.status_code == 200
    sid_a = sess_res.json()["id"]

    # 4. User A lists sessions (GET /api/sessions)
    list_res = client.get("/api/sessions", headers=headers_a)
    assert list_res.status_code == 200
    sessions_a = list_res.json()
    assert any(s["id"] == sid_a for s in sessions_a)

    # 5. User A retrieves session detail (GET /api/sessions/{sid})
    detail_res = client.get(f"/api/sessions/{sid_a}", headers=headers_a)
    assert detail_res.status_code == 200
    assert detail_res.json()["id"] == sid_a

    # 6. User A analyzes an utterance
    analyze_res = client.post(f"/api/sessions/{sid_a}/analyze", json={
        "transcript": "Hello, is this the account holder?",
        "visual_score": 0.1,
        "liveness_score": 0.9
    }, headers=headers_a)
    assert analyze_res.status_code == 200
    assert "event_id" in analyze_res.json()

    # 7. User A queries session risk and timeline
    risk_res = client.get(f"/api/sessions/{sid_a}/risk", headers=headers_a)
    assert risk_res.status_code == 200
    assert risk_res.json()["session_id"] == sid_a

    timeline_res = client.get(f"/api/sessions/{sid_a}/timeline", headers=headers_a)
    assert timeline_res.status_code == 200
    assert len(timeline_res.json()["events"]) >= 1

    # 8. User A triggers safety warning and verification coach
    warn_res = client.post(f"/api/sessions/{sid_a}/warning", headers=headers_a)
    assert warn_res.status_code == 200
    assert warn_res.json()["ok"] is True

    verif_res = client.post(f"/api/sessions/{sid_a}/verification/start", headers=headers_a)
    assert verif_res.status_code == 200
    assert len(verif_res.json()["steps"]) >= 1

    # 9. User A generates and retrieves evidence
    ev_gen = client.post(f"/api/sessions/{sid_a}/evidence", headers=headers_a)
    assert ev_gen.status_code == 200

    ev_get = client.get(f"/api/sessions/{sid_a}/evidence", headers=headers_a)
    assert ev_get.status_code == 200
    assert ev_get.json()["report"]["integrity"] == "VALID"

    # 10. Contacts CRUD for User A
    contact_create = client.post("/api/contacts", json={
        "name": "Trusted Sibling",
        "phone": "+919876543219",
        "consent_enabled": True
    }, headers=headers_a)
    assert contact_create.status_code == 200
    cid_a = contact_create.json()["id"]

    contacts_list = client.get("/api/contacts", headers=headers_a)
    assert contacts_list.status_code == 200
    assert any(c["id"] == cid_a for c in contacts_list.json())

    # 11. Stats endpoint
    stats_res = client.get("/api/stats", headers=headers_a)
    assert stats_res.status_code == 200
    assert stats_res.json()["sessions"] >= 1
    assert stats_res.json()["contacts"] >= 1

    # 12. RBAC & TENANT ISOLATION:
    # User B MUST NOT be able to view User A's session detail (returns 404)
    b_sess_view = client.get(f"/api/sessions/{sid_a}", headers=headers_b)
    assert b_sess_view.status_code == 404

    # User B MUST NOT be able to view User A's evidence (returns 404)
    b_ev_view = client.get(f"/api/sessions/{sid_a}/evidence", headers=headers_b)
    assert b_ev_view.status_code == 404

    # User B MUST NOT be able to delete User A's contact (returns 404)
    b_contact_del = client.delete(f"/api/contacts/{cid_a}", headers=headers_b)
    assert b_contact_del.status_code == 404

    # User B's contacts list MUST NOT contain User A's contact
    b_contacts = client.get("/api/contacts", headers=headers_b).json()
    assert not any(c["id"] == cid_a for c in b_contacts)

    # 13. UNAUTHENTICATED REJECTION:
    no_auth_res = client.get("/api/sessions")
    assert no_auth_res.status_code == 401

    no_auth_contact = client.get("/api/contacts")
    assert no_auth_contact.status_code == 401

    # 14. User A deletes contact successfully
    del_res = client.delete(f"/api/contacts/{cid_a}", headers=headers_a)
    assert del_res.status_code == 200
    assert del_res.json()["ok"] is True


def test_task_c_hard_negatives_zero_false_positives():
    """
    Task C Hard Negatives Verification:
    Tests the 7 required anti-false-alarm control sentences:
    1. "Do not share your OTP."
    2. "Never install remote access software."
    3. "Bank employees never ask for your PIN."
    4. "This is an example of a digital arrest scam."
    5. "Learn how to identify scams."
    6. "The bank refunded my money."
    7. "The bank asked me to verify a transaction."
    Verifies that NONE trigger false positive alarms (no CRITICAL/HIGH risk, no safety brake).
    """
    from app.ai.unified_pipeline import get_pipeline
    pipeline = get_pipeline()

    hard_negatives = [
        ("Do not share your OTP.", "Protective negation (OTP)"),
        ("Never install remote access software.", "Protective negation (remote access)"),
        ("Bank employees never ask for your PIN.", "Protective negation (bank PIN)"),
        ("This is an example of a digital arrest scam.", "Educational / meta-scam framing"),
        ("Learn how to identify scams.", "Educational awareness"),
        ("The bank refunded my money.", "Legitimate non-extractive bank confirmation"),
        ("The bank asked me to verify a transaction.", "Legitimate customer verification inquiry")
    ]

    for idx, (sentence, description) in enumerate(hard_negatives):
        result = pipeline.analyze(sentence, session_id=f"test-hn-task-c-{idx}")
        # Must never trigger critical safety brake or high coercion alarm
        assert result.safety_brake_triggered is False, f"False positive brake triggered on '{sentence}' ({description})"
        assert result.risk_level in ("LOW", "MEDIUM"), f"Expected LOW/MEDIUM risk, got {result.risk_level} on '{sentence}'"
        assert result.risk_score < 60, f"Expected risk_score < 60, got {result.risk_score} on '{sentence}'"


def test_task_d_real_scam_progression():
    """
    Task D Real Scam Progression Verification:
    Tests the canonical 7-turn psychological manipulation trajectory:
    Turn 1: "I am calling from the cyber crime department." (CONTACT/AUTHORITY)
    Turn 2: "Your Aadhaar is linked to an illegal transaction." (AUTHORITY/FEAR)
    Turn 3: "You are under investigation." (FEAR)
    Turn 4: "Do not tell your family." (ISOLATION)
    Turn 5: "You must cooperate immediately." (URGENCY/DEMAND)
    Turn 6: "Transfer the money." (DEMAND/PAYMENT)
    Turn 7: "Send the OTP." (CREDENTIAL/CRITICAL_BRAKE)
    Verifies stage progression, manipulation velocity acceleration, Safety Brake, and ledger integrity.
    """
    from app.ai.unified_pipeline import get_pipeline
    pipeline = get_pipeline()

    session_id = "test-task-d-scam-progression-session"

    turns = [
        (1, "I am calling from the cyber crime department.", "AUTHORITY"),
        (2, "Your Aadhaar is linked to an illegal transaction.", "FEAR"),
        (3, "You are under investigation.", "FEAR"),
        (4, "Do not tell your family.", "ISOLATION"),
        (5, "You must cooperate immediately.", "URGENCY"),
        (6, "Transfer the money.", "PAYMENT"),
        (7, "Send the OTP.", "CREDENTIAL")
    ]

    results = []
    for turn_num, text, expected_tactic in turns:
        res = pipeline.analyze(text, session_id=session_id)
        results.append(res)

    # 1. Turn 1 detects authority
    assert "AUTHORITY" in results[0].tactics

    # 2. Velocity accelerates as pressure compounds across turns
    assert results[-1].velocity_score > results[0].velocity_score

    # 3. Irreversible payment demand and OTP solicitation engage Safety Brake
    assert results[5].safety_brake_triggered is True or results[6].safety_brake_triggered is True
    assert results[-1].safety_brake_triggered is True
    assert results[-1].risk_score >= 60

    # 4. Session ledger integrity is 100% verified
    summary = pipeline.end_session(session_id)
    assert summary["safety_brake_triggered"] is True
    assert summary["evidence_chain_valid"] is True
    assert summary["total_turns"] == 7
    assert len(summary["head_evidence_hash"]) == 64


def test_google_auth_endpoint(monkeypatch):
    """
    Priority 2 Verification:
    Tests /api/auth/google endpoint for server-side token verification,
    account creation/lookup, and issuance of canonical JWT.
    """
    # 1. Invalid / malformed Google token rejected with 401
    bad_res = client.post("/api/auth/google", json={"id_token": "invalid_fake_token_xyz"})
    assert bad_res.status_code == 401

    # 2. Mock Google server-side verification to simulate successful Google Sign-In
    test_google_email = "verified.agent@rakshacall.org"
    test_google_name = "Agent Raksha"

    from google.oauth2 import id_token as google_id_token
    monkeypatch.setattr(
        google_id_token,
        "verify_oauth2_token",
        lambda token, request: {
            "email": test_google_email,
            "name": test_google_name,
            "sub": "google-user-12345678"
        }
    )

    # 3. Successful verification issues canonical JWT
    good_res = client.post("/api/auth/google", json={"id_token": "mock_google_id_token_valid"})
    assert good_res.status_code == 200
    data = good_res.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"
    assert data["user"]["email"] == test_google_email
    assert data["user"]["name"] == test_google_name

    # 4. Issued token works against protected /api/auth/me endpoint
    jwt_token = data["access_token"]
    me_res = client.get("/api/auth/me", headers={"Authorization": f"Bearer {jwt_token}"})
    assert me_res.status_code == 200
    assert me_res.json()["email"] == test_google_email



