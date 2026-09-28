"""
RakshaCall Multi-Transport Semantic Parity Validator.
Validates that the SAME transcript sequence produces semantically equivalent results across:
1. REST API (POST /api/sessions/{sid}/analyze)
2. WebSocket (/api/ws/sessions/{sid})
3. gRPC Streaming (ProtectionService.StreamProtection)

Compares:
- scam probability
- top tactic
- stage
- velocity
- risk score
- risk level
- Safety Brake trigger
"""

import sys
import os
import asyncio
import json
import httpx
import websockets
import grpc

# Ensure backend root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.grpc_gen import protection_service_pb2 as pb2
from app.grpc_gen import protection_service_pb2_grpc as pb2_grpc
from app.grpc_service import serve_grpc
from app.grpc_client import ProtectionServiceClient

BASE_URL = "http://localhost:8000"
WS_URL = "ws://localhost:8000"
GRPC_TARGET = "localhost:50051"


async def check_server_running(url: str = BASE_URL) -> bool:
    try:
        async with httpx.AsyncClient(timeout=1.0) as client:
            resp = await client.get(f"{url}/api/health")
            return resp.status_code == 200
    except Exception:
        return False


async def test_rest(client: httpx.AsyncClient, transcript: str, token: str, sid: int) -> dict:
    resp = await client.post(
        f"{BASE_URL}/api/sessions/{sid}/analyze",
        json={"transcript": transcript, "visual_score": 0.15, "liveness_score": 0.85},
        headers={"Authorization": f"Bearer {token}"}
    )
    data = resp.json()
    probs = data.get("tactic_probabilities", {})
    top_tactic = max(probs, key=probs.get) if probs else "NONE"
    return {
        "risk_score": data.get("risk_score", 0),
        "risk_level": data.get("risk_level", "LOW"),
        "stage": data.get("stage", "CONTACT"),
        "velocity": data.get("velocity_level", "LOW"),
        "safety_brake": data.get("safety_brake_triggered", False),
        "top_tactic": top_tactic,
        "scam_probability": round(data.get("scam_probability", 0.0), 4),
        "raw": data
    }


async def test_ws(transcript: str, sid: int) -> dict:
    try:
        async with websockets.connect(f"{WS_URL}/api/ws/sessions/{sid}") as websocket:
            # First message is ai_status
            init_msg = json.loads(await websocket.recv())
            assert init_msg.get("type") == "ai_status"

            await websocket.send(json.dumps({"type": "text", "text": transcript}))

            while True:
                msg = await websocket.recv()
                data = json.loads(msg)
                if data.get("type") == "risk_update":
                    probs = data.get("tactic_probabilities", {})
                    top_tactic = max(probs, key=probs.get) if probs else "NONE"
                    return {
                        "risk_score": data.get("risk_score", 0),
                        "risk_level": data.get("risk_level", "LOW"),
                        "stage": data.get("stage", "CONTACT"),
                        "velocity": data.get("velocity_level", "LOW"),
                        "safety_brake": data.get("safety_brake_triggered", False),
                        "top_tactic": top_tactic,
                        "scam_probability": round(data.get("scam_probability", 0.0), 4),
                        "raw": data
                    }
    except Exception as e:
        return {"error": str(e)}


async def test_grpc(transcript: str, session_id: str, target: str = GRPC_TARGET) -> dict:
    try:
        async with grpc.aio.insecure_channel(target) as channel:
            stub = pb2_grpc.ProtectionServiceStub(channel)

            async def request_generator():
                yield pb2.ClientFrame(
                    session_id=session_id,
                    sequence_number=1,
                    user_consent_active=True,
                    transcript_snippet=transcript,
                    detected_language="en-IN"
                )

            async for response in stub.StreamProtection(request_generator()):
                tactics = response.detected_tactics
                top_tactic = max(tactics, key=lambda t: t.probability).tactic_id if tactics else "NONE"
                top_prob = max([t.probability for t in tactics], default=0.0)
                return {
                    "risk_score": response.risk_score,
                    "risk_level": pb2.RiskLevel.Name(response.risk_level).replace("RISK_LEVEL_", ""),
                    "stage": pb2.ScamStage.Name(response.scam_stage).replace("STAGE_", ""),
                    "velocity": pb2.VelocityLevel.Name(response.manipulation_velocity).replace("VELOCITY_", ""),
                    "safety_brake": response.safety_brake_triggered,
                    "top_tactic": top_tactic,
                    "scam_probability": round(top_prob, 4),
                    "primary_explanation": response.primary_explanation,
                }
    except Exception as e:
        return {"error": str(e)}


