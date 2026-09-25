"""
Phase 2: Model Calibration Assessment.
Measures whether scam_probability behaves as a calibrated probability.
Computes: Brier score, Expected Calibration Error (ECE), reliability curve data.
Uses the frozen VALIDATION set only.
If not calibrated, recommends renaming to 'model_score' in user-facing output.
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
import numpy as np

backend_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "backend"))
sys.path.insert(0, backend_path)

from app.ml.scam_classifier import get_scam_classifier

N_BINS = 10  # For reliability curve


def compute_brier_score(y_true, y_scores):
    """Lower is better. Perfect = 0.0. Baseline (predict mean) ~ variance of labels."""
    return float(np.mean((np.array(y_scores) - np.array(y_true)) ** 2))


def compute_ece(y_true, y_scores, n_bins=10):
    """Expected Calibration Error. Lower is better."""
    y_true = np.array(y_true)
    y_scores = np.array(y_scores)
    bins = np.linspace(0, 1, n_bins + 1)
    ece = 0.0
    n = len(y_true)
    bin_data = []
    for i in range(n_bins):
        mask = (y_scores >= bins[i]) & (y_scores < bins[i + 1])
        if mask.sum() == 0:
            bin_data.append(None)
            continue
        bin_scores = y_scores[mask]
        bin_labels = y_true[mask]
        mean_confidence = float(np.mean(bin_scores))
        fraction_positives = float(np.mean(bin_labels))
        ece += (mask.sum() / n) * abs(mean_confidence - fraction_positives)
        bin_data.append({
            "bin_lower": round(float(bins[i]), 2),
            "bin_upper": round(float(bins[i + 1]), 2),
            "count": int(mask.sum()),
            "mean_confidence": round(mean_confidence, 4),
            "fraction_positives": round(fraction_positives, 4),
            "calibration_error": round(abs(mean_confidence - fraction_positives), 4)
        })
    return round(ece, 4), bin_data


def load_conversations(filepath):
    items = []
    with open(filepath, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                items.append(json.loads(line))
    return items


def run_calibration_analysis(val_file: str = "ml/datasets/final/val_conversations.jsonl"):
    print("=" * 60)
    print("MODEL CALIBRATION ANALYSIS")
    print(f"Using validation set: {val_file}")
    print("=" * 60)

    clf = get_scam_classifier()
    convs = load_conversations(val_file)

    y_true = []
    y_scores = []
    for conv in convs:
        caller_texts = [t["text"] for t in conv.get("turns", []) if t.get("text")]
        full_text = " ".join(caller_texts)
        pred = clf.predict(full_text)
        y_true.append(1 if conv.get("is_scam", False) else 0)
        y_scores.append(pred["scam_probability"])

    y_true = np.array(y_true)
    y_scores = np.array(y_scores)

    brier = compute_brier_score(y_true, y_scores)
    # Baseline: predict mean of y_true for everyone
    baseline_brier = float(np.mean((np.mean(y_true) - y_true) ** 2))
    ece, bin_data = compute_ece(y_true, y_scores, n_bins=N_BINS)

    # Is it calibrated? Heuristic threshold
    is_calibrated = ece < 0.10

    print(f"\nBrier Score: {brier:.4f}  (Baseline = {baseline_brier:.4f})")
    print(f"ECE:         {ece:.4f}")
    print(f"Calibrated:  {'YES (ECE < 0.10)' if is_calibrated else 'NO  (ECE >= 0.10)'}")
    if not is_calibrated:
        print("\n  [WARNING] Output is NOT a calibrated probability.")
        print("  User-facing field should be documented as 'model_score' not 'probability'.")

    print("\nReliability Curve (per bin):")
    print(f"{'Bin':15} | {'N':5} | {'Mean Conf':10} | {'Frac Pos':9} | {'Cal Err':8}")
    print("-" * 57)
    for b in bin_data:
        if b is None:
            continue
        print(
            f"[{b['bin_lower']:.1f}, {b['bin_upper']:.1f})    "
            f"| {b['count']:5d} | {b['mean_confidence']:10.4f} | "
            f"{b['fraction_positives']:9.4f} | {b['calibration_error']:8.4f}"
        )

    result = {
        "dataset": val_file,
        "total_conversations": len(convs),
        "brier_score": brier,
        "baseline_brier_score": baseline_brier,
        "brier_skill_score": round(1.0 - brier / max(baseline_brier, 1e-9), 4),
        "expected_calibration_error": ece,
        "is_calibrated": bool(is_calibrated),
        "recommendation": (
            "Model output behaves approximately as a probability."
            if is_calibrated
            else "ECE >= 0.10: Output is NOT well-calibrated. Document as 'model_score' "
                 "in all user-facing text, reports, and API responses until calibration "
                 "(e.g., Platt scaling or isotonic regression) is applied."
        ),
        "reliability_curve": [b for b in bin_data if b is not None]
    }

    report_path = os.path.join(
        os.path.dirname(__file__), "..", "reports", "calibration_analysis.json"
    )
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
    print(f"\nSaved: {report_path}")
    return result


if __name__ == "__main__":
    run_calibration_analysis()
