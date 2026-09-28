# RAKSHA CALL — FINAL HARDENING BASELINE
**Mission**: 48-Hour Technova Grand Finale Stability & Validation Freeze  
**Timestamp**: 2026-09-26T09:20:00+05:30  
**Phase**: Phase 0 — Baseline Project Freeze

---

## 1. System & Environment Specifications

| Component | Value / Version |
| :--- | :--- |
| **Git Commit Hash** | `511e6f72e6c04516ad6b44c0dc28e006e64e70b0` |
| **Operating System** | Windows 11 10.0 amd64 |
| **Python Version** | Python 3.11.0 |
| **JVM Version** | Oracle JDK 23.0.2+7-58 |
| **Gradle Version** | Gradle 9.3.1 (Kotlin DSL 2.2.21, Groovy 4.0.29) |
| **Framework Nature** | Native Android (Kotlin/Jetpack) + FastAPI (Python Async/gRPC) |
| **Flutter Status** | Not Applicable (Codebase is 100% Native Kotlin Android + FastAPI) |

---

## 2. Baseline Test & Service Execution Status

| Subsystem | Command / Target | Result | Evidence / Details |
| :--- | :--- | :--- | :--- |
| **Backend API Unit Tests** | `pytest backend/tests/ -v` | **25 / 25 PASSED** | 0 failed, 1 warning (starlette deprecation), time: 62.5s |
| **ML Engine Tests** | `pytest tests/ -v` | **17 / 17 PASSED** | `test_jev_integration.py` (7/7), `test_scam_classifier.py` (10/10) |
| **Android Unit Tests** | `gradlew.bat testDebugUnitTest` | **108 / 108 PASSED** | 0 failed, 0 skipped, time: 2.88s |
| **Database Startup** | `init_db()` -> SQLite `rakshacall.db` | **OPERATIONAL** | SQLAlchemy tables verified |
| **gRPC Service** | `ProtectionService` via `app.grpc_service` | **OPERATIONAL** | Liveness, bidirectional streams, Tanglish stream verified |
| **Debug APK Status** | `RakshaCall-Debug.apk` | **PRESENT** | File size: 29,757,400 bytes |
| **Release APK Status** | `RakshaCall-Release.apk` | **PRESENT** | File size: 20,838,245 bytes |

---

## 3. Current Machine Learning Model Status

| Asset Path | Type / Framework | Size | Status / Notes |
| :--- | :--- | :--- | :--- |
| `ml/models/scam_classifier/v2/model_weights.pt` | PyTorch Neural Net | 25,761,803 bytes (~25.7 MB) | v2 Checkpoint with tokenizer.json & inference.py |
| `ml/models/scam_classifier/v1/model_weights.pt` | PyTorch Linear/MLP | 663,869 bytes (~663 KB) | v1 legacy weights + vectorizer.pkl |
| `backend/app/ai/jev_provider.py` | Deterministic Rule/Keyword Floor | ~20 KB code | Primary deterministic tactic fallback & baseline |
| `yolo11n.pt` | Ultralytics YOLOv11 nano | 5,613,764 bytes (~5.6 MB) | Visual evidence support model |

---

## 4. Current Evaluation Baseline
- **Original Evaluation (`backend/evaluation/dataset.json`)**: 22 synthetic scenarios used in early smoke tests. Showed 100% precision/recall on its own keyword phrases due to high dictionary overlap.
- **Objective of Hardening**: Evaluate honestly on held-out datasets (`ml/evaluation/hard_negatives.jsonl`, `unseen_scam_conversations.jsonl`) without phrase contamination.

---

## 5. Integrity & Verification Checksum
- Baseline committed and frozen prior to applying fixes or running held-out validation.
- All subsequent changes will follow strict reporting after each phase.
