# RakshaCall ML Phase 1 — Pre-Integration Baseline Report

**Execution Timestamp:** 2026-09-26 00:05 IST  
**Git Checkpoint Commit:** `checkpoint: baseline before Phase 1 neural JEV integration`  
**Target Goal:** Wire the existing PyTorch model at `ml/models/scam_classifier/v2/` into the live JEV inference path while preserving deterministic rule-based safety floor.

---

## 1. Automated Test Suite Baseline

### Backend Pytest Suite
- **Command:** `pytest -o pythonpath=backend backend/tests -v`
- **Result:** PASSED (Exit Code: 0)
- **Total Tests Collected:** 25
- **Passed:** 25 (100%)
- **Failed:** 0
- **Duration:** 12.72s
- **Suites Checked:**
  - `backend/tests/test_advanced_engine.py` (3 tests)
  - `backend/tests/test_auth_and_sessions.py` (3 tests)
  - `backend/tests/test_evidence_vault.py` (3 tests)
  - `backend/tests/test_grpc_streaming.py` (3 tests)
  - `backend/tests/test_multilingual_intelligence.py` (4 tests)
  - `backend/tests/test_multimodal_fusion.py` (3 tests)
  - `backend/tests/test_realtime_connectors.py` (3 tests)
  - `backend/tests/test_stage_and_velocity.py` (3 tests)

### Android Unit Test Suite
- **Command:** `python count_tests.py` (Reading `app/build/test-results/testDebugUnitTest/*.xml`)
- **Total Tests:** 108
- **Passed:** 108 (100%)
- **Failures:** 0

---

## 2. Benchmark Evaluation Baseline

### 22-Scenario Internal Evaluation
- **Command:** `python backend/evaluation/evaluate.py` (with `PYTHONPATH=backend`)
- **Result:** PASSED
- **Total Scenarios Evaluated:** 22 (13 Scam, 9 Benign)
- **Precision:** 100.0%
- **Recall:** 100.0%
- **F1-Score:** 100.0%
- **Mean Pipeline Latency:** 7.07 ms
- **Caveat Noted in Audit:** 
  The 22 scenarios in `dataset.json` closely mirror the regex keywords and prototype phrases in `backend/app/ai/jev_provider.py`. This serves as an internal functional smoke test, not an independent generalization metric.

---

## 3. Current JEV & Risk Behavior Baseline

1. **Active JEV Provider:**
   `LocalSemanticJEVProvider` in `backend/app/ai/jev_provider.py`.
2. **Detection Mechanism:**
   Heuristic string matching: token set intersection, regex pattern matching (`re.search`), and word overlap against `INTENT_PROTOTYPES`.
3. **Neural Model Status:**
   Pretrained weights at `ml/models/scam_classifier/v2/model_weights.pt` (25.7 MB) exist in repository but are NOT yet wired into `backend/app/ai/jev_provider.py` or the live gRPC / REST pipeline.
4. **Current Risk Scoring Behavior:**
   - Heuristic tactic probabilities are assigned if token similarity exceeds 0.30.
   - Stage machine enforces progression dampening.
   - In production database (`rakshacall.db`), logged real sessions show severe attacks scoring `0.586` to `0.68` (MEDIUM) due to conservative velocity and damping factors.
