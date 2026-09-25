"""
Phase 11: Comprehensive Evaluation on Independent Test Split.
Evaluates the PyTorch ScamClassifier on ml/datasets/final/test_conversations.jsonl.
Measures:
- Binary scam classification (Precision, Recall, F1, Accuracy, Confusion Matrix)
- 9 Tactic classification (Per-tactic Precision, Recall, F1, Macro-F1, Micro-F1)
- Language-wise breakdown (English, Hindi, Tamil, Hinglish, Tanglish)
"""

import os
import sys

os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

# Windows DLL directory fix for PyTorch
torch_lib = r"C:\Users\gangs\AppData\Local\Programs\Python\Python311\Lib\site-packages\torch\lib"
if os.path.exists(torch_lib):
    os.environ["PATH"] = torch_lib + os.pathsep + os.environ.get("PATH", "")
    if hasattr(os, "add_dll_directory"):
        try:
            os.add_dll_directory(torch_lib)
        except Exception:
            pass

import torch
import json
import time
import numpy as np
from typing import Dict, List, Any
from sklearn.metrics import (
    precision_score, recall_score, f1_score, accuracy_score,
    confusion_matrix, classification_report
)

# Ensure backend is on sys.path
backend_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "backend"))
sys.path.insert(0, backend_path)

from app.ml.scam_classifier import get_scam_classifier, CANONICAL_TACTIC_KEYS


