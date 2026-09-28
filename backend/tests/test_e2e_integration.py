"""
RakshaCall End-to-End Automated Integration Test Suite.
Validates the complete lifecycle:
User -> Session -> Conversation turns -> AI analysis -> Risk calculation ->
Safety Brake -> Verification Coach -> Evidence ledger -> Trusted contact alert ->
Persistence and schema across SQLite and PostgreSQL configurations.
"""

import hashlib
import json
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, event
from sqlalchemy.orm import sessionmaker
from sqlalchemy.dialects import postgresql, sqlite
from sqlalchemy.schema import CreateTable
from sqlalchemy.exc import IntegrityError

from app.main import app
from app.db import Base, init_db, SessionLocal
from app.models import User, Session, RiskEvent, TrustedContact, EvidenceReport, AuditEvent


@pytest.fixture(autouse=True)
def ensure_db():
    init_db()


def test_e2e_complete_protection_lifecycle():
    """
    Complete end-to-end integration test of the full protection flow:
    1. Register user
    2. Login user
    3. Add consent-enabled trusted contact
    4. Start protection session
    5. Multi-turn dialogue analysis
    6. Verify risk escalation & Safety Brake activation
    7. Start verification coach
    8. Trigger trusted contact alert (with honest status)
    9. Generate SHA-256 tamper-evident evidence report
    10. Verify timeline, session risk, and audit trail
    """
    client = TestClient(app)

    # 1. Register User
    user_email = "e2e_citizen@rakshacall.safe"
    user_pw = "CitizenStrongPass123!"
    reg_resp = client.post("/api/auth/register", json={
        "email": user_email,
        "name": "E2E Citizen",
        "password": user_pw
    })
    assert reg_resp.status_code in (200, 409)

    # 2. Login
    login_resp = client.post("/api/auth/login", json={
        "email": user_email,
        "password": user_pw
    })
    assert login_resp.status_code == 200
    token = login_resp.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 3. Add Consent-Enabled Trusted Contact
    contact_resp = client.post("/api/contacts", json={
        "name": "Sister / Guardian",
        "phone": "+919876543210",
        "consent_enabled": True
    }, headers=headers)
    assert contact_resp.status_code == 200
    contact_id = contact_resp.json()["id"]

    # Verify contacts list
    contacts_list = client.get("/api/contacts", headers=headers).json()
    assert any(c["id"] == contact_id for c in contacts_list)

    # 4. Start Protection Session
    sess_resp = client.post("/api/sessions", json={"source": "E2E_INCOMING_CALL"}, headers=headers)
    assert sess_resp.status_code == 200
    session_data = sess_resp.json()
    sid = session_data["id"]
    assert session_data["risk_level"] == "LOW"
    assert session_data["status"] == "active"

    # 5. Multi-Turn Scam Sequence (Digital Arrest extortion scenario)
    conversation_script = [
        "I am calling from the cyber crime department.",
        "Your Aadhaar is linked to an illegal transaction.",
        "Do not tell your family.",
        "Transfer the money immediately.",
        "Send the OTP."
    ]

    analysis_results = []
    for turn_idx, utterance in enumerate(conversation_script, start=1):
        resp = client.post(
            f"/api/sessions/{sid}/analyze",
            json={"transcript": utterance, "visual_score": 0.15, "liveness_score": 0.85},
            headers=headers
        )
        assert resp.status_code == 200
        analysis_results.append(resp.json())

    # 6. Verify Risk Escalation & Safety Brake Trigger
    final_turn = analysis_results[-1]
    peak_risk = max(r["risk_score"] for r in analysis_results)
    assert peak_risk >= 70, f"Expected peak risk >= 70, got {peak_risk}"

    # Verify Safety Brake triggered during the escalation
    brake_triggers = [r["safety_brake_triggered"] for r in analysis_results]
    assert True in brake_triggers, "Safety Brake failed to trigger during coercive payment turns"

    # Verify intervention guidance steps present when brake triggers
    active_brake_turns = [r for r in analysis_results if r["safety_brake_triggered"]]
    assert len(active_brake_turns[0]["intervention"]["verification_steps"]) > 0

    # 7. Start Verification Coach
    coach_resp = client.post(f"/api/sessions/{sid}/verification/start", headers=headers)
    assert coach_resp.status_code == 200
    coach_data = coach_resp.json()
    assert coach_data["ok"] is True
    assert len(coach_data["steps"]) >= 4

    # 8. Trigger Trusted Contact Notification (verify honest unconfigured handling)
    alert_resp = client.post(f"/api/sessions/{sid}/trusted-alert", headers=headers)
    assert alert_resp.status_code == 200
    alert_data = alert_resp.json()
    # In dev without TWILIO keys, must honestly report provider_not_configured
    assert alert_data["ok"] is False
    assert alert_data["status"] == "provider_not_configured"
    assert alert_data["destination"] == "+919876543210"

    # 9. Generate Evidence Ledger Report
    ev_create_resp = client.post(f"/api/sessions/{sid}/evidence", headers=headers)
    assert ev_create_resp.status_code == 200
    evidence_payload = ev_create_resp.json()["report"]

    assert evidence_payload["session_id"] == sid
    assert evidence_payload["integrity"] == "VALID"
    assert len(evidence_payload["events"]) == len(conversation_script)

    # Verify genesis hash rule: first event must point to 64 zeros
    first_event = evidence_payload["events"][0]
    assert first_event["previous_hash"] == "0" * 64

    # Mathematically verify SHA-256 hash chaining across all events
    expected_prev = "0" * 64
    for evt in evidence_payload["events"]:
        assert evt["previous_hash"] == expected_prev
        body = {
            "timestamp": evt["timestamp"],
            "transcript": evt["transcript"],
            "risk_level": evt["risk_level"],
            "score": evt["score"],
            "reasons": evt["reasons"],
        }
        material = expected_prev + ":" + json.dumps(body, sort_keys=True, separators=(",", ":"))
        expected_hash = hashlib.sha256(material.encode()).hexdigest()
        assert evt["hash"] == expected_hash, "Hash chain link broken"
        expected_prev = expected_hash

    # Fetch evidence report via GET
    ev_get_resp = client.get(f"/api/sessions/{sid}/evidence", headers=headers)
    assert ev_get_resp.status_code == 200
    assert ev_get_resp.json()["report"]["head_hash"] == expected_prev

    # 10. Session Timeline & Stats
    timeline_resp = client.get(f"/api/sessions/{sid}/timeline", headers=headers)
    assert timeline_resp.status_code == 200
    assert len(timeline_resp.json()["events"]) == len(conversation_script)

    risk_resp = client.get(f"/api/sessions/{sid}/risk", headers=headers)
    assert risk_resp.status_code == 200
    assert risk_resp.json()["event_count"] == len(conversation_script)

    stats_resp = client.get("/api/stats", headers=headers)
    assert stats_resp.status_code == 200
    stats = stats_resp.json()
    assert stats["sessions"] >= 1
    assert stats["events"] >= 5
    assert stats["contacts"] >= 1
    assert stats["reports"] >= 1


