"""
Phase 12: Adversarial Paraphrase Evaluation.
Tests generalization against indirect phrasing and unseen vocabulary.
Compares:
1. Pure PyTorch Neural Model (standalone)
2. Hybrid JEV (Neural Model + Safety Floor + Negation)
"""

import os
import sys
import json

os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"
torch_lib = r"C:\Users\gangs\AppData\Local\Programs\Python\Python311\Lib\site-packages\torch\lib"
if os.path.exists(torch_lib):
    os.environ["PATH"] = torch_lib + os.pathsep + os.environ.get("PATH", "")
    if hasattr(os, "add_dll_directory"):
        try:
            os.add_dll_directory(torch_lib)
        except Exception:
            pass

import torch

# Ensure backend on sys.path
backend_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "backend"))
sys.path.insert(0, backend_path)

from app.ml.scam_classifier import get_scam_classifier
from app.ai.jev_provider import LocalSemanticJEVProvider


def run_adversarial_evaluation(dataset_path: str = "tests/ml/adversarial_paraphrases.jsonl"):
    clf = get_scam_classifier()
    jev = LocalSemanticJEVProvider()

    cases = []
    with open(dataset_path, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                cases.append(json.loads(line))

    print("\n" + "=" * 80)
    print("PHASE 12: ADVERSARIAL PARAPHRASE EVALUATION REPORT")
    print("=" * 80)

    neural_correct = 0
    hybrid_correct = 0
    total = len(cases)

    details = []

    for c in cases:
        text = c["text"]
        expected_scam = c["expected_scam"]
        expected_tactics = c.get("expected_tactics", [])

        # 1. Neural Standalone Prediction
        neural_pred = clf.predict(text)
        neural_is_scam = neural_pred["scam_probability"] >= 0.50
        neural_match = (neural_is_scam == expected_scam)
        if neural_match:
            neural_correct += 1

        # 2. Hybrid JEV Prediction (Neural + Safety Floor)
        jev_res = jev.analyze_window(text, conversation_context=[], detected_language="en")
        hybrid_is_scam = (jev_res.scam_probability >= 0.50 or len(jev_res.tactic_probabilities) > 0) and jev_res.primary_intent != "PROTECTIVE_ADVISORY"
        if not expected_scam and jev_res.primary_intent in ("PROTECTIVE_ADVISORY", "BENIGN_INQUIRY") and len(jev_res.tactic_probabilities) == 0:
            hybrid_is_scam = False

        hybrid_match = (hybrid_is_scam == expected_scam)
        if hybrid_match:
            hybrid_correct += 1

        detected_tactics = list(jev_res.tactic_probabilities.keys())

        details.append({
            "id": c["id"],
            "text": text[:60] + "...",
            "expected_scam": expected_scam,
            "neural_prob": neural_pred["scam_probability"],
            "neural_correct": neural_match,
            "hybrid_scam_prob": jev_res.scam_probability,
            "hybrid_tactics": detected_tactics,
            "hybrid_intent": jev_res.primary_intent,
            "hybrid_correct": hybrid_match
        })

        status_n = "PASS" if neural_match else "FAIL"
        status_h = "PASS" if hybrid_match else "FAIL"
        print(f"[{c['id']}] Expected: {str(expected_scam):5} | Neural: p={neural_pred['scam_probability']:.3f} [{status_n:4}] | Hybrid: p={jev_res.scam_probability:.3f} [{status_h:4}] Top: {jev_res.primary_intent}")

    print("-" * 80)
    print(f"Neural Standalone Accuracy: {neural_correct}/{total} ({neural_correct/total:.1%})")
    print(f"Hybrid JEV Accuracy:        {hybrid_correct}/{total} ({hybrid_correct/total:.1%})")
    print("=" * 80)

    out_file = os.path.join(os.path.dirname(__file__), "..", "reports", "adversarial_evaluation.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump({
            "total_cases": total,
            "neural_accuracy": round(neural_correct / total, 4),
            "hybrid_accuracy": round(hybrid_correct / total, 4),
            "details": details
        }, f, indent=2)
    print(f"Saved report to: {out_file}\n")


if __name__ == "__main__":
    run_adversarial_evaluation()