def run_test_evaluation(test_file: str = "ml/datasets/final/test_conversations.jsonl"):
    clf = get_scam_classifier()
    print(f"Loaded classifier: {clf.model_version} on {clf.device_str}")

    y_true_scam = []
    y_pred_scam = []

    y_true_tactics = []
    y_pred_tactics = []

    langs = []

    total_convs = 0
    total_turns = 0

    t0 = time.perf_counter()

    with open(test_file, "r", encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            conv = json.loads(line)
            total_convs += 1
            conv_lang = conv.get("language", "en")
            conv_is_scam = conv.get("is_scam", False)
            conv_tactics = set(conv.get("tactics", []))

            # Concatenate conversation caller turns or full dialogue to classify
            caller_texts = [t["text"] for t in conv.get("turns", []) if t.get("text")]
            full_text = " ".join(caller_texts)
            total_turns += len(caller_texts)

            pred = clf.predict(full_text)
            pred_is_scam = pred["scam_probability"] >= 0.50

            y_true_scam.append(1 if conv_is_scam else 0)
            y_pred_scam.append(1 if pred_is_scam else 0)
            langs.append(conv_lang)

            # Ground truth tactic vector
            true_t_vec = [1 if t in conv_tactics else 0 for t in CANONICAL_TACTIC_KEYS]
            y_true_tactics.append(true_t_vec)

            # Predicted tactic vector (threshold 0.35)
            pred_t_vec = [1 if pred["tactic_probabilities"].get(t, 0.0) >= 0.35 else 0 for t in CANONICAL_TACTIC_KEYS]
            y_pred_tactics.append(pred_t_vec)

    total_time = time.perf_counter() - t0

    y_true_scam = np.array(y_true_scam)
    y_pred_scam = np.array(y_pred_scam)
    y_true_tactics = np.array(y_true_tactics)
    y_pred_tactics = np.array(y_pred_tactics)
    langs = np.array(langs)

    # 1. Binary Metrics
    acc = accuracy_score(y_true_scam, y_pred_scam)
    prec = precision_score(y_true_scam, y_pred_scam, zero_division=0)
    rec = recall_score(y_true_scam, y_pred_scam, zero_division=0)
    f1 = f1_score(y_true_scam, y_pred_scam, zero_division=0)
    cm = confusion_matrix(y_true_scam, y_pred_scam)  # [[TN, FP], [FN, TP]]
    tn, fp, fn, tp = cm.ravel() if cm.shape == (2, 2) else (0, 0, 0, 0)

    # 2. Tactic Multi-Label Metrics
    tactic_prec_macro = precision_score(y_true_tactics, y_pred_tactics, average="macro", zero_division=0)
    tactic_rec_macro = recall_score(y_true_tactics, y_pred_tactics, average="macro", zero_division=0)
    tactic_f1_macro = f1_score(y_true_tactics, y_pred_tactics, average="macro", zero_division=0)

    tactic_prec_micro = precision_score(y_true_tactics, y_pred_tactics, average="micro", zero_division=0)
    tactic_rec_micro = recall_score(y_true_tactics, y_pred_tactics, average="micro", zero_division=0)
    tactic_f1_micro = f1_score(y_true_tactics, y_pred_tactics, average="micro", zero_division=0)

    per_tactic = {}
    for idx, tactic in enumerate(CANONICAL_TACTIC_KEYS):
        t_true = y_true_tactics[:, idx]
        t_pred = y_pred_tactics[:, idx]
        per_tactic[tactic] = {
            "support": int(np.sum(t_true)),
            "predicted": int(np.sum(t_pred)),
            "precision": round(float(precision_score(t_true, t_pred, zero_division=0)), 4),
            "recall": round(float(recall_score(t_true, t_pred, zero_division=0)), 4),
            "f1": round(float(f1_score(t_true, t_pred, zero_division=0)), 4)
        }

    # 3. Language Breakdown
    per_language = {}
    for lang in np.unique(langs):
        mask = (langs == lang)
        sub_true = y_true_scam[mask]
        sub_pred = y_pred_scam[mask]
        per_language[str(lang)] = {
            "count": int(np.sum(mask)),
            "scam_count": int(np.sum(sub_true)),
            "accuracy": round(float(accuracy_score(sub_true, sub_pred)), 4),
            "precision": round(float(precision_score(sub_true, sub_pred, zero_division=0)), 4),
            "recall": round(float(recall_score(sub_true, sub_pred, zero_division=0)), 4),
            "f1": round(float(f1_score(sub_true, sub_pred, zero_division=0)), 4)
        }

    results = {
        "evaluation_dataset": test_file,
        "total_conversations": total_convs,
        "total_turns": total_turns,
        "elapsed_seconds": round(total_time, 2),
        "ms_per_conversation": round((total_time / max(1, total_convs)) * 1000.0, 2),
        "binary_scam_metrics": {
            "accuracy": round(float(acc), 4),
            "precision": round(float(prec), 4),
            "recall": round(float(rec), 4),
            "f1_score": round(float(f1), 4),
            "confusion_matrix": {
                "true_negative": int(tn),
                "false_positive": int(fp),
                "false_negative": int(fn),
                "true_positive": int(tp)
            }
        },
        "multilabel_tactic_metrics": {
            "macro_precision": round(float(tactic_prec_macro), 4),
            "macro_recall": round(float(tactic_rec_macro), 4),
            "macro_f1": round(float(tactic_f1_macro), 4),
            "micro_precision": round(float(tactic_prec_micro), 4),
            "micro_recall": round(float(tactic_rec_micro), 4),
            "micro_f1": round(float(tactic_f1_micro), 4),
            "per_tactic": per_tactic
        },
        "per_language_metrics": per_language
    }

    print("\n" + "=" * 60)
    print("RAKSHACALL TEST SPLIT EVALUATION RESULTS")
    print("=" * 60)
    print(f"Total Conversations: {total_convs} | Total Turns: {total_turns}")
    print(f"Binary Scam - Acc: {acc:.2%}, Prec: {prec:.2%}, Rec: {rec:.2%}, F1: {f1:.4f}")
    print(f"Confusion Matrix: TP={tp}, FP={fp}, TN={tn}, FN={fn}")
    print(f"Tactic Multi-Label - Macro F1: {tactic_f1_macro:.4f} | Micro F1: {tactic_f1_micro:.4f}")
    print("\nPer-Language Breakdown:")
    for lang, metrics in per_language.items():
        print(f"  {lang:8}: N={metrics['count']:3} | F1: {metrics['f1']:.4f} | Prec: {metrics['precision']:.4f} | Rec: {metrics['recall']:.4f}")
    print("=" * 60)

    # Save to reports
    out_path = os.path.join(os.path.dirname(__file__), "..", "reports", "test_split_evaluation.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)
    print(f"Saved report to: {out_path}")

    return results


if __name__ == "__main__":
    run_test_evaluation()