def test_database_persistence_and_schema_cross_engine():
    """
    Test database lifecycle, transactions, foreign keys, and DDL compatibility
    across SQLite and PostgreSQL engines.
    """
    # 1. Verify all 6 tables compile valid DDL for PostgreSQL
    for table in Base.metadata.sorted_tables:
        pg_ddl = str(CreateTable(table).compile(dialect=postgresql.dialect()))
        assert len(pg_ddl) > 50, f"PostgreSQL DDL generation failed for {table.name}"
        assert f"CREATE TABLE {table.name}" in pg_ddl or f'CREATE TABLE "{table.name}"' in pg_ddl

    # 2. Verify all 6 tables compile valid DDL for SQLite
    for table in Base.metadata.sorted_tables:
        sqlite_ddl = str(CreateTable(table).compile(dialect=sqlite.dialect()))
        assert len(sqlite_ddl) > 50, f"SQLite DDL generation failed for {table.name}"
        assert f"CREATE TABLE {table.name}" in sqlite_ddl or f'CREATE TABLE "{table.name}"' in sqlite_ddl

    # 3. Test clean in-memory SQLite database with foreign keys enabled
    mem_engine = create_engine("sqlite:///:memory:")
    
    @event.listens_for(mem_engine, "connect")
    def set_sqlite_pragma(dbapi_connection, connection_record):
        cursor = dbapi_connection.cursor()
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.close()

    Base.metadata.create_all(mem_engine)
    MemSession = sessionmaker(bind=mem_engine)

    with MemSession() as db:
        # A. Foreign Key enforcement test: invalid session_id must fail
        with pytest.raises(IntegrityError):
            bad_event = RiskEvent(session_id=999999, transcript="orphan")
            db.add(bad_event)
            db.commit()
        db.rollback()

        # B. Clean insert with valid foreign keys
        u = User(email="test_user@clean.db", password_hash="hashed", name="Test User")
        db.add(u)
        db.commit()
        db.refresh(u)

        s = Session(user_id=u.id, source="TEST")
        db.add(s)
        db.commit()
        db.refresh(s)

        e = RiskEvent(session_id=s.id, transcript="valid event")
        db.add(e)
        db.commit()

        # C. Cascade delete: deleting session should delete associated risk events
        db.delete(s)
        db.commit()

        orphan_check = db.query(RiskEvent).filter_by(session_id=s.id).all()
        assert len(orphan_check) == 0, "Cascade delete failed on session delete"
