"""
Phase 2: Comprehensive Adversarial + Hard Negative + Indic Holdout Evaluation.
Runs all three new benchmarks and computes:
- Adversarial v2 (14 categories, 75+ examples)
- Hard negatives (50 examples - false positive measurement)
- Indic holdout (Tamil, Tanglish, Hindi, Hinglish)
- Ablation: Neural Only vs Rule Floor Only vs Hybrid
Uses the frozen test set hash check as a safety guard before any run.
"""

import os
import sys
import json
import hashlib

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

backend_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "backend"))
sys.path.insert(0, backend_path)

from app.ml.scam_classifier import get_scam_classifier
from app.ai.jev_provider import LocalSemanticJEVProvider, RuleBasedSafetyFloor

# =====================================================================
# CONSTANTS
# =====================================================================
FROZEN_TEST_HASH = "6696db171f4e33d76ac01c622ee4c89ac71de9ffa07b137818e36f806bfa56a0"
FROZEN_TEST_FILE = "ml/datasets/final/test_conversations.jsonl"
ADV_V2_FILE = "tests/ml/adversarial_benchmark_v2.jsonl"
HARD_NEG_FILE = "tests/ml/hard_negatives.jsonl"
INDIC_HOLDOUT_FILE = "ml/datasets/evaluation/indic_holdout/indic_holdout_conversations.jsonl"
SCAM_THRESHOLD = 0.50


# =====================================================================
# SAFETY CHECK: Verify test set is frozen
# =====================================================================
def verify_frozen_test_set():
    with open(FROZEN_TEST_FILE, "rb") as f:
        actual_hash = hashlib.sha256(f.read()).hexdigest()
    if actual_hash != FROZEN_TEST_HASH:
        raise RuntimeError(
            f"FROZEN TEST SET INTEGRITY VIOLATION!\n"
            f"  Expected: {FROZEN_TEST_HASH}\n"
            f"  Got:      {actual_hash}\n"
            f"  The test set has been modified - aborting all evaluation."
        )
    print(f"[OK] Frozen test set integrity verified: {actual_hash[:16]}...")


# =====================================================================
# LOADERS
# =====================================================================
def load_jsonl(path):
    items = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                items.append(json.loads(line))
    return items


# =====================================================================
# EVALUATION HELPERS
# =====================================================================
def neural_predict(clf, text: str, threshold=SCAM_THRESHOLD) -> bool:
    res = clf.predict(text)
    return res["scam_probability"] >= threshold, res["scam_probability"]


def rule_only_predict(floor: RuleBasedSafetyFloor, text: str) -> bool:
    if floor.is_protective(text):
        return False
    total_score = 0.0
    for tactic in floor.TACTIC_PHRASES:
        score, _ = floor.evaluate_tactic_match(text, text, tactic)
        total_score = max(total_score, score)
    return total_score >= 0.75


def hybrid_predict(clf, floor: RuleBasedSafetyFloor, text: str, threshold=SCAM_THRESHOLD) -> bool:
    if floor.is_protective(text):
        return False
    res = clf.predict(text)
    p_neural = res["scam_probability"]
    # Rule floor score
    floor_score = 0.0
    for tactic in floor.TACTIC_PHRASES:
        score, _ = floor.evaluate_tactic_match(text, text, tactic)
        floor_score = max(floor_score, score)
    p_hybrid = max(p_neural, floor_score if floor_score >= 0.75 else 0.0)
    return p_hybrid >= threshold


def compute_metrics(y_true, y_pred):
    tp = sum(1 for t, p in zip(y_true, y_pred) if t and p)
    fp = sum(1 for t, p in zip(y_true, y_pred) if not t and p)
    tn = sum(1 for t, p in zip(y_true, y_pred) if not t and not p)
    fn = sum(1 for t, p in zip(y_true, y_pred) if t and not p)
    prec = tp / max(tp + fp, 1)
    rec = tp / max(tp + fn, 1)
    f1 = 2 * prec * rec / max(prec + rec, 1e-9)
    fpr = fp / max(fp + tn, 1)
    fnr = fn / max(fn + tp, 1)
    return {
        "precision": round(prec, 4),
        "recall": round(rec, 4),
        "f1": round(f1, 4),
        "false_positive_rate": round(fpr, 4),
        "false_negative_rate": round(fnr, 4),
        "TP": tp, "FP": fp, "TN": tn, "FN": fn
    }


