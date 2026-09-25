"""
RakshaCall Architectural Ablation Study Suite.
Evaluates the incremental contribution of each subsystem:
1. ML Only (Raw Semantic Intent Probabilities)
2. ML + Stage Machine (Temporal Progression Gating)
3. ML + Stage + Manipulation Velocity (Temporal Acceleration Context)
4. ML + Stage + Velocity + YOLO11 Vision (Supporting Context)
5. Full Production Stack: ML + Stage + Velocity + Vision + JEV Safety Guardrail
Measures F1, False Positives (FPR), False Negatives (FNR), and Latency (ms).
"""

import os
import sys
import torch
import json
import time
import numpy as np
from sklearn.metrics import f1_score, precision_score, recall_score, confusion_matrix

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

from ml.models.scam_classifier.v2.inference import ProductionMultilingualClassifier
from backend.app.ai.jev_provider import LocalSemanticJEVProvider
from backend.app.ai.stage_machine import ScamStageMachine
from backend.app.ai.manipulation_velocity import ManipulationVelocityEngine
from backend.app.ai.multimodal_fusion import MultimodalRiskFusionEngine
from backend.app.ai.yolo_vision import VisualContextSignal

FINAL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets", "final")
EVAL_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "evaluation")
REPORTS_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "reports")
os.makedirs(REPORTS_DIR, exist_ok=True)

def run_ablation_benchmark():
    # Load test turns and hard negatives
    test_path = os.path.join(FINAL_DIR, "test_turns.jsonl")
    hn_path = os.path.join(EVAL_DIR, "hard_negatives.jsonl")
    
    samples = []
    with open(test_path, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                samples.append(json.loads(line))
    with open(hn_path, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                item = json.loads(line)
                samples.append({
                    "text": item["text"],
                    "is_scam": False,
                    "tactics": [],
                    "language": item.get("language", "en")
                })

    print(f"Loaded {len(samples)} evaluation samples (including {len(open(hn_path, 'r', encoding='utf-8').readlines())} hard negatives).")

    y_true = [1 if s["is_scam"] else 0 for s in samples]

    # Components
    classifier = ProductionMultilingualClassifier()
    jev = LocalSemanticJEVProvider()
    fusion = MultimodalRiskFusionEngine()

    configs = [
        "ML Only",
        "ML + Stage",
        "ML + Stage + Velocity",
        "ML + Stage + Velocity + YOLO",
        "ML + Stage + Velocity + YOLO + JEV Guardrail"
    ]

    ablation_results = {}

    for cfg in configs:
        stage_machine = ScamStageMachine()
        velocity_engine = ManipulationVelocityEngine()
        y_pred = []
        latencies = []

        t_sim = 1000.0
        for sample in samples:
            t0 = time.perf_counter()
            text = sample["text"]
            t_sim += 3.0  # 3 seconds between turns

            # 1. ML inference
            ml_res = classifier.predict(text)
            p_scam = ml_res["scam_probability"]
            tactic_probs = ml_res["tactic_probabilities"]

            pred_trigger = False

            if cfg == "ML Only":
                pred_trigger = (p_scam >= 0.50)

            elif cfg == "ML + Stage":
                stage, s_conf = stage_machine.update_stage(tactic_probs, t_sim)
                pred_trigger = (p_scam >= 0.45 and stage in ["DEMAND", "PAYMENT_CREDENTIAL", "CRITICAL_BRAKE"])

            elif cfg == "ML + Stage + Velocity":
                stage, s_conf = stage_machine.update_stage(tactic_probs, t_sim)
                velo = velocity_engine.record_event(t_sim, tactic_probs, 5)
                # Gated by temporal acceleration
                pred_trigger = (p_scam >= 0.40 and (stage in ["DEMAND", "PAYMENT_CREDENTIAL", "CRITICAL_BRAKE"] or velo.level in ["MODERATE", "HIGH"]))

            elif cfg == "ML + Stage + Velocity + YOLO":
                stage, s_conf = stage_machine.update_stage(tactic_probs, t_sim)
                velo = velocity_engine.record_event(t_sim, tactic_probs, 5)
                # Mock supporting vision: phone present
                vis = VisualContextSignal(person_count=1, secondary_phone_detected=True, screen_or_laptop_detected=False, document_detected=False, visual_confidence=0.85)
                decision = fusion.fuse(
                    tactic_probs=tactic_probs,
                    scam_stage=stage,
                    velocity=velo,
                    semantic_confidence=p_scam,
                    visual_signal=vis
                )
                pred_trigger = decision.risk_score >= 60

            elif cfg == "ML + Stage + Velocity + YOLO + JEV Guardrail":
                # Full production stack: JEV deterministic guardrail for irreversible actions
                stage, s_conf = stage_machine.update_stage(tactic_probs, t_sim)
                velo = velocity_engine.record_event(t_sim, tactic_probs, 5)
                jev_res = jev.analyze_window(text, [], sample.get("language", "en"))
                
                # Merge tactic probs with JEV safety floor
                merged_tactics = tactic_probs.copy()
                for k, v in jev_res.tactic_probabilities.items():
                    merged_tactics[k] = max(merged_tactics.get(k, 0.0), v)

                vis = VisualContextSignal(person_count=1, secondary_phone_detected=True, screen_or_laptop_detected=False, document_detected=False, visual_confidence=0.85)
                decision = fusion.fuse(
                    tactic_probs=merged_tactics,
                    scam_stage=stage,
                    velocity=velo,
                    semantic_confidence=max(p_scam, jev_res.confidence),
                    visual_signal=vis
                )
                pred_trigger = decision.safety_brake_triggered or decision.risk_score >= 65

            latencies.append((time.perf_counter() - t0) * 1000.0)
            y_pred.append(1 if pred_trigger else 0)

        prec = precision_score(y_true, y_pred, zero_division=0)
        rec = recall_score(y_true, y_pred, zero_division=0)
        f1 = f1_score(y_true, y_pred, zero_division=0)
        tn, fp, fn, tp = confusion_matrix(y_true, y_pred, labels=[0, 1]).ravel()
        fpr = (fp / max(fp + tn, 1)) * 100.0
        fnr = (fn / max(fn + tp, 1)) * 100.0

        ablation_results[cfg] = {
            "precision": round(float(prec), 4),
            "recall": round(float(rec), 4),
            "f1_score": round(float(f1), 4),
            "fpr": round(float(fpr), 2),
            "fnr": round(float(fnr), 2),
            "false_positives": int(fp),
            "false_negatives": int(fn),
            "latency_ms": round(float(np.mean(latencies)), 3)
        }

    report_path = os.path.join(REPORTS_DIR, "ablation_study_results.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(ablation_results, f, indent=2)

    print("\n--- ABLATION BENCHMARK RESULTS ---")
    print(json.dumps(ablation_results, indent=2))
    return ablation_results

if __name__ == "__main__":
    run_ablation_benchmark()
