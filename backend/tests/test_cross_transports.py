"""
Integration test suite for RakshaCall Cross-Transport Semantic Parity.
Compares the identical multi-turn transcript sequences through:
- REST API (POST /api/sessions/{sid}/analyze)
- WebSocket (/api/ws/sessions/{sid})
- gRPC Streaming (ProtectionService.StreamProtection)

Verifies 100% semantic equivalence across:
1. scam probability
2. top tactic
3. scam stage
4. manipulation velocity
5. risk score
6. risk level
7. Safety Brake trigger
"""

import pytest
import grpc
from fastapi.testclient import TestClient

from app.main import app
from app.grpc_gen import pb2, pb2_grpc
from app.grpc_service import serve_grpc
from app.grpc_client import ProtectionServiceClient


@pytest.mark.asyncio
async def test_cross_transport_semantic_parity():
    """Verify identical multi-turn scenario yields semantically equivalent decisions across REST, WebSocket, and gRPC."""
    grpc_port = 50062
    grpc_server = await serve_grpc(port=grpc_port)
    grpc_client = ProtectionServiceClient(target=f"localhost:{grpc_port}")
    await grpc_client.connect()

    http_client = TestClient(app)

    # 1. Setup authenticated user
    http_client.post("/api/auth/register", json={
        "email": "transport_audit@rakshacall.safe",
        "name": "Transport Auditor",
        "password": "AuditPassword123!"
    })
    login_resp = http_client.post("/api/auth/login", json={
        "email": "transport_audit@rakshacall.safe",
        "password": "AuditPassword123!"
    })
    token = login_resp.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 2. Initialize separate sessions for each transport
    rest_sid = http_client.post("/api/sessions", json={"source": "REST_AUDIT"}, headers=headers).json()["id"]
    ws_sid = http_client.post("/api/sessions", json={"source": "WS_AUDIT"}, headers=headers).json()["id"]
    grpc_sid = "test-grpc-transport-parity-001"

    # Canonical 5-turn demo sequence
    transcript_sequence = [
        "I am calling from the cyber crime department.",
        "Your Aadhaar is linked to an illegal transaction.",
        "Do not tell your family.",
        "Transfer the money immediately.",
        "Send the OTP."
    ]

    try:
        # 3. Stream through gRPC
        grpc_updates = await grpc_client.stream_utterances(
            session_id=grpc_sid,
            utterances=transcript_sequence,
            language_code="en-IN"
        )
        assert len(grpc_updates) == len(transcript_sequence)

        # 4. Stream through WebSocket
        ws_results = []
        with http_client.websocket_connect(f"/api/ws/sessions/{ws_sid}") as ws:
            init_msg = ws.receive_json()
            assert init_msg.get("type") == "ai_status"

            for text in transcript_sequence:
                ws.send_json({"type": "text", "text": text})
                msg = ws.receive_json()
                assert msg.get("type") == "risk_update"
                ws_results.append(msg)

        assert len(ws_results) == len(transcript_sequence)

        # 5. Process through REST
        rest_results = []
        for text in transcript_sequence:
            resp = http_client.post(
                f"/api/sessions/{rest_sid}/analyze",
                json={"transcript": text},
                headers=headers
            )
            assert resp.status_code == 200
            rest_results.append(resp.json())

        assert len(rest_results) == len(transcript_sequence)

        # 6. Verify cross-transport semantic equivalence turn by turn
        for turn_idx in range(len(transcript_sequence)):
            rest = rest_results[turn_idx]
            ws = ws_results[turn_idx]
            grpc_u = grpc_updates[turn_idx]

            # gRPC protocol enum unpacking
            grpc_stage = pb2.ScamStage.Name(grpc_u.scam_stage).replace("STAGE_", "")
            grpc_vel = pb2.VelocityLevel.Name(grpc_u.manipulation_velocity).replace("VELOCITY_", "")
            grpc_level = pb2.RiskLevel.Name(grpc_u.risk_level).replace("RISK_LEVEL_", "")
            grpc_score = grpc_u.risk_score
            grpc_brake = grpc_u.safety_brake_triggered

            # Tactics extraction
            rest_top_tactic = max(rest["tactic_probabilities"], key=rest["tactic_probabilities"].get) if rest.get("tactic_probabilities") else "NONE"
            ws_top_tactic = max(ws["tactic_probabilities"], key=ws["tactic_probabilities"].get) if ws.get("tactic_probabilities") else "NONE"
            grpc_top_tactic = max(grpc_u.detected_tactics, key=lambda t: t.probability).tactic_id if grpc_u.detected_tactics else "NONE"

            # Check 1: Risk Score Parity
            assert rest["risk_score"] == ws["risk_score"] == grpc_score, (
                f"Turn {turn_idx+1} risk score mismatch: REST={rest['risk_score']} WS={ws['risk_score']} gRPC={grpc_score}"
            )

            # Check 2: Risk Level Parity
            assert rest["risk_level"] == ws["risk_level"] == grpc_level, (
                f"Turn {turn_idx+1} risk level mismatch: REST={rest['risk_level']} WS={ws['risk_level']} gRPC={grpc_level}"
            )

            # Check 3: Scam Stage Parity
            assert rest["stage"] == ws["stage"] == grpc_stage, (
                f"Turn {turn_idx+1} stage mismatch: REST={rest['stage']} WS={ws['stage']} gRPC={grpc_stage}"
            )

            # Check 4: Manipulation Velocity Parity
            assert rest["velocity_level"] == ws["velocity_level"] == grpc_vel, (
                f"Turn {turn_idx+1} velocity mismatch: REST={rest['velocity_level']} WS={ws['velocity_level']} gRPC={grpc_vel}"
            )

            # Check 5: Safety Brake Trigger Parity
            assert rest["safety_brake_triggered"] == ws["safety_brake_triggered"] == grpc_brake, (
                f"Turn {turn_idx+1} brake mismatch: REST={rest['safety_brake_triggered']} WS={ws['safety_brake_triggered']} gRPC={grpc_brake}"
            )

            # Check 6: Top Tactic Parity
            assert rest_top_tactic == ws_top_tactic == grpc_top_tactic, (
                f"Turn {turn_idx+1} top tactic mismatch: REST={rest_top_tactic} WS={ws_top_tactic} gRPC={grpc_top_tactic}"
            )

            # Check 7: Scam Probability Parity (REST vs WS)
            assert abs(rest["scam_probability"] - ws["scam_probability"]) < 1e-4, (
                f"Turn {turn_idx+1} scam probability mismatch: REST={rest['scam_probability']} WS={ws['scam_probability']}"
            )

    finally:
        await grpc_client.close()
        await grpc_server.stop(grace=0.5)


