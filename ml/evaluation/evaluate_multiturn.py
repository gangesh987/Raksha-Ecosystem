"""
Phase 2: Multi-turn conversation stage/velocity progression test.
Tests that the JEV pipeline correctly tracks risk escalation and
verifies the reversal case (victim recognizes scam) reduces risk.
"""

import os
import sys
import json

os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

def _setup_torch_dll():
    if os.name != "nt":
        return
    try:
        import importlib.util
        spec = importlib.util.find_spec("torch")
        if spec and spec.origin:
            torch_lib = os.path.join(os.path.dirname(spec.origin), "lib")
            if os.path.isdir(torch_lib):
                os.environ["PATH"] = torch_lib + os.pathsep + os.environ.get("PATH", "")
                if hasattr(os, "add_dll_directory"):
                    os.add_dll_directory(torch_lib)
    except Exception:
        pass

_setup_torch_dll()

import torch

backend_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "backend"))
sys.path.insert(0, backend_path)

from app.ml.scam_classifier import get_scam_classifier
from app.ai.jev_provider import LocalSemanticJEVProvider, RuleBasedSafetyFloor

MULTI_TURN_FILE = "tests/ml/multiturn_test_conversations.jsonl"
SCAM_THRESHOLD = 0.50


def load_jsonl(path):
    items = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                items.append(json.loads(line))
    return items


def run_multiturn_tests():
    clf = get_scam_classifier()
    floor = RuleBasedSafetyFloor()
    conversations = load_jsonl(MULTI_TURN_FILE)

    results = []
    print("=" * 70)
    print("MULTI-TURN PROGRESSION TEST")
    print("=" * 70)

    for conv in conversations:
        conv_id = conv["conversation_id"]
        scenario = conv.get("scenario", "")
        turns = conv.get("turns", [])
        print(f"\nConversation: {conv_id}")
        print(f"Scenario:     {scenario}")
        print(f"{'Turn':6} | {'Score':8} | {'Predicted':10} | {'Expected Risk':14} | Text")
        print("-" * 85)

        turn_results = []
        cumulative_text = []

        for turn in turns:
            text = turn["text"]
            cumulative_text.append(text)
            window_text = " ".join(cumulative_text)

            res = clf.predict(window_text)
            score = res["scam_probability"]
            is_scam = score >= SCAM_THRESHOLD
            is_protective = floor.is_protective(window_text)

            expected_risk = turn.get("expected_risk", "unknown")
            safety_brake = turn.get("safety_brake_expected", False)

            status = "OK"
            if expected_risk in ["high", "critical"] and not is_scam:
                status = "MISS"
            elif expected_risk in ["low"] and is_scam:
                # Check if it's the reversal case
                if is_protective:
                    status = "CORRECT (protective)"
                else:
                    status = "FP"

            safe_text = text[:50].encode("ascii", errors="replace").decode("ascii")
            print(
                f"  {turn['turn_id']:4d} | {score:8.4f} | {'SCAM' if is_scam else 'BENIGN':10s} | "
                f"{expected_risk:14s} | {safe_text}..."
            )

            turn_results.append({
                "turn_id": turn["turn_id"],
                "score": round(score, 4),
                "predicted_scam": is_scam,
                "is_protective": is_protective,
                "expected_risk": expected_risk,
                "safety_brake_expected": safety_brake,
                "status": status
            })

        results.append({
            "conversation_id": conv_id,
            "scenario": scenario,
            "turn_results": turn_results
        })

    out_path = os.path.join(
        os.path.dirname(__file__), "..", "reports", "multiturn_test_results.json"
    )
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    print(f"\n\nMulti-turn results saved: {out_path}")
    return results


if __name__ == "__main__":
    run_multiturn_tests()