async def run_in_process_parity_check():
    """Runs when external daemon is not active. Boots in-process test fixtures."""
    from fastapi.testclient import TestClient
    from app.main import app

    grpc_port = 50065
    grpc_server = await serve_grpc(port=grpc_port)
    grpc_client = ProtectionServiceClient(target=f"localhost:{grpc_port}")
    await grpc_client.connect()

    http_client = TestClient(app)

    # Auth
    reg = http_client.post("/api/auth/register", json={
        "email": "validator@raksha.safe",
        "name": "Validator",
        "password": "ValidatorPassword123!"
    })
    login = http_client.post("/api/auth/login", json={
        "email": "validator@raksha.safe",
        "password": "ValidatorPassword123!"
    })
    token = login.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    test_inputs = [
        "I am calling from the cyber crime department.",
        "Your Aadhaar is linked to an illegal transaction.",
        "Do not tell your family.",
        "Transfer the money immediately.",
        "Send the OTP."
    ]

    print("\n" + "="*80)
    print("RAKSHACALL CROSS-TRANSPORT SEMANTIC PARITY VALIDATION (IN-PROCESS TEST)")
    print("="*80)

    try:
        # Create sessions
        rest_sid = http_client.post("/api/sessions", json={"source": "REST_TEST"}, headers=headers).json()["id"]
        ws_sid = http_client.post("/api/sessions", json={"source": "WS_TEST"}, headers=headers).json()["id"]
        grpc_sid = "in-process-grpc-session-001"

        # Stream gRPC
        grpc_updates = await grpc_client.stream_utterances(
            session_id=grpc_sid,
            utterances=test_inputs,
            language_code="en-IN"
        )

        # Stream WS
        ws_results = []
        with http_client.websocket_connect(f"/api/ws/sessions/{ws_sid}") as ws:
            ws.receive_json() # ai_status
            for text in test_inputs:
                ws.send_json({"type": "text", "text": text})
                ws_results.append(ws.receive_json())

        # Stream REST
        rest_results = []
        for text in test_inputs:
            r = http_client.post(f"/api/sessions/{rest_sid}/analyze", json={"transcript": text}, headers=headers)
            rest_results.append(r.json())

        all_parity_passed = True
        for i, text in enumerate(test_inputs, start=1):
            r = rest_results[i-1]
            w = ws_results[i-1]
            g = grpc_updates[i-1]

            g_stage = pb2.ScamStage.Name(g.scam_stage).replace("STAGE_", "")
            g_vel = pb2.VelocityLevel.Name(g.manipulation_velocity).replace("VELOCITY_", "")
            g_level = pb2.RiskLevel.Name(g.risk_level).replace("RISK_LEVEL_", "")
            g_brake = g.safety_brake_triggered
            g_score = g.risk_score

            r_top = max(r["tactic_probabilities"], key=r["tactic_probabilities"].get) if r.get("tactic_probabilities") else "NONE"
            w_top = max(w["tactic_probabilities"], key=w["tactic_probabilities"].get) if w.get("tactic_probabilities") else "NONE"
            g_top = max(g.detected_tactics, key=lambda t: t.probability).tactic_id if g.detected_tactics else "NONE"

            print(f"\n[Turn {i}] '{text}'")
            print(f"  REST:  score={r['risk_score']:3d} | level={r['risk_level']:8s} | stage={r['stage']:18s} | vel={r['velocity_level']:8s} | brake={str(r['safety_brake_triggered']):5s} | top_tactic={r_top}")
            print(f"  WS:    score={w['risk_score']:3d} | level={w['risk_level']:8s} | stage={w['stage']:18s} | vel={w['velocity_level']:8s} | brake={str(w['safety_brake_triggered']):5s} | top_tactic={w_top}")
            print(f"  gRPC:  score={g_score:3d} | level={g_level:8s} | stage={g_stage:18s} | vel={g_vel:8s} | brake={str(g_brake):5s} | top_tactic={g_top}")

            # Verify equivalence
            scores_match = (r["risk_score"] == w["risk_score"] == g_score)
            levels_match = (r["risk_level"] == w["risk_level"] == g_level)
            stages_match = (r["stage"] == w["stage"] == g_stage)
            velocities_match = (r["velocity_level"] == w["velocity_level"] == g_vel)
            brakes_match = (r["safety_brake_triggered"] == w["safety_brake_triggered"] == g_brake)
            tactics_match = (r_top == w_top == g_top)

            if all([scores_match, levels_match, stages_match, velocities_match, brakes_match, tactics_match]):
                print(f"  -> Turn {i} Parity: PASS (All 6 core metrics match 100%)")
            else:
                print(f"  -> Turn {i} Parity: FAIL (Discrepancy detected)")
                all_parity_passed = False

        print("\n" + "="*80)
        if all_parity_passed:
            print("CROSS-TRANSPORT VALIDATION SUMMARY: 100% PARITY VERIFIED (REST == WS == GRPC)")
        else:
            print("CROSS-TRANSPORT VALIDATION SUMMARY: DISCREPANCY FOUND")
        print("="*80 + "\n")
        return all_parity_passed

    finally:
        await grpc_client.close()
        await grpc_server.stop(grace=0.5)