@pytest.mark.asyncio
async def test_cross_transport_hard_negative_parity():
    """Verify negative control / protective banking advisory produces consistent zero false alarms across transports."""
    grpc_port = 50063
    grpc_server = await serve_grpc(port=grpc_port)
    grpc_client = ProtectionServiceClient(target=f"localhost:{grpc_port}")
    await grpc_client.connect()

    http_client = TestClient(app)

    # Auth
    login_resp = http_client.post("/api/auth/login", json={
        "email": "transport_audit@rakshacall.safe",
        "password": "AuditPassword123!"
    })
    token = login_resp.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    rest_sid = http_client.post("/api/sessions", json={"source": "REST_AUDIT_NEG"}, headers=headers).json()["id"]
    ws_sid = http_client.post("/api/sessions", json={"source": "WS_AUDIT_NEG"}, headers=headers).json()["id"]
    grpc_sid = "test-grpc-transport-neg-002"

    negative_control = [
        "Please remember, bank officials will never ask for your confidential password or OTP.",
        "If anyone calls asking for payment or PIN, please hang up and visit the nearest branch."
    ]

    try:
        # gRPC
        grpc_updates = await grpc_client.stream_utterances(
            session_id=grpc_sid,
            utterances=negative_control,
            language_code="en-IN"
        )

        # WS
        ws_results = []
        with http_client.websocket_connect(f"/api/ws/sessions/{ws_sid}") as ws:
            ws.receive_json()
            for text in negative_control:
                ws.send_json({"type": "text", "text": text})
                ws_results.append(ws.receive_json())

        # REST
        rest_results = []
        for text in negative_control:
            resp = http_client.post(f"/api/sessions/{rest_sid}/analyze", json={"transcript": text}, headers=headers)
            rest_results.append(resp.json())

        # Verify cross-transport semantic parity across all turns
        for turn_idx in range(len(negative_control)):
            rest = rest_results[turn_idx]
            ws = ws_results[turn_idx]
            grpc_u = grpc_updates[turn_idx]

            grpc_level = pb2.RiskLevel.Name(grpc_u.risk_level).replace("RISK_LEVEL_", "")
            grpc_stage = pb2.ScamStage.Name(grpc_u.scam_stage).replace("STAGE_", "")
            grpc_vel = pb2.VelocityLevel.Name(grpc_u.manipulation_velocity).replace("VELOCITY_", "")
            grpc_brake = grpc_u.safety_brake_triggered

            # Cross-transport equivalence
            assert rest["risk_score"] == ws["risk_score"] == grpc_u.risk_score
            assert rest["risk_level"] == ws["risk_level"] == grpc_level
            assert rest["stage"] == ws["stage"] == grpc_stage
            assert rest["velocity_level"] == ws["velocity_level"] == grpc_vel
            assert rest["safety_brake_triggered"] == ws["safety_brake_triggered"] == grpc_brake

        # Turn 0: verify explicit negation suppression on protective bank advice
        assert rest_results[0]["risk_level"] == "LOW"
        assert rest_results[0]["risk_score"] == 0
        assert rest_results[0]["safety_brake_triggered"] is False

    finally:
        await grpc_client.close()
        await grpc_server.stop(grace=0.5)
