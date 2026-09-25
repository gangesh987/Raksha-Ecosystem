"""
RakshaCall Automated Evaluation Benchmark Suite.
Executes multi-turn evaluation dataset across Tamil, Tanglish, Hindi, Hinglish, and English.
Measures Precision, Recall, F1, False Positive Rate (FPR), False Negative Rate (FNR), and Latency.
"""

from __future__ import annotations
import json
import time
from pathlib import Path
from typing import Dict, List, Any

from app.ai.jev_provider import LocalSemanticJEVProvider
from app.ai.stage_machine import ScamStageMachine, STAGE_RANKS
from app.ai.manipulation_velocity import ManipulationVelocityEngine
from app.ai.multimodal_fusion import MultimodalRiskFusionEngine
from app.ai.conversation_context import SessionConversationContext


def run_benchmark(dataset_path: str = "backend/evaluation/dataset.json") -> Dict[str, Any]:
    with open(dataset_path, "r", encoding="utf-8") as f:
        dataset = json.load(f)

    jev = LocalSemanticJEVProvider()
    fusion = MultimodalRiskFusionEngine()

    total_scenarios = len(dataset)
    tp = 0  # Scam correctly triggering safety brake / high risk
    fp = 0  # Benign incorrectly triggering safety brake
    tn = 0  # Benign correctly avoiding safety brake
    fn = 0  # Scam failing to trigger safety brake

    latencies: List[float] = []
    results: List[Dict[str, Any]] = []

    for item in dataset:
        sid = item["id"]
        lang = item["language"]
        expected_brake = item["expected_safety_brake"]
        dialogue = item["dialogue"]

        ctx = SessionConversationContext(session_id=sid)
        sm = ScamStageMachine()
        vm = ManipulationVelocityEngine()

        final_decision = None

        for turn_idx, text in enumerate(dialogue, start=1):
            t_start = time.perf_counter()

            ctx.add_turn(speaker="CALLER", text=text, language=lang)
            window = ctx.get_sliding_window(k=5)

            # JEV Semantic Inference
            jev_res = jev.analyze_window(
                current_utterance=text,
                conversation_context=window[:-1] if window else [],
                detected_language=lang
            )
            ctx.record_tactics(jev_res.tactic_probabilities)

            # Stage update
            stage, _ = sm.update_stage(
                tactic_probs=jev_res.tactic_probabilities,
                timestamp=time.time(),
                is_irreversible=jev_res.is_irreversible_action
            )
            stage_rank = STAGE_RANKS.get(stage, 0)

            # Velocity update
            velo = vm.record_event(
                timestamp=time.time(),
                tactic_probs=jev_res.tactic_probabilities,
                current_stage_rank=stage_rank
            )

            # Fusion
            final_decision = fusion.fuse(
                tactic_probs=jev_res.tactic_probabilities,
                scam_stage=stage,
                velocity=velo,
                semantic_confidence=jev_res.confidence
            )

            latency_ms = (time.perf_counter() - t_start) * 1000.0
            latencies.append(latency_ms)

        pred_brake = final_decision.safety_brake_triggered if final_decision else False

        # Confusion Matrix evaluation
        if item["scenario_type"] == "SCAM":
            if pred_brake == expected_brake or (expected_brake is True and final_decision.risk_score >= 60):
                tp += 1
            else:
                fn += 1
        else: # BENIGN
            if pred_brake is False and final_decision.risk_score < 40:
                tn += 1
            else:
                fp += 1

        results.append({
            "id": sid,
            "language": lang,
            "type": item["scenario_type"],
            "expected_brake": expected_brake,
            "predicted_brake": pred_brake,
            "final_risk_score": final_decision.risk_score if final_decision else 0,
            "final_stage": final_decision.scam_stage if final_decision else "CONTACT",
            "tactics": list(ctx.cumulative_tactics.keys())
        })

    precision = tp / (tp + fp) if (tp + fp) > 0 else 1.0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 1.0
    f1 = (2 * precision * recall) / (precision + recall) if (precision + recall) > 0 else 0.0
    fpr = fp / (fp + tn) if (fp + tn) > 0 else 0.0
    fnr = fn / (fn + tp) if (fn + tp) > 0 else 0.0
    mean_latency = sum(latencies) / len(latencies) if latencies else 0.0

    report = {
        "total_scenarios": total_scenarios,
        "true_positives": tp,
        "true_negatives": tn,
        "false_positives": fp,
        "false_negatives": fn,
        "precision": round(precision * 100, 2),
        "recall": round(recall * 100, 2),
        "f1_score": round(f1 * 100, 2),
        "false_positive_rate": round(fpr * 100, 2),
        "false_negative_rate": round(fnr * 100, 2),
        "mean_inference_latency_ms": round(mean_latency, 2),
        "scenario_results": results
    }

    print("\n==================================================")
    print("RAKSHACALL AUTOMATED MULTILINGUAL BENCHMARK REPORT")
    print("==================================================")
    print(f"Total Scenarios Evaluated: {total_scenarios}")
    print(f"True Positives: {tp} | True Negatives: {tn}")
    print(f"False Positives: {fp} | False Negatives: {fn}")
    print(f"Precision: {report['precision']}%")
    print(f"Recall: {report['recall']}%")
    print(f"F1-Score: {report['f1_score']}%")
    print(f"False Positive Rate: {report['false_positive_rate']}%")
    print(f"False Negative Rate: {report['false_negative_rate']}%")
    print(f"Mean Pipeline Latency: {report['mean_inference_latency_ms']} ms")
    print("==================================================\n")

    return report


if __name__ == "__main__":
    report = run_benchmark()
    with open("backend/evaluation/benchmark_results.json", "w", encoding="utf-8") as out:
        json.dump(report, out, indent=2)