# =====================================================================
# ADVERSARIAL V2 EVALUATION
# =====================================================================
def run_adversarial_v2(clf, floor):
    print("\n" + "=" * 60)
    print("ADVERSARIAL BENCHMARK V2 (14 categories)")
    print("=" * 60)
    examples = load_jsonl(ADV_V2_FILE)

    results_by_category = {}
    y_true_neural, y_pred_neural = [], []
    y_true_rule, y_pred_rule = [], []
    y_true_hybrid, y_pred_hybrid = [], []

    for ex in examples:
        text = ex["text"]
        label = ex.get("expected_scam", True)
        cat = ex.get("category", "unknown")

        n_pred, _ = neural_predict(clf, text)
        r_pred = rule_only_predict(floor, text)
        h_pred = hybrid_predict(clf, floor, text)

        y_true_neural.append(label)
        y_pred_neural.append(n_pred)
        y_true_rule.append(label)
        y_pred_rule.append(r_pred)
        y_true_hybrid.append(label)
        y_pred_hybrid.append(h_pred)

        if cat not in results_by_category:
            results_by_category[cat] = {"total": 0, "neural_correct": 0, "hybrid_correct": 0}
        results_by_category[cat]["total"] += 1
        if n_pred == label:
            results_by_category[cat]["neural_correct"] += 1
        if h_pred == label:
            results_by_category[cat]["hybrid_correct"] += 1

    neural_m = compute_metrics(y_true_neural, y_pred_neural)
    rule_m = compute_metrics(y_true_rule, y_pred_rule)
    hybrid_m = compute_metrics(y_true_hybrid, y_pred_hybrid)

    print(f"Total examples: {len(examples)}")
    print(f"\nNeural Only:   F1={neural_m['f1']:.4f} | Prec={neural_m['precision']:.4f} | Rec={neural_m['recall']:.4f} | FNR={neural_m['false_negative_rate']:.4f}")
    print(f"Rule Only:     F1={rule_m['f1']:.4f} | Prec={rule_m['precision']:.4f} | Rec={rule_m['recall']:.4f} | FNR={rule_m['false_negative_rate']:.4f}")
    print(f"Hybrid JEV:    F1={hybrid_m['f1']:.4f} | Prec={hybrid_m['precision']:.4f} | Rec={hybrid_m['recall']:.4f} | FNR={hybrid_m['false_negative_rate']:.4f}")

    print("\nPer-Category Results (Neural | Hybrid):")
    for cat, r in sorted(results_by_category.items()):
        n_acc = r["neural_correct"] / max(r["total"], 1)
        h_acc = r["hybrid_correct"] / max(r["total"], 1)
        print(f"  {cat:30s}: Neural {n_acc:.2%} | Hybrid {h_acc:.2%} (N={r['total']})")

    return {
        "total": len(examples),
        "neural": neural_m,
        "rule_only": rule_m,
        "hybrid": hybrid_m,
        "per_category": results_by_category
    }


# =====================================================================
# HARD NEGATIVES EVALUATION
# =====================================================================
def run_hard_negatives(clf, floor):
    print("\n" + "=" * 60)
    print("HARD NEGATIVES (False Positive Measurement)")
    print("=" * 60)
    examples = load_jsonl(HARD_NEG_FILE)

    fp_neural = 0
    fp_rule = 0
    fp_hybrid = 0
    category_fp = {}

    for ex in examples:
        text = ex["text"]
        cat = ex.get("category", "unknown")
        # All hard negatives are benign (label=False)
        n_pred, score = neural_predict(clf, text)
        r_pred = rule_only_predict(floor, text)
        h_pred = hybrid_predict(clf, floor, text)

        if n_pred:
            fp_neural += 1
        if r_pred:
            fp_rule += 1
        if h_pred:
            fp_hybrid += 1

        if cat not in category_fp:
            category_fp[cat] = {"total": 0, "neural_fp": 0, "hybrid_fp": 0}
        category_fp[cat]["total"] += 1
        if n_pred:
            category_fp[cat]["neural_fp"] += 1
        if h_pred:
            category_fp[cat]["hybrid_fp"] += 1

    total = len(examples)
    print(f"Total hard-negative examples: {total}")
    print(f"Neural FP:  {fp_neural}/{total} ({fp_neural/max(total,1):.2%})")
    print(f"Rule FP:    {fp_rule}/{total} ({fp_rule/max(total,1):.2%})")
    print(f"Hybrid FP:  {fp_hybrid}/{total} ({fp_hybrid/max(total,1):.2%})")

    print("\nPer-Category False Positives:")
    for cat, r in sorted(category_fp.items()):
        n_fpr = r["neural_fp"] / max(r["total"], 1)
        h_fpr = r["hybrid_fp"] / max(r["total"], 1)
        print(f"  {cat:30s}: Neural {n_fpr:.2%} | Hybrid {h_fpr:.2%} (N={r['total']})")

    return {
        "total": total,
        "neural_false_positives": fp_neural,
        "neural_fpr": round(fp_neural / max(total, 1), 4),
        "rule_false_positives": fp_rule,
        "rule_fpr": round(fp_rule / max(total, 1), 4),
        "hybrid_false_positives": fp_hybrid,
        "hybrid_fpr": round(fp_hybrid / max(total, 1), 4),
        "per_category": category_fp
    }


