"""
Phase 2: Threshold Calibration Analysis.
Evaluates precision, recall, F1, FPR, FNR at thresholds 0.30 - 0.90.
Uses the FROZEN val set only - NEVER the test set.
Recommends threshold using safety objective: minimize FNR without unacceptable FPR.
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
import numpy as np
from sklearn.metrics import precision_score, recall_score, f1_score, confusion_matrix

backend_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "backend"))
sys.path.insert(0, backend_path)

from app.ml.scam_classifier import get_scam_classifier

THRESHOLDS = [0.30, 0.40, 0.50, 0.60, 0.70, 0.80, 0.90]


def load_conversations(filepath: str):
    items = []
    with open(filepath, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                items.append(json.loads(line))
    return items


def collect_scores(clf, conversations):
    y_true = []
    y_scores = []
    for conv in conversations:
        caller_texts = [t["text"] for t in conv.get("turns", []) if t.get("text")]
        full_text = " ".join(caller_texts)
        pred = clf.predict(full_text)
        score = pred["scam_probability"]
        y_true.append(1 if conv.get("is_scam", False) else 0)
        y_scores.append(score)
    return np.array(y_true), np.array(y_scores)


def evaluate_threshold(y_true, y_scores, threshold):
    y_pred = (y_scores >= threshold).astype(int)
    prec = precision_score(y_true, y_pred, zero_division=0)
    rec = recall_score(y_true, y_pred, zero_division=0)
    f1 = f1_score(y_true, y_pred, zero_division=0)
    cm = confusion_matrix(y_true, y_pred, labels=[0, 1])
    tn, fp, fn, tp = cm.ravel()
    fpr = fp / max(fp + tn, 1)
    fnr = fn / max(fn + tp, 1)
    return {
        "threshold": threshold,
        "precision": round(float(prec), 4),
        "recall": round(float(rec), 4),
        "f1": round(float(f1), 4),
        "false_positive_rate": round(float(fpr), 4),
        "false_negative_rate": round(float(fnr), 4),
        "TP": int(tp), "FP": int(fp), "TN": int(tn), "FN": int(fn)
    }


def choose_threshold(results: list) -> dict:
    """
    Safety objective:
    Minimize FNR (missed scams are dangerous).
    Subject to: FPR <= 0.50 (false alarm rate must stay below 50%).
    Among candidates, prefer highest F1 as tiebreaker.
    """
    candidates = [r for r in results if r["false_positive_rate"] <= 0.50]
    if not candidates:
        candidates = results  # fallback
    best = min(candidates, key=lambda r: (r["false_negative_rate"], -r["f1"]))
    return best


def run_calibration(val_file: str = "ml/datasets/final/val_conversations.jsonl"):
    print("=" * 60)
    print("THRESHOLD CALIBRATION (VALIDATION SET ONLY)")
    print("Using: " + val_file)
    print("=" * 60)

    clf = get_scam_classifier()
    print(f"Model: {clf.model_version} on {clf.device_str}")

    convs = load_conversations(val_file)
    print(f"Loaded {len(convs)} validation conversations")

    y_true, y_scores = collect_scores(clf, convs)
    scam_count = int(np.sum(y_true))
    benign_count = int(len(y_true) - scam_count)
    print(f"Scam: {scam_count}, Benign: {benign_count}")

    results = []
    print("\nThreshold | Prec   | Rec    | F1     | FPR    | FNR    | TP  | FP  | TN  | FN")
    print("-" * 85)
    for t in THRESHOLDS:
        r = evaluate_threshold(y_true, y_scores, t)
        results.append(r)
        print(
            f"  {t:.2f}    | {r['precision']:.4f} | {r['recall']:.4f} | {r['f1']:.4f} | "
            f"{r['false_positive_rate']:.4f} | {r['false_negative_rate']:.4f} | "
            f"{r['TP']:3d} | {r['FP']:3d} | {r['TN']:3d} | {r['FN']:3d}"
        )

    recommended = choose_threshold(results)
    print("\n" + "=" * 60)
    print(f"RECOMMENDED THRESHOLD: {recommended['threshold']}")
    print(f"  Safety objective: Minimize FNR, FPR <= 0.50")
    print(f"  FNR: {recommended['false_negative_rate']:.4f} | FPR: {recommended['false_positive_rate']:.4f}")
    print(f"  F1:  {recommended['f1']:.4f}")
    print("=" * 60)

    out = {
        "dataset": val_file,
        "total_conversations": len(convs),
        "scam_count": scam_count,
        "benign_count": benign_count,
        "threshold_results": results,
        "recommended_threshold": recommended,
        "safety_objective": "Minimize FNR subject to FPR <= 0.50. Test set NOT touched."
    }

    report_path = os.path.join(
        os.path.dirname(__file__), "..", "ml", "reports", "threshold_calibration.json"
    )
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)
    print(f"\nReport saved: {report_path}")
    return out


if __name__ == "__main__":
    run_calibration()
