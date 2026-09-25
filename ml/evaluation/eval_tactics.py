"""
RakshaCall Comprehensive Tactic & Scam Model Evaluation Suite.
Calculates:
- Binary scam metrics: Precision, Recall, F1, Accuracy
- 9 Tactic multi-label metrics: micro F1, macro F1, per-class precision, recall, F1
- False Positive Rate (FPR) on explicit Negative Controls (Safety Advice)
- Latency profiling per inference forward pass
Outputs evaluation results to ml/reports/tactic_eval_report.json and markdown summary.
"""

import os
import sys
import json
import time
import logging
import torch
import numpy as np
from sklearn.metrics import precision_score, recall_score, f1_score, accuracy_score, confusion_matrix

# Ensure project root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

from ml.models.scam_classifier.v1.inference import TrainedScamClassifier
from ml.preprocessing.label_mapper import RAKSHACALL_TACTICS

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("TacticEvaluator")

FINAL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets", "final")
REPORTS_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "reports")

def evaluate_test_set():
    os.makedirs(REPORTS_DIR, exist_ok=True)
    classifier = TrainedScamClassifier()
    if not classifier.is_loaded:
        logger.error("Model weights not available. Train the model first.")
        return {}

    test_file = os.path.join(FINAL_DIR, "test_turns.jsonl")
    if not os.path.exists(test_file):
        logger.error(f"Test split not found at {test_file}")
        return {}

    test_samples = []
    with open(test_file, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                test_samples.append(json.loads(line))

    logger.info(f"Evaluating model on {len(test_samples)} untouched test set turns...")

    y_scam_true, y_scam_pred = [], []
    y_tactics_true, y_tactics_pred = [], []
    latencies = []

    neg_controls_total, neg_controls_false_alarms = 0, 0

    for sample in test_samples:
        text = sample["text"]
        true_scam = 1 if sample["is_scam"] else 0
        true_tactics = [1 if t in sample.get("tactics", []) else 0 for t in RAKSHACALL_TACTICS]

        # Track negative controls specifically
        if not sample["is_scam"]:
            neg_controls_total += 1

        t0 = time.perf_counter()
        res = classifier.predict(text)
        latencies.append((time.perf_counter() - t0) * 1000.0)

        pred_scam = 1 if res["is_scam"] else 0
        pred_tactics = [1 if res["tactic_probabilities"].get(t, 0.0) >= 0.40 else 0 for t in RAKSHACALL_TACTICS]

        if not sample["is_scam"] and pred_scam == 1:
            neg_controls_false_alarms += 1

        y_scam_true.append(true_scam)
        y_scam_pred.append(pred_scam)
        y_tactics_true.append(true_tactics)
        y_tactics_pred.append(pred_tactics)

    # Compute binary scam metrics
    scam_prec = precision_score(y_scam_true, y_scam_pred, zero_division=0)
    scam_rec = recall_score(y_scam_true, y_scam_pred, zero_division=0)
    scam_f1 = f1_score(y_scam_true, y_scam_pred, zero_division=0)
    scam_acc = accuracy_score(y_scam_true, y_scam_pred)
    tn, fp, fn, tp = confusion_matrix(y_scam_true, y_scam_pred, labels=[0, 1]).ravel()

    # Compute multi-label tactic metrics
    y_tactics_true = np.array(y_tactics_true)
    y_tactics_pred = np.array(y_tactics_pred)

    macro_f1 = f1_score(y_tactics_true, y_tactics_pred, average="macro", zero_division=0)
    micro_f1 = f1_score(y_tactics_true, y_tactics_pred, average="micro", zero_division=0)

    per_tactic = {}
    for i, tactic in enumerate(RAKSHACALL_TACTICS):
        t_prec = precision_score(y_tactics_true[:, i], y_tactics_pred[:, i], zero_division=0)
        t_rec = recall_score(y_tactics_true[:, i], y_tactics_pred[:, i], zero_division=0)
        t_f1 = f1_score(y_tactics_true[:, i], y_tactics_pred[:, i], zero_division=0)
        per_tactic[tactic] = {
            "precision": round(float(t_prec), 4),
            "recall": round(float(t_rec), 4),
            "f1": round(float(t_f1), 4),
            "support": int(np.sum(y_tactics_true[:, i]))
        }

    fpr_negative_controls = (neg_controls_false_alarms / max(neg_controls_total, 1)) * 100.0

    report = {
        "evaluation_dataset": "RakshaCall-Test-Split-Untouched",
        "sample_count": len(test_samples),
        "binary_scam_classification": {
            "precision": round(float(scam_prec), 4),
            "recall": round(float(scam_rec), 4),
            "f1_score": round(float(scam_f1), 4),
            "accuracy": round(float(scam_acc), 4),
            "confusion_matrix": {"TN": int(tn), "FP": int(fp), "FN": int(fn), "TP": int(tp)}
        },
        "multilabel_tactics_classification": {
            "macro_f1": round(float(macro_f1), 4),
            "micro_f1": round(float(micro_f1), 4),
            "per_class": per_tactic
        },
        "safety_guardrails": {
            "negative_control_samples": neg_controls_total,
            "false_alarms": neg_controls_false_alarms,
            "negative_control_fpr_pct": round(fpr_negative_controls, 2)
        },
        "latency_profile_ms": {
            "mean": round(float(np.mean(latencies)), 2),
            "p50": round(float(np.percentile(latencies, 50)), 2),
            "p95": round(float(np.percentile(latencies, 95)), 2),
            "p99": round(float(np.percentile(latencies, 99)), 2)
        }
    }

    report_path = os.path.join(REPORTS_DIR, "tactic_evaluation.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)

    logger.info(f"Tactic Evaluation Report saved at {report_path}")
    return report

if __name__ == "__main__":
    evaluate_test_set()
