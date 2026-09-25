"""
Test suite for RakshaCall gRPC Streaming Service.
Verifies bidirectional streaming, multi-turn scam detection, Safety Brake engagement,
and tamper-evident ledger generation over HTTP/2 Protobuf transport.
"""

import asyncio
import pytest
import grpc

from app.grpc_gen import pb2, pb2_grpc
from app.grpc_service import serve_grpc
from app.grpc_client import ProtectionServiceClient
from app.ai.evidence_vault import get_session_ledger


@pytest.mark.asyncio
async def test_grpc_liveness_check():
    """Verify gRPC server responds to unary liveness check."""
    server = await serve_grpc(port=50055)
    client = ProtectionServiceClient(target="localhost:50055")
    await client.connect()

    try:
        resp = await client.check_liveness()
        assert resp.is_ready is True
        assert "grpc.aio" in resp.active_model_mode
        assert "ta-IN" in resp.supported_languages
        assert "en-IN" in resp.supported_languages
    finally:
        await client.close()
        await server.stop(grace=0.5)


@pytest.mark.asyncio
async def test_grpc_bidirectional_digital_arrest_stream():
    """
    Simulate a complete 5-turn digital arrest attack scenario over gRPC streaming:
    Turn 1: CBI Authority claim
    Turn 2: Narcotics allegation & FIR fear
    Turn 3: Isolation & digital custody
    Turn 4: Urgency & do not delay
    Turn 5: Payment demand to RBI verification account (Safety Brake MUST trigger!)
    """
    server = await serve_grpc(port=50056)
    client = ProtectionServiceClient(target="localhost:50056")
    await client.connect()

    attack_script = [
        "I am calling from CBI Cyber Crime Headquarters in Mumbai.",
        "Your Aadhaar card was used in an illegal narcotics parcel and money laundering FIR.",
        "Do not disconnect this call or tell anyone. You are placed under digital arrest in your room.",
        "You must clear your name within 15 minutes immediately.",
        "Transfer 50,000 rupees security deposit to the RBI clearance account right now."
    ]

    session_id = "test-grpc-digital-arrest-001"

    try:
        updates = await client.stream_utterances(
            session_id=session_id,
            utterances=attack_script,
            language_code="en-IN"
        )

        assert len(updates) == len(attack_script)

        # Sequence numbers must be monotonically acknowledged
        for idx, u in enumerate(updates, start=1):
            assert u.ack_sequence_number == idx
            assert u.session_id == session_id

        # Turn 1 should detect Authority
        t1_tactics = [t.tactic_id for t in updates[0].detected_tactics]
        assert "AUTHORITY" in t1_tactics
        assert updates[0].risk_score > 0

        # Final turn must trigger Safety Brake!
        final_update = updates[-1]
        assert final_update.risk_score >= 60
        assert final_update.safety_brake_triggered is True
        assert final_update.intervention.primary_action == pb2.InterventionPlan.ENGAGE_SAFETY_BRAKE
        assert "PAYMENT" in [t.tactic_id for t in final_update.detected_tactics]

        # Verify cryptographic block hash is present
        assert len(final_update.current_block_hash) == 64
        assert final_update.current_block_hash != "0" * 64

        # Verify session termination
        end_resp = await client.end_session(session_id)
        assert end_resp.session_id == session_id
        assert end_resp.peak_risk_score >= 60
        assert end_resp.final_risk_level in ("HIGH", "CRITICAL")
        assert len(end_resp.head_evidence_hash) == 64

        # Verify ledger cryptographic integrity
        ledger = get_session_ledger(session_id)
        is_valid, broken_idx, msg = ledger.verify_integrity()
        assert is_valid is True
        assert broken_idx is None

    finally:
        await client.close()
        await server.stop(grace=0.5)


@pytest.mark.asyncio
async def test_grpc_tanglish_coercion_stream():
    """
    Verify real-time streaming detection on colloquial Tanglish input.
    """
    server = await serve_grpc(port=50057)
    client = ProtectionServiceClient(target="localhost:50057")
    await client.connect()

    tanglish_script = [
        "Mumbai police station-la irunthu pesuren.",
        "Unga Aadhaar card drugs case parcel-la maatirukku.",
        "Call disconnect pannathinga, room door lock pannunga.",
        "Mobile-ku vantha 6 digit OTP ippove sollunga."
    ]

    session_id = "test-grpc-tanglish-002"

    try:
        updates = await client.stream_utterances(
            session_id=session_id,
            utterances=tanglish_script,
            language_code="ta-Latn"
        )

        assert len(updates) == 4
        final = updates[-1]
        
        # Must detect credential pressure in Tanglish
        assert any(t.tactic_id == "CREDENTIAL" for t in final.detected_tactics)
        assert final.safety_brake_triggered is True
        assert "ta" in final.intervention.localized_audio_alert_key

    finally:
        await client.close()
        await server.stop(grace=0.5)
