"""
Phase 13: Neural JEV Latency Benchmark.
Measures empirical execution latencies strictly for text NLP inference:
1. Pure PyTorch Neural Text Inference (Preprocessing -> Forward Pass -> Sigmoid Probabilities)
2. Complete Hybrid JEV Inference (Neural Forward Pass + Safety Floor + Fusion + Telemetry)

DOES NOT SIMULATE OR CLAIM ASR LATENCY.
Measures real CPU timing over cold-start and 100 warm iterations.
"""

import os
import sys
import time
import numpy as np

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
backend_path = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, backend_path)

from app.ml.scam_classifier import ScamClassifier, get_scam_classifier
from app.ai.jev_provider import LocalSemanticJEVProvider


def benchmark_neural_jev(iterations: int = 100):
    test_utterance = (
        "This is Inspector Sharma from Cyber Crime Headquarters. "
        "Your Aadhaar card was linked to illegal money laundering. "
        "Transfer 50,000 rupees to the verification account right now."
    )

    print("\n" + "=" * 70, flush=True)
    print("RAKSHACALL NEURAL JEV LATENCY BENCHMARK (TEXT NLP ONLY)", flush=True)
    print("=" * 70, flush=True)

    # -------------------------------------------------------------
    # 1. Cold Start Benchmark
    # -------------------------------------------------------------
    t_cold_start_0 = time.perf_counter()
    fresh_clf = ScamClassifier()
    fresh_clf.load()
    cold_load_ms = (time.perf_counter() - t_cold_start_0) * 1000.0

    t_cold_infer_0 = time.perf_counter()
    cold_pred = fresh_clf.predict(test_utterance)
    cold_infer_ms = (time.perf_counter() - t_cold_infer_0) * 1000.0

    print(f"Cold Start Model Load Time:      {cold_load_ms:6.2f} ms")
    print(f"Cold Start First Inference:      {cold_infer_ms:6.2f} ms")
    print("-" * 70)

    # -------------------------------------------------------------
    # 2. Warm PyTorch Neural Inference (100 Iterations)
    # -------------------------------------------------------------
    clf = get_scam_classifier()
    neural_latencies = []

    for _ in range(iterations):
        t0 = time.perf_counter()
        _ = clf.predict(test_utterance)
        neural_latencies.append((time.perf_counter() - t0) * 1000.0)

    n_arr = np.array(neural_latencies)
    n_mean = float(np.mean(n_arr))
    n_p50 = float(np.percentile(n_arr, 50))
    n_p95 = float(np.percentile(n_arr, 95))
    n_p99 = float(np.percentile(n_arr, 99))

    print(f"Standalone Neural Model Latency ({iterations} warm iterations):")
    print(f"  Mean:  {n_mean:6.2f} ms")
    print(f"  P50:   {n_p50:6.2f} ms")
    print(f"  P95:   {n_p95:6.2f} ms")
    print(f"  P99:   {n_p99:6.2f} ms")
    print("-" * 70)

    # -------------------------------------------------------------
    # 3. Complete Hybrid JEV Inference (Neural + Safety Floor + Fusion)
    # -------------------------------------------------------------
    jev = LocalSemanticJEVProvider(classifier=clf)
    hybrid_latencies = []

    for _ in range(iterations):
        t0 = time.perf_counter()
        _ = jev.analyze_window(
            current_utterance=test_utterance,
            conversation_context=["Initial call setup"],
            detected_language="en"
        )
        hybrid_latencies.append((time.perf_counter() - t0) * 1000.0)

    h_arr = np.array(hybrid_latencies)
    h_mean = float(np.mean(h_arr))
    h_p50 = float(np.percentile(h_arr, 50))
    h_p95 = float(np.percentile(h_arr, 95))
    h_p99 = float(np.percentile(h_arr, 99))

    print(f"Complete Hybrid JEV (Neural + Safety Floor + Fusion, {iterations} warm iterations):")
    print(f"  Mean:  {h_mean:6.2f} ms")
    print(f"  P50:   {h_p50:6.2f} ms")
    print(f"  P95:   {h_p95:6.2f} ms")
    print(f"  P99:   {h_p99:6.2f} ms")
    print("=" * 70)
    print("NOTE: These measurements represent text-to-intent inference on CPU.")
    print("      They do not include microphone capture, audio framing, or ASR transcription.\n")

    return {
        "cold_start": {"load_ms": round(cold_load_ms, 2), "infer_ms": round(cold_infer_ms, 2)},
        "neural_only": {"mean_ms": round(n_mean, 2), "p50_ms": round(n_p50, 2), "p95_ms": round(n_p95, 2), "p99_ms": round(n_p99, 2)},
        "hybrid_jev": {"mean_ms": round(h_mean, 2), "p50_ms": round(h_p50, 2), "p95_ms": round(h_p95, 2), "p99_ms": round(h_p99, 2)}
    }


if __name__ == "__main__":
    benchmark_neural_jev()
