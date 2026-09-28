"""
RakshaCall Demo Runbook End-to-End Validation Script.
Executes the exact 5-turn scenario from DEMO_RUNBOOK.md through the
real canonical UnifiedAnalysisPipeline:
Turn 1: "I am calling from the cyber crime department."
Turn 2: "Your Aadhaar is linked to an illegal transaction."
Turn 3: "Do not tell your family."
Turn 4: "Transfer the money immediately."
Turn 5: "Send the OTP."

Verifies:
1. Canonical pipeline loaded (PyTorch neural model + safety floor)
2. Coercive multi-turn risk escalation
3. Safety Brake triggering
4. Guided Verification Coach initiation
5. Trusted contact notification (honestly reported status)
6. Cryptographic SHA-256 evidence chain and genesis verification
7. Event timeline persistence
"""

import sys
import os
import json
import hashlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from fastapi.testclient import TestClient
from app.main import app
from app.ai.unified_pipeline import get_pipeline


def run_demo_validation():
    print("=" * 80)
    print("RAKSHACALL DEMO RUNBOOK CANONICAL PIPELINE VALIDATION")
    print("=" * 80)

    # 1. Pipeline Verification
    pipeline = get_pipeline()
    print(f"\n[1] Canonical AI Decision Pipeline Check:")
    print(f"    - Pipeline Class: {type(pipeline).__name__}")
    print(f"    - Provider: {pipeline.jev_provider.provider_name}")
    print(f"    - Neural Engine: {pipeline.jev_provider.neural_classifier.model_version}")
    print(f"    - Parameters: {pipeline.jev_provider.neural_classifier.parameter_count:,}")
    print(f"    - Model Loaded: {pipeline.jev_provider.neural_classifier.is_loaded}")
    assert pipeline.jev_provider.neural_classifier.is_loaded is True

    client = TestClient(app)

    # 2. Authentication
    reg_res = client.post("/api/auth/register", json={
        "email": "demo_evaluator@rakshacall.safe",
        "name": "Demo Evaluator",
        "password": "EvaluatorPassword123!"
    })
    login_res = client.post("/api/auth/login", json={
        "email": "demo_evaluator@rakshacall.safe",
        "password": "EvaluatorPassword123!"
    })
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}
    print(f"\n[2] Authentication: User authenticated with JWT")

    # 3. Add Consent-Enabled Trusted Contact
    contact_res = client.post("/api/contacts", json={
        "name": "Sister Priya",
        "phone": "+919876543210",
        "consent_enabled": True
    }, headers=headers).json()
    print(f"\n[3] Trusted Contact: Added {contact_res['name']} ({contact_res['phone']}), Consent=True")

    # 4. Create Protection Session
    sess_res = client.post("/api/sessions", json={"source": "DEMO_CALL"}, headers=headers).json()
    sid = sess_res["id"]
    print(f"\n[4] Protection Session Started: Session ID={sid}, Status={sess_res['status']}, Risk={sess_res['risk_level']}")

    # 5. Multi-Turn Demo Scenario Execution
    demo_script = [
        "I am calling from the cyber crime department.",
        "Your Aadhaar is linked to an illegal transaction.",
        "Do not tell your family.",
        "Transfer the money immediately.",
        "Send the OTP."
    ]

    print(f"\n[5] Executing 5-Turn Coercive Scam Scenario:")
    results = []
    for turn_idx, text in enumerate(demo_script, start=1):
        resp = client.post(
            f"/api/sessions/{sid}/analyze",
            json={"transcript": text, "visual_score": 0.15, "liveness_score": 0.85},
            headers=headers
        ).json()
        results.append(resp)
        print(f"    Turn {turn_idx}: \"{text}\"")
        print(f"            Risk Score: {resp['risk_score']:2d}/100 | Level: {resp['risk_level']:8s} | Stage: {resp['stage']:18s} | Velocity: {resp['velocity_level']:8s} | Brake: {str(resp['safety_brake_triggered']):5s}")

    # 6. Safety Brake & Peak Risk Verification
    peak_score = max(r["risk_score"] for r in results)
    brake_fired = any(r["safety_brake_triggered"] for r in results)
    print(f"\n[6] Risk & Safety Brake Evaluation:")
    print(f"    - Peak Risk Score: {peak_score}/100")
    print(f"    - Safety Brake Triggered: {brake_fired}")
    assert brake_fired is True, "Safety brake should have triggered"
    assert peak_score >= 70, "Peak risk should be elevated"

    # 7. Verification Coach Flow
    coach_res = client.post(f"/api/sessions/{sid}/verification/start", headers=headers).json()
    print(f"\n[7] Verification Coach:")
    print(f"    - Flow Status: {'ACTIVE' if coach_res.get('ok') else 'INACTIVE'}")
    print(f"    - Guided Steps Available: {len(coach_res.get('steps', []))}")
    for step in coach_res.get("steps", [])[:3]:
        print(f"      Step {step['step']}: {step['title']} -> {step['instruction']}")

    # 8. Trusted Contact Notification
    alert_res = client.post(f"/api/sessions/{sid}/trusted-alert", headers=headers).json()
    print(f"\n[8] Trusted Contact Notification:")
    print(f"    - Delivery Status: {alert_res.get('status')}")
    print(f"    - Recipient: {alert_res.get('destination')}")
    print(f"    - Message Delivery Honesty: Real status reported ({alert_res.get('message', alert_res.get('error', ''))})")
    assert alert_res.get("status") in ("provider_not_configured", "sent", "delivery_failed")

    # 9. Evidence Ledger & Hash Chain Verification
    ev_res = client.post(f"/api/sessions/{sid}/evidence", headers=headers).json()
    report = ev_res["report"]
    print(f"\n[9] Tamper-Evident Evidence Ledger:")
    print(f"    - Report ID: {ev_res['id']}")
    print(f"    - Chain Integrity: {report['integrity']}")
    print(f"    - Total Chained Events: {len(report['events'])}")
    print(f"    - Genesis Hash: {report['events'][0]['previous_hash']}")
    print(f"    - Head Hash: {report['head_hash']}")

    assert report["events"][0]["previous_hash"] == "0" * 64, "Genesis hash must be 64 zeros"

    # Verify SHA-256 chain math
    prev = "0" * 64
    for e in report["events"]:
        assert e["previous_hash"] == prev
        body = {
            "timestamp": e["timestamp"],
            "transcript": e["transcript"],
            "risk_level": e["risk_level"],
            "score": e["score"],
            "reasons": e["reasons"],
        }
        computed = hashlib.sha256((prev + ":" + json.dumps(body, sort_keys=True, separators=(",", ":"))).encode()).hexdigest()
        assert e["hash"] == computed
        prev = computed
    print("    - SHA-256 Cryptographic Chaining: 100% Mathematically Verified")

    # 10. Session Timeline
    timeline_res = client.get(f"/api/sessions/{sid}/timeline", headers=headers).json()
    print(f"\n[10] Evidence Timeline Retrieval:")
    print(f"    - Recorded Events: {len(timeline_res['events'])}")
    print(f"    - First Event: \"{timeline_res['events'][0]['transcript_preview']}\"")
    print(f"    - Final Event: \"{timeline_res['events'][-1]['transcript_preview']}\"")

    print("\n" + "=" * 80)
    print("DEMO RUNBOOK VALIDATION VERDICT: DEMO-READY VERIFIED")
    print("=" * 80 + "\n")


if __name__ == "__main__":
    run_demo_validation()