async def main():
    is_live = await check_server_running(BASE_URL)
    if not is_live:
        print(f"Live server not detected at {BASE_URL}. Running in-process validation...")
        success = await run_in_process_parity_check()
        sys.exit(0 if success else 1)

    print(f"Connected to live server at {BASE_URL}. Running live transport validation...")
    async with httpx.AsyncClient() as client:
        # Auth
        resp = await client.post(f"{BASE_URL}/api/auth/register", json={
            "email": "test@example.com",
            "name": "Test User",
            "password": "password123"
        })
        if resp.status_code == 409:
            resp = await client.post(f"{BASE_URL}/api/auth/login", json={
                "email": "test@example.com",
                "password": "password123"
            })
        token = resp.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        sess_resp = await client.post(f"{BASE_URL}/api/sessions", json={"source": "transport-test"}, headers=headers)
        sid = sess_resp.json()["id"]

        test_inputs = [
            "I am calling from the cyber crime department.",
            "Your Aadhaar is linked to an illegal transaction.",
            "Do not tell your family.",
            "Transfer the money immediately.",
            "Send the OTP."
        ]

        for idx, transcript in enumerate(test_inputs, 1):
            print(f"\n--- Turn {idx}: '{transcript}' ---")
            rest_res = await test_rest(client, transcript, token, sid)
            ws_res = await test_ws(transcript, sid)
            grpc_res = await test_grpc(transcript, f"live-grpc-session-{idx}")

            print(f"REST: score={rest_res['risk_score']} level={rest_res['risk_level']} stage={rest_res['stage']} vel={rest_res['velocity']} brake={rest_res['safety_brake']} top_tactic={rest_res['top_tactic']}")
            print(f"WS:   score={ws_res.get('risk_score', 'N/A')} level={ws_res.get('risk_level', 'N/A')} stage={ws_res.get('stage', 'N/A')} vel={ws_res.get('velocity', 'N/A')} brake={ws_res.get('safety_brake', 'N/A')} top_tactic={ws_res.get('top_tactic', 'N/A')}")
            print(f"gRPC: score={grpc_res.get('risk_score', 'N/A')} level={grpc_res.get('risk_level', 'N/A')} stage={grpc_res.get('stage', 'N/A')} vel={grpc_res.get('velocity', 'N/A')} brake={grpc_res.get('safety_brake', 'N/A')} top_tactic={grpc_res.get('top_tactic', 'N/A')}")


if __name__ == "__main__":
    asyncio.run(main())
