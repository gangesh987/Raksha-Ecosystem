"""
Repeated Jury Demo Scenario Validator (3 Consecutive Independent Runs).
Executes the exact 5-turn scenario across 3 separate, fresh sessions to
empirically demonstrate consistency without cherry-picking.
"""

import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from fastapi.testclient import TestClient
from app.main import app

def run_triplicate_demo():
    client = TestClient(app)

    # Auth
    login_res = client.post("/api/auth/login", json={
        "email": "demo_evaluator@rakshacall.safe",
        "password": "EvaluatorPassword123!"
    })
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    demo_script = [
        "I am calling from the cyber crime department.",
        "Your Aadhaar is linked to an illegal transaction.",
        "Do not tell your family.",
        "Transfer the money immediately.",
        "Send the OTP."
    ]

    print("=" * 80)
    print("RAKSHACALL JURY DEMO SCENARIO: 3 CONSECUTIVE INDEPENDENT RUNS")
    print("=" * 80)

    trials_summary = []

    for trial_idx in range(1, 4):
        # Create fresh session
        sess = client.post("/api/sessions", json={"source": f"JURY_DEMO_TRIAL_{trial_idx}"}, headers=headers).json()
        sid = sess["id"]
        print(f"\n--- TRIAL {trial_idx} (Session ID: {sid}) ---")

        trial_results = []
        for turn_idx, text in enumerate(demo_script, start=1):
            r = client.post(
                f"/api/sessions/{sid}/analyze",
                json={"transcript": text, "visual_score": 0.15, "liveness_score": 0.85},
                headers=headers
            ).json()
            trial_results.append(r)
            print(f"Turn {turn_idx}: score={r['risk_score']:2d} | level={r['risk_level']:8s} | stage={r['stage']:18s} | vel={r['velocity_level']:8s} | brake={r['safety_brake_triggered']}")

        peak_score = max(r["risk_score"] for r in trial_results)
        final_stage = trial_results[-1]["stage"]
        brakes = [r["safety_brake_triggered"] for r in trial_results]

        trials_summary.append({
            "trial": trial_idx,
            "session_id": sid,
            "peak_risk_score": peak_score,
            "peak_risk_level": "HIGH" if peak_score >= 61 else "MEDIUM" if peak_score >= 31 else "LOW",
            "final_stage": final_stage,
            "safety_brake_fired": any(brakes),
            "brake_turns": [i+1 for i, b in enumerate(brakes) if b]
        })

    print("\n" + "=" * 80)
    print("TRIPLICATE EXECUTION SUMMARY TABLE")
    print("=" * 80)
    print(f"{'Trial':<8}{'Session ID':<14}{'Peak Score':<14}{'Risk Level':<14}{'Brake Fired?':<16}{'Brake Active Turns'}")
    print("-" * 80)
    for t in trials_summary:
        print(f"{t['trial']:<8}{t['session_id']:<14}{t['peak_risk_score']:<14}{t['peak_risk_level']:<14}{str(t['safety_brake_fired']):<16}{t['brake_turns']}")
    print("=" * 80)

if __name__ == "__main__":
    run_triplicate_demo()