# =====================================================================
# INDIC HOLDOUT EVALUATION
# =====================================================================
def run_indic_holdout(clf, floor):
    print("\n" + "=" * 60)
    print("INDIC HOLDOUT EVALUATION (Tamil/Tanglish/Hindi/Hinglish)")
    print("=" * 60)
    convs = load_jsonl(INDIC_HOLDOUT_FILE)

    by_lang = {}
    for conv in convs:
        lang = conv.get("language", "unknown")
        label = conv.get("is_scam", False)
        turns = conv.get("turns", [])
        text = " ".join(t["text"] for t in turns if t.get("text"))

        n_pred, score = neural_predict(clf, text)
        h_pred = hybrid_predict(clf, floor, text)

        if lang not in by_lang:
            by_lang[lang] = {"y_true": [], "y_neural": [], "y_hybrid": [], "texts": []}
        by_lang[lang]["y_true"].append(label)
        by_lang[lang]["y_neural"].append(n_pred)
        by_lang[lang]["y_hybrid"].append(h_pred)
        by_lang[lang]["texts"].append({"text": text[:100], "label": label, "neural": n_pred, "hybrid": h_pred, "score": round(score, 4)})

    print(f"{'Lang':12} | {'N':4} | {'Neural F1':10} | {'Hybrid F1':10} | {'FNR Neural':11} | {'FPR Neural':11}")
    print("-" * 70)

    lang_results = {}
    for lang, data in sorted(by_lang.items()):
        y_t = data["y_true"]
        y_n = data["y_neural"]
        y_h = data["y_hybrid"]
        if len(set(y_t)) < 2:
            # Can't compute full F1 with only one class
            n_correct = sum(1 for a, b in zip(y_t, y_n) if a == b)
            print(f"  {lang:10}: N={len(y_t)}, Neural acc={n_correct/max(len(y_t),1):.2%} (single class)")
            lang_results[lang] = {"count": len(y_t), "single_class": True}
            continue
        n_m = compute_metrics(y_t, y_n)
        h_m = compute_metrics(y_t, y_h)
        print(
            f"  {lang:10} | {len(y_t):4d} | {n_m['f1']:10.4f} | {h_m['f1']:10.4f} | "
            f"{n_m['false_negative_rate']:11.4f} | {n_m['false_positive_rate']:11.4f}"
        )
        lang_results[lang] = {
            "count": len(y_t),
            "neural": n_m,
            "hybrid": h_m,
            "sample_predictions": data["texts"][:5]
        }

    print("\n[NOTE] Small N per language - treat as directional, not statistically robust.")
    return lang_results


# =====================================================================
# MAIN
# =====================================================================
def run_phase2_evaluation():
    verify_frozen_test_set()
    clf = get_scam_classifier()
    floor = RuleBasedSafetyFloor()

    adv_results = run_adversarial_v2(clf, floor)
    hn_results = run_hard_negatives(clf, floor)
    indic_results = run_indic_holdout(clf, floor)

    full_report = {
        "phase": "Phase 2",
        "evaluation_date": "2026-09-26",
        "frozen_test_set_verified": True,
        "frozen_test_hash": FROZEN_TEST_HASH,
        "adversarial_v2": adv_results,
        "hard_negatives": hn_results,
        "indic_holdout": indic_results
    }

    out_path = os.path.join(
        os.path.dirname(__file__), "..", "reports", "phase2_evaluation_results.json"
    )
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(full_report, f, indent=2, ensure_ascii=False)
    print(f"\nPhase 2 evaluation report saved: {out_path}")
    return full_report


if __name__ == "__main__":
    run_phase2_evaluation()
